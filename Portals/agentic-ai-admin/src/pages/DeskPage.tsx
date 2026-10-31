import { useEffect, useState } from "react"
import { api, catalogApi, customersApi, offersApi, ordersApi } from "../api"
import { useDesk } from "../App"
import { includes, money, when } from "../format"
import type { Customer, Offer, Product, ShopOrder } from "../types"
import { Pill } from "../widgets"

export function DeskPage() {
  const { query } = useDesk()
  const [products, setProducts] = useState<Product[]>([])
  const [offers, setOffers] = useState<Offer[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [orders, setOrders] = useState<ShopOrder[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    Promise.all([
      api<Product[]>(`${catalogApi}/api/products`),
      api<Offer[]>(`${offersApi}/api/offers`),
      api<Customer[]>(`${customersApi}/api/customers`),
      api<ShopOrder[]>(`${ordersApi}/api/orders`),
    ]).then(([nextProducts, nextOffers, nextCustomers, nextOrders]) => {
      if (!active) return
      setProducts(nextProducts)
      setOffers(nextOffers)
      setCustomers(nextCustomers)
      setOrders(nextOrders)
    }).catch((reason: Error) => {
      if (active) setError(reason.message)
    })
    return () => { active = false }
  }, [])

  const visibleOrders = orders.filter((order) =>
    !query || includes(`${order.orderNumber} ${order.customerName} ${order.offerName}`, query))
  const counts = ["PLACED", "CONFIRMED", "FULFILLED", "CANCELLED"].map((status) => ({
    status,
    count: orders.filter((order) => order.status === status).length,
  }))
  const max = Math.max(1, ...counts.map((item) => item.count))

  return (
    <section>
      <div className="page-head">
        <div>
          <p className="crumb">Heddle / Desk</p>
          <h1>Store desk</h1>
          <p className="lede">Live counts from the latest records in each service.</p>
        </div>
      </div>
      {error ? <div className="banner">{error}</div> : null}
      <div className="stats">
        <article className="card stat"><span className="blob orange">P</span><div><span>Products</span><strong>{products.length}</strong></div></article>
        <article className="card stat"><span className="blob violet">O</span><div><span>Offers</span><strong>{offers.filter((offer) => offer.status === "ACTIVE").length}</strong></div></article>
        <article className="card stat"><span className="blob aqua">C</span><div><span>Customers</span><strong>{customers.length}</strong></div></article>
        <article className="card stat"><span className="blob pink">R</span><div><span>Orders</span><strong>{orders.length}</strong></div></article>
      </div>
      <div className="split">
        <article className="card">
          <h2>Recent orders</h2>
          <table>
            <thead><tr><th>Order</th><th>Customer</th><th>Offer</th><th>Total</th><th>Status</th></tr></thead>
            <tbody>
              {visibleOrders.slice(0, 6).map((order) => (
                <tr key={order.id}>
                  <td><strong>{order.orderNumber}</strong><div className="muted">{when(order.placedAt)}</div></td>
                  <td>{order.customerName}</td>
                  <td>{order.offerName}</td>
                  <td>{money(order.totalAmount)}</td>
                  <td><Pill value={order.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          {visibleOrders.length === 0 ? <div className="empty">No orders match this search.</div> : null}
        </article>
        <article className="card">
          <h2>Order mix</h2>
          <div className="bars">
            {counts.map((item) => (
              <div className="bar-row" key={item.status}>
                <span>{item.status.charAt(0) + item.status.slice(1).toLowerCase()}</span>
                <div className="track"><span style={{ width: `${(item.count / max) * 100}%` }} /></div>
                <strong>{item.count}</strong>
              </div>
            ))}
          </div>
        </article>
      </div>
    </section>
  )
}
