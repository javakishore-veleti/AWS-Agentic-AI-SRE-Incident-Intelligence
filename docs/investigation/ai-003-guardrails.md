# AI-003 guardrails

The investigation agent may read health and the four telemetry sources. Any other tool name is rejected. A run stops after 12 tool calls.

Telemetry text that tells the agent to ignore its instructions is stored as `[untrusted telemetry omitted]`. A hypothesis that repeats that text, or that names a shell or a delete, is marked unsupported. A recommendation with those words is replaced with a no-change sentence.

`POST /api/approvals` checks the approver, ticket, environment, expiry, and change. A missing field, an expired time, an `executed` flag, or a change that names a delete without retrieved evidence is denied. A complete approval is recorded with `executed` false. This agent does not apply the change.
