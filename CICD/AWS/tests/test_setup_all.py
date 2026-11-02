"""Setup-all calls the component workflows in order. It is not executed."""

from pathlib import Path

import yaml

WORKFLOW = Path(__file__).resolve().parents[3] / ".github" / "workflows" / "AWS-900-Setup-All.yml"
ORDER = ["network", "security", "ecr", "rds", "redis", "eks", "observability", "ingress"]


def test_setup_all_order():
    body = yaml.safe_load(WORKFLOW.read_text())
    jobs = body["jobs"]
    assert list(jobs) == ORDER
    assert "needs" not in jobs["network"]
    for earlier, later in zip(ORDER, ORDER[1:]):
        assert jobs[later]["needs"] == earlier
    assert jobs["network"]["with"]["vpc_mode"] == "${{ inputs.vpc_mode }}"
    assert "pull_request" not in WORKFLOW.read_text()
    assert body["on"]["workflow_dispatch"]["inputs"]["dry_run"]["default"] is True
