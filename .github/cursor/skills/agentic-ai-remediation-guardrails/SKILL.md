---
name: agentic-ai-remediation-guardrails
description: Build SRE remediation recommendation agents human approval gates and safety guardrails.
---

# agentic-ai-remediation-guardrails

## Scope
Provide a remediation advisor that outputs ranked, reversible recommendations with expected impact, blast radius, rollback and validation checks. First release is strictly read-only and advisory.
- Never execute restart, scaling, config change, database mutation or AWS API mutation without explicit human approval and separate scoped executor capability.
- Validate tool calls against allowlists, redact secrets and customer data, treat logs as untrusted input, enforce least privilege, query budgets, rate limits and audit trails.
- Add structured approval state, expiration, identity, environment, change ticket and dry-run. Deny by default on missing approval. Include tests for prompt injection, forged approval, dangerous recommendations, and evidence-free actions.
