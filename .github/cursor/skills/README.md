# Cursor code-generation skills — AWS Agentic AI SRE Incident Intelligence

These files are instructions for Cursor to generate implementation code, **not implemented application features**. Read the existing repository and `CURRENT_STATE_AND_GAPS.md` before executing each skill. Do not claim AWS resources or models have been tested unless actually verified.

## Skills
- `gh-actions-aws-codegen` — Generate AWS CloudFormation infrastructure and manual GitHub Actions setup/destroy/deploy workflows for this SRE project.
- `agentic-ai-architecture` — Design the production-minded LangGraph multi-agent SRE incident intelligence system and shared investigation contracts.
- `agentic-ai-telemetry-tools` — Implement read-only Prometheus Elasticsearch Jaeger and deployment-history tools for LangGraph SRE agents.
- `agentic-ai-incident-triage` — Generate the LangGraph incident triage and telemetry collection agents for real microservice incidents.
- `agentic-ai-root-cause` — Generate evidence correlation hypothesis analysis and validation agents for SRE incidents using LangGraph.
- `agentic-ai-remediation-guardrails` — Build SRE remediation recommendation agents human approval gates and safety guardrails.
- `agentic-ai-evaluation` — Build a reproducible multi-model benchmark for agentic SRE incident investigation with real evidence.
- `sre-incident-injection` — Generate controlled repeatable SRE incident scenarios load tests rollback and evidence capture for Java microservices.
- `sre-investigation-reporting` — Generate evidence-cited SRE incident reports and a reviewable investigation UI or export.

## Suggested order
1. `agentic-ai-architecture` and `agentic-ai-telemetry-tools` (primary project theme)
2. `sre-incident-injection` to generate reproducible ground-truth evidence
3. `agentic-ai-incident-triage`, `agentic-ai-root-cause`, `agentic-ai-remediation-guardrails`
4. `agentic-ai-investigation-reporting` and `agentic-ai-evaluation`
5. `gh-actions-aws-codegen` for manual CloudFormation provisioning, application deployment and safe teardown

## GitHub issues/milestones
Use `[AI-###][P0/P1]`, `[SRE-###][P0/P1]`, `[AWS-###][P0/P1]` and `[CICD-###][P0/P1]` issue titles. AI is central: create milestone M5 Agentic AI Investigation and M8 Model Evaluation. AWS infrastructure is M6 and deployment M7. Cross-reference actual issue IDs after creation.

## Cursor discovery
Cursor's conventional project skill location is `.cursor/skills/<name>/SKILL.md`. The requested canonical repository storage path here is `.github/cursor/skills/<name>/SKILL.md`; if Cursor does not discover it automatically, copy or symlink each skill directory into `.cursor/skills/` (or configure the version of Cursor you use). The entrypoint is singular `SKILL.md`, not `SKILLS.md`.
