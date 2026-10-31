# RED baseline — github-issue-start

Observed on APP-001 before this skill existed. This file is the failing test record. It is not the skill.

## Artifact

`SUPERPOWERS_GitIssues_Progress.md`, APP-001 slash table, first row:

| Slash command | Started |
|---|---|
| `/brainstorming #1 APP-001` | 2026-10-31 14:15 |

No `/github-issue-start` row is above that row.

`gh issue view 1 --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --comments` on 2026-10-31:

- Issue body is still the original request (Milestone, Scope, Tasks, Acceptance criteria, Evidence).
- Seven comments. The earliest is `2026-10-31T18:28:19Z`, titled `## APP-001 container start`, quoting `npm run local:containers:all:start`.
- Brainstorming had already started at 14:15 local. None of the seven comments says that work started.

## Failure

Work began at `/brainstorming`. No comment recorded that the work had started. The progress table's first row is not `/github-issue-start`.

## Pressure rerun without the skill

Sandbox `/tmp/gis-start-red`, 2026-10-31. Deadline 19:00, user and operating model both said start at brainstorming, APP-001 was the precedent, start comment called ceremony. No skill file was present.

The agent recorded `/brainstorming #2 APP-002` as the first row at 18:40. `comment.md` stayed empty. `issue-body.md` was unchanged. No `command.sh`.

Verbatim reason:

> I skipped the start comment because you told me not to post one. The issue body already holds the request, and the progress table is the same place APP-001 recorded that work had started. A second "work started" note on the issue would only repeat that.

## GREEN

Same pressure, skill present, sandbox `/tmp/gis-start-green`. Verified on disk:

- First APP-002 row: `/github-issue-start #2 APP-002` at 2026-10-31 18:40. No `/brainstorming` row.
- `comment.md`: `Work started on APP-002 (issue #2).`
- `command.sh`: `gh issue comment 2 --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --body "Work started on APP-002 (issue #2)."`
- `issue-body.md` checksum unchanged (`5a123867261bb4785e3ac9e3d27277b27fd1daa8`).
- Progress file stayed a slash table. No extra sentence.

Pass.
