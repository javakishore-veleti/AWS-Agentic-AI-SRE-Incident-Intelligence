---
name: agentic-ai-root-cause
description: Generate evidence correlation hypothesis analysis and validation agents for SRE incidents using LangGraph.
---

# agentic-ai-root-cause

## Responsibilities
Correlate metrics, structured logs, traces, deployment changes and temporal ordering; distinguish symptom, contributing factors and verified root cause. Generate competing hypotheses and seek disconfirming evidence.
- For Hikari pool exhaustion inspect pending connections, pool sizing, query latency, 5xx and request traces. For Inventory latency inspect cross-service spans, timeout/retry behavior and inventory logs. Never hardcode known injected cause into model prompt.
- Each hypothesis includes supporting evidence IDs, contradicting evidence, confidence, missing checks and a next-query plan. Confidence must be justified, not arbitrary precision.
- Evidence validation agent verifies source IDs, timestamp consistency, permissions, scope, and whether conclusion is actually supported. Route insufficient-evidence cases back to telemetry collection within budget.
- Produce machine-readable report plus readable executive incident summary. Test false positives, partial evidence, conflicting telemetry and prompt-injected log messages.
