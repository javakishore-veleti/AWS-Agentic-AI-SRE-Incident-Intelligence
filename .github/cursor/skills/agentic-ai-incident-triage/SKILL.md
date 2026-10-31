---
name: agentic-ai-incident-triage
description: Generate the LangGraph incident triage and telemetry collection agents for real microservice incidents.
---

# agentic-ai-incident-triage

## Responsibilities
Implement deterministic first-pass triage of incident request: incident ID, severity, affected services, impact, UTC timeline, user symptom, known changes, evidence availability. Query health endpoints only as one signal; health green does not imply service healthy. Delegate telemetry gathering to dedicated read-only tools.
- Normalize service names for product-catalog, offers, customers, orders and inventory; propagate trace and request IDs.
- Produce a typed triage result, query plan and missing-data checklist. Respect max tool calls, time window, timeout and cancellation.
- Tests cover unknown service, partial outage, degraded latency with healthy actuator, and missing telemetry. Do not infer root cause at triage stage.
