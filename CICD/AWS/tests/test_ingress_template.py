"""The load balancer is owned by CloudFormation. No AWS account is called."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "008-ingress.yaml"


def test_cfn_lint_ingress():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_one_load_balancer_in_the_public_subnets():
    body = load_template(TEMPLATE)
    balancers = [
        resource
        for resource in body["Resources"].values()
        if resource["Type"] == "AWS::ElasticLoadBalancingV2::LoadBalancer"
    ]
    assert len(balancers) == 1
    properties = balancers[0]["Properties"]
    assert properties["Scheme"] == "internet-facing"
    assert [item["Ref"] for item in properties["Subnets"]] == [
        "PublicSubnet1",
        "PublicSubnet2",
    ]
    assert body["Outputs"]["IngressOwner"]["Value"] == "cloudformation"
    text = TEMPLATE.read_text()
    assert "AWS::ElasticLoadBalancingV2::LoadBalancer" in text
    assert "kind: Ingress" not in text
