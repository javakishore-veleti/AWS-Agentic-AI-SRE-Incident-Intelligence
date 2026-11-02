# REL-001 idempotent checkout

Issue #8. A repeated purchase uses the same idempotency key and does not create a second order or a second reservation.

## Decision

`POST /api/orders` requires `Idempotency-Key`. The key is unique for one customer. The order row is committed as `RESERVING` before inventory is called.

A successful reservation moves that row to `PLACED` and returns 201. The same key then returns 200 and the same order. A 409 from inventory keeps the row as `RESERVATION_FAILED` and later calls with that key return the same conflict. A timeout or an unreachable inventory call leaves `RESERVING`. The next call with that key tries the reservation again.

Inventory stores the reservation under `orderRef`. The same reference and the same lines return success without changing stock. A different set of lines for that reference is a conflict.

The shop creates one key per click. The button stays disabled while the request runs. If inventory is unreachable, the shop sends that same key once more. There is no payment call.
