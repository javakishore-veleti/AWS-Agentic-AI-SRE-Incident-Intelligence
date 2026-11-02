def competing_hypotheses(evidence: list[dict]) -> list[str]:
    successful = [row for row in evidence if not row.get("error") and row.get("items")]
    if not successful:
        return ["inconclusive. No telemetry rows were retrieved for this window."]
    by_source: dict[str, dict] = {}
    for row in successful:
        by_source.setdefault(row["source"], row)
    deployment = by_source.get("deployments")
    others = [by_source[name] for name in ("prometheus", "elasticsearch", "jaeger") if name in by_source]
    if deployment and others:
        hypotheses = [
            f"candidate cause. A deployment change is cited as {deployment['evidenceId']}.",
        ]
        for row in others:
            hypotheses.append(
                f"symptom. The {row['source']} row cited as {row['evidenceId']} is not by itself a cause."
            )
        return hypotheses
    cited = ", ".join(row["evidenceId"] for row in successful)
    return [f"symptom. Retrieved rows are cited as {cited}. A cause is not assigned from these rows alone."]
