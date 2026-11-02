"""Static checks for IAM, security groups, and the generated secret."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "002-security.yaml"


def test_cfn_lint_security():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_secret_is_generated_and_not_printed():
    text = TEMPLATE.read_text()
    assert "GenerateSecretString" in text
    assert "GetSecretValue" not in text
    assert "MasterUserPassword" not in text
    body = load_template(TEMPLATE)
    secret = body["Resources"]["DbSecret"]
    assert secret["DeletionPolicy"] == "Retain"
    assert secret["Properties"]["GenerateSecretString"]["GenerateStringKey"] == "password"
    assert "SecretString" not in str(body["Outputs"])


def test_database_ports_are_not_public():
    body = load_template(TEMPLATE)
    data = body["Resources"]["DataSg"]["Properties"]
    assert data["SecurityGroupEgress"] == []
    assert "SecurityGroupIngress" not in data
    for name, resource in body["Resources"].items():
        if resource.get("Type") != "AWS::EC2::SecurityGroupIngress":
            continue
        if resource["Properties"].get("GroupId", {}).get("Ref") != "DataSg":
            continue
        assert "CidrIp" not in resource["Properties"]
        assert resource["Properties"]["SourceSecurityGroupId"]["Ref"] == "AppSg"


def test_key_rotation_is_on():
    key = load_template(TEMPLATE)["Resources"]["Key"]
    assert key["Properties"]["EnableKeyRotation"] is True
    assert key["DeletionPolicy"] == "Retain"
