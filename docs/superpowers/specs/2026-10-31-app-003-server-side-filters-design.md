# APP-003 server-side list filters

Approved for GitHub issue #3. The user asked to run the full APP-003 command sequence without a further design stop.

## Goal

The desk search and the order date range are applied on the server before the latest-100 limit. An older row that matches the criteria is returned even when it is outside the unfiltered latest 100. The desk paginates that bounded response.

## API

Blank or missing text means no filter. Text is a case-insensitive contains match. Status is an exact enum value. An unknown status or an unparseable date is 400. A call with no parameters still returns the latest 100. Sort stays newest first.

| Route | Parameters |
|---|---|
| `GET /api/products` | `name`, `sku`, `category`, `status` |
| `GET /api/offers` | `name`, `code`, `product`, `status` |
| `GET /api/customers` | `name`, `email`, `status` |
| `GET /api/orders` | `customer`, `email`, `offer`, `status`, `from`, `to` |

`product` matches a catalog product name or SKU on the offer. That match happens after the catalog lookup, then the list is limited to 100. Name, code, and status are applied in the query first, up to 1000 rows, so a product-text search can see past the unfiltered latest 100.

`from` and `to` are inclusive UTC calendar days. `placedAt >=` the start of `from`, and `placedAt <` the start of the day after `to`.

Shop routes stay as they are: `GET /api/offers/active` and `GET /api/orders/mine` do not gain these parameters.

## Desk

Products, offers, customers, and orders send the current criteria on each change and page the returned array. They do not filter that array again in the browser.
