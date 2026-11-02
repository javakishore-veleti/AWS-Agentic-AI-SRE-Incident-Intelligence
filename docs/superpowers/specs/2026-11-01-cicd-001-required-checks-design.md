# CICD-001 required pull request checks

Approved for GitHub issue #4. The user asked to run the full CICD-001 command sequence without a further design stop.

## Goal

A pull request to `develop` or `main` shows build and test status. A failed or missing check blocks the merge.

## What already runs

`.github/workflows/ci.yaml` runs on pull requests and pushes to `develop` and `main`.

| Check | What it runs |
|---|---|
| Middleware | Java 21, `mvn -B test` in `Middleware` |
| Investigation agent | Install the agent requirements and import `app.main` |
| Portals (agentic-ai-admin) | `npm ci` and `npm run build` |
| Portals (ecommerce-portal) | `npm ci` and `npm run build` |

## Change

`develop` and `main` have no branch protection and no ruleset, so a person can merge while a check is failing. Add one active ruleset on those two branches. It requires the four check names above. A passing check is enough. The workflow file stays as it is.

`ARCHITECTURE.md` records the four names.
