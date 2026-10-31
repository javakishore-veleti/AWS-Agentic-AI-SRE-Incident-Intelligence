---
name: github-issue-assign-pr
description: Use when the user runs /github-issue-assign-pr, when a GitHub issue's Development list has no linked branch or pull request, or when a pull request only mentions the issue number in prose
---

# GitHub Issue Assign PR

## Overview

The Development link is a pull request into `develop` whose body has its own line `Fixes #N`, plus a comment on issue `#N`. The progress file gains one slash-table row.

**Violating the letter of the rules is violating the spirit of the rules.**

## When to Use

- The user runs `/github-issue-assign-pr #N APP-00N`
- After `/finishing-a-development-branch`, or anytime the Development list is empty
- Before `/github-issue-finish`

This command does not merge and does not wait on CI.

## Required record

Keep the existing pull request text. Add this as its own final line if it is absent:

```text
Fixes #N
```

A sentence that contains `#N` is not that line. `Closes` and `Resolves` are not that line.

```bash
gh pr edit PR --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --base develop --body-file PR_BODY
gh pr view PR --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --json closingIssuesReferences,baseRefName,headRefName
```

If no open pull request exists, create one with `--base develop`, `--head` the issue branch, and the same final line. Substitute the real issue number, pull request number, and branch. The view must show base `develop`, the issue branch, and issue `#N` in `closingIssuesReferences`.

Then comment:

```bash
gh issue comment N --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --body "Linked APP-00N (issue #N) to PR #P, branch BRANCH into develop."
```

Append one row. Do not rewrite earlier rows. `Started` and `Completed` are the local time of the comment, `YYYY-MM-DD HH:MM`.

| Slash command | Issues | Started | Completed | Pull request |
|---|---|---|---|---|
| `/github-issue-assign-pr #N APP-00N` | [#N APP-00N](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/issues/N) | YYYY-MM-DD HH:MM | YYYY-MM-DD HH:MM | [#P](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/pull/P) |

The progress file receives that row only. The issue body stays the original request.

## Rationalization

| Excuse | Reality |
|---|---|
| "The body already says issue #N." | Pull request #40 mentioned issue #1. `closingIssuesReferences` was `[]`. |
| "The sidebar catches up on its own." | Issue #1's linked branch list was empty after that mention. |
| "A comment does not create this link." | The link is the `Fixes #N` line. The comment is still required. |
| "Record the PR on the finishing row." | That row stays. This command adds its own row. |
| "Put a note under the table." | The file gets the row and nothing else. |
| "Two minutes left, skip the write." | The line, the comment, and the row are this command. |

## Red flags

- The pull request body has no line `Fixes #N`
- No issue comment
- A sentence under the slash table, or the pull request written onto another row
- The issue body edited, or a merge command run

**All of these mean: stop. Add `Fixes #N`, post the comment, and append the assign-pr row.**
