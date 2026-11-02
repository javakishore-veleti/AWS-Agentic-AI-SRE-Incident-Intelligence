# SEC-002 Order Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. The user chose inline execution.

**Goal:** Stop one customer from reading another customer's order.

**Architecture:** `GET /api/orders/{id}` checks the bearer token. Admin reads any order. A customer reads only a matching `customerId`. Everyone else gets the existing not-found response.

**Tech Stack:** Java 21, Spring Boot, the existing order JWT parser.

**Spec:** `docs/superpowers/specs/2026-11-01-sec-002-order-ownership-design.md`

## Global Constraints

- 401 message is `Sign in to continue.`
- Another customer and a missing order both return 404 `Order not found.`
- Desk list, update, and delete stay admin-only.

### Task 1: Owner check and test

Modify `OrderApi.get` to take the authorization header. Add `OrderOwnershipTest`. Update the unknown-order test to send an admin token. Record the rule in `ARCHITECTURE.md`.

Run: `mvn -B -pl orders-service -am test`
Expected: BUILD SUCCESS, failures 0.
