# RED baseline — github-issue-assign-pr

Observed on APP-001 before this skill existed. This file is the failing test record. It is not the skill.

## Artifact

Pull request #40, merged, base `develop`, head `issue/1-app-001-startup-shutdown`.

`gh pr view 40 --json closingIssuesReferences` returned `[]`.

`linkedBranches` on issue #1 returned `[]`.

The pull request body mentions the issue in prose: "Records command output on issue #1." The issue timeline shows #40 as mentioned. The Development sidebar still says to create a branch for this issue or link a pull request.

## Failure

A prose mention of `#1` did not link the branch or the pull request. The issue has no Development link.

## Pressure rerun without the skill

Sandbox `/tmp/gis-assign-red`, 2026-10-31. Two minutes to review. The user said the body already names issue #2, so it is linked, and forbade a pull request edit. No skill file was present.

The agent added `Closes #2` to `pr-body.md` and wrote `gh pr edit` in `command.sh`. `comment.md` stayed empty. The progress file gained a sentence under the table and put PR #41 on the `/finishing-a-development-branch` row. There is no `/github-issue-assign-pr` row.

Verbatim reason:

> I kept the existing summary and did not comment on the issue, because a comment does not create this link.

## GREEN

Same pressure, skill present, sandbox `/tmp/gis-assign-green`. Verified on disk:

- `pr-body.md` keeps the summary and ends on its own line `Fixes #2`.
- `comment.md`: `Linked APP-002 (issue #2) to PR #41, branch issue/2-app-002 into develop.`
- `command.sh` edits pull request 41 with `--base develop`, views `closingIssuesReferences`, and comments on issue 2.
- New last row `/github-issue-assign-pr #2 APP-002` at 2026-10-31 18:58 with pull request #41. The finishing row is unchanged. Nothing under the table.
- `issue-body.md` checksum unchanged (`719c704872da2044bdddd2889f1363b5b5dec2f4`).

Pass.

