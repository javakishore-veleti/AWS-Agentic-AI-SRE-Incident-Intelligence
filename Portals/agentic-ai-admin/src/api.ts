import type { Customer, Offer, Product, ShopOrder } from "./types"

const catalog = import.meta.env.VITE_CATALOG_API ?? "http://localhost:8081"
const offers = import.meta.env.VITE_OFFERS_API ?? "http://localhost:8082"
const customers = import.meta.env.VITE_CUSTOMERS_API ?? "http://localhost:8083"
const orders = import.meta.env.VITE_ORDERS_API ?? "http://localhost:8084"
const deskKey = "heddle.desk"

export type DeskSession = { token: string; name: string; email: string }

export function readDesk(): DeskSession | null {
  const raw = sessionStorage.getItem(deskKey)
  if (!raw) return null
  try {
    return JSON.parse(raw) as DeskSession
  } catch {
    return null
  }
}

export function writeDesk(session: DeskSession | null) {
  if (session) sessionStorage.setItem(deskKey, JSON.stringify(session))
  else sessionStorage.removeItem(deskKey)
}

export async function signInDesk(email: string, password: string): Promise<DeskSession> {
  const result = await api<{ token: string; customer: { role: string; fullName: string; email: string } }>(
    `${customers}/api/auth/signin`,
    { method: "POST", body: JSON.stringify({ email, password }) },
  )
  if (result.customer.role !== "ADMIN") {
    throw new Error("This desk is for administrators.")
  }
  const session = { token: result.token, name: result.customer.fullName, email: result.customer.email }
  writeDesk(session)
  return session
}

export async function api<T>(url: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  const desk = readDesk()
  if (desk && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${desk.token}`)
  }
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

function query(params: Record<string, string>) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value.trim()) search.set(key, value.trim())
  }
  const text = search.toString()
  return text ? `?${text}` : ""
}

export const loadProducts = (criteria: { name: string; sku: string; category: string; status: string } = { name: "", sku: "", category: "", status: "" }) =>
  api<Product[]>(`${catalog}/api/products${query(criteria)}`)
export const saveProduct = (body: unknown, id?: string) =>
  api<Product>(id ? `${catalog}/api/products/${id}` : `${catalog}/api/products`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(body),
  })
export const removeProduct = (id: string) => api<void>(`${catalog}/api/products/${id}`, { method: "DELETE" })

export const loadOffers = (criteria: { name: string; code: string; product: string; status: string } = { name: "", code: "", product: "", status: "" }) =>
  api<Offer[]>(`${offers}/api/offers${query(criteria)}`)
export const saveOffer = (body: unknown, id?: string) =>
  api<Offer>(id ? `${offers}/api/offers/${id}` : `${offers}/api/offers`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(body),
  })
export const removeOffer = (id: string) => api<void>(`${offers}/api/offers/${id}`, { method: "DELETE" })

export const loadCustomers = (criteria: { name: string; email: string; status: string } = { name: "", email: "", status: "" }) =>
  api<Customer[]>(`${customers}/api/customers${query(criteria)}`)
export const saveCustomer = (body: unknown, id?: string) =>
  api<Customer>(id ? `${customers}/api/customers/${id}` : `${customers}/api/customers`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(body),
  })
export const removeCustomer = (id: string) => api<void>(`${customers}/api/customers/${id}`, { method: "DELETE" })

export const loadOrders = (criteria: { customer: string; email: string; offer: string; status: string; from: string; to: string } = { customer: "", email: "", offer: "", status: "", from: "", to: "" }) =>
  api<ShopOrder[]>(`${orders}/api/orders${query(criteria)}`)
export const saveOrder = (id: string, body: unknown) =>
  api<ShopOrder>(`${orders}/api/orders/${id}`, { method: "PUT", body: JSON.stringify(body) })
export const removeOrder = (id: string) => api<void>(`${orders}/api/orders/${id}`, { method: "DELETE" })
