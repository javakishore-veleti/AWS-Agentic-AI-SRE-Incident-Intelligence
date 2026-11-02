# SEC-001 Admin Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. The user chose inline execution by asking to take decisions and proceed.

**Goal:** Deny desk management calls unless the bearer token role is ADMIN.

**Architecture:** Customers issue a `role` claim. Catalog, offers, customers, and orders check that claim on the desk routes named in the spec. The desk stores an admin session and sends the token.

**Tech Stack:** Java 21, Spring Boot 3.4, jjwt 0.12.6, React desk on Vite.

**Spec:** `docs/superpowers/specs/2026-11-01-sec-001-admin-authorization-design.md`

## Global Constraints

- 401 message is `Sign in to continue.`
- 403 message is `An administrator must approve this request.`
- Shop and service-to-service routes named in the spec stay open.
- Do not copy the local demo password into a new document.
- `GET /api/orders/{id}` is out of scope.

---

### Task 1: Role claim and desk gate

**Files:**
- Modify: `Middleware/customers-service/src/main/java/com/larkspur/customers/Customer.java`
- Modify: `Middleware/customers-service/src/main/java/com/larkspur/customers/CustomerApi.java`
- Modify: `Middleware/customers-service/src/main/java/com/larkspur/customers/CustomerSeed.java`
- Modify: `Middleware/services-test-common/src/main/java/com/larkspur/testing/ShopperTokens.java`
- Create: `Middleware/product-catalog-service/src/main/java/com/larkspur/catalog/AdminGate.java`
- Create: `Middleware/offers-service/src/main/java/com/larkspur/offers/AdminGate.java`
- Modify: catalog and offers controllers, poms, and `application.yml`
- Modify: `Middleware/orders-service/src/main/java/com/larkspur/orders/OrderApi.java`
- Test: `AdminDeskTest` in catalog, offers, customers, and orders

**Interfaces:**
- Produces: `ShopperTokens.admin()` returns `Bearer ` plus a token whose `role` is `ADMIN`. `ShopperTokens.bearer` sets `role` to `CUSTOMER`.
- Produces: `AdminGate.require(String header)` throws 401 or 403.

### Task 2: Existing tests send the admin token

Add `ShopperTokens.admin()` only on the desk calls listed in the spec. Leave auth, lookup, active offers, offer get-by-id, purchase, and mine unchanged.

### Task 3: Desk sign-in

**Files:**
- Modify: `Portals/agentic-ai-admin/src/api.ts`
- Modify: `Portals/agentic-ai-admin/src/App.tsx`
- Modify: `Portals/agentic-ai-admin/src/types.ts`
- Modify: `ARCHITECTURE.md`, `DESIGN.md`, `CURRENT_STATE_AND_GAPS.md`

The desk rejects a non-admin sign-in and sends the stored token on later calls.

### Task 4: Verify

Run `mvn -B test` in `Middleware` with Java 21, then `npm run build` in `Portals/agentic-ai-admin`.
