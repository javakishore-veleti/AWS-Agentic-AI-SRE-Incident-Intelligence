"""Private PostgreSQL. No AWS account is called."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "004-rds.yaml"


def test_cfn_lint_rds():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_database_is_private_and_snapshotted():
    body = load_template(TEMPLATE)
    database = body["Resources"]["Database"]
    assert database["Type"] == "AWS::RDS::DBInstance"
    assert database["DeletionPolicy"] == "Snapshot"
    assert database["Properties"]["Engine"] == "postgres"
    assert database["Properties"]["PubliclyAccessible"] is False
    assert database["Properties"]["StorageEncrypted"] is True
    text = TEMPLATE.read_text()
    assert "0.0.0.0/0" not in text
    assert "MasterUserPassword:" in text
    assert "resolve:secretsmanager" in text
