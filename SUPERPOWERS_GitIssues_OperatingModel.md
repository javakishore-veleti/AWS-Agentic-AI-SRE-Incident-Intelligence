# Superpowers + GitHub Issues — Cursor Operating Model

**Repository:** `javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence`  
**Purpose:** Execute GitHub issues and milestone outcomes using Superpowers as the engineering methodology and Cursor as the only coding environment for now. Claude Code is out of this phase.  
**Status:** Operating procedure; no issue, deployment, test, or milestone is considered complete solely because an AI agent says so.

## 1. Operating principles

1. **GitHub issue is the unit of scope.** Each change maps to one issue or a documented, dependency-aware batch; every PR references the issues it addresses.
2. **Milestone is the unit of delivery.** Milestone closure requires working software, evidence, and a documented gate—not just closed issue counts.
3. **Superpowers is the method, not a model.** Use its brainstorming, planning, worktrees, TDD, systematic debugging, subagent execution, code review, and verification skills in the coding environment where work happens.
4. **Cursor is the control center and the implementer.** Review issues, inspect code and diffs, invoke Cursor Agent, monitor tests, review PRs, and record evidence. Do not hand implementation to Claude Code during this phase.
5. **Human approves architecture, external side effects, and merges.** Never auto-deploy, destroy, change production secrets, or execute remediation merely because an issue says to.
6. **Evidence before status.** Capture commands, tests, logs, PR links, CloudFormation stack events, and model evaluation artifacts; distinguish observed from simulated evidence.
7. **Small batches, clean branches.** Prefer one issue per PR. Batch 2–3 tightly related issues only when tests and rollback boundaries remain clear.

## 2. One-time Cursor and repository setup

### 2.1 Prerequisites

- Open the repository in Cursor and verify the default branch is up to date.
- Install/authenticate `git`, GitHub CLI (`gh`), Java 21, Node/npm, Python, Docker and relevant project tools.
- Install Superpowers in Cursor Agent. The documented path is `/add-plugin superpowers` or the Cursor plugin marketplace. Verify the installed version and that its skills are discoverable.
  - **31 Oct 2026.** `/add-plugin` is a Cursor chat command and was not available as a tool in this session. The marketplace plugin is still not installed, so its session-start hook does not run. The 14 Superpowers 6.3.0 skill directories were copied from the local Claude plugin cache (`~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills`) into `.cursor/skills/` so Cursor can discover them as project skills. The MIT license is `.cursor/skills/SUPERPOWERS-LICENSE.txt`. Each copied directory contains `SKILL.md`. A new agent session is required before this session's skill list includes them.
- Put project-specific `SKILL.md` entrypoints in a Cursor-discoverable directory, ordinarily `.cursor/skills/<name>/SKILL.md`. If retaining `.github/cursor/skills/`, explicitly link or copy them into `.cursor/skills/`; do not assume Cursor discovers an arbitrary directory automatically.
  - **31 Oct 2026.** The nine skills under `.github/cursor/skills/` were kept in place and linked with relative symlinks: `.cursor/skills/<name>` → `../../.github/cursor/skills/<name>`. `agentic-ai-architecture/SKILL.md` resolves through the link. Together with the Superpowers copies, `.cursor/skills` contains 23 `SKILL.md` files.
- Confirm that `CURRENT_STATE_AND_GAPS.md`, `SUPERPOWERS_ADOPTION.md`, this operating model, `README.md`, `CLAUDE.md`, and architecture documents exist where referenced; if absent, note the gap instead of inventing content.
- Confirm native GitHub milestones M1–M8 and labels exist. Issue titles and description text are **not** substitutes for GitHub milestone assignments.

### 2.2 Verify GitHub and working tree

```bash
gh auth status
gh repo view javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence
git status --short
git pull --ff-only origin main
gh issue list --state open --limit 100
gh api repos/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/milestones --paginate --jq '.[].title'
```

If milestones do not exist, create them through GitHub UI or an authenticated `gh api` script before attempting milestone-based execution. Never assume they have been created merely because issue bodies contain M1–M8.

### 2.3 Configure issue board

Use GitHub Project statuses: **Backlog → Ready → In Progress → In Review → Done**, plus **Blocked**. Filter by native milestone and `priority:P0/P1/P2`, category labels, and dependencies. Keep one issue in progress per coding worktree unless concurrency is deliberate.

