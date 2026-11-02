# Architecture

This file records how Heddle is structured and which contracts exist today. Product behavior is in [DESIGN.md](DESIGN.md). Agent working rules, the incident report contract, and experiments INC-001 through INC-004 are in [CLAUDE.md](CLAUDE.md).

## In the tree

1. **Two React and TypeScript portals** call the Java services directly. `Portals/agentic-ai-admin` uses Vite on port 5173. `Portals/ecommerce-portal` uses Vite on port 5174. There is no API gateway or BFF.
2. **Five Java 21 Spring Boot services** live under `Middleware/`. Each one owns a PostgreSQL database, uses HikariCP (`HIKARI_MAX_POOL`, default 10), and updates schema with Hibernate `ddl-auto: update`. A catalog test sets that pool to 1, records `hikaricp_connections_timeout` and a span longer than 300ms while a product read fails, then succeeds after the connection is released. `CICD/Local/k6/inc-001-pool.js` is the matching load profile. It was not run. There are no Flyway or Liquibase migrations in the tree.
3. **The investigation agent** is `Middleware/investigation-agent` (FastAPI, LangGraph, Uvicorn). `Middleware/agents` is not a folder. Use that name only if a future task creates it, and label the path as future.
4. **Local dependencies** are independent Compose projects in `CICD/Local/Postgres`, `Redis`, `Prometheus`, `ELK`, and `Jaeger`. Root npm scripts call `docker-all-up.sh`, `docker-all-down.sh`, and `docker-all-status.sh`. `local:middleware:all:*` and `local:portals:all:*` start the Java services and the two Vite apps. `local:middleware-portals:all:*` calls those scripts in order.
5. **AWS** (EKS, RDS, CloudWatch, least-privilege IAM) is a later deployment target. Nothing in this repository provisions it.

| Service | Port | Database |
|---|---|---|
| `product-catalog-service` | 8081 | `product_catalog` |
| `offers-service` | 8082 | `offers` |
| `customers-service` | 8083 | `customers` |
| `orders-service` | 8084 | `orders` |
| `inventory-service` | 8085 | `inventory` |

Postgres 16 listens on `localhost:5432`. The init script creates `offers`, `customers`, `orders`, and `inventory`; the Compose `POSTGRES_DB` is `product_catalog`. Redis 7.4 listens on `localhost:6379`. Offers uses it as a 60-second cache of catalog products (`catalog:product:{id}`). Redis is not a system of record. Java packages are `com.larkspur`.

The database password and the JWT signing secret are read from `DB_PASSWORD` and `JWT_SECRET`. `application.yml` and the Postgres Compose file have no default for either value. A root `.env.example` is not in the tree. Do not copy a secret into a new document. Postgres applies `POSTGRES_PASSWORD` only when the data volume is first created.

## Request flow today

```mermaid
flowchart LR
  Shop[ecommerce-portal :5174] --> Customers[customers-service :8083]
  Shop --> Offers[offers-service :8082]
  Shop --> Orders[orders-service :8084]
  Desk[agentic-ai-admin :5173] --> Catalog[product-catalog-service :8081]
  Desk --> Offers
  Desk --> Customers
  Desk --> Orders
  Orders --> Offers
  Orders --> Inventory[inventory-service :8085]
  Offers --> Catalog
  Offers --> Redis[Redis]
  Agent[investigation-agent] --> Health[Actuator health]
```

Purchase is the only order create path. `POST /api/orders` requires `Authorization: Bearer <token>` and an `Idempotency-Key` header. The token is a JWT from `POST /api/auth/signup` or `POST /api/auth/signin`. The body is `{ "offerId" }`. Orders loads the offer, rejects a non-`ACTIVE` offer or an offer with no products, copies customer identity from the token, sets `totalAmount` from `offerPrice`, and snapshots each product as a line with quantity 1. It commits the row first with status `RESERVING`, then calls `POST /api/reservations` using the order number as `orderRef`. A successful reservation moves the order to `PLACED`. A definite inventory rejection keeps the row as `RESERVATION_FAILED`. A timeout or an unreachable inventory leaves the row `RESERVING`. The same customer and key returns the original order, or finishes the reservation, and does not create a second row. Inventory treats the same `orderRef` as the same reservation and does not decrement stock again. There is no payment call. `DELETE /api/orders/{id}` cancels an order for an administrator. The row stays with status `CANCELLED`. One audit row records the previous status and that administrator. Inventory `POST /api/reservations/release` returns the reserved quantity for that order number. A second cancellation does not write another audit row or release stock again. If inventory cannot be reached, the order stays unchanged.

