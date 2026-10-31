import { NavLink, Navigate, Route, Routes } from "react-router-dom"
import { CustomersPage } from "./pages/CustomersPage"
import { OffersPage } from "./pages/OffersPage"
import { OrdersPage } from "./pages/OrdersPage"
import { OverviewPage } from "./pages/OverviewPage"
import { ProductsPage } from "./pages/ProductsPage"

function Icon({ d }: { d: string }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d={d} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

const links = [
  { to: "/overview", label: "Overview", group: "Desk", d: "M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1z" },
  { to: "/products", label: "Product catalog", group: "Catalog", d: "M4 7h16M4 12h16M4 17h10" },
  { to: "/offers", label: "Offers", group: "Commerce", d: "M12 3 4 7v10l8 4 8-4V7z" },
  { to: "/customers", label: "Customers", group: "Commerce", d: "M16 19v-1a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v1M9.5 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7M20 8v6M17 11h6" },
  { to: "/orders", label: "Orders", group: "Commerce", d: "M6 6h15l-1.5 9h-12zM6 6 5 3H2M9 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2M18 20a1 1 0 1 0 0-2 1 1 0 0 0 0 2" },
]

export function App() {
  let previous = ""
  return (
    <div className="desk">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">H</div>
          <div>
            <strong>Heddle</strong>
            <span>Desk</span>
          </div>
        </div>
        <nav className="nav">
          {links.map((link) => {
            const show = link.group !== previous
            previous = link.group
            return (
              <div key={link.to}>
                {show ? <div className="nav-label">{link.group.toUpperCase()}</div> : null}
                <NavLink to={link.to} className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
                  <Icon d={link.d} />
                  {link.label}
                </NavLink>
              </div>
            )
          })}
        </nav>
        <div className="sidebar-foot">Commerce desk for the Heddle house.</div>
      </aside>
      <main className="canvas">
        <Routes>
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="/overview" element={<OverviewPage />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/offers" element={<OffersPage />} />
          <Route path="/customers" element={<CustomersPage />} />
          <Route path="/orders" element={<OrdersPage />} />
        </Routes>
      </main>
    </div>
  )
}
