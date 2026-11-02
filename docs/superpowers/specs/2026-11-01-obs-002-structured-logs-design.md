# OBS-002 structured logs

Issue #11. A failed request can be found by correlation id.

## Decision

Every service writes JSON logs. A filter stores `correlationId` and `service` for the request and returns `X-Correlation-Id`. The caller’s id is kept only when it is a short safe token. A bearer token is not reused as the id and is not written into the failure log.

Logstash already accepts that JSON on port 5044. Kibana would query `correlationId`. This change proves the JSON line. It does not open Kibana.
