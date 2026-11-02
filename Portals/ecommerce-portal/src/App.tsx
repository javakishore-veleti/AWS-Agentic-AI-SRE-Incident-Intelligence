import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react"
import { Link, NavLink, Navigate, Route, Routes, useNavigate, useParams, useSearchParams } from "react-router-dom"
import { loadActiveOffers, loadMyOrders, loadOffer, purchase, readSession, signin, signup, writeSession } from "./api"
import type { Offer, Session, ShopOrder } from "./types"

const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" })
const washes = ["wash-sea", "wash-sky", "wash-sand", "wash-leaf"]

function useSession() {
  const [session, setSession] = useState<Session | null>(() => readSession())
  useEffect(() => {
    const sync = () => setSession(readSession())
    window.addEventListener("heddle-session", sync)
    window.addEventListener("storage", sync)
    return () => {
      window.removeEventListener("heddle-session", sync)
      window.removeEventListener("storage", sync)
    }
  }, [])
  return session
}

function Shell(props: { children: ReactNode; wide?: boolean }) {
  const session = useSession()
  return (
    <div className="store">
      <header className="top">
        <Link className="logo" to="/offers">Heddle</Link>
        <nav className="nav">
          <NavLink to="/offers">Offers</NavLink>
          <NavLink to="/orders">Orders</NavLink>
        </nav>
        <div className="account">
          {session ? (
            <>
              <span>{session.customer.fullName}</span>
              <button type="button" onClick={() => writeSession(null)}>Sign out</button>
            </>
          ) : (
            <Link className="signin" to="/signin">Sign in</Link>
          )}
        </div>
      </header>
      <main className={props.wide ? "page wide" : "page"}>{props.children}</main>
    </div>
  )
}

function Pager(props: { page: number; pages: number; from: number; to: number; total: number; onPage: (page: number) => void }) {
  return (
    <div className="pager">
      <span>{props.total === 0 ? "Nothing in this view" : `${props.from}–${props.to} of ${props.total}`}</span>
      <div>
        <button type="button" disabled={props.page <= 1} onClick={() => props.onPage(props.page - 1)}>Previous</button>
        <button type="button" disabled={props.page >= props.pages} onClick={() => props.onPage(props.page + 1)}>Next</button>
      </div>
    </div>
  )
}

