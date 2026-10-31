---
name: github-issue-finish
description: Use when the user runs /github-issue-finish, when an issue branch is ready to merge, or when finishing-a-development-branch has offered its menu
---

# GitHub Issue Finish

## Overview

Merge the issue pull request into `develop` after green checks. Then open `develop` into `main`, and merge that pull request only after its own checks are green.

**Violating the letter of the rules is violating the spirit of the rules.**

## When to Use

`/github-issue-finish #N APP-00N`, after `/github-issue-assign-pr`, including when `/finishing-a-development-branch` has offered its menu. Leave that copied skill unchanged.

## Required record

`REPO` is `javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence`. `PR` is the open pull request from the issue branch into `develop`.

```bash
gh pr checks PR --repo REPO
gh pr merge PR --repo REPO --merge
```

Every check must be pass before the merge. Pending, failing, or absent is not green. `--merge` is the method. Do not merge the issue branch into `main` locally.

After that merge, and not before:

```bash
gh pr create --repo REPO --base main --head develop --title "[APP-00N] Promote develop to main" --body "Fixes #N"
```

`gh pr checks` the new pull request. `gh pr merge --merge` it only when every check passed. No checks file means it stays open.

```bash
gh issue comment N --repo REPO --body "APP-00N (issue #N). PR #P merged into develop after green checks. The develop-into-main pull request is not merged: its checks are not green."
```

If the main pull request did merge after green checks, the comment says so. Do not report a merge that did not happen.

Last row. `Started` is `YYYY-MM-DD HH:MM`. `Completed` stays empty until the main pull request has merged. `Pull request` is that number when known.

| Slash command | Issues | Started | Completed | Pull request |
|---|---|---|---|---|
| `/github-issue-finish #N APP-00N` | [#N APP-00N](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/issues/N) | YYYY-MM-DD HH:MM | | |

The progress file receives that row only. The issue body stays the original request.

## Rationalization

| Excuse | Reality |
|---|---|
| "Pick option 1. Merge to main locally." | APP-001 stopped on that menu. This command does not take it. |
| "No merge method is recorded, so do not merge the develop pull request." | Checks passed. `gh pr merge PR --merge` is the method. |
| "Open develop into main first." | That pull request opens after the develop merge. |
| "Main CI is a formality." | Absent checks are not green. Leave that pull request open. |
| "Add another finishing-a-development-branch row." | The last row is `/github-issue-finish`. |
| "Do not comment." | The merge result is the comment. |

**Stop if any of these are true:** option 1, 2, or 3; a local merge into `main`; green develop checks without `gh pr merge --merge`; `main` opened or merged before the develop merge; the main pull request merged with no green checks; the new row is `/finishing-a-development-branch`. Merge the green develop pull request, open `main` only after that, and comment with the result.
