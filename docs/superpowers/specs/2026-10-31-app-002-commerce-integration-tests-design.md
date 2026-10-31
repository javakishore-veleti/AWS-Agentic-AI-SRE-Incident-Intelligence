# APP-002 commerce service tests

Approved in chat on 31 October 2026 for GitHub issue #2.

## Goal

Prove signup, signin, offer search, checkout, inventory reservation, and order history, plus every error status the five Java services already return. Each service runs these tests in its own Maven test phase against H2. Downstream HTTP is stubbed. The local Compose stack stays down.

## Harness

`Middleware/services-test-common` is a plain jar on the `middleware` reactor. It does not use the Spring Boot repackage plugin. It does not depend on the five services. Each service depends on it with Maven `test` scope, so H2, the mock server, embedded Redis, and the Kafka test library stay out of the runtime image.

The jar compiles against:

- `spring-boot-starter-test`, which supplies JUnit, Mockito, MockMvc, and `MockRestServiceServer`
- H2
- the jjwt artifacts already in the parent `dependencyManagement`
- `com.github.codemonstur:embedded-redis`, version pinned in the parent `dependencyManagement` to a Java 21 release
- `spring-kafka-test`, version taken from the Spring Boot 3.4.5 BOM

`@CommerceServiceTest` starts the service with MockMvc and H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`) and `ddl-auto: update`. Logs stay on the console. Traces stay off. The test does not open Postgres, Logstash, or Jaeger. `FAULT_DELAY_MS` is `0`. The JWT HMAC key is a test-only value of at least 32 bytes in `src/test/resources`.

Production `RestClient` `@Bean` methods stay as they are. They build the client inside the method and do not expose the builder. In the test application context, `DownstreamClientOverrides` replaces a bean only when that bean name exists, using a client built from `MockRestServiceServer.bindTo(RestClient.builder())`. The names are `offersClient`, `inventoryClient`, `catalogRestClient`, and `inventoryRestClient`. Customers and inventory define none of those beans, so their contexts are unchanged. `DownstreamServers` returns the mock server for a bean name.

`Datasets` builds shopper, product, offer, and stock payloads as records owned by the test jar. `ShopperTokens` signs a Bearer token with the test JWT secret. `EmbeddedRedis` starts an in-process Redis on a random port and points `spring.data.redis` at it. Only offers tests enable it. `EmbeddedKafkaSupport` can start an in-memory broker. No APP-002 test enables it, because none of the five services has a Kafka client.

## Tests

| Test | Service | Owns |
|---|---|---|
| `CustomerAccessTest` | customers | Signup, signin, `GET /api/auth/me`, and that service's error responses |
| `ProductApiTest` | catalog | Product create, read, update, delete, validation, and the inventory stock-sync call |
| `OfferApiTest` | offers | Offer create and read, `GET /api/offers`, `GET /api/offers/active`, catalog lookup, and the Redis cache |
| `PurchaseHistoryTest` | orders | Purchase, `GET /api/orders/mine`, and that service's error responses |
| `ReservationTest` | inventory | Stock upsert and `POST /api/reservations` |

Advice-only cases live beside those classes and call the package-visible advice directly. They do not start a Spring context. `services-test-common` has no Spring test of its own.

## Data flow

MockMvc sends HTTP into the service under test. Rows go to that service's H2 database. A call to another service stops at `MockRestServiceServer`. Offers tests write `catalog:product:{id}` to the in-process Redis with a 60-second TTL.

Signup stores a bcrypt password. Signin returns a JWT. `GET /api/auth/me` with that token returns the same shopper.

`POST /api/products` saves the product and then calls `PUT /api/stock/{sku}` with the product name. `ProductApiTest` asserts that call on the inventory stub. Quantity changes are asserted in `ReservationTest`.

Creating an offer looks up each product id. A cache miss calls `GET /api/products/lookup?ids=` and stores the Redis key. The next read uses Redis, and the catalog stub receives no second request. `GET /api/offers` returns the latest 100 offers by `createdAt`. `GET /api/offers/active` keeps, from that list, `ACTIVE` offers whose `startsOn` is null or on or before today UTC and whose `endsOn` is null or on or after today UTC.

`PurchaseHistoryTest` signs its own bearer token. `POST /api/orders` with `{ "offerId" }` loads `GET /api/offers/{id}`. The stub returns an `ACTIVE` offer and its products. Orders saves an `HD-` order in `PLACED` status, copies customer id, name, and email from the token, sets `totalAmount` from `offerPrice`, and snapshots each product as a line with quantity 1. It then `POST /api/reservations` with `orderRef` set to the order number. `GET /api/orders/mine` with the same token returns that order. A token for a different customer id returns an empty list.

`PUT /api/stock/{sku}` stores the sku in upper case. A missing `availableQty` stores 100. `POST /api/reservations` subtracts the line quantity from `availableQty` and adds it to `reservedQty`.

Each test creates its own email, SKU, and offer code. The duplicate-email, duplicate-SKU, and duplicate-code tests are the ones that reuse a key.

## Error handling

Error bodies are `{ "message": "..." }`. A MockMvc test asserts the status and the message. Bean-validation failures use the advice format `field + " " + defaultMessage` for the first field error.

These handlers have no request that throws them, so the test calls the advice with the exception:

- each service's generic 500 advice
- the invalid-value 400 advice on catalog, offers, and orders (`A field has an invalid value.`)
- inventory's duplicate-stock 409 (`That stock record already exists.`)

Customers has no invalid-value advice. Orders has no duplicate-key advice.

| Service | Status | Message | How it is triggered |
|---|---|---|---|
| Customers | 401 | `Email or password is incorrect.` | Wrong email or password |
| Customers | 403 | `This account is suspended.` | Right password, suspended account |
| Customers | 401 | `Sign in to continue.` | Missing or invalid token on `GET /api/auth/me` |
| Customers | 404 | `Customer not found.` | Unknown customer id |
| Customers | 409 | `An account with this email already exists.` | Second signup with the same email |
| Customers | 400 | field validation message | Signup password shorter than 8 characters |
| Customers | 400 | `password must be at least 8 characters.` | `POST /api/customers` when the password is shorter than 8 characters |
| Customers | 500 | `Customers could not complete that request.` | Advice call |
| Catalog | 404 | `Product not found.` | Unknown product |
| Catalog | 409 | `A product with that SKU already exists.` | Second product with the same SKU |
| Catalog | 400 | field validation message | Blank or oversized field |
| Catalog | 400 | `A field has an invalid value.` | Advice call |
| Catalog | 500 | `Product catalog could not complete that request.` | Advice call |
| Catalog | 201 | product body | Inventory stub fails `PUT /api/stock/{sku}`; the product is still created |
| Offers | 404 | `Offer not found.` | Unknown offer |
| Offers | 400 | `The end date is before the start date.` | End date before start date |
| Offers | 400 | `Choose each product once.` | Repeated product id |
| Offers | 400 | `One or more products are not in the catalog.` | Catalog stub omits a requested id |
| Offers | 502 | `Product catalog is unreachable.` | Catalog connection failure on create or update |
| Offers | 200 | offer, empty product summary | Same catalog failure on a list |
| Offers | 409 | `An offer with that code already exists.` | Second offer with the same code |
| Offers | 400 | `A field has an invalid value.` | Advice call |
| Offers | 500 | `Offers could not complete that request.` | Advice call |
| Orders | 401 | `Sign in to purchase an offer.` | Missing or invalid token |
| Orders | 404 | `Offer not found.` | Offers stub returns 404 |
| Orders | 502 | `Offers service rejected the lookup.` | Offers stub returns another HTTP error |
| Orders | 502 | `Offers service returned an empty response.` | Offers stub returns an empty body |
| Orders | 502 | `Offers service is unreachable.` | Offers stub drops the connection |
| Orders | 409 | `This offer is not available to purchase.` | Inactive offer; no order row is saved |
| Orders | 409 | `This offer does not include any products.` | Offer with no products; no order row is saved |
| Orders | 409 | JSON `message` from the inventory body, otherwise `Inventory could not reserve this offer.` | Inventory stub returns 409; the new order row is deleted |
| Orders | 502 | `Inventory service is unreachable.` | Inventory stub drops the connection; the new order row is deleted |
| Orders | 404 | `Order not found.` | Unknown order id |
| Orders | 400 | `A field has an invalid value.` | Advice call |
| Orders | 500 | `Orders could not complete that request.` | Advice call |
| Inventory | 409 | `No stock record for {SKU}.` | Reservation for a missing sku; `{SKU}` is upper case |
| Inventory | 409 | `{product name} does not have enough stock.` | Quantity above `availableQty` |
| Inventory | 400 | field validation message | Blank product name, blank sku, quantity below 1, or an empty line list |
| Inventory | 409 | `That stock record already exists.` | Advice call |
| Inventory | 500 | `Inventory could not complete that request.` | Advice call |

After either inventory failure, `PurchaseHistoryTest` calls `GET /api/orders` and asserts that the new order number is absent.

## Pass

From `Middleware`, `mvn test` exits 0. Surefire runs the five test classes above and the existing observability unit test. This document does not claim that command has been run. The command and its output are the issue evidence when implementation runs.

## Out of scope

Server-side filtering before the latest-100 limit is APP-003. WireMock is not used. The tests do not start the other four services, the portals, or the local Compose stack. No APP-002 test starts the Kafka broker.
