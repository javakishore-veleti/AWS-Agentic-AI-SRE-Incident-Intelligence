"""The templates and workflows are in order. No account is called."""

from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[3]
WORKFLOWS = ROOT / ".github" / "workflows"
TEMPLATES = ROOT / "CICD" / "AWS" / "CloudFormation"


def test_templates_and_workflow_order():
    names = [path.name for path in sorted(TEMPLATES.glob("00*.yaml"))]
    assert names == [
        "001-network.yaml",
        "002-security.yaml",
        "003-ecr.yaml",
        "004-rds.yaml",
        "005-redis.yaml",
        "006-eks.yaml",
        "007-observability.yaml",
        "008-ingress.yaml",
    ]
    setup = yaml.safe_load((WORKFLOWS / "AWS-900-Setup-All.yml").read_text())
    assert list(setup["jobs"]) == [
        "network",
        "security",
        "ecr",
        "rds",
        "redis",
        "eks",
        "observability",
        "ingress",
    ]
    destroy = yaml.safe_load((WORKFLOWS / "AWS-901-Destroy-All.yml").read_text())
    assert list(destroy["jobs"])[:3] == ["confirm", "undeploy", "ingress"]
    assert list(destroy["jobs"])[-1] == "network"
    deploy = yaml.safe_load((WORKFLOWS / "AWS-910-Deploy-All.yml").read_text())
    assert list(deploy["jobs"]) == ["backend", "portals", "investigation", "smoke"]
    undeploy = (WORKFLOWS / "AWS-911-Undeploy-All.yml").read_text().lower()
    assert "delete-vpc" not in undeploy
    network = (WORKFLOWS / "AWS-001-Destroy-Network.yml").read_text().lower()
    assert "delete-vpc" not in network


def test_evaluation_recorded_that_no_model_was_called():
    import importlib.util

    harness = ROOT / "CICD" / "evaluation" / "harness.py"
    spec = importlib.util.spec_from_file_location("heddle_eval_harness", harness)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = module.run_evaluation("")
    assert result["modelCalled"] is False
    assert "accuracy" not in result
