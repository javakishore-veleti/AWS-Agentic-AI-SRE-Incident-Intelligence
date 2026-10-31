---
name: agentic-ai-telemetry-tools
description: Implement read-only Prometheus Elasticsearch Jaeger and deployment-history tools for LangGraph SRE agents.
---

# agentic-ai-telemetry-tools

## Scope
Build real, read-only tools for Prometheus query_range/query, Elasticsearch filtered search, Jaeger trace search/fetch, and deployment metadata lookup. Inspect existing telemetry configuration and validate real API endpoints; never fabricate query results.
- Accept typed time windows, service names, request/trace IDs and bounded limits; return normalized timestamped evidence with source URL/query metadata and errors.
- Prometheus: availability, HTTP request rates/errors/p95/p99, JVM heap/GC, Hikari active/idle/pending/acquisition latency. Confirm metric names against observed exporters rather than assuming them.
- ELK: error patterns, correlation/trace IDs, exception stack summaries; redact PII; restrict index/time and result size.
- Jaeger: parent/child spans, critical path, latency distribution and Order→Inventory trace linkage.
- Deployment history: commit SHA, image digest, rollout time and config change; avoid claiming a deployment cause without corroboration.
- Enforce read-only service credentials, timeouts, retries, rate limits, circuit breakers and query budgets. Add fixture-backed contract tests and integration tests using running services. Report unavailable telemetry as missing evidence.
