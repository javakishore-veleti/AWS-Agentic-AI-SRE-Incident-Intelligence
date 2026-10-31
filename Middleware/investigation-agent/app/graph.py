import json
import operator
import urllib.error
import urllib.request
from typing import Annotated, TypedDict

from langgraph.graph import END, StateGraph

SERVICES = {
    "product-catalog-service": "http://localhost:8081/actuator/health",
    "offers-service": "http://localhost:8082/actuator/health",
    "customers-service": "http://localhost:8083/actuator/health",
    "orders-service": "http://localhost:8084/actuator/health",
    "inventory-service": "http://localhost:8085/actuator/health",
}


class InvestigationState(TypedDict):
    service: str
    signals: dict
    facts: Annotated[list[str], operator.add]
    hypotheses: Annotated[list[str], operator.add]
    recommendations: Annotated[list[str], operator.add]


def _probe(url: str) -> dict:
    try:
        with urllib.request.urlopen(url, timeout=2) as response:
            body = json.loads(response.read().decode("utf-8"))
            return {"status": body.get("status", "UNKNOWN"), "reachable": True}
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
        return {"status": "UNREACHABLE", "reachable": False, "detail": str(exc)}


def triage(state: InvestigationState) -> dict:
    subject = state["service"]
    if subject not in SERVICES:
        subject = "orders-service"
    return {
        "service": subject,
        "facts": [f"Investigation opened for {subject}. Health checks are read-only."],
    }


def signals(state: InvestigationState) -> dict:
    collected = {name: _probe(url) for name, url in SERVICES.items()}
    facts = [
        f"{name} health endpoint reported {signal['status']}."
        for name, signal in collected.items()
    ]
    return {"signals": collected, "facts": facts}


def assess(state: InvestigationState) -> dict:
    subject = state["service"]
    signal = state["signals"].get(subject, {})
    hypotheses: list[str] = []
    if not signal.get("reachable"):
        hypotheses.append(f"{subject} may be stopped, misconfigured, or blocked from this workspace.")
    elif signal.get("status") != "UP":
        hypotheses.append(f"{subject} is reachable, but its health status is {signal.get('status')}.")
    else:
        hypotheses.append("No competing failure hypothesis is supported by these health signals.")
    return {"hypotheses": hypotheses}


def recommend(state: InvestigationState) -> dict:
    subject = state["service"]
    signal = state["signals"].get(subject, {})
    if signal.get("status") == "UP":
        text = "No production change is recommended. These signals do not establish an incident."
    else:
        text = (
            f"Confirm whether {subject} is running and whether its dependency configuration is current. "
            "Do not apply a production change from this report alone."
        )
    return {"recommendations": [text]}


def build_graph():
    graph = StateGraph(InvestigationState)
    graph.add_node("triage", triage)
    graph.add_node("collect_signals", signals)
    graph.add_node("assess", assess)
    graph.add_node("recommend", recommend)
    graph.set_entry_point("triage")
    graph.add_edge("triage", "collect_signals")
    graph.add_edge("collect_signals", "assess")
    graph.add_edge("assess", "recommend")
    graph.add_edge("recommend", END)
    return graph.compile()
