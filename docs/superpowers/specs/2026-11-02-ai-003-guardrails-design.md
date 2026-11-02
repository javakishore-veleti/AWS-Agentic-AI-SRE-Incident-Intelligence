# AI-003 guardrails

The agent reads health and the four telemetry sources. Other tool names are rejected. A run stops after 12 calls.

Telemetry that tells the agent to ignore its instructions is omitted before a hypothesis is written. A hypothesis or recommendation that asks for a shell or a delete is rejected.

`POST /api/approvals` records a complete, unexpired approval and always returns `executed: false`. A missing field, an expired time, an execution flag, or a delete without retrieved evidence is denied.
