#!/usr/bin/env bash
set -euo pipefail

REPO="javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence"
command -v gh >/dev/null || { echo 'Install GitHub CLI (gh) first.' >&2; exit 1; }
gh auth status >/dev/null

# Native macOS Bash 3.2 compatible: avoid associative arrays.
keys=(M1 M2 M3 M4 M5 M6 M7 M8)
titles=(
  "M1 - Local Application Baseline"
  "M2 - Security & Reliability"
  "M3 - Observability"
  "M4 - Incident Engineering"
  "M5 - Agentic AI Investigation"
  "M6 - AWS Infrastructure Foundation"
  "M7 - AWS Application Deployment"
  "M8 - Model Evaluation & Release"
)
descriptions=(
  "All five Spring Boot services, both React portals, local infrastructure, and automated CI validated."
  "Secure access controls, secrets, checkout idempotency, and order lifecycle reliability."
  "Real Prometheus metrics, structured ELK logs, and Jaeger distributed traces verified."
  "Repeatable incident scenarios with controlled injection, telemetry evidence, and recovery."
  "Evidence-based LangGraph agent contracts, telemetry tools, reasoning, guardrails, and reporting."
  "CloudFormation AWS foundation and manually dispatched setup/destroy orchestration."
  "ECR images, EKS workloads, portals, agent, smoke tests, and deployment lifecycle workflows."
  "Reproducible multi-model incident evaluations and full AWS lifecycle validation."
)
issue_groups=(
  "1 2 3 4"
  "5 6 7 8 9"
  "10 11 12"
  "13 14 15"
  "16 17 18 19 37 38 39"
  "22 23 24 25 26 27 28 29 30 31 32"
  "33 34 35"
  "20 36"
)

# Create only if absent. This script is safe to re-run.
for i in "${!keys[@]}"; do
  title="${titles[$i]}"
  number="$(gh api --paginate "repos/$REPO/milestones?state=all&per_page=100" --jq ".[] | select(.title == \"$title\") | .number" | head -n 1)"
  if [[ -z "$number" ]]; then
    number="$(gh api -X POST "repos/$REPO/milestones" -f title="$title" -f description="${descriptions[$i]}" --jq '.number')"
    echo "Created milestone $title (#$number)"
  else
    echo "Reusing milestone $title (#$number)"
  fi
  for issue in ${issue_groups[$i]}; do
    gh api -X PATCH "repos/$REPO/issues/$issue" -F milestone="$number" --silent
    echo "  Assigned issue #$issue"
  done
done

echo
echo 'Verification: open issues grouped by milestone'
gh api --paginate "repos/$REPO/issues?state=open&per_page=100" --jq '.[] | select(.pull_request == null) | "#\(.number) | \(.milestone.title // "NO MILESTONE") | \(.title)"' | sort -V
