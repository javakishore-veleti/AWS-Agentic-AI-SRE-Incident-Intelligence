# Product design

Heddle is the commerce system this lab investigates. The desk and the shop must work as applications. Incident rules, evidence requirements, and the investigation report contract are in [CLAUDE.md](CLAUDE.md). Endpoints and service ownership are in [ARCHITECTURE.md](ARCHITECTURE.md).

The customer-facing name is Heddle. Sample rows use `heddle.example` mail, SKU prefix `HED`, and order numbers that start with `HD-`. Java packages remain `com.larkspur`.

## Personas

**Shopper.** Register, sign in, browse active offers and the products in each offer, purchase one offer, and review their own orders.

**Desk operator.** Maintain the catalog, offers, and customers. Inspect orders, change status and note, and remove a row with the current hard delete. The desk does not create orders.

**SRE.** Investigate a real incident from evidence. The agent recommends; a person approves any change. The desk has no incidents screen yet. That screen is a later section, separate from commerce.

## What the shop does today

Routes in `Portals/ecommerce-portal` (Vite on port 5174): `/offers`, `/offers/:id`, `/orders`, `/signin`, `/signup`.

- Sign up and sign in call the customers service. The shop stores the returned token in `localStorage` under `heddle.session`.
- The offer list loads `GET /api/offers/active`, then filters by offer name, product name, and maximum price in the browser.
- Offer detail shows the offer and its products. Purchase sends `POST /api/orders` with `Authorization: Bearer <token>` and `{ "offerId" }`. The client does not send a price, a customer id, or a quantity.
- Order history loads `GET /api/orders/mine` with the same bearer token. Date filters compare `placedAt` to the browser's local calendar day. Both the from day and the to day are inclusive. Pagination is client-side.

There is no payment step and no idempotency key. Submitting purchase again creates another order.

## What the desk does today

Routes in `Portals/agentic-ai-admin` (Vite on port 5173): `/overview`, `/products`, `/offers`, `/customers`, `/orders`.

| Screen | Writes | How the list works |
|---|---|---|
| Product catalog | Create, update, hard delete | `GET /api/products`, then browser filters and pages |
| Offers | Create, update, hard delete. The form requires one or more existing products (`productIds`) | `GET /api/offers`, then browser filters and pages |
| Customers | Create, update, hard delete | `GET /api/customers`, then browser filters and pages |
| Orders | Update status and note. Hard delete. No create action | `GET /api/orders`, then browser filters and pages |

Order filters include customer name, email, offer name, status, and a from/to range on `placedAt`. The range uses the local calendar day and includes both ends. The screens do not send those criteria to the API.

Empty, error, and confirmation states exist on the current forms. Result copy counts the filtered browser list. It does not mean the API applied the filter.

## Target behavior

These behaviors are the product contract. They are not what the screens do today.

- List APIs accept the filters, apply them, sort newest first, and return at most 100 matches. The browser paginates only that bounded set. Copy should say how many of those latest 100 matches are on screen. That is not global pagination. A later server-side page parameter can cover older history.
- Order dates filter on `placedAt`. Document the timezone. The start instant is inclusive and the end instant is exclusive (`placedAt >= from` and `placedAt < toExclusive`).
- Search is debounced, resettable, keyboard accessible, and reflected in the URL where that helps the operator return to the same view.
- Order removal becomes cancellation with an audit trail. Keep the financial record. `CANCELLED` already exists as a status; the current `DELETE` still erases the row.
- Checkout stays authenticated. The server continues to own identity and price. Add an idempotency key so a repeated purchase does not create a second order. Add a payment step only as a clearly labeled simulated test integration until a real provider is approved.
- A shopper can read only their own orders on every order read they can call.
- Offers stay sets of products. Reject a save that references a missing product.
- An incidents view can join the desk later. It shows investigations and evidence. It does not invent metrics.

## Design system

One brand, two visual states. The target palette is the next UI pass. The spruce and coral treatment is what the portals render now. Do not describe the current sidebar as navy or the current actions as azure.

**Target.** Ivory canvas `#FCFBF7`, white surfaces `#FFFFFF`, deep navy text `#142B4A`, azure primary `#1769AA`, teal accent `#087F78`, amber attention `#D99226`. Neutral borders are fine. The canvas stays light. Avoid a gray or black dominant admin theme, oversized gradients, and decorative charts.

**Current Heddle portals.** Warm ivory canvas (`#fff6ee` on the desk, `#fff3ea` on the shop), spruce text `#1a3c34`, and a deep spruce desk sidebar (`#18483d` to `#0f312b`). Actions and the active nav item use a coral-to-rose gradient (`#f07832` to `#e4547c`). Display type is Fraunces; UI type is Outfit. The shop is a light storefront with a narrow rail, not a copy of the desk sidebar.

Shared craft for both portals: clear page hierarchy, readable tables, responsive navigation, visible focus, semantic labels, and feedback for loading, empty, error, success, and destructive confirmation. Connected screens show data from the APIs. Sample catalog rows are seed data, not live sales.

## Records

- **Product.** `id`, unique `sku`, `name`, `description`, `category`, `unitPrice`, `status` (`ACTIVE` or `DISCONTINUED`), `createdAt`, `updatedAt`. The shop formats amounts as USD. There is no currency column.
- **Offer.** `id`, unique `code`, `name`, `description`, `offerPrice`, `status` (`DRAFT`, `ACTIVE`, `EXPIRED`), `startsOn`, `endsOn`, `productIds` (required, one or many), `createdAt`, `updatedAt`. Detail responses also include the matching product summaries.
- **Customer.** `id`, `fullName`, unique `email`, `phone`, `status`, timestamps. The service stores a password hash and does not return it.
- **Order.** `id`, `orderNumber`, customer snapshot, `offerId`, `offerName`, `totalAmount`, `status` (`PLACED`, `CONFIRMED`, `FULFILLED`, `CANCELLED`), `note`, line snapshots, `placedAt`, `updatedAt`. Each line has `productId`, `sku`, `productName`, `quantity`, and `unitPrice`. Purchase currently writes quantity `1` for every product in the offer and sets the total from `offerPrice`.
- **Inventory.** Stock is keyed by SKU, with available and reserved quantities. Reservation is part of purchase, as described in ARCHITECTURE.md.

## Acceptance

A shopper can sign up, sign in, open an active offer, purchase it, and see that order. A desk operator can find the same order and change it, and cannot create one. Closing the target list, cancellation, idempotency, and payment gaps above is separate work. Incident acceptance is the definition of done in CLAUDE.md.
