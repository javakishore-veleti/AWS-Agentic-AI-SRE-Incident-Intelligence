# Read-only telemetry tools

`query_evidence` in `Middleware/investigation-agent/app/tools.py` issues a GET to one of Prometheus, Elasticsearch, Jaeger, or a deployment-history URL. The window is at most 6 hours and the result is at most 20 items. Bearer tokens and JWTs in an Elasticsearch hit are replaced with `[redacted]`.

Set `PROMETHEUS_URL`, `ELASTICSEARCH_URL`, `JAEGER_URL`, or `DEPLOYMENT_HISTORY_URL` before a call. If a variable is unset, the result has an error and an empty item list.

The tests start a local HTTP server or pass a fake opener. Prometheus, Elasticsearch, Jaeger, and a deployment history service were not queried.
