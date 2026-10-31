---
name: sre-incident-injection
description: Generate controlled repeatable SRE incident scenarios load tests rollback and evidence capture for Java microservices.
---

# sre-incident-injection

## Requirements
Build isolated, opt-in fault injection for local and explicitly approved AWS dev environments only. Do not run experiments against production. Prefer k6 load and configurable fault toggles with strict start/stop/restore scripts.
- INC-001: HikariCP pool exhaustion with observable pending/acquisition latency, request failure and recovery.
- INC-002: Inventory Service artificial delay/timeouts; observe Order→Inventory trace and order consistency.
- INC-003: app-level bad configuration/behavior while health endpoint stays green.
- INC-004: inspect current project design and implement only if scenario specified; otherwise request definition.
- Each incident has setup, expected symptoms, hidden ground truth, load profile, start/stop timestamps, telemetry evidence, rollback and validation checklist. Capture artifacts for agent evaluation. Enforce resource limits and cleanup on interruption.
