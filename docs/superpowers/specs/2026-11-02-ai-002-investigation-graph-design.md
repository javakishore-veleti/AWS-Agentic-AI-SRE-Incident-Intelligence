# AI-002 investigation graph

The graph in `Middleware/investigation-agent` keeps the five commerce services as the system under test. It stays read-only.

Stages, in order: triage, collect, correlate, hypothesize, verify, recommend.

Collect reads actuator health and the four AI-001 sources. A failed or unconfigured read is an evidence row with an error and no items. Correlate names a trace id only when that id is present on rows from more than one source. Hypothesize uses `INVESTIGATION_MODEL_URL` when it is set. When it is unset, a deterministic reader writes the hypothesis and the report says no model was called. Verify keeps a hypothesis only when it cites an evidence id from a row that returned items, or when the text is marked inconclusive. Recommend does not apply a change.

`modelId` is recorded only when a model response includes one. A model answer that asks for a deletion or a shell is discarded.
