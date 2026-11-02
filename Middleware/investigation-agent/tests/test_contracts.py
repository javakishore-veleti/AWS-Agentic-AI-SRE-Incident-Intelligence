import pytest

from app.contracts import ReportContract
from app.graph import build_graph

START = "2026-11-02T05:00:00+00:00"
END = "2026-11-02T05:15:00+00:00"


def _row(source: str, items: list, error: str | None) -> dict:
    return {
        "evidenceId": f"ev-{source}-abc12345",
        "source": source,
        "service": "orders-service",
        "query": source,
        "retrievedAt": START,
        "items": items,
        "error": error,
    }


def test_report_contract_accepts_a_cited_row_and_rejects_a_bad_id():
    report = ReportContract.model_validate(
        {
            "service": "orders-service",
            "signals": {},
            "facts": ["opened"],
            "hypotheses": ["inconclusive. No telemetry rows were retrieved for this window."],
            "recommendations": ["No change is recommended."],
            "evidence": [_row("prometheus", [], "prometheus is not configured")],
            "uncertainty": ["prometheus for orders-service: prometheus is not configured"],
            "modelCalled": False,
            "modelId": None,
        }
    )
    assert report.checked().evidence[0].evidenceId == "ev-prometheus-abc12345"
    report.evidence[0].evidenceId = "not-an-id"
    with pytest.raises(ValueError):
        report.checked()


def test_a_failed_read_without_uncertainty_is_rejected():
    report = ReportContract.model_validate(
        {
            "service": "orders-service",
            "signals": {},
            "facts": [],
            "hypotheses": [],
            "recommendations": [],
            "evidence": [_row("jaeger", [], "jaeger is not configured")],
            "uncertainty": [],
            "modelCalled": False,
            "modelId": None,
        }
    )
    with pytest.raises(ValueError, match="uncertainty"):
        report.checked()


def test_a_failed_read_is_retried_once_and_then_kept():
    calls = {"prometheus": 0}

    def query(source, service, start, end, limit=5, **_kwargs):
        if source != "prometheus":
            return _row(source, [], f"{source} is not configured")
        calls["prometheus"] += 1
        if calls["prometheus"] == 1:
            return _row(source, [], "temporary failure")
        return _row(source, [{"value": "1"}], None)

    result = build_graph(probe=lambda _url: {"status": "UP", "reachable": True}, evidence_query=query).invoke(_blank())
    assert calls["prometheus"] == 2
    prometheus = next(row for row in result["evidence"] if row["source"] == "prometheus")
    assert prometheus["error"] is None
    assert "ev-prometheus-abc12345" in result["report_hypotheses"][0]


def _blank() -> dict:
    return {
        "service": "orders-service",
        "start": START,
        "end": END,
        "signals": {},
        "facts": [],
        "hypotheses": [],
        "recommendations": [],
        "evidence": [],
        "uncertainty": [],
        "report_hypotheses": [],
        "model_called": False,
        "model_id": None,
    }
