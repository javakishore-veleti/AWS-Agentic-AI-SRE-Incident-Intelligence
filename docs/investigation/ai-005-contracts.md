# AI-005 contracts

`app/contracts.py` checks an investigation report before the API returns it. An evidence id must look like `ev-prometheus-abc12345`: the source name and eight hex characters. A row with an error must be accompanied by uncertainty.

A failed telemetry read is tried once more. The tool-call budget stays 12. When the budget is already spent, the error from the last read is kept and no further call is made.

Validation command: `python -m pytest -q` in `Middleware/investigation-agent`.
