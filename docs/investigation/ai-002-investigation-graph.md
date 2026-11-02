# AI-002 investigation graph

`POST /api/investigations` runs six read-only stages: triage, collect, correlate, hypothesize, verify, and recommend.

Collect calls the AI-001 readers for Prometheus, Elasticsearch, Jaeger, and deployment history. A missing URL or a failed read is an evidence row with an error and no items. The report lists that error under `uncertainty`.

A hypothesis is shown only when its text contains an evidence id from a row that returned items. Otherwise the hypothesis is marked `unsupported`. When no source returns items, the hypothesis is `inconclusive`.

`INVESTIGATION_MODEL_URL` is optional. When it is unset, the deterministic reader writes the hypothesis and `modelCalled` is false. When the URL is set and the call fails, or the answer asks for a change, the deterministic reader is used and the failure is recorded. `modelId` is set only from a model response that includes one.

Recommendations stay advisory. This change did not query a live store and did not call a model endpoint.
