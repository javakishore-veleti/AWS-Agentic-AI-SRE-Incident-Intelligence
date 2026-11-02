# CICD-001 Required Checks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make a failed or missing CI check block a merge into `develop` or `main`.

**Architecture:** Keep `.github/workflows/ci.yaml`. Add one GitHub ruleset that requires the four check names that workflow already publishes. Record those names in `ARCHITECTURE.md`.

**Tech Stack:** GitHub Actions, GitHub rulesets.

---

### Task 1: Ruleset

Create an active branch ruleset named `Require CI checks` for `refs/heads/develop` and `refs/heads/main`. Required contexts:

- `Middleware`
- `Investigation agent`
- `Portals (agentic-ai-admin)`
- `Portals (ecommerce-portal)`

Do not require the branch to be updated again before merge. Do not add a bypass actor.

Confirm with `gh api repos/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/rulesets/<id>` that enforcement is `active` and those four contexts are present.

### Task 2: Docs

Update `ARCHITECTURE.md` with the four check names and the rule that a failed or missing check blocks the merge. Update the CI sentences in `CURRENT_STATE_AND_GAPS.md` so they no longer say there is no workflow.
