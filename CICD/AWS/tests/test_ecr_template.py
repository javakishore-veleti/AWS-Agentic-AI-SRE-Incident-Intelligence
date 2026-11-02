"""Registries for the eight Heddle images. No AWS account is called."""

import subprocess

from cfnlib import load_template, template_dir

TEMPLATE = template_dir() / "003-ecr.yaml"
NAMES = (
    "catalog",
    "offers",
    "customers",
    "orders",
    "inventory",
    "desk",
    "shop",
    "investigation-agent",
)


def test_cfn_lint_ecr():
    result = subprocess.run(
        ["cfn-lint", "-t", str(TEMPLATE)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


def test_eight_repositories_are_retained_and_scanned():
    repos = [
        resource
        for resource in load_template(TEMPLATE)["Resources"].values()
        if resource["Type"] == "AWS::ECR::Repository"
    ]
    assert len(repos) == 8
    names = []
    for repo in repos:
        assert repo["DeletionPolicy"] == "Retain"
        assert repo["Properties"]["ImageScanningConfiguration"]["ScanOnPush"] is True
        names.append(repo["Properties"]["RepositoryName"]["Sub"])
    for suffix in NAMES:
        assert any(suffix in name for name in names)
