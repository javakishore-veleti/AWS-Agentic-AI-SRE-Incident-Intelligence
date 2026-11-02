# AI-002 plan

1. Extend the LangGraph state with evidence, uncertainty, and the model result.
2. Add collect, correlate, and verify around the existing health read.
3. Keep the model call behind `INVESTIGATION_MODEL_URL`, with a deterministic reader when that URL is unset.
4. Test citations, missing telemetry, an unknown service, a shared trace, an uncited hypothesis, and a failed model call.
5. Document the request and response fields in `ARCHITECTURE.md`.
