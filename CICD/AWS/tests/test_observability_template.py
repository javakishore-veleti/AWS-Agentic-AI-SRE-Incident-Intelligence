"""Telemetry mode is explicit. No AWS account is called."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "007-observability.yaml"


def test_cfn_lint_observability():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_self_hosted_is_the_default_and_xray_is_absent():
    body = load_template(TEMPLATE)
    assert body["Parameters"]["TelemetryMode"]["Default"] == "self-hosted"
    log_group = body["Resources"]["ApplicationLogGroup"]
    assert log_group["Condition"] == "UseCloudWatch"
    text = TEMPLATE.read_text().lower()
    assert "xray" not in text
    assert "does not record ingested telemetry" in text