function OffersPage() {
  const [rows, setRows] = useState<Offer[]>([])
  const [error, setError] = useState<string | null>(null)
  const [ready, setReady] = useState(false)
  const [criteria, setCriteria] = useState({ offer: "", product: "", max: "" })
  const [page, setPage] = useState(1)

  useEffect(() => {
    loadActiveOffers()
      .then(setRows)
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Offers are unavailable."))
      .finally(() => setReady(true))
  }, [])

  const filtered = useMemo(() => rows.filter((row) => {
    const products = row.products.map((product) => product.name).join(" ").toLowerCase()
    if (criteria.offer && !row.name.toLowerCase().includes(criteria.offer.toLowerCase())) return false
    if (criteria.product && !products.includes(criteria.product.toLowerCase())) return false
    if (criteria.max && row.offerPrice > Number(criteria.max)) return false
    return true
  }), [rows, criteria])

  useEffect(() => { setPage(1) }, [criteria.offer, criteria.product, criteria.max])
  const pageSize = 6
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const current = Math.min(page, pages)
  const start = (current - 1) * pageSize
  const view = filtered.slice(start, start + pageSize)

  return (
    <Shell wide>
      <section className="intro">
        <p className="eyebrow">Ready to purchase</p>
        <h1>Offers, with every product inside.</h1>
        <p className="sub">Browse the latest open offers. Each one is a set of catalog products at one price.</p>
      </section>
      <form className="filters" onSubmit={(event) => event.preventDefault()}>
        <label>Offer name<input value={criteria.offer} onChange={(event) => setCriteria({ ...criteria, offer: event.target.value })} placeholder="Sunday Hosting" /></label>
        <label>Included product<input value={criteria.product} onChange={(event) => setCriteria({ ...criteria, product: event.target.value })} placeholder="Oak" /></label>
        <label>Highest price<input type="number" min="0" value={criteria.max} onChange={(event) => setCriteria({ ...criteria, max: event.target.value })} placeholder="150" /></label>
        <button type="button" className="text" onClick={() => setCriteria({ offer: "", product: "", max: "" })}>Clear</button>
      </form>
      {error ? <p className="warn">{error}</p> : null}
      <div className="grid">
        {view.map((offer, index) => (
          <article className="card" key={offer.id}>
            <div className={`band ${washes[index % washes.length]}`}>
              <span>{offer.products.length} {offer.products.length === 1 ? "product" : "products"}</span>
              <strong>{money.format(offer.offerPrice)}</strong>
            </div>
            <div className="card-body">
              <h2>{offer.name}</h2>
              <p>{offer.description}</p>
              <ul>
                {offer.products.map((product) => <li key={product.id}>{product.name}</li>)}
              </ul>
              <Link to={`/offers/${offer.id}`}>View offer</Link>
            </div>
          </article>
        ))}
      </div>
      {!ready && !error ? <p className="empty">Loading offers…</p> : null}
      {ready && !error && filtered.length === 0 ? <p className="empty">No offers match these criteria.</p> : null}
      {ready ? <Pager page={current} pages={pages} from={filtered.length ? start + 1 : 0} to={Math.min(start + pageSize, filtered.length)} total={filtered.length} onPage={setPage} /> : null}
    </Shell>
  )
}

function OfferDetailPage() {
  const { id } = useParams()
  const session = useSession()
  const navigate = useNavigate()
  const [offer, setOffer] = useState<Offer | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [receipt, setReceipt] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!id) return
    loadOffer(id).then(setOffer).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "This offer is unavailable."))
  }, [id])

  async function buy() {
    if (!offer) return
    if (!session) {
      navigate(`/signin?next=/offers/${offer.id}`)
      return
    }
    setBusy(true)
    setError(null)
    const idempotencyKey = crypto.randomUUID()
    try {
      let order
      try {
        order = await purchase(session.token, offer.id, idempotencyKey)
      } catch (reason) {
        const message = reason instanceof Error ? reason.message : ""
        if (!message.includes("unreachable")) throw reason
        order = await purchase(session.token, offer.id, idempotencyKey)
      }
      setReceipt(`Order ${order.orderNumber} is placed.`)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The purchase did not go through.")
    } finally {
      setBusy(false)
    }
  }

  const listTotal = offer?.products.reduce((sum, product) => sum + Number(product.unitPrice), 0) ?? 0

  return (
    <Shell>
      <Link className="back" to="/offers">All offers</Link>
      {!offer ? <p className="empty">{error ?? "Loading this offer…"}</p> : (
        <div className="detail">
          <section>
            <p className="eyebrow">{offer.code}</p>
            <h1>{offer.name}</h1>
            <p className="sub">{offer.description}</p>
            <ul className="lines">
              {offer.products.map((product) => (
                <li key={product.id}>
                  <div>
                    <strong>{product.name}</strong>
                    <span>{product.category}</span>
                  </div>
                  <b>{money.format(product.unitPrice)}</b>
                </li>
              ))}
            </ul>
          </section>
          <aside className="buy">
            <p>Products together</p>
            <span className="compare">{money.format(listTotal)}</span>
            <h2>{money.format(offer.offerPrice)}</h2>
            <button type="button" disabled={busy || offer.status !== "ACTIVE"} onClick={() => void buy()}>
              {busy ? "Placing order…" : "Purchase offer"}
            </button>
            {receipt ? <p className="receipt">{receipt} <Link to="/orders">See your orders</Link></p> : null}
            {error ? <p className="warn">{error}</p> : null}
          </aside>
        </div>
      )}
    </Shell>
  )
}

