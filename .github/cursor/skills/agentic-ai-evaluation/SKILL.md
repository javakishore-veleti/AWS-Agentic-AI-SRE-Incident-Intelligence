---
name: agentic-ai-evaluation
description: Build a reproducible multi-model benchmark for agentic SRE incident investigation with real evidence.
---

# agentic-ai-evaluation

## Benchmark contract
Compare only actually available models under identical incident fixtures, tool permissions, data windows and budgets. Record model/provider/version, prompt version, tools, trace, token usage, cost, latency, errors and final report.
- Ground truth must be authored separately from agent input and never leaked. Evaluate root-cause correctness, evidence citations validity, unsupported claims, time-to-diagnosis, safety, cost and repeatability; include multiple seeds/runs and confidence intervals where practical.
- Cover INC-001 pool exhaustion, INC-002 inventory latency, INC-003 healthy infrastructure but broken behavior, and INC-004 as defined in project docs after inspection. Include clean baseline and unknown-cause controls.
- Export machine-readable JSON/CSV and readable Markdown with methodology and limitations. Do not invent Astra or GPT model performance or claim published benchmark results based on local tests.
