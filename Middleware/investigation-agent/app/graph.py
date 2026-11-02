import json
import operator
import os
import re
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from typing import Annotated, Optional, TypedDict

from langgraph.graph import END, StateGraph

from app.guardrails import allow_tool, note_call, rejected_text, safe_recommendation, scrub
from app.tools import SOURCES, query_evidence

SERVICES = {
    "product-catalog-service": "http://localhost:8081/actuator/health",
    "offers-service": "http://localhost:8082/actuator/health",
    "customers-service": "http://localhost:8083/actuator/health",
    "orders-service": "http://localhost:8084/actuator/health",
    "inventory-service": "http://localhost:8085/actuator/health",
}
_CHANGE = re.compile(r"(?i)\b(kubectl|shell|sudo|rm|delete|drop)\b")


class InvestigationState(TypedDict):
    service: str
    start: str
    end: str
    signals: dict
    facts: Annotated[list[str], operator.add]
    hypotheses: Annotated[list[str], operator.add]
    recommendations: Annotated[list[str], operator.add]
    evidence: Annotated[list[dict], operator.add]
    uncertainty: Annotated[list[str], operator.add]
    report_hypotheses: list[str]
    model_called: bool
    model_id: Optional[str]


def _probe(url: str) -> dict:
    try:
        with urllib.request.urlopen(url, timeout=2) as response:
            body = json.loads(response.read().decode("utf-8"))
            return {"status": body.get("status", "UNKNOWN"), "reachable": True}
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
        return {"status": "UNREACHABLE", "reachable": False, "detail": str(exc)}


def _trace_of(item) -> str | None:
    if not isinstance(item, dict):
        return None
    for key in ("traceId", "traceID", "trace_id"):
        value = item.get(key)
        if isinstance(value, str) and value:
            return value
    source = item.get("_source")
    if isinstance(source, dict):
        return _trace_of(source)
    return None


def _successful(evidence: list[dict]) -> list[dict]:
    return [row for row in evidence if not row.get("error") and row.get("items")]


def _deterministic_text(state: InvestigationState) -> str:
    successful = _successful(state["evidence"])
    if not successful:
        return "inconclusive. No telemetry rows were retrieved for this window."
    cited = ", ".join(row["evidenceId"] for row in successful)
    return f"Retrieved rows are cited as {cited}. A root cause is not assigned from these rows alone."


def _configured_model(state: InvestigationState, opener=None):
    url = os.environ.get("INVESTIGATION_MODEL_URL", "").strip()
    if not url:
        return False, None, _deterministic_text(state), None
    successful = _successful(state["evidence"])
    payload = json.dumps(
        {
            "service": state["service"],
            "evidenceIds": [row["evidenceId"] for row in successful],
            "errors": [row["error"] for row in state["evidence"] if row.get("error")],
        }
    ).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=payload,
        method="POST",
        headers={"Content-Type": "application/json"},
    )
    fetch = opener or urllib.request.urlopen
    try:
        with fetch(request, timeout=3) as response:
            body = json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError):
        return False, None, _deterministic_text(state), "The configured model call failed."
    hypothesis = body.get("hypothesis") if isinstance(body, dict) else None
    model_id = body.get("modelId") if isinstance(body, dict) else None
    if not isinstance(hypothesis, str) or not hypothesis.strip():
        return False, None, _deterministic_text(state), "The model response did not include a hypothesis."
    if _CHANGE.search(hypothesis):
        return False, None, _deterministic_text(state), "The model response asked for a change and was not used."
    if not isinstance(model_id, str) or not model_id.strip():
        return True, None, hypothesis.strip(), "The model response did not include a model id."
    return True, model_id.strip(), hypothesis.strip(), None


