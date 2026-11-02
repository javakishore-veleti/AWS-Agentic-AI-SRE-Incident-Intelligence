# SEC-002 order ownership

Approved for GitHub issue #6. The user asked to take the design decisions and continue Milestone 2 without a further stop.

## Goal

`GET /api/orders/{id}` is readable by the customer who owns the order, or by an administrator. Another customer cannot read it.

## Rule

A missing token is 401, `Sign in to continue.` An administrator token reads any order. A customer token reads the order only when the token subject equals `customerId`. Any other customer receives 404, `Order not found.`, the same response as a missing order.

Desk list, update, and delete already require `ADMIN` from SEC-001. A customer token on those routes stays 403.
