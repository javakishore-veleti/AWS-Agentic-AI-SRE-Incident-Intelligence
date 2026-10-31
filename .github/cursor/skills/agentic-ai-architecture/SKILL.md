---
name: agentic-ai-architecture
description: Design the production-minded LangGraph multi-agent SRE incident intelligence system and shared investigation contracts.
---

# agentic-ai-architecture

## Mission
AI-based SRE incident investigation is the primary purpose of this repository. Preserve existing commerce services as the system under test. Use LangGraph in `Middleware/investigation-agent`; implement evidence-based, read-only diagnosis before automated remediation. Inspect `CURRENT_STATE_AND_GAPS.md`, `ARCHITECTURE.md`, `DESIGN.md`, and current code before modifications.

## Agent roles and graph
`incident-triage` → `telemetry-collection` → `signal-correlation` → `root-cause-analysis` → `evidence-validation` → `remediation-advisor` → `human-approval` (optional controlled executor). Conditional loops return to collection when evidence is missing, bounded by budgets/timeouts. Prefer deterministic code for collection/validation and LLM reasoning only where needed; avoid agent proliferation.
- Shared typed state: incident_id, UTC start/end, environment, impacted services, signals, trace IDs, query provenance, hypotheses, confidence calibration, missing evidence, recommended next steps, approvals, token/cost budget, execution audit.
- All agent outputs structured (Pydantic models); stable incident report schema; explicit unknown/insufficient evidence state. No unsupported root-cause claims.
- Separate tools from prompts and policies; inject model and telemetry clients; support mocked offline fixtures and real read-only integrations. Maintain correlation/causation distinction and competing hypotheses.
- Model router configured by environment/model IDs; record provider, exact model version, latency, token usage, cost, retries, and errors. Never claim any model benchmark without measured experiments.
- Protect PII, secrets, tool responses, and untrusted logs from prompt injection. Redact sensitive data, enforce least privilege and bounded query windows. No direct autonomous changes to production.

## Deliverables
Graph modules, shared state/schema, agent interfaces, deterministic tests, incident report examples, sequence diagram, integration plan, documentation and telemetry tool contract. Use explicit test fixtures for INC-001 and INC-002; implement one complete vertical slice before expanding.
