"""The evaluation harness does not call a model when no endpoint is set."""

import os

from harness import run_evaluation


def test_no_model_is_called_without_an_endpoint(monkeypatch):
    monkeypatch.delenv("EVAL_MODEL_URL", raising=False)
    result = run_evaluation()
    assert result["modelCalled"] is False
    assert result["modelId"] is None
    assert result["trials"]
    assert "accuracy" not in result
    assert os.environ.get("EVAL_MODEL_URL") in (None, "")
