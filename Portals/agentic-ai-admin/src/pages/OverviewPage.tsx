import { useEffect, useState } from "react"
import { Link } from "react-router-dom"
import { loadCustomers, loadOffers, loadOrders, loadProducts } from "../api"

export function OverviewPage() {
  const [counts, setCounts] = useState({ products: 0, offers: 0, customers: 0, orders: 0 })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    Promise.all([loadProducts(), loadOffers(), loadCustomers(), loadOrders()])
      .then(([products, offers, customers, orders]) => {
        if (!active) return
        setCounts({ products: products.length, offers: offers.length, customers: customers.length, orders: orders.length })
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "The desk could not reach the services.")
      })
    return () => { active = false }
  }, [])

  return (
    <>
      <p className="crumb">Desk / Overview</p>
      <div className="page-head">
        <div>
          <h1>Overview</h1>
          <p className="lede">Latest records from catalog, offers, customers, and orders.</p>
        </div>
      </div>
      {error ? <div className="banner warn">{error}</div> : null}
      <div className="stats">
        <Link className="stat" to="/products"><span><i className="dot coral" />Products</span><b>{counts.products}</b></Link>
        <Link className="stat" to="/offers"><span><i className="dot rose" />Offers</span><b>{counts.offers}</b></Link>
        <Link className="stat" to="/customers"><span><i className="dot teal" />Customers</span><b>{counts.customers}</b></Link>
        <Link className="stat" to="/orders"><span><i className="dot gold" />Orders</span><b>{counts.orders}</b></Link>
      </div>
    </>
  )
}
