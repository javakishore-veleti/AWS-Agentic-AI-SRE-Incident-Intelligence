# REL-001 plan

1. Add `RESERVING` and `RESERVATION_FAILED`, plus the idempotency key, on the order.
2. Commit the order before calling inventory. Keep it on a definite failure. Leave it reserving when inventory cannot be reached.
3. Return the original order for the same customer and key.
4. Remember a reservation by order reference so a repeat does not decrement stock again.
5. Send `Idempotency-Key` from the shop, and reuse it for one unreachable retry.
6. Cover a repeated success, a repeated conflict, an unreachable call followed by a successful retry, and a repeated reservation.
