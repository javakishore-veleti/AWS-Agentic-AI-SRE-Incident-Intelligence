# AI-006 correlation

When a retrieved deployment row shares the window with a metric, log, or trace row, the report calls the deployment a candidate cause and the other row a symptom. Each line cites its evidence id. Metrics, logs, or traces alone stay a symptom, and no cause is assigned. No rows stay inconclusive.

The comparison uses the rows returned by the read-only tools. It does not query a live store.
