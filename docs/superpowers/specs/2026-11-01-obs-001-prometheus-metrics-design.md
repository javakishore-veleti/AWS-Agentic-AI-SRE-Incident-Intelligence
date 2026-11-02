# OBS-001 Prometheus metrics

Issue #10. The five commerce services already expose `/actuator/prometheus`. This check proves the series a scrape would collect.

## Decision

HTTP latency uses the `http_server_requests_seconds` histogram. Each service turns on `management.metrics.distribution.percentiles-histogram.http.server.requests`. Error rate is the same series counted by `status`. Pool size is `hikaricp_connections`.

A test in each service requests a missing path, then reads `/actuator/prometheus`. The body must contain the histogram bucket, a 4xx or 5xx status, and `hikaricp_connections`. Prometheus already scrapes ports 8081 through 8085. The Prometheus UI was not queried in this change.