function OrdersPage() {
  const session = useSession()
  const [rows, setRows] = useState<ShopOrder[]>([])
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)

  useEffect(() => {
    if (!session) return
    loadMyOrders(session.token).then(setRows).catch((reason: unknown) => {
      setError(reason instanceof Error ? reason.message : "Orders are unavailable.")
    })
  }, [session])

  if (!session) return <Navigate to="/signin?next=/orders" replace />
  const pageSize = 5
  const pages = Math.max(1, Math.ceil(rows.length / pageSize))
  const current = Math.min(page, pages)
  const start = (current - 1) * pageSize
  const view = rows.slice(start, start + pageSize)

  return (
    <Shell>
      <section className="intro">
        <p className="eyebrow">Your account</p>
        <h1>Orders</h1>
        <p className="sub">Purchases placed with {session.customer.email}.</p>
      </section>
      {error ? <p className="warn">{error}</p> : null}
      <div className="orders">
        {view.map((order) => (
          <article key={order.id}>
            <div>
              <strong>{order.orderNumber}</strong>
              <span>{new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" }).format(new Date(order.placedAt))}</span>
            </div>
            <h2>{order.offerName}</h2>
            <ul>{order.lines.map((line) => <li key={line.sku}>{line.productName}</li>)}</ul>
            <footer>
              <b>{money.format(order.totalAmount)}</b>
              <em>{order.status.charAt(0) + order.status.slice(1).toLowerCase()}</em>
            </footer>
          </article>
        ))}
      </div>
      {rows.length === 0 && !error ? <p className="empty">No orders yet. Purchase an offer to see it here.</p> : null}
      <Pager page={current} pages={pages} from={rows.length ? start + 1 : 0} to={Math.min(start + pageSize, rows.length)} total={rows.length} onPage={setPage} />
    </Shell>
  )
}

function AuthPage({ mode }: { mode: "signin" | "signup" }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const next = params.get("next") || "/offers"
  const [fullName, setFullName] = useState("")
  const [email, setEmail] = useState("")
  const [phone, setPhone] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const result = mode === "signup"
        ? await signup({ fullName, email, phone, password })
        : await signin({ email, password })
      writeSession(result)
      navigate(next)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not continue.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <section className="auth-story">
        <Link to="/offers">Heddle</Link>
        <h1>{mode === "signup" ? "Create an account and purchase an offer." : "Sign in to purchase an offer."}</h1>
        <p>Your orders stay on this account. The desk does not create them.</p>
      </section>
      <form onSubmit={(event) => void submit(event)}>
        <h2>{mode === "signup" ? "Create account" : "Sign in"}</h2>
        {mode === "signup" ? <label>Full name<input value={fullName} onChange={(event) => setFullName(event.target.value)} required /></label> : null}
        <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
        {mode === "signup" ? <label>Phone<input value={phone} onChange={(event) => setPhone(event.target.value)} /></label> : null}
        <label>Password<input type="password" minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
        {error ? <p className="warn">{error}</p> : null}
        <button type="submit" disabled={busy}>{mode === "signup" ? "Create account" : "Sign in"}</button>
        <p className="switch">
          {mode === "signup"
            ? <Link to={`/signin?next=${encodeURIComponent(next)}`}>Already have an account? Sign in</Link>
            : <Link to={`/signup?next=${encodeURIComponent(next)}`}>New here? Create an account</Link>}
        </p>
      </form>
    </div>
  )
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/offers" replace />} />
      <Route path="/offers" element={<OffersPage />} />
      <Route path="/offers/:id" element={<OfferDetailPage />} />
      <Route path="/orders" element={<OrdersPage />} />
      <Route path="/signin" element={<AuthPage mode="signin" />} />
      <Route path="/signup" element={<AuthPage mode="signup" />} />
    </Routes>
  )
}
