# APP-001 local startup and shutdown

Approved in chat on 31 October 2026 for GitHub issue #1.

## Goal

Prove the existing local start and stop path. Record every failed command and its output, fix that failure in this issue, and rerun until health checks pass and shutdown leaves nothing behind.

## Path

Use the scripts already wired in the root `package.json`. Do not add a new runner.

1. `npm run local:containers:all:start` then `npm run local:containers:all:status`
2. `npm run local:middleware:all:start` then `npm run local:middleware:all:status`
3. `npm run local:portals:all:start` then `npm run local:portals:all:status`
4. `npm run local:portals:all:stop`
5. `npm run local:middleware:all:stop`
6. `npm run local:containers:all:stop`
7. Run the three status commands again.

Run these from `.worktrees/issue-1-app-001`.

## Pass

After start, `middleware-all-status.sh` prints `up` for ports 8081, 8082, 8083, 8084, 8085, and 8090. `portals-all-status.sh` prints `up` for ports 5173 and 5174.

After stop, those same status lines print `down`. `docker-all-status.sh` prints `Network sre-incident-net: absent`. No listener remains on 8081–8085, 8090, 5173, or 5174.

## Failure

Post the command and its full output as a comment on GitHub issue #1 before editing anything. Keep `SUPERPOWERS_GitIssues_Progress.md` to the slash-command table. Change only the script, config, or runbook line that the output shows is wrong. Rerun that command. The closing evidence is the clean rerun, also as an issue comment.