Desk writes do not send a bearer token. `GET /api/orders` is also unauthenticated and returns the latest 100 orders. `GET /api/orders/mine` is the shopper's scoped read.

## HTTP contracts today

Desk list routes apply optional filters, sort newest first, and return at most 100 matches. A blank parameter is no filter, so a call with no parameters is still the latest 100. Text is a case-insensitive contains match. Status is an exact enum value; an unknown status is 400.

| Route | Parameters |
|---|---|
| `GET /api/products` | `name`, `sku`, `category`, `status` |
| `GET /api/offers` | `name`, `code`, `product`, `status` |
| `GET /api/customers` | `name`, `email`, `status` |
| `GET /api/orders` | `customer`, `email`, `offer`, `status`, `from`, `to` |

`product` matches the catalog product name or SKU after the catalog lookup, then the list is limited to 100. `from` and `to` are inclusive UTC calendar days: `placedAt` is at or after the start of `from`, and before the start of the day after `to`. An unparseable date is 400.

`/api/offers/active` still starts from the unfiltered latest 100 and keeps `ACTIVE` offers whose `startsOn` is null or on or before today UTC, and whose `endsOn` is null or on or after today UTC. Shop routes do not take these desk parameters. Updates use `PUT`. Deletes call `deleteById`.

| Service | Routes |
|---|---|
| Catalog | `GET /api/products`, `GET /api/products/lookup?ids=`, `GET /api/products/{id}`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` |
| Offers | `GET /api/offers`, `GET /api/offers/active`, `GET /api/offers/{id}`, `POST /api/offers`, `PUT /api/offers/{id}`, `DELETE /api/offers/{id}` |
| Customers | `GET /api/customers`, `GET /api/customers/{id}`, `POST /api/customers`, `PUT /api/customers/{id}`, `DELETE /api/customers/{id}`, `POST /api/auth/signup`, `POST /api/auth/signin`, `GET /api/auth/me` |
| Orders | `GET /api/orders`, `GET /api/orders/mine`, `GET /api/orders/{id}`, `POST /api/orders`, `PUT /api/orders/{id}`, `DELETE /api/orders/{id}` |
| Inventory | `GET /api/stock`, `PUT /api/stock/{sku}`, `POST /api/reservations` |

Offer writes require a non-empty `productIds` list. The service checks those ids with the catalog and rejects duplicates. Responses include `productIds` and product summaries. Sort for catalog, offers, and customers is `createdAt` descending. Orders sort by `placedAt` descending. Stock sorts by `updatedAt` descending and is keyed by SKU.

`PUT /api/orders/{id}` accepts `status` and `note`. `POST /api/reservations` accepts `orderRef` and `lines` of `{ sku, qty }`. Inventory sleeps for `FAULT_DELAY_MS` milliseconds before reserving when that value is greater than zero. The default is `0`.

Passwords are bcrypt hashed. `GET /api/auth/me` requires the bearer token. The token includes `role`. Signup sets `CUSTOMER`. The customers seed adds `desk@heddle.example` with `ADMIN` when that email is absent. The password is the local demo password already defined on `CustomerSeed`.

Desk management requires a bearer token whose `role` claim is `ADMIN`. No token is 401 with `Sign in to continue.` A token with another role is 403 with `An administrator must approve this request.` The desk routes are catalog list, get by id, create, update, and delete; offer list, create, update, and delete; customer list, get by id, create, update, and delete; order list, update, and delete. `GET /api/products/lookup`, `GET /api/offers/active`, `GET /api/offers/{id}`, signup, signin, `GET /api/auth/me`, `POST /api/orders`, and `GET /api/orders/mine` stay open to the callers they already have. `GET /api/orders/{id}` requires a bearer token. An administrator can read any order. A customer can read the order only when the token subject is that order's `customerId`. Another customer receives the same 404 as a missing order. A missing token is 401.

The investigation agent exposes `GET /health`, `GET /api/services`, and `POST /api/investigations` with `{ "service" }`. The graph probes `GET /actuator/health` on the five services and returns `service`, `signals`, `facts`, `hypotheses`, and `recommendations`. The module does not pin a listen port. It does not mutate services.

## Target contract

Still to build. Do not document these as present:

- An API gateway in front of the five services.
- Shop offer search and shop order dates applied on the server before the latest-100 limit. The desk lists already do this. Shop order dates, where the screen has them, still compare the browser's local calendar day.
- A payment step labeled as a simulation until a real provider is approved. Keep using `PUT` for order updates.
- Versioned migrations in place of `ddl-auto: update`.
- Investigation queries against the trace store. Services can export OTLP when `observability.traces.exporter=otlp`. Prometheus remains the metrics store.
- Read-only investigation tools `query_logs`, `query_metrics`, `query_traces`, `get_kubernetes_events`, and `get_deployment_history`, returning the report contract in CLAUDE.md.
- A root `.env.example` that lists local ports and placeholders. The Compose files remain the source for the ports below until that file exists.
- Amazon EKS, RDS, CloudWatch, and least-privilege IAM, validated separately from local Compose.

## Observability today

Each Java service exposes Actuator `health`, `info`, `prometheus`, and `metrics`, and depends on Micrometer. `CICD/Local/Prometheus/prometheus.yml` scrapes `host.docker.internal:8081` through `8085` at `/actuator/prometheus`. HTTP latency is `histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket[5m])))`. The error count is `http_server_requests_seconds_count` filtered by `status`. Pool size is `hikaricp_connections`. A test in each service reads `/actuator/prometheus` and requires those series.

Exporter choice is `observability.metrics.exporter`, `observability.logs.exporter`, and `observability.traces.exporter`, overridable with `OBSERVABILITY_METRICS_EXPORTER`, `OBSERVABILITY_LOGS_EXPORTER`, and `OBSERVABILITY_TRACES_EXPORTER`.

| Signal | Wired values | Reserved, startup fails if selected |
|---|---|---|
| Metrics | `prometheus`, `none` | `cloudwatch`, `azure-monitor` |
| Logs | `console`, `logstash` | `cloudwatch`, `azure-monitor` |
| Traces | `none`, `otlp` | `xray`, `azure-monitor` |

`logstash` activates the `elk` Spring profile. Logback then sends JSON to Logstash TCP `localhost:5044`. `console` does not. The ELK Compose project publishes Elasticsearch on `localhost:9200` and Kibana on `localhost:5601`.

Console and Logstash both write one JSON object per line. Each request stores `correlationId` and `service` on the log context and returns `X-Correlation-Id`. A supplied id is kept when it is 1–80 letters, digits, dots, underscores, or hyphens. Anything else, including a token, is replaced with a new id. The failure log does not include the `Authorization` header. Kibana can search `correlationId:"obs-002-failure"` once Logstash has indexed that line. This change did not open Kibana.

`otlp` turns on Micrometer tracing with the OpenTelemetry bridge and posts spans to `observability.traces.otlp-endpoint`, default `http://127.0.0.1:4318/v1/traces`. A purchase test records one trace: the orders server span is the parent of the offers and inventory client spans, each has a duration, and both calls send `traceparent`. An inventory HTTP 500 stays on that trace and the client span is an error. Local Jaeger all-in-one listens on the OTLP port and serves its UI on `http://localhost:16686`. This change did not open Jaeger. The same OTLP endpoint is the switch for a later collector. `none` leaves tracing off. `local:middleware:all:start` selects `logstash` when port 5044 is open and `otlp` when port 4318 is open, unless those variables are already set.

