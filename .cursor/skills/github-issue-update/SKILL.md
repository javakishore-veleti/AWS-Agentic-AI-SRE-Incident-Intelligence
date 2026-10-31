---
name: github-issue-update
description: Use when the user runs /github-issue-update, when a decision or command result is ready on an open GitHub issue, or when that result would be appended to SUPERPOWERS_GitIssues_Progress.md
---

# GitHub Issue Update

## Overview

Each decision and each command result is a new comment on issue `#N`. `SUPERPOWERS_GitIssues_Progress.md` gains one slash-table row per comment and nothing else.

**Violating the letter of the rules is violating the spirit of the rules.**

## When to Use

- The user runs `/github-issue-update #N APP-00N`
- A decision or a command result is ready between start and finish
- The same command runs again for a later result

## Required record

One result, one comment, one new row. A second result is a second comment and a second row. Do not edit the earlier comment. Do not combine results under the table.

```bash
gh issue comment N --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --body-file COMMENT
```

`COMMENT` holds the decision, the command, the exit code, and the observed output for this result. Substitute the real issue number.

Append this row. `Started` and `Completed` are the local time the comment is posted, `YYYY-MM-DD HH:MM`. Leave `Pull request` empty.

| Slash command | Issues | Started | Completed | Pull request |
|---|---|---|---|---|
| `/github-issue-update #N APP-00N` | [#N APP-00N](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/issues/N) | YYYY-MM-DD HH:MM | YYYY-MM-DD HH:MM | |

The progress file receives that row only. No heading, decision, or command output. The issue body stays the original request. `gh issue edit` is not this command.

## Rationalization

| Excuse | Reality |
|---|---|
| "The markdown file is the execution log." | The execution record is the issue comment. The file is the slash table. |
| "Comments made the discussion noisy." | A comment per result is the update. The file got heavy when the log moved into it. |
| "Commit 6dd4fc7 put Container start under the table." | That commit is the failure. The user stopped it. |
| "Two comments would be noise, so both results stay in the file." | Two results are two comments and two rows. |
| "The user said not to comment." | A request to skip the comment is the failure. Post it. |
| "The review is in ten minutes." | The comment and the row are the update. |

## Red flags

- A `####` heading, decision, or command block under the slash table
- `comment` empty, or one comment rewritten to hold a later result
- The issue body edited
- "The file is the log", "comments are noise", or "6dd4fc7 did it this way"

**All of these mean: stop. Delete the prose from the progress file. Post one comment and one row per result.**