def build_graph(probe=None, evidence_query=None, model=None):
    read_health = probe or _probe
    read_evidence = evidence_query or query_evidence
    read_model = model or _configured_model

    def triage(state: InvestigationState) -> dict:
        requested = state.get("service") or "orders-service"
        subject = requested if requested in SERVICES else "orders-service"
        facts = [f"Investigation opened for {subject}. Health checks and telemetry reads are read-only."]
        if requested not in SERVICES:
            facts.append(f"{requested} is not one of the five commerce services. The subject is {subject}.")
        start = state.get("start") or ""
        end = state.get("end") or ""
        if not start or not end:
            now = datetime.now(timezone.utc)
            end = now.isoformat()
            start = (now - timedelta(minutes=15)).isoformat()
            facts.append("No window was supplied. The read window is the last 15 minutes.")
        return {"service": subject, "start": start, "end": end, "facts": facts}

    def collect(state: InvestigationState) -> dict:
        calls = 0
        signals = {}
        for name, url in SERVICES.items():
            calls = note_call(calls)
            allow_tool("health")
            signals[name] = read_health(url)
        facts = [
            f"{name} health endpoint reported {signal['status']}."
            for name, signal in signals.items()
        ]
        subject = state["service"]
        if signals.get(subject, {}).get("status") == "UP":
            facts.append(f"{subject} health is UP. That status does not show whether the purchase path succeeded.")
        evidence = []
        uncertainty = []
        for source in SOURCES:
            calls = note_call(calls)
            allow_tool(source)
            row = dict(read_evidence(source, subject, state["start"], state["end"], limit=5))
            row["items"] = [scrub(item) for item in row.get("items") or []]
            evidence.append(row)
            if row.get("error"):
                uncertainty.append(f"{source} for {subject}: {row['error']}")
        return {"signals": signals, "facts": facts, "evidence": evidence, "uncertainty": uncertainty}

    def correlate(state: InvestigationState) -> dict:
        traces: dict[str, list[str]] = {}
        for row in state["evidence"]:
            if row.get("error"):
                continue
            for item in row.get("items") or []:
                trace = _trace_of(item)
                if trace:
                    traces.setdefault(trace, []).append(row["evidenceId"])
        shared = {trace: ids for trace, ids in traces.items() if len(set(ids)) > 1}
        if not shared:
            return {
                "facts": ["No shared trace id was present in the retrieved rows."],
                "uncertainty": ["Correlation did not find a trace id in more than one source."],
            }
        facts = [
            f"Trace {trace} appears in {', '.join(sorted(set(ids)))}."
            for trace, ids in sorted(shared.items())
        ]
        return {"facts": facts}

    def hypothesize(state: InvestigationState) -> dict:
        called, model_id, text, note = read_model(state)
        uncertainty = [] if called else ["No model was called."]
        if note:
            uncertainty.append(note)
        return {
            "hypotheses": [text],
            "uncertainty": uncertainty,
            "model_called": called,
            "model_id": model_id,
        }

    def verify(state: InvestigationState) -> dict:
        supported = {row["evidenceId"] for row in _successful(state["evidence"])}
        checked = []
        uncertainty = []
        for hypothesis in state["hypotheses"]:
            if rejected_text(hypothesis):
                checked.append("unsupported. The hypothesis repeated untrusted text or asked for a change.")
                uncertainty.append("A hypothesis was rejected because it repeated untrusted text or asked for a change.")
                continue
            if hypothesis.startswith("inconclusive"):
                checked.append(hypothesis)
                uncertainty.append("The report is inconclusive because retrieved telemetry did not support a cause.")
                continue
            if any(evidence_id in hypothesis for evidence_id in supported):
                checked.append(hypothesis)
                continue
            checked.append(f"unsupported. {hypothesis}")
            uncertainty.append("A hypothesis did not cite retrieved evidence.")
        return {"report_hypotheses": checked, "uncertainty": uncertainty}

    def recommend(state: InvestigationState) -> dict:
        checked = state.get("report_hypotheses") or []
        cited = [row["evidenceId"] for row in _successful(state["evidence"])]
        blocked = any(item.startswith("inconclusive") or item.startswith("unsupported") for item in checked)
        if not cited or blocked:
            text = "No change is recommended. Retrieved evidence does not support a cause. A person approves any later change."
        else:
            text = (
                "Review the cited evidence before changing configuration. "
                f"Cited: {', '.join(cited)}. A person approves any rollback or configuration change. This report does not apply one."
            )
        return {"recommendations": [safe_recommendation(text, cited if not blocked else [])]}

    graph = StateGraph(InvestigationState)
    graph.add_node("triage", triage)
    graph.add_node("collect", collect)
    graph.add_node("correlate", correlate)
    graph.add_node("hypothesize", hypothesize)
    graph.add_node("verify", verify)
    graph.add_node("recommend", recommend)
    graph.set_entry_point("triage")
    graph.add_edge("triage", "collect")
    graph.add_edge("collect", "correlate")
    graph.add_edge("correlate", "hypothesize")
    graph.add_edge("hypothesize", "verify")
    graph.add_edge("verify", "recommend")
    graph.add_edge("recommend", END)
    return graph.compile()
