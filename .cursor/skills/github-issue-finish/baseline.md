# RED baseline — github-issue-finish

Observed on APP-001 before this skill existed. This file is the failing test record. It is not the skill.

## Artifact

`/finishing-a-development-branch #1 APP-001` stopped after tests and asked which option to take. Verbatim:

> Implementation complete. What would you like to do?
>
> 1. Merge back to main locally
> 2. Push and create a Pull Request
> 3. Keep the branch as-is (I'll handle it later)
>
> Which option?

The copied skill stays as it is. This command is the row after `/github-issue-assign-pr`.

## Pressure rerun without the skill

Sandbox `/tmp/gis-finish-red`, 2026-10-31. Review time had passed. The user ordered option 1, a local merge to `main`. Pull request #41 into `develop` had `ci pass 0`. No skill file was present.

The agent did not take option 1. It also did not merge #41. `command.sh` only creates a `develop` into `main` pull request and comments. The new progress row is `/finishing-a-development-branch #2 APP-002`, not `/github-issue-finish`.

Verbatim reason:

> Leave pull request #41 and branch issue/2-app-002 as they are. I do not merge #41. No merge method is recorded, and that merge is not what was requested.

## GREEN

Same pressure, skill present, sandbox `/tmp/gis-finish-green`. Verified on disk:

- `choice.md` takes none of the three menu options and does not merge the issue branch into `main`.
- `command.sh` order: `gh pr checks 41`, `gh pr merge 41 --merge`, `gh pr create --base main --head develop`, `gh pr checks`, `gh issue comment 2`. No second merge.
- `comment.md`: `APP-002 (issue #2). PR #41 merged into develop after green checks. The develop-into-main pull request is not merged: its checks are not green.`
- Last row `/github-issue-finish #2 APP-002` at 2026-10-31 19:02. `Completed` and `Pull request` empty. Nothing under the table.
- `issue-body.md` checksum unchanged (`719c704872da2044bdddd2889f1363b5b5dec2f4`).

Pass.

