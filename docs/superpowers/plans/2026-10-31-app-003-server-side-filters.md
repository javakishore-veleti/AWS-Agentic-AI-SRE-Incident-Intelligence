# APP-003 Server-Side Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Apply desk search and order dates on the server before the latest-100 limit, and page that response in the desk.

**Architecture:** Each list repository query filters, sorts newest first, and limits to 100. Offer product text is applied after catalog lookup. The desk sends the same field names as query parameters.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Data JPA, H2 tests, Vite desk.

---

### Task 1: Catalog, customers, offers, and orders

**Files:**
- `Middleware/product-catalog-service/src/main/java/com/larkspur/catalog/ProductRepository.java`
- `Middleware/product-catalog-service/src/main/java/com/larkspur/catalog/ProductService.java`
- `Middleware/product-catalog-service/src/main/java/com/larkspur/catalog/ProductApi.java`
- `Middleware/customers-service/src/main/java/com/larkspur/customers/CustomerApi.java`
- `Middleware/offers-service/src/main/java/com/larkspur/offers/OfferRepository.java`
- `Middleware/offers-service/src/main/java/com/larkspur/offers/OfferApi.java`
- `Middleware/orders-service/src/main/java/com/larkspur/orders/OrderApi.java`
- Test classes `ProductSearchTest`, `CustomerSearchTest`, `OfferSearchTest`, `OrderSearchTest`

**Steps:**
1. Add a search query for each list. Keep a no-parameter call as the latest 100.
2. Write a test that saves one old matching row and 100 newer non-matching rows. The unfiltered list omits the old row. The filtered list returns it.
3. Assert unknown status is 400, and an unparseable order date is 400.
4. Run `mvn -B -pl product-catalog-service,customers-service,offers-service,orders-service test` from `Middleware` with Temurin 21.

### Task 2: Desk

**Files:**
- `Portals/agentic-ai-admin/src/api.ts`
- `Portals/agentic-ai-admin/src/pages/ProductsPage.tsx`
- `Portals/agentic-ai-admin/src/pages/OffersPage.tsx`
- `Portals/agentic-ai-admin/src/pages/CustomersPage.tsx`
- `Portals/agentic-ai-admin/src/pages/OrdersPage.tsx`

**Steps:**
1. Add the criteria to each load function as a query string. Omit blank values.
2. Reload the list when the criteria change. Page `rows`, not a second browser filter.
3. Run `npm run build` in `Portals/agentic-ai-admin`.

### Task 3: Docs

**Files:** `ARCHITECTURE.md`, `DESIGN.md`

Update the desk list contract to the behavior above. Leave shop offer search and shop order dates as they are today.
