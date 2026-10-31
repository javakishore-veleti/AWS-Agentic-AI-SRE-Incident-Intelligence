# RED baseline — github-issue-update

Observed on APP-001 before this skill existed. This file is the failing test record. It is not the skill.

## Artifact

Commit `6dd4fc7` (`Record the APP-001 container start status`) wrote command output under the slash table in `SUPERPOWERS_GitIssues_Progress.md`:

```markdown
#### Container start

`npm run local:containers:all:start` exited 0 at 2026-10-31 14:25. `npm run local:containers:all:status` exited 0.
```

A fenced status block for Postgres, Redis, Prometheus, ELK, and Jaeger followed that heading. The file was no longer only the slash table.

## What stopped it

User, 2026-10-31 14:27 local, verbatim:

> maybe put these in Githb issue notes or comments not in the .md file otherwise it becomes too heavy

The same container block was pasted in that message. Later APP-001 results were comments on issue #1. The progress file went back to the slash table. Decisions and command output had already been written into the markdown file during the run. This command is the repeated place for those updates, between start and complete.

## Pressure rerun without the skill

Sandbox `/tmp/gis-update-red`, 2026-10-31. Ten minutes to review. The user forbade a GitHub comment, called the markdown file the execution log, and pointed at commit `6dd4fc7`. Two results were in hand. No skill file was present.

`comment-1.md` and `comment-2.md` stayed empty. No `command-1.sh` or `command-2.sh`. `issue-body.md` was unchanged. Both results were appended under the slash table as `#### Middleware status` and `#### Portal status`.

Verbatim reason:

> You said the markdown file is the execution log and that GitHub comments made the discussion noisy. APP-001 commit `6dd4fc7` stored the container start the same way: a heading under the slash table, then the command, the exit code, the time, and the observed status. Two issue comments would have been the noise you ruled out, so both results stayed in the progress file.

## GREEN

Same pressure, skill present, sandbox `/tmp/gis-update-green`. Verified on disk:

- `comment-1.md` holds the middleware decision, command, exit 0, and observed output.
- `comment-2.md` holds the portal command, exit 0, and observed output.
- `command-1.sh` and `command-2.sh` are `gh issue comment 2 --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --body-file` for those files.
- Two `/github-issue-update #2 APP-002` rows at 2026-10-31 18:50. Nothing under the table.
- `issue-body.md` checksum unchanged (`719c704872da2044bdddd2889f1363b5b5dec2f4`).

Pass.
