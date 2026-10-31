---
name: github-issues-pr-develop-to-main
description: Use when a pull request from develop into main is open, when the merge workflow did not merge it, or when the user runs /github-issues-pr-develop-to-main
---

# Develop to main

## Overview

Merge the open pull request from `develop` into `main` when every check has passed. The workflows "Merge pull request after CI passes" and "Merge develop into main" do this without an agent. This command is the same merge when that pull request is still open.

**Violating the letter of the rules is violating the spirit of the rules.**

## When to Use

- The user runs `/github-issues-pr-develop-to-main`
- A pull request from `develop` into `main` is open and its checks passed
- The CI workflow created that pull request and no later run merged it

## Required record

`REPO` is `javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence`.

```bash
gh pr list --repo REPO --base main --head develop --state open --json number,url
gh pr checks PR --repo REPO
gh pr merge PR --repo REPO --merge
```

`PR` is that open pull request. Every check must pass before the merge. A pending, failing, or missing check is not a pass. `--merge` is the method. Do not merge `develop` into `main` locally. If no pull request is open, stop. If a check has not passed, leave the pull request open.

Confirm `gh pr view PR --json state,mergedAt` shows `MERGED`.

## Rationalization

| Excuse | Reality |
|---|---|
| "CI on develop already passed, so merge while this pull request is still running." | This command waits until this pull request's own checks pass. |
| "The workflow will merge it later." | The workflow that opened this pull request does not start again. Merge it here. |
| "Push develop onto main." | The merge is `gh pr merge --merge`. |

## Red flags

- A local merge of `develop` into `main`
- A merge while a check is pending or failing
- A report that the pull request merged when `state` is still `OPEN`
