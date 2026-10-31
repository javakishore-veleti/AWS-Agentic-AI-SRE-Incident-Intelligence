# Product Design — AWS Agentic AI SRE Incident Intelligence

## Purpose
Build a credible, working e-commerce system whose microservices produce realistic telemetry and controlled incidents for an Agentic AI SRE investigation platform. The two portals must be useful as applications in their own right; the SRE workflow must diagnose real failures, not scripted answers.

## Personas and workflows
**Admin:** Manage Product Catalog, Offers (each tied to a product), Customers and Orders. Orders originate exclusively from customer checkout. Admin can inspect, update allowed order fields/statuses, and cancel/soft-delete with audit history; cannot create orders.

**Customer:** Register, authenticate, search active offers and see the underlying product, select an offer, complete simulated checkout, and view only their own orders.

**SRE:** Inspect a real incident, evidence, telemetry, hypotheses, diagnosis and human-reviewed recovery recommendations. Never expose raw production-changing controls to the agent.

## Admin navigation and screens
| Menu | Operations | Search/filter | Default listing |
|---|---|---|---|
| Product Catalog | Create, read, update, delete/archive | Name, SKU, category, status | Newest 100 matching |
| Offers | Create, read, update, delete/deactivate; map to product | Offer name, product/SKU, status, validity | Newest 100 matching |
| Customers | Create, read, update, deactivate/delete subject to retention | Name, email, status | Newest 100 matching |
| Orders | Read, update allowed status, cancel/soft-delete | Order ID, customer, status, created-from/to dates | Newest 100 matching |
| SRE Incidents (later) | Read investigations and evidence | Service, severity, state, time | Newest 100 matching |

**List semantics:** Every filter change requests up to 100 matching rows from the API ordered by `createdAt DESC, id DESC`. Client-side pagination applies **only to those 100 rows** (e.g. 10/20/25 per page). UI must say “Showing X of the latest 100 matching records” and never imply there are only 100 globally. Backend validates filter input and applies filters *before* the limit. Server-side pagination is a future enhancement for full history. Use ISO 8601 timestamps; orders date range uses `createdAt >= from` and `createdAt < toExclusive`, converted from selected local calendar dates in a documented timezone. Search must be debounced, resettable, accessible and reflected in the URL where appropriate.

## Customer portal
- Sign up with validated name, email and password; securely hash password on server.
- Sign in with secure session/token handling, rate limiting and appropriate logout/expiry.
- Browse searchable active offers, displaying product title, description, image, availability, offer price, validity and terms.
- Checkout uses authenticated customer identity, server-calculated pricing, inventory validation/reservation and an idempotency key. A simulated payment is explicitly marked as such.
- Order creation happens in the Order API only as a consequence of customer checkout. Snapshot purchased offer/product names, prices and quantities; provide order confirmation and order history.
- Never allow users to access another customer's data by changing request parameters.

## Design system — professional, not academic
- **Light, premium enterprise look**: ivory `#FCFBF7` canvas, white `#FFFFFF` surfaces, deep navy `#142B4A` text, azure `#1769AA` primary, teal `#087F78` success/action accent, amber `#D99226` attention. No gray/black-dominant UI; neutral borders are permitted for clarity.
- Desktop: clean left navigation in admin, top utility bar, informative page headers, aligned filters and dense but readable data tables. Mobile: collapsible navigation and responsive cards/tables.
- E-commerce: polished product imagery, clear offer pricing and checkout progress; consistent brand with admin but distinct shopper navigation.
- Typography: modern system font or licensed web-safe equivalent; accessible focus indicators, semantic labels, AA contrast, keyboard navigation.
- Every screen: skeleton/loading, error/retry, empty/no-results, success confirmation, destructive-action confirmation, validation messages and toast feedback.
- No fabricated KPIs, fake orders or decorative data in connected views. Seed data must be labeled as sample data.

## Core data concepts
- **Product**: id, sku (unique), name, description, category, price/basePrice, currency, status, createdAt, updatedAt.
- **Offer**: id, productId (required), title, offerPrice, currency, validFrom, validUntil, status, createdAt, updatedAt. Validate product exists and price/validity rules.
- **Customer**: id, name, email (unique), passwordHash or external identity reference, status, createdAt, updatedAt. Never expose passwordHash.
- **Order**: id, customerId, status, total, currency, createdAt, updatedAt, immutable purchased item snapshots and audit trail. Checkout creates; admin can perform only permitted transitions.
- **Inventory**: productId, available/reserved quantities, version; prevent oversell with atomic operations.

## Definition of done
A real user can sign up, sign in, search offers, purchase using the clearly marked test checkout, see their order, and an admin can inspect/update that order without being able to create one. All admin lists support the latest-100 filtered/paginated contract. A controlled service fault appears in observability and is investigated using actual evidence. Automated tests cover auth, data ownership, validation, purchase idempotency, status transitions, filters, date boundaries, and telemetry correlation.