The investigation agent does not query Prometheus, Elasticsearch, or Jaeger.

## Pull request checks

`.github/workflows/ci.yaml` runs on pull requests and pushes to `develop` and `main`. The ruleset `Require CI checks` requires these checks on both branches. A failed or missing check blocks the merge. A passing check is enough.

| Check | Command |
|---|---|
| Middleware | Java 21, `mvn -B test` in `Middleware` |
| Investigation agent | Install requirements and import `app.main` |
| Portals (agentic-ai-admin) | `npm ci` and `npm run build` |
| Portals (ecommerce-portal) | `npm ci` and `npm run build` |

## Local operations

```bash
npm run local:containers:all:start
npm run local:containers:all:status
npm run local:containers:all:stop
```

`docker-all-up.sh` creates the external network `sre-incident-net` when it is missing, then starts Postgres, Redis, Prometheus, ELK, and Jaeger with Docker Compose v2. `docker-all-up.sh` and `docker-all-down.sh` use `set -euo pipefail` and fail if Docker or Compose is missing. `docker-all-status.sh` prints each stack and continues when a stack is down. Starting the containers does not start the Java processes, the agent, or the portals.

Default published ports: Postgres `5432`, Redis `6379`, Prometheus `9090`, Elasticsearch `9200`, Kibana `5601`, Logstash TCP `5044`, Jaeger UI `16686`, Jaeger OTLP `4318`.

## Delivery gaps

The commerce path and the health-only agent are in the tree. Still open: server-side filtered lists, desk and order-list authorization, order soft cancellation, idempotent purchase, a labeled payment simulation, investigation tools beyond health, k6, and any AWS or Azure deployment. Claim a gap closed only after the code and a run show it.
