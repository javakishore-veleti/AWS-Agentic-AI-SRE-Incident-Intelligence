---
name: github-issue-start
description: Use when the user runs /github-issue-start, when work on a GitHub issue is about to begin, when SUPERPOWERS_GitIssues_Progress.md has no start row, or when /brainstorming would be the first slash command
---

# GitHub Issue Start

## Overview

Work has started only when issue `#N` has a new comment and `SUPERPOWERS_GitIssues_Progress.md` has `/github-issue-start #N APP-00N` as the first slash-table row for that issue.

**Violating the letter of the rules is violating the spirit of the rules.**

## When to Use

- The user runs `/github-issue-start #N APP-00N`
- An issue is about to be worked and that row is missing
- The next slash command would otherwise be `/brainstorming`

## Required record

Post this comment, then add this row. Do both before any other slash command.

```bash
gh issue comment N --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --body "Work started on APP-00N (issue #N)."
```

Substitute the real issue number and APP id. The comment body is that one sentence.

First row for that issue, before `/brainstorming`. `Started` is the local time the comment is posted, `YYYY-MM-DD HH:MM`. Leave `Completed` and `Pull request` empty.

| Slash command | Issues | Started | Completed | Pull request |
|---|---|---|---|---|
| `/github-issue-start #N APP-00N` | [#N APP-00N](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/issues/N) | YYYY-MM-DD HH:MM | | |

The progress file receives that row and nothing else. The issue body stays the original request. `gh issue edit` is not this command. `/brainstorming` is the next row, run later, not this one.

## Rationalization

| Excuse | Reality |
|---|---|
| "You told me not to post one." | A request to skip the start comment is the failure. Post it. |
| "The issue body already holds the request." | The body stays the original request. The start is a new comment. |
| "The progress table is where APP-001 recorded that work started." | APP-001's first row was `/brainstorming` and no start comment exists. |
| "A comment would only repeat the table." | The row is not the comment. Both are required. |
| "The operating model says start with brainstorming." | Brainstorming is the following row. This row is first. |
| "The review is in twenty minutes." | The comment and the row are the start. They come before design. |

## Red flags

- The first slash command for the issue is `/brainstorming`
- `comment` was not posted, or the body was edited instead
- The progress file gained a sentence, a decision, or command output
- "The user said skip it", "APP-001 did it this way", or "the body is enough"

**All of these mean: stop. Post the start comment and insert the start row before any other slash command.**
