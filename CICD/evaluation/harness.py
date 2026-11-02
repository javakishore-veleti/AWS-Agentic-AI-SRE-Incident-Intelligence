"""Score saved reports. Call a model only when an endpoint is configured."""

import json
import os
from pathlib import Path

FIXTURES = Path(__file__).resolve().parent / "fixtures"


def run_evaluation(model_url: str | None = None) -> dict:
    endpoint = model_url if model_url is not None else os.environ.get("EVAL_MODEL_URL", "")
    trials = []
    for path in sorted(FIXTURES.glob("*.json")):
        report = json.loads(path.read_text())
        evidence = report.get("evidence") or []
        trials.append(
            {
                "fixture": path.name,
                "evidenceCount": len(evidence),
                "hypothesis": report.get("hypothesis") or "inconclusive",
                "modelCalled": False,
            }
        )
    configured = bool(endpoint)
    return {
        "modelCalled": False,
        "modelId": None,
        "endpointConfigured": configured,
        "trials": trials,
        "note": (
            "No model endpoint was configured. No accuracy, cost, or restoration time was measured."
            if not configured
            else "An endpoint is configured. This run did not call it, so no accuracy, cost, or restoration time was measured."
        ),
    }


def main() -> None:
    print(json.dumps(run_evaluation(), indent=2))


if __name__ == "__main__":
    main()
