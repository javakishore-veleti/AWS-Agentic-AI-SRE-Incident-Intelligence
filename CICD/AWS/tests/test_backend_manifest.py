"""Backend manifests stay read-only and do not create a load balancer."""

from pathlib import Path

import yaml

MANIFEST = Path(__file__).resolve().parents[1] / "kubernetes" / "backend.yaml"
WORKFLOW = Path(__file__).resolve().parents[3] / ".github" / "workflows" / "AWS-100-Deploy-Backend.yml"


def test_backend_manifest_has_no_ingress_or_secret_value():
    text = MANIFEST.read_text()
    assert "kind: Ingress" not in text
    assert "Heddle#1" not in text
    assert "sre_local_password" not in text
    docs = [doc for doc in yaml.safe_load_all(text) if doc]
    services = [doc for doc in docs if doc["kind"] == "Service"]
    assert [doc["spec"]["ports"][0]["nodePort"] for doc in services] == [
        30081,
        30082,
        30083,
        30084,
        30085,
    ]
    assert all(doc["spec"]["type"] == "NodePort" for doc in services)


def test_backend_workflow_is_manual_and_dry_run_stops_first():
    text = WORKFLOW.read_text()
    body = yaml.safe_load(text)
    assert set(body["on"]) <= {"workflow_dispatch", "workflow_call"}
    assert "pull_request" not in text
    assert body["on"]["workflow_dispatch"]["inputs"]["dry_run"]["default"] is True
    dry = text.index("Dry run only")
    secret = text.index("get-secret-value")
    assert dry < secret
