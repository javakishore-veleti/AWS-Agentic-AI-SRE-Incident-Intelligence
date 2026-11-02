# REL-002 order cancellation

Issue #9. Cancelling an order keeps the row and records who cancelled it.

## Decision

`DELETE /api/orders/{id}` stays the administrator action. It no longer removes the row. The status becomes `CANCELLED`. One `order_cancellations` row stores the order, the previous status, the administrator id and email, and the time. A second cancellation finds that row and does nothing else.

Before the status changes, orders call `POST /api/reservations/release` with the order number. Inventory returns the reserved quantity to available and forgets that reservation. A release for an order that has no reservation succeeds without changing stock. If inventory cannot be reached, the order stays in its current status and no audit row is written.

A customer token is still forbidden. The desk button says Cancel and tells the operator the order stays on record.
