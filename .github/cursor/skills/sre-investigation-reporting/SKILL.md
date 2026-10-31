---
name: sre-investigation-reporting
description: Generate evidence-cited SRE incident reports and a reviewable investigation UI or export.
---

# sre-investigation-reporting

## Output contract
Provide incident metadata, timeline UTC, customer impact, service dependency graph, signal summary, evidence registry (Prometheus/ELK/Jaeger/deployment IDs), hypotheses with supporting/contradictory evidence, validated root cause or unknown, recommended remediation, risk, rollback and approval status.
- Distinguish observed facts from inference. Never invent traces or metric values. Preserve auditability and reproducibility via model/tool versions and correlation IDs.
- Generate JSON schema/Pydantic models, Markdown export, API endpoint and UI requirements. Ensure PII redaction, access control and test fixtures. No automatic execution of remediation.
