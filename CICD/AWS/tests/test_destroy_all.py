"""Destroy-all runs in reverse and stops unless undeploy was confirmed."""

from pathlib import Path

import yaml

WORKFLOW = Path(__file__).resolve().parents[3] / ".github" / "workflows" / "AWS-901-Destroy-All.yml"
ORDER = [
    "confirm",
    "ingress",
    "observability",
    "eks",
    "redis",
    "rds",
    "ecr",
    "security",
    "network",
]


def test_destroy_all_order_and_confirmation():
    text = WORKFLOW.read_text()
    body = yaml.safe_load(text)
    jobs = body["jobs"]
    assert list(jobs) == ORDER
    for earlier, later in zip(ORDER, ORDER[1:]):
        assert jobs[later]["needs"] == earlier
    assert "AWS-911-Undeploy-All" in text
    assert "DESTROY-$ENV" in text
    assert body["on"]["workflow_dispatch"]["inputs"]["dry_run"]["default"] is True
    assert body["on"]["workflow_dispatch"]["inputs"]["workloads_removed"]["default"] is False
    assert "delete-vpc" not in text.lower()
    assert jobs["network"]["uses"].endswith("AWS-001-Destroy-Network.yml")
    assert jobs["ingress"]["uses"].endswith("AWS-008-Destroy-Ingress.yml")
