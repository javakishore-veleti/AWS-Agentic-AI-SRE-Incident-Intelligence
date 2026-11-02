"""Deploy-all and undeploy order. These workflows are not dispatched."""

from pathlib import Path

import yaml

WORKFLOWS = Path(__file__).resolve().parents[3] / ".github" / "workflows"


def test_deploy_all_order():
    body = yaml.safe_load((WORKFLOWS / "AWS-910-Deploy-All.yml").read_text())
    order = ["backend", "portals", "investigation", "smoke"]
    assert list(body["jobs"]) == order
    for earlier, later in zip(order, order[1:]):
        assert body["jobs"][later]["needs"] == earlier
    assert "pull_request" not in (WORKFLOWS / "AWS-910-Deploy-All.yml").read_text()


def test_undeploy_is_manual_and_does_not_delete_a_vpc():
    text = (WORKFLOWS / "AWS-911-Undeploy-All.yml").read_text()
    assert "kubectl delete namespace heddle" in text
    assert "delete-vpc" not in text.lower()
    assert "Dry run only" in text
    assert "pull_request" not in text
    smoke = (WORKFLOWS / "AWS-103-Run-Smoke-Tests.yml").read_text()
    assert "No request was sent" in smoke
    assert smoke.index("Dry run only") < smoke.index("curl")
