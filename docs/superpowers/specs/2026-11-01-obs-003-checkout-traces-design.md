# OBS-003 checkout traces

Issue #12. A purchase should show linked spans for orders, offers, and inventory.

## Decision

Orders already builds its clients with the observation registry. Tests now do the same, so a purchase records the trace that production sends. The orders server span is the parent of the offers and inventory client spans. Each span has a duration. The downstream calls carry `traceparent`. An inventory HTTP 500 stays on that trace and the client span is an error.

Jaeger would show that trace id. This change did not open Jaeger.
