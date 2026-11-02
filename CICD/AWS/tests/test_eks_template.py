"""EKS in the private app subnets. No AWS account is called."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "006-eks.yaml"


def test_cfn_lint_eks():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_nodes_use_the_private_app_subnets():
    body = load_template(TEMPLATE)
    assert "PublicSubnet1" not in body["Parameters"]
    node = body["Resources"]["NodeGroup"]["Properties"]
    assert [item["Ref"] for item in node["Subnets"]] == ["AppSubnet1", "AppSubnet2"]
    vpc = body["Resources"]["Cluster"]["Properties"]["ResourcesVpcConfig"]
    assert [item["Ref"] for item in vpc["SubnetIds"]] == ["AppSubnet1", "AppSubnet2"]
    assert vpc["EndpointPrivateAccess"] is True
