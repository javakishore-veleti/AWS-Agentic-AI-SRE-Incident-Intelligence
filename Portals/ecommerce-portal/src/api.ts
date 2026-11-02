import type { Customer, Offer, Session, ShopOrder } from "./types"

const offers = import.meta.env.VITE_OFFERS_API ?? "http://localhost:8082"
const customers = import.meta.env.VITE_CUSTOMERS_API ?? "http://localhost:8083"
const orders = import.meta.env.VITE_ORDERS_API ?? "http://localhost:8084"
const sessionKey = "heddle.session"

export function readSession(): Session | null {
  localStorage.removeItem(sessionKey)
  const raw = sessionStorage.getItem(sessionKey)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function writeSession(session: Session | null) {
  localStorage.removeItem(sessionKey)
  if (session) sessionStorage.setItem(sessionKey, JSON.stringify(session))
  else sessionStorage.removeItem(sessionKey)
  window.dispatchEvent(new Event("heddle-session"))
}

async function api<T>(url: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json")
  const response = await fetch(url, { ...init, headers })
  const text = await response.text()
  let payload: unknown = null
  if (text) {
    try { payload = JSON.parse(text) } catch { payload = null }
  }
  if (!response.ok) {
    const message = payload && typeof payload === "object" && "message" in payload
      ? String((payload as { message: string }).message)
      : `Request failed (${response.status})`
    throw new Error(message)
  }
  return payload as T
}

export const loadActiveOffers = () => api<Offer[]>(`${offers}/api/offers/active`)
export const loadOffer = (id: string) => api<Offer>(`${offers}/api/offers/${id}`)

export function signup(body: { fullName: string; email: string; phone: string; password: string }) {
  return api<{ token: string; customer: Customer }>(`${customers}/api/auth/signup`, {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function signin(body: { email: string; password: string }) {
  return api<{ token: string; customer: Customer }>(`${customers}/api/auth/signin`, {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export function purchase(token: string, offerId: string, idempotencyKey: string) {
  return api<ShopOrder>(`${orders}/api/orders`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Idempotency-Key": idempotencyKey },
    body: JSON.stringify({ offerId }),
  })
}

export function loadMyOrders(token: string) {
  return api<ShopOrder[]>(`${orders}/api/orders/mine`, {
    headers: { Authorization: `Bearer ${token}` },
  })
}
