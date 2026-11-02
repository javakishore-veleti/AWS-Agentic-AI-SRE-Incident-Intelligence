# INC-001 pool exhaustion

Reduce the catalog pool, send concurrent product reads, then put the pool back.

## Setup

Set `HIKARI_MAX_POOL=1` on product-catalog-service and restart it. Leave the other services at 10.

## What to watch

`/actuator/prometheus` on port 8081 gains `hikaricp_connections_timeout` while product reads fail. The failed read's trace span lasts at least the connection timeout. `/api/products` returns 500 while the only connection is held.

## Recovery

Set `HIKARI_MAX_POOL=10`, restart catalog, and read `/api/products` again. The read returns 200.

The automated proof is `PoolExhaustionTest`. It holds the one connection, records the timeout metric and a span longer than 300ms, releases the connection, and reads the products successfully. The k6 script was not run.
