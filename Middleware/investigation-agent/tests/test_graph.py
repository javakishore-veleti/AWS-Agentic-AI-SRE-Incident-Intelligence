import json
import os
import urllib.error

from app.graph import _configured_model, build_graph

START = "2026-11-02T05:00:00+00:00"
END = "2026-11-02T05:15:00+00:00"


def _up(_url: str) -> dict:
    return {"status": "UP", "reachable": True}


def _row(source: str, items: list, error: str | None = None) -> dict:
    return {
        "evidenceId": f"ev-{source}-abc12345",
        "source": source,
        "service": "orders-service",
        "query": source,
        "retrievedAt": START,
        "items": items,
        "error": error,
    }


def _mixed(source, service, start, end, limit=5, **_kwargs):
    assert service == "orders-service"
    assert start == START
    assert end == END
    if source == "prometheus":
        return _row(source, [{"metric": {"service": service}, "value": [1, "4"]}])
    return _row(source, [], f"{source} is not configured")


def _traced(source, service, start, end, limit=5, **_kwargs):
    if source == "prometheus":
        return _row(source, [{"traceId": "0123456789abcdef", "value": "1"}])
    if source == "jaeger":
        return _row(source, [{"traceID": "0123456789abcdef"}])
    return _row(source, [], f"{source} is not configured")


def _empty(source, service, start, end, limit=5, **_kwargs):
    return _row(source, [], f"{source} is not configured")


def _run(query, service="orders-service", model=None):
    saved = os.environ.pop("INVESTIGATION_MODEL_URL", None)
    graph = build_graph(probe=_up, evidence_query=query, model=model)
    try:
        return graph.invoke(
        {
            "service": service,
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
    )
    finally:
        if saved is not None:
            os.environ["INVESTIGATION_MODEL_URL"] = saved


def test_report_cites_retrieved_evidence_and_says_no_model_was_called():
    result = _run(_mixed)
    assert result["report_hypotheses"] == [
        "symptom. Retrieved rows are cited as ev-prometheus-abc12345. A cause is not assigned from these rows alone."
    ]
    assert result["model_called"] is False
    assert result["model_id"] is None
    assert "No model was called." in result["uncertainty"]
    assert "ev-prometheus-abc12345" in result["recommendations"][0]
    assert "kubectl" not in result["recommendations"][0]


def test_missing_telemetry_is_inconclusive():
    result = _run(_empty)
    assert result["report_hypotheses"][0].startswith("inconclusive.")
    assert all(row["items"] == [] for row in result["evidence"])
    assert any("not configured" in item for item in result["uncertainty"])
    assert result["recommendations"][0].startswith("No change is recommended.")


def test_unknown_service_uses_orders_and_says_so():
    result = _run(_empty, service="payments")
    assert result["service"] == "orders-service"
    assert any("payments" in fact for fact in result["facts"])


def test_health_up_does_not_hide_a_retrieved_row():
    result = _run(_mixed)
    assert any("health is UP" in fact for fact in result["facts"])
    assert "ev-prometheus-abc12345" in result["report_hypotheses"][0]
    assert "No competing failure" not in " ".join(result["report_hypotheses"])


def test_shared_trace_is_named():
    result = _run(_traced)
    assert any("0123456789abcdef" in fact for fact in result["facts"])
    assert "ev-prometheus-abc12345" in result["report_hypotheses"][0]
    assert "ev-jaeger-abc12345" in result["report_hypotheses"][0]


def test_hypothesis_without_an_evidence_id_is_unsupported():
    def model(_state):
        return True, "local-test", "The database pool is exhausted.", None

    result = _run(_mixed, model=model)
    assert result["report_hypotheses"] == ["unsupported. The database pool is exhausted."]
    assert result["model_called"] is True
    assert result["model_id"] == "local-test"
    assert result["recommendations"][0].startswith("No change is recommended.")


def test_failed_model_call_keeps_the_deterministic_report(monkeypatch):
    state = _run(_mixed)
    monkeypatch.setenv("INVESTIGATION_MODEL_URL", "http://127.0.0.1:9/model")

    def boom(*_args, **_kwargs):
        raise urllib.error.URLError("refused")

    called, model_id, text, note = _configured_model(state, opener=boom)
    assert called is False
    assert model_id is None
    assert text.startswith("Retrieved rows are cited as ev-prometheus-abc12345")
    assert note == "The configured model call failed."


def test_model_answer_records_its_id(monkeypatch):
    monkeypatch.setenv("INVESTIGATION_MODEL_URL", "http://127.0.0.1:9/model")
    state = {
        "service": "orders-service",
        "evidence": [_row("prometheus", [{"value": "1"}])],
    }

    class _Body:
        def read(self):
            return json.dumps(
                {
                    "modelId": "local-test",
                    "hypothesis": "Retrieved rows are cited as ev-prometheus-abc12345.",
                }
            ).encode("utf-8")

        def __enter__(self):
            return self

        def __exit__(self, *_args):
            return False

    called, model_id, text, note = _configured_model(state, opener=lambda *_args, **_kwargs: _Body())
    assert called is True
    assert model_id == "local-test"
    assert "ev-prometheus-abc12345" in text
    assert note is None
