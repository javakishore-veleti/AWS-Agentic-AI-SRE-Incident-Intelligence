"""The component workflows are manual and do not run from a pull request."""

from pathlib import Path

import yaml

WORKFLOWS = Path(__file__).resolve().parents[3] / ".github" / "workflows"


def test_component_workflows_are_manual():
    names = sorted(path.name for path in WORKFLOWS.glob("AWS-00*.yml"))
    assert len(names) == 16
    for name in names:
        text = (WORKFLOWS / name).read_text()
        body = yaml.safe_load(text)
        assert set(body["on"]) <= {"workflow_dispatch", "workflow_call"}
        assert "pull_request" not in text
        assert "AWS_SECRET_ACCESS_KEY" not in text
        assert "actions/checkout@v4" in text
        assert body["on"]["workflow_dispatch"]["inputs"]["dry_run"]["default"] is True
        if "Destroy" in name:
            assert "DESTROY-$ENV" in text
            assert "Refusing to delete" in text
    network = (WORKFLOWS / "AWS-001-Destroy-Network.yml").read_text().lower()
    assert "delete-vpc" not in network
    for setup in WORKFLOWS.glob("AWS-00*-Setup-*.yml"):
        for line in setup.read_text().splitlines():
            if "--template-file" in line:
                relative = line.split("--template-file", 1)[1].strip()
                assert (setup.parents[2] / relative).is_file()
