import type { Customer, Offer, Product, ShopOrder } from "./types"

const catalog = import.meta.env.VITE_CATALOG_API ?? "http://localhost:8081"
const offers = import.meta.env.VITE_OFFERS_API ?? "http://localhost:8082"
const customers = import.meta.env.VITE_CUSTOMERS_API ?? "http://localhost:8083"
const orders = import.meta.env.VITE_ORDERS_API ?? "http://localhost:8084"

export async function api<T>(url: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json")
  }
  const response = await fetch(url, { ...init, headers })
  if (response.status === 204) {
    return undefined as T
  }
  const text = await response.text()
  let payload: unknown = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch {
      payload = null
    }
  }
  if (!response.ok) {
    const message = payload && typeof payload === "object" && "message" in payload
      ? String((payload as { message: string }).message)
      : `Request failed (${response.status})`
    throw new Error(message)
  }
  return payload as T
}

export const loadProducts = () => api<Product[]>(`${catalog}/api/products`)
export const saveProduct = (body: unknown, id?: string) =>
  api<Product>(id ? `${catalog}/api/products/${id}` : `${catalog}/api/products`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(body),
  })
export const removeProduct = (id: string) => api<void>(`${catalog}/api/products/${id}`, { method: "DELETE" })

export const loadOffers = () => api<Offer[]>(`${offers}/api/offers`)
export const saveOffer = (body: unknown, id?: string) =>
  api<Offer>(id ? `${offers}/api/offers/${id}` : `${offers}/api/offers`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(body),
  })
export const removeOffer = (id: string) => api<void>(`${offers}/api/offers/${id}`, { method: "DELETE" })

export const loadCustomers = () => api<Customer[]>(`${customers}/api/customers`)
export const saveCustomer = (body: unknown, id?: string) =>
  api<Customer>(id ? `${customers}/api/customers/${id}` : `${customers}/api/customers`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(body),
  })
export const removeCustomer = (id: string) => api<void>(`${customers}/api/customers/${id}`, { method: "DELETE" })

export const loadOrders = () => api<ShopOrder[]>(`${orders}/api/orders`)
export const saveOrder = (id: string, body: unknown) =>
  api<ShopOrder>(`${orders}/api/orders/${id}`, { method: "PUT", body: JSON.stringify(body) })
export const removeOrder = (id: string) => api<void>(`${orders}/api/orders/${id}`, { method: "DELETE" })
