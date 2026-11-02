"""Portal and agent manifests do not create a second load balancer."""

from pathlib import Path

import yaml

MANIFEST = Path(__file__).resolve().parents[1] / "kubernetes" / "portals-and-agent.yaml"


def test_portals_and_agent_use_node_ports():
    text = MANIFEST.read_text()
    assert "kind: Ingress" not in text
    docs = [doc for doc in yaml.safe_load_all(text) if doc]
    ports = [
        doc["spec"]["ports"][0]["nodePort"]
        for doc in docs
        if doc["kind"] == "Service"
    ]
    assert ports == [30073, 30074, 30090]