**31 Oct 2026 — done.** The first `gh` token had `gist`, `read:org`, `repo`, and `workflow` only. `gh project list` and a `projectV2` query returned `INSUFFICIENT_SCOPES`. `gh auth refresh -h github.com -s project,read:project` completed after the device login, and `gh auth status` then showed the `project` scope. Created user project 7, [SRE Incident Intelligence](https://github.com/users/javakishore-veleti/projects/7) (`PVT_kwHOAFyDNM4BpnmP`), and linked it to this repository. Replaced the default Status options with Backlog, Ready, In Progress, In Review, Done, and Blocked. Added all 38 open issues and set each status to Backlog. `gh project item-list` returned 38 items, every one Backlog. Created the missing `priority:P2` label so the board filter in this section exists. No open issue uses P2 yet.

## 3. Standard issue lifecycle — exact steps in Cursor

### Step 1 — Select the next issue

1. Open **Cursor → repository workspace** and **GitHub Issues** in the browser or GitHub extension.
2. Filter to the current native milestone, open issues, `priority:P0` first, then check blockers.
3. Read issue body, acceptance criteria, comments, relevant baseline documents and adjacent code.
4. Decide whether the issue is ready. If unclear, add clarifying questions to the issue; do not begin speculative implementation.
5. Assign yourself and move the GitHub Project status to **In Progress** when actually starting.

CLI:

```bash
gh issue view ISSUE_NUMBER --repo javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence --comments
```

### Step 2 — Open a clean worktree/branch

In Cursor terminal, update `main` and ask Superpowers `using-git-worktrees` to create an isolated workspace. Example manual fallback:

```bash
git fetch origin
git worktree add ../sre-issue-ISSUE_NUMBER -b issue/ISSUE_NUMBER-short-description origin/main
```

Open the worktree folder in a separate Cursor window. Do not implement directly on `main`. Confirm clean baseline and available test commands before editing.

### Step 3 — Brainstorm and approve design

In **Cursor Agent with Superpowers**, give the issue number and request `brainstorming`. Require:

- Current behavior and desired behavior; constraints and out-of-scope items.
- Dependencies, security, operational impact, failure cases and rollback plan.
- Explicit design decisions and alternatives, particularly for agent architecture and AWS.
- Small, reviewable design document or a short issue comment for trivial changes.

**Human gate A:** Approve the design before significant architectural changes.

### Step 4 — Write an executable plan

Invoke Superpowers `writing-plans`. Require exact file paths, test-first steps, commands, acceptance-criteria mapping, and commit boundaries. Keep the plan in a PR description or an issue-specific `docs/plans/` file when substantial. Do not confuse a plan with completed work.

**Human gate B:** Approve the implementation plan, especially if it changes APIs, data models, permissions, cloud resources, or agent behavior.

### Step 5 — Implement with Superpowers

Choose the coding executor:

| Issue type | Primary | Review |
|---|---|---|
| Java, integrations, LangGraph, telemetry, SRE, AWS | Cursor Agent + Superpowers | Cursor review; human approval for high-risk changes |
| React/UI | Cursor Agent + Superpowers | Cursor review |
| Multi-model evaluation and independent test harness | Cursor Agent + Superpowers | Cursor review |
| Small docs/config changes | Cursor Agent + Superpowers | Lightweight review |

Use `subagent-driven-development` or `executing-plans` depending on issue complexity. Apply `test-driven-development` where meaningful: reproduce failure, write failing test, implement minimum fix, refactor. Do not use autonomous subagents for unrestricted cloud operations.

### Step 6 — Validate and review

Run applicable tests **in the worktree**. Include unit, integration, security, smoke, schema, lint, workflow syntax, CloudFormation validation, or controlled incident tests as relevant. Use Superpowers `systematic-debugging` on failures and `verification-before-completion` before claiming success. Request Superpowers code review in Cursor. For security, AWS, and destructive workflows, the human approval gate still applies.

Collect an evidence table:

| Acceptance criterion | Test/command | Actual result | Evidence link |
|---|---|---|---|
| Example criterion | `./mvnw test` | PASS/FAIL/NOT RUN | CI run or log |

Never mark a criterion PASS without running or observing the relevant check.

### Step 7 — Commit and create PR

```bash
git status --short
git diff --check
git add <reviewed-files>
git commit -m "feat(scope): implement ISSUE_NUMBER requirement"
git push -u origin issue/ISSUE_NUMBER-short-description
gh pr create --base main --head issue/ISSUE_NUMBER-short-description --title "[ISSUE-ID] Short outcome" --body "Closes #ISSUE_NUMBER"
```

Include issue link, scope, design, risks, test evidence, screenshots/telemetry where applicable, and rollback guidance. Check GitHub Actions results. Use `finishing-a-development-branch` after verification and review.

### Step 8 — Merge, close, and update project

- Confirm approved PR, green required checks and acceptance evidence.
- Merge through the repository's permitted merge strategy; issue auto-closes only if the PR references it correctly and is merged to the default branch.
- Confirm issue is closed and native milestone is correct; update GitHub Project status to Done.
- Record actual elapsed time, review findings, test outcomes and lessons for Superpowers mastery metrics.
- Clean up worktree only after merged work is safely available on `main`.

## 4. Reusable Cursor Agent prompt — one issue

```text
Use Superpowers to implement GitHub issue #<NUMBER> in
javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence.
Read the issue, CURRENT_STATE_AND_GAPS.md, SUPERPOWERS_ADOPTION.md,
SUPERPOWERS_GitIssues_OperatingModel.md, README.md and relevant skill(s).
Start with brainstorming, then an exact-file writing plan. Do not code
until I approve significant design changes. Use an isolated worktree.
Implement test-first where practical; run real validation commands.
Review acceptance criteria and report PASS/FAIL/NOT RUN with evidence.
Do not deploy, destroy, mutate cloud infrastructure or execute remediation
without my explicit approval. Prepare a PR referencing the issue; do not
merge without approval. Show remaining risks and next dependencies.
```

## 5. Batch operating model — related issues

Use a batch only if issues share a testable deliverable and can be reviewed together (e.g., OBS-001/002/003 for one correlated checkout trace). Otherwise use separate PRs and a common milestone design.

1. Create a short batch design listing included issues, explicit exclusions and dependencies.
2. Keep each issue independently testable and traceable to code and evidence.
3. Prefer one branch/PR per issue; when a shared refactor is necessary, create a prerequisite PR first.
4. For a single batch PR, include `Closes #N` for each issue **only if all acceptance criteria are satisfied**.
5. Run full regression and cross-component tests after the last change.
6. Do not close the milestone until all required integration outcomes work together.

**Good batch:** `OBS-001 + OBS-002 + OBS-003` with a cross-service request correlation test.  
**Avoid batch:** `AWS-001 + AWS-011` in one PR; provisioning and destructive orchestration need separate review and approval.

## 6. Milestone-by-milestone delivery

The issue numbers below are **GitHub issue numbers**, not the stable category IDs. Confirm current milestone assignments and issue state before starting.

| Milestone | Issues | Delivery outcome | Gate to close |
|---|---|---|---|
| **M1 — Local Application Baseline** | #1 APP-001, #2 APP-002, #3 APP-003, #4 CICD-001 | Local services/portals reproducible, commerce integration tests, correct search, CI | Clean startup, end-to-end purchase tests and green CI |
| **M2 — Security & Reliability** | #5 SEC-001, #6 SEC-002, #7 SEC-003, #8 REL-001, #9 REL-002 | Access control, secrets hygiene, idempotent orders, audit trail | Negative authorization and retry/failure tests pass |
| **M3 — Observability** | #10 OBS-001, #11 OBS-002, #12 OBS-003 | Metrics, logs and distributed traces correlated | One checkout transaction traceable across systems |
| **M4 — Incident Engineering** | #13 SRE-001, #14 SRE-002, #15 SRE-003 | Three controlled, recoverable incidents | Repeatable fault, evidence and recovery for each |
| **M5 — Agentic AI Investigation** | #16 AI-001, #17 AI-002, #18 AI-003, #19 AI-004, #37 AI-005, #38 AI-006, #39 AI-007 | LangGraph state/contracts, telemetry tools, hypothesis testing, guardrails and investigation UI | Grounded root-cause reports for known incidents; no unapproved remediation |
| **M6 — AWS Infrastructure Foundation** | #22–#32 AWS-001–AWS-011 | CloudFormation stacks and manual setup/destroy workflows | Safe setup, validation and teardown; reused network preserved |
| **M7 — AWS Application Deployment** | #33–#35 AWS-012–AWS-014 | ECR deployment, portals/agents, smoke tests, undeploy | Versioned deployment and rollback/undeploy demonstrated |
| **M8 — Model Evaluation & Release** | #20 EVAL-001, #36 AWS-015 | Reproducible multi-model evaluation and full AWS lifecycle evidence | Ground-truth results, cost/latency data, safe teardown documented |

**Important:** The work may be implemented in dependency order across milestones rather than strictly sequentially. In particular, M5 agent contracts and telemetry tools can be developed alongside M3/M4, and M6 may be prototyped while incident tests mature. **Close milestones only after their own gates pass.**

### M1 execution checklist

- [ ] APP-001: start/status/stop infrastructure, five services and two portals; capture health evidence.
- [ ] APP-002: signup → signin → offer search → checkout → order history integration test.
- [ ] APP-003: server-side search/date criteria before latest-100 limit.
- [ ] CICD-001: PR builds/tests and portal checks.
- [ ] Demo from clean checkout; record commands and CI run; close M1 only if all pass.

### M2 execution checklist

- [ ] SEC-001/002: test unauthorized admin and cross-customer order access.
- [ ] SEC-003: inspect secret injection and rotate any exposed credentials as needed.
- [ ] REL-001/002: verify retries, inventory consistency, cancellation and audit history.
- [ ] Document threat model and recovery behavior; close M2 after negative tests pass.

### M3 execution checklist

- [ ] OBS-001: verify Prometheus targets and real HTTP/HikariCP metrics.
- [ ] OBS-002: verify structured logs and correlation IDs in ELK.
- [ ] OBS-003: verify Jaeger spans across checkout.
- [ ] Save one incident-ready telemetry evidence package; close M3 after cross-source correlation works.

### M4 execution checklist

- [ ] SRE-001: controlled HikariCP pool exhaustion with k6.
- [ ] SRE-002: controlled Inventory Service delay and timeout.
- [ ] SRE-003: business failure with process health still green.
- [ ] For each: record baseline, fault trigger, time window, symptoms, telemetry, recovery and ground truth.

### M5 execution checklist — primary project theme

- [ ] AI-005: typed graph state, tool contracts, budgets, retries, evidence IDs.
- [ ] AI-001: read-only Prometheus, Elasticsearch, Jaeger and deployment-history tools.
- [ ] AI-002: triage → collect → correlate → hypothesize → verify → recommend graph.
- [ ] AI-006: compare competing hypotheses against logs, metrics and traces.
- [ ] AI-003: injection defenses, bounded tools, evidence requirements, human approval.
- [ ] AI-004: incident investigation UI and evidence-linked report export.
- [ ] AI-007: rubric-based and LLM-as-judge evaluation, human review of counterexamples.
- [ ] Validate on M4 incidents with blind ground truth where feasible; no unsupported model claims.

### M6 execution checklist — CloudFormation and manual GitHub Actions

- [ ] AWS-001: choose `new` or `existing` VPC; validate `AWS_VPC_ID` and existing subnet IDs from repository/environment configuration. Two AZs; public, private-app and private-data subnet roles. Use nonoverlapping CIDRs. Never delete reused resources.
- [ ] AWS-002–008: IAM/security, ECR, RDS, Redis, EKS, observability, ingress/ALB ownership.
- [ ] AWS-009: create eight `AWS-###-Setup-<Aspect>.yml` and corresponding `AWS-###-Destroy-<Aspect>.yml` workflows. Each supports `workflow_dispatch` and `workflow_call`.
- [ ] AWS-010: `AWS-900-Setup-All.yml` calls setup workflows 001→008 with `jobs.<job>.uses` and `needs` dependency sequencing.
- [ ] AWS-011: `AWS-901-Destroy-All.yml` undeploys workloads and invokes destroy workflows 008→001, with typed confirmation and approvals.
- [ ] Use CloudFormation stack events/outputs and drift/ownership checks for verification. Never run setup or destroy automatically from a push or PR.

**AWS safety:** Prefer GitHub OIDC and least-privilege IAM. If access keys already exist as repository secrets, do not print them. A VPC ID is typically a repository variable, though secrets can be supported. Require approval before any AWS-mutating workflow, particularly destruction. Preserve RDS data unless explicitly authorized. Distinguish resources owned by CloudFormation from those created by Kubernetes controllers.

### M7 execution checklist

- [ ] AWS-012: versioned images → ECR → backend Helm/manifests.
- [ ] AWS-013: portals and investigation agent with secure ingress.
- [ ] AWS-014: `AWS-910-Deploy-All.yml`, `AWS-911-Undeploy-All.yml`, smoke tests and rollback.
- [ ] Capture image digests, deployment revisions, health checks, rollout and rollback evidence.

### M8 execution checklist

- [ ] EVAL-001: identical incident datasets, ground truth, model versions, correctness, evidence quality, latency, token/cost metrics and reproducible reports.
- [ ] AWS-015: approved setup → deploy → fault → investigation → evaluation → undeploy → destroy.
- [ ] Confirm CloudFormation stacks and Kubernetes-owned resources cleaned up according to policy; document any retained snapshots/costs.
- [ ] Publish an evidence-based project summary suitable for a resume portfolio.

## 7. Milestone closure procedure in Cursor

1. In GitHub, filter issues by the **native milestone**. Confirm all required issues are closed and linked to merged PRs.
2. From Cursor terminal, pull `main`, run milestone-level regression/smoke commands, and save real results.
3. Review cross-issue integration, open defects, security exceptions, and known limitations.
4. Create `docs/milestones/M<NUMBER>-completion.md` with outcome, issue/PR links, test commands, observed evidence, architecture decisions, unresolved risks and sign-off.
5. Review the milestone in Cursor when it includes security, AI reasoning, or AWS infrastructure, then obtain human acceptance.
6. Obtain human acceptance. Close the native milestone in GitHub UI or authorized CLI/API operation.
7. Record learning outcomes in the Superpowers adoption metrics and update resume claims **only for implemented, demonstrated capabilities**.

## 8. Issue and milestone definition of done

**Issue DoD:** scope met; reviewed code merged; tests actually run; acceptance criteria supported; docs updated; no unapproved side effects; GitHub issue closed and correctly labeled/milestoned.

**Milestone DoD:** all mandatory issue DoDs plus integrated demonstration, regression evidence, risk review, documented operational runbook, and explicit acceptance.

**Superpowers mastery evidence:** maintain a short record per issue of skills invoked, design decisions, plan quality, red-green-refactor evidence, review findings, verification results and lessons learned. Do not infer skill usage from tool installation alone.

## 9. Weekly cadence

| Cadence | Cursor/GitHub activity | Output |
|---|---|---|
| Monday | Review milestone backlog, priorities, blockers, choose 1–3 issues | Weekly issue plan |
| Daily start | Read issue, update worktree, invoke Superpowers design/plan | Approved execution plan |
| Daily end | Run tests, record evidence, push safe commits, update issue | Traceable progress |
| Midweek | Independent review of high-risk PRs and incident evidence | Review findings |
| Friday | Merge approved PRs, demo integrated outcomes, update milestone status | Weekly engineering report |
| Milestone gate | Run full milestone closure procedure | Signed-off milestone record |

## 10. Common failure modes and recovery

- **Agent starts coding without a plan:** stop, request brainstorming/writing-plans and approve scope.
- **Cursor cannot find project skills:** verify `.cursor/skills/<name>/SKILL.md`, plugin installation and restart/new agent session.
- **Tests fail:** use Superpowers systematic debugging; preserve failing logs and do not mark complete.
- **AI agent invents evidence:** require source IDs, timestamps and verifiable tool output; mark root cause uncertain when evidence is insufficient.
- **AWS workflow attempts to delete reused infrastructure:** stop, review stack ownership, require explicit manual approval, and fix the workflow before re-running.
- **Issue title says M5 but GitHub milestone is empty:** assign the native milestone; do not count issue as milestone-managed until confirmed.
- **PR merges but issue remains open:** verify `Closes #N` and default-branch merge; close only after acceptance is checked.

## 11. Quick links

- [Repository](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence)
- [Issues](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/issues)
- [Milestones](https://github.com/javakishore-veleti/AWS-Agentic-AI-SRE-Incident-Intelligence/milestones)
- [Superpowers](https://github.com/obra/superpowers)
- [Adoption plan](SUPERPOWERS_ADOPTION.md)

**Start here:** APP-001 / GitHub issue #1. Run one full Superpowers lifecycle end-to-end before attempting a large parallel batch.
