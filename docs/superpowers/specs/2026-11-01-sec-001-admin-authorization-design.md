# SEC-001 admin authorization

Approved for GitHub issue #5. The user asked to take the design decisions and run the Milestone 2 sequence without a further stop.

## Goal

A desk request that changes or lists products, offers, customers, or orders is denied unless it carries a bearer token whose `role` claim is `ADMIN`. No token is 401. A customer token is 403.

## Token

The customers service already issues the shopper token. That token gains `role`. Signup stores `CUSTOMER`. The customers seed adds `desk@heddle.example` with `ADMIN` when that email is absent. The password is the local demo password already defined on `CustomerSeed`. A desk create or update cannot change `role`.

## Routes that require ADMIN

| Service | Routes |
|---|---|
| Catalog | `GET /api/products`, `GET /api/products/{id}`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` |
| Offers | `GET /api/offers`, `POST /api/offers`, `PUT /api/offers/{id}`, `DELETE /api/offers/{id}` |
| Customers | `GET /api/customers`, `GET /api/customers/{id}`, `POST /api/customers`, `PUT /api/customers/{id}`, `DELETE /api/customers/{id}` |
| Orders | `GET /api/orders`, `PUT /api/orders/{id}`, `DELETE /api/orders/{id}` |

These stay as they are: `GET /api/products/lookup`, `GET /api/offers/active`, `GET /api/offers/{id}`, signup, signin, `GET /api/auth/me`, `POST /api/orders`, and `GET /api/orders/mine`. `GET /api/orders/{id}` stays for SEC-002.

401 message: `Sign in to continue.` 403 message: `An administrator must approve this request.`

## Desk

The desk signs in through `POST /api/auth/signin`. A response whose role is not `ADMIN` is rejected on the page. The token is kept in `sessionStorage` under `heddle.desk` and sent as `Authorization` on desk calls.
