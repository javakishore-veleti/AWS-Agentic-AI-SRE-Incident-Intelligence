# APP-001 Startup and Shutdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the existing local containers, five Java services, investigation agent, and both portals start cleanly and stop with no listeners left behind.

**Architecture:** Run the root npm scripts in start order, record each status command, stop in reverse order, and record status again. A failure is written down before any edit. The edit touches only the file the failed output identifies, then that same command is run again.

**Tech Stack:** npm scripts, Bash scripts in `CICD/Local/`, Docker Compose, Java 21 services on 8081–8085, investigation agent on 8090, Vite portals on 5173 and 5174.

**Spec:** `docs/superpowers/specs/2026-10-31-app-001-startup-shutdown-design.md`

## Global Constraints

- Work only in `.worktrees/issue-1-app-001` on branch `issue/1-app-001-startup-shutdown`.
- Use the existing `local:*` npm scripts. Do not add a new runner.
- Append a failing command and its full output to `SUPERPOWERS_GitIssues_Progress.md` before any edit.
- Change only the script, config, or runbook line that the failed output names.
- The accepted result is the clean rerun, not the earlier failure.
- After start, ports 8081, 8082, 8083, 8084, 8085, 8090, 5173, and 5174 report `up`.
- After stop, those ports report `down` and `docker-all-status.sh` prints `Network sre-incident-net: absent`.

---

### Task 1: Container start evidence

**Files:**
- Modify: `SUPERPOWERS_GitIssues_Progress.md`
- Test: `CICD/Local/docker-all-status.sh`

**Interfaces:**
- Consumes: root script `local:containers:all:start`
- Produces: status text containing `Network sre-incident-net: present` and a `docker compose ps` section for Postgres, Redis, Prometheus, ELK, and Jaeger

- [ ] **Step 1: Run the container start**

```bash
npm run local:containers:all:start
```

- [ ] **Step 2: Run the status check**

```bash
npm run local:containers:all:status
```

Expected: exit 0, first line `Network sre-incident-net: present`, and a section for Postgres, Redis, Prometheus, ELK, and Jaeger.

- [ ] **Step 3: Record or repair**

If the status command fails or the network line is `absent`, append the command and the full output to `SUPERPOWERS_GitIssues_Progress.md`, edit only the file that output identifies, and rerun Step 1 and Step 2.

- [ ] **Step 4: Commit the evidence note when the status passes**

```bash
git add SUPERPOWERS_GitIssues_Progress.md
git commit -m "Record the APP-001 container start status."
```

### Task 2: Middleware health evidence

**Files:**
- Modify: `SUPERPOWERS_GitIssues_Progress.md`
- Test: `CICD/Local/middleware-all-status.sh`

**Interfaces:**
- Consumes: container network `sre-incident-net` from Task 1
- Produces: one `up` line each for ports 8081, 8082, 8083, 8084, 8085, and 8090

- [ ] **Step 1: Run the middleware start**

```bash
npm run local:middleware:all:start
```

- [ ] **Step 2: Run the health check**

```bash
npm run local:middleware:all:status
```

Expected: exit 0 and these lines:

```text
product-catalog-service      8081   up
offers-service               8082   up
customers-service            8083   up
orders-service               8084   up
inventory-service            8085   up
investigation-agent          8090   up
```

- [ ] **Step 3: Record or repair**

If any line is `down`, append the start command and the status output to `SUPERPOWERS_GitIssues_Progress.md` before editing. When the start output says `Install Middleware/investigation-agent/requirements.txt`, run:

```bash
python3 -m pip install -r Middleware/investigation-agent/requirements.txt
```

Then rerun Step 1 and Step 2. For any other `down` line, edit only the file named by that output and rerun Step 1 and Step 2.

- [ ] **Step 4: Commit the evidence note when every line is `up`**

```bash
git add SUPERPOWERS_GitIssues_Progress.md
git commit -m "Record the APP-001 middleware health status."
```

### Task 3: Portal evidence

**Files:**
- Modify: `SUPERPOWERS_GitIssues_Progress.md`
- Test: `CICD/Local/portals-all-status.sh`

**Interfaces:**
- Consumes: nothing from the Java health lines except that the portal scripts are separate processes
- Produces: `up` for ports 5173 and 5174

- [ ] **Step 1: Run the portal start**

```bash
npm run local:portals:all:start
```

- [ ] **Step 2: Run the portal status check**

```bash
npm run local:portals:all:status
```

Expected: exit 0 and these lines:

```text
agentic-ai-admin       5173   up
ecommerce-portal       5174   up
```

- [ ] **Step 3: Record or repair**

If either line is `down`, append the command and the full output to `SUPERPOWERS_GitIssues_Progress.md`, edit only the file that output identifies, and rerun Step 1 and Step 2.

- [ ] **Step 4: Commit the evidence note when both lines are `up`**

```bash
git add SUPERPOWERS_GitIssues_Progress.md
git commit -m "Record the APP-001 portal status."
```

### Task 4: Shutdown evidence

**Files:**
- Modify: `SUPERPOWERS_GitIssues_Progress.md`
- Test: `CICD/Local/portals-all-status.sh`
- Test: `CICD/Local/middleware-all-status.sh`
- Test: `CICD/Local/docker-all-status.sh`

**Interfaces:**
- Consumes: the processes started in Tasks 1–3
- Produces: `down` for 8081–8085, 8090, 5173, and 5174, and `Network sre-incident-net: absent`

- [ ] **Step 1: Stop in reverse order**

```bash
npm run local:portals:all:stop
npm run local:middleware:all:stop
npm run local:containers:all:stop
```

- [ ] **Step 2: Run all three status checks**

```bash
npm run local:portals:all:status
npm run local:middleware:all:status
npm run local:containers:all:status
```

Expected: both portal lines `down`, all six middleware lines `down`, and `Network sre-incident-net: absent`.

- [ ] **Step 3: Confirm no listener remains**

```bash
lsof -nP -iTCP:8081 -iTCP:8082 -iTCP:8083 -iTCP:8084 -iTCP:8085 -iTCP:8090 -iTCP:5173 -iTCP:5174 -sTCP:LISTEN
```

Expected: exit 1 and no output. `lsof` exits 1 when it finds no listener.

- [ ] **Step 4: Record or repair**

If any status line is `up`, or `lsof` prints a listener, append the commands and the full output to `SUPERPOWERS_GitIssues_Progress.md`, edit only the file that output identifies, and rerun Steps 1–3.

- [ ] **Step 5: Commit the shutdown evidence when the ports are clear**

```bash
git add SUPERPOWERS_GitIssues_Progress.md
git commit -m "Record the APP-001 shutdown status."
```
