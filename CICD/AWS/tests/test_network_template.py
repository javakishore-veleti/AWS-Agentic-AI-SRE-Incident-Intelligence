"""Static checks for the network template. No AWS account is called."""

import ipaddress
import subprocess
from itertools import combinations

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "001-network.yaml"


def test_cfn_lint_network():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_existing_vpc_is_not_a_stack_resource():
    body = load_template(TEMPLATE)
    assert body["Parameters"]["VpcMode"]["Default"] == "existing"
    vpc = body["Resources"]["Vpc"]
    assert vpc["Type"] == "AWS::EC2::VPC"
    assert vpc["Condition"] == "CreateVpc"
    assert vpc["DeletionPolicy"] == "Delete"
    owned = [
        name
        for name, resource in body["Resources"].items()
        if resource.get("Type") == "AWS::EC2::VPC"
    ]
    assert owned == ["Vpc"]


def test_data_subnets_have_no_internet_route():
    body = load_template(TEMPLATE)
    routes = [
        resource
        for resource in body["Resources"].values()
        if resource.get("Type") == "AWS::EC2::Route"
    ]
    data_routes = [
        route
        for route in routes
        if route["Properties"].get("RouteTableId", {}).get("Ref") == "DataRouteTable"
    ]
    assert data_routes == []
    internet = [
        route
        for route in routes
        if route["Properties"].get("DestinationCidrBlock") == "0.0.0.0/0"
        and "GatewayId" in route["Properties"]
    ]
    assert len(internet) == 1
    assert internet[0]["Properties"]["RouteTableId"]["Ref"] == "PublicRouteTable"


def test_default_cidrs_do_not_overlap():
    body = load_template(TEMPLATE)
    names = [
        "VpcCidr",
        "PublicSubnet1Cidr",
        "PublicSubnet2Cidr",
        "AppSubnet1Cidr",
        "AppSubnet2Cidr",
        "DataSubnet1Cidr",
        "DataSubnet2Cidr",
    ]
    networks = {
        name: ipaddress.ip_network(body["Parameters"][name]["Default"])
        for name in names
    }
    vpc = networks["VpcCidr"]
    assert str(vpc) == "10.40.0.0/16"
    subnets = [networks[name] for name in names if name != "VpcCidr"]
    for subnet in subnets:
        assert subnet.subnet_of(vpc)
    for left, right in combinations(subnets, 2):
        assert not left.overlaps(right)


def test_outputs_cover_both_modes():
    outputs = load_template(TEMPLATE)["Outputs"]
    for name in (
        "VpcId",
        "StackOwnsVpc",
        "PublicSubnetIds",
        "PrivateAppSubnetIds",
        "PrivateDataSubnetIds",
    ):
        assert name in outputs
    assert outputs["StackOwnsVpc"]["Value"]["If"][1] == "true"
    assert outputs["StackOwnsVpc"]["Value"]["If"][2] == "false"
