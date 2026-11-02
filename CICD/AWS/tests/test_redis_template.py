"""Private Redis. No AWS account is called."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "005-redis.yaml"


def test_cfn_lint_redis():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_redis_is_encrypted_in_the_data_subnets():
    body = load_template(TEMPLATE)
    redis = body["Resources"]["Redis"]["Properties"]
    assert redis["TransitEncryptionEnabled"] is True
    assert redis["AtRestEncryptionEnabled"] is True
    assert redis["SecurityGroupIds"][0]["Ref"] == "DataSecurityGroupId"
    subnets = body["Resources"]["CacheSubnetGroup"]["Properties"]["SubnetIds"]
    assert [item["Ref"] for item in subnets] == ["DataSubnet1", "DataSubnet2"]
    assert "0.0.0.0/0" not in TEMPLATE.read_text()
