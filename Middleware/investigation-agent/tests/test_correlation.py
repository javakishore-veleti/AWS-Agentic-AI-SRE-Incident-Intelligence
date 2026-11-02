from app.correlation import competing_hypotheses


def _row(source: str, items: list) -> dict:
    return {
        "evidenceId": f"ev-{source}-abc12345",
        "source": source,
        "items": items,
        "error": None,
    }


def test_a_deployment_is_a_candidate_and_the_metric_is_a_symptom():
    hypotheses = competing_hypotheses(
        [
            _row("deployments", [{"revision": "2"}]),
            _row("prometheus", [{"value": "4"}]),
            _row("jaeger", [], ),
        ]
    )
    assert hypotheses[0] == "candidate cause. A deployment change is cited as ev-deployments-abc12345."
    assert hypotheses[1] == "symptom. The prometheus row cited as ev-prometheus-abc12345 is not by itself a cause."
    assert all("cause" in item or "symptom" in item or "candidate" in item for item in hypotheses)


def test_metrics_alone_do_not_become_a_cause():
    hypotheses = competing_hypotheses([_row("prometheus", [{"value": "4"}])])
    assert hypotheses == [
        "symptom. Retrieved rows are cited as ev-prometheus-abc12345. A cause is not assigned from these rows alone."
    ]


def test_no_rows_stay_inconclusive():
    hypotheses = competing_hypotheses(
        [{"evidenceId": "ev-prometheus-abc12345", "source": "prometheus", "items": [], "error": "not configured"}]
    )
    assert hypotheses[0].startswith("inconclusive.")
