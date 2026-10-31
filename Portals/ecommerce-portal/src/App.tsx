import { useEffect, useMemo, useState, type FormEvent } from "react"
import { Link, NavLink, Navigate, Route, Routes, useNavigate, useParams, useSearchParams } from "react-router-dom"
import { loadActiveOffers, loadMyOrders, loadOffer, purchase, readSession, signin, signup, writeSession } from "./api"
import type { Offer, Session, ShopOrder } from "./types"

const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" })

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

function Icon({ d }: { d: string }) {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d={d} stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function Rail() {
  return (
    <aside className="rail">
      <Link className="mark" to="/offers" aria-label="Heddle home">H</Link>
      <NavLink className={({ isActive }) => `item${isActive ? " active" : ""}`} to="/offers" aria-label="Offers">
        <Icon d="M4 7h16M4 12h16M4 17h10" />
      </NavLink>
      <NavLink className={({ isActive }) => `item${isActive ? " active" : ""}`} to="/orders" aria-label="Your orders">
        <Icon d="M6 6h15l-1.5 9h-12zM6 6 5 3H2" />
      </NavLink>
      <NavLink className={({ isActive }) => `item${isActive ? " active" : ""}`} to="/signin" aria-label="Account">
        <Icon d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4M5 20a7 7 0 0 1 14 0" />
      </NavLink>
    </aside>
  )
}

function Pager(props: { page: number; pages: number; from: number; to: number; total: number; onPage: (page: number) => void }) {
  return (
    <div className="pager">
      <span>{props.total === 0 ? "No sets in this view" : `${props.from}–${props.to} of ${props.total}`}</span>
      <div>
        <button type="button" disabled={props.page <= 1} onClick={() => props.onPage(props.page - 1)}>Previous</button>
        <button type="button" disabled={props.page >= props.pages} onClick={() => props.onPage(props.page + 1)}>Next</button>
      </div>
    </div>
  )
}

function OffersPage() {
  const session = useSession()
  const [rows, setRows] = useState<Offer[]>([])
  const [error, setError] = useState<string | null>(null)
  const [ready, setReady] = useState(false)
  const [criteria, setCriteria] = useState({ offer: "", product: "", max: "" })
  const [page, setPage] = useState(1)

  useEffect(() => {
    loadActiveOffers()
      .then(setRows)
      .catch((reason: unknown) => {
        setError(reason instanceof Error ? reason.message : "Offers are unavailable.")
      })
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
    <div className="stage">
      <div className="shop">
        <Rail />
        <aside className="filters">
          <h2>Heddle</h2>
          <p className="quiet">House sets, with the goods inside each one.</p>
          <label className="field">Offer<input value={criteria.offer} onChange={(event) => setCriteria({ ...criteria, offer: event.target.value })} /></label>
          <label className="field">Included product<input value={criteria.product} onChange={(event) => setCriteria({ ...criteria, product: event.target.value })} /></label>
          <label className="field">Highest price<input type="number" min="0" value={criteria.max} onChange={(event) => setCriteria({ ...criteria, max: event.target.value })} /></label>
          <button className="clear" type="button" onClick={() => setCriteria({ offer: "", product: "", max: "" })}>Clear criteria</button>
        </aside>
        <main className="main">
          <div className="main-head">
            <div>
              <p className="quiet">Showing the latest open sets</p>
              <h1>Sets</h1>
            </div>
            <div className="who">
              {session ? <><strong>{session.customer.fullName}</strong><button className="ghost" type="button" onClick={() => writeSession(null)}>Sign out</button></> : <Link to="/signin">Sign in</Link>}
            </div>
          </div>
          {error ? <p className="warn">{error}</p> : null}
          <div className="grid">
            {view.map((offer) => (
              <article className="offer" key={offer.id}>
                <div className="kicker">Set</div>
                <h3>{offer.name}</h3>
                <div className="price">{money.format(offer.offerPrice)}</div>
                <div className="chips">{offer.products.map((product) => <span className="chip" key={product.id}>{product.name}</span>)}</div>
                <Link className="view" to={`/offers/${offer.id}`}>View set</Link>
              </article>
            ))}
          </div>
          {ready && !error && filtered.length === 0 ? <p className="quiet">No sets match these criteria.</p> : null}
          <Pager page={current} pages={pages} from={filtered.length ? start + 1 : 0} to={Math.min(start + pageSize, filtered.length)} total={filtered.length} onPage={setPage} />
        </main>
      </div>
    </div>
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
    loadOffer(id).then(setOffer).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "This set is unavailable."))
  }, [id])

  async function buy() {
    if (!offer) return
    if (!session) {
      navigate(`/signin?next=/offers/${offer.id}`)
      return
    }
    setBusy(true)
    setError(null)
    try {
      const order = await purchase(session.token, offer.id)
      setReceipt(`Order ${order.orderNumber} is placed.`)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The purchase did not go through.")
    } finally {
      setBusy(false)
    }
  }

  const listTotal = offer?.products.reduce((sum, product) => sum + Number(product.unitPrice), 0) ?? 0

  return (
    <div className="stage">
      <div className="shop plain">
        <Rail />
        <main className="main">
          <Link className="quiet" to="/offers">Back to sets</Link>
          {!offer ? <p className="quiet">Loading this set…</p> : (
            <div className="detail">
              <section>
                <p className="kicker" style={{ color: "#e4547c", fontWeight: 750 }}>Set</p>
                <h1>{offer.name}</h1>
                <p className="quiet" style={{ margin: "8px 0 16px" }}>{offer.description}</p>
                {offer.products.map((product) => (
                  <div className="product-row" key={product.id}>
                    <div><strong>{product.name}</strong><div className="quiet">{product.category}</div></div>
                    <div>{money.format(product.unitPrice)}</div>
                  </div>
                ))}
              </section>
              <aside className="panel">
                <p className="quiet">Goods together</p>
                <p>{money.format(listTotal)}</p>
                <h2 style={{ margin: "10px 0" }}>{money.format(offer.offerPrice)}</h2>
                <button className="primary" type="button" disabled={busy || offer.status !== "ACTIVE"} onClick={() => void buy()}>Purchase set</button>
                {receipt ? <p className="receipt" style={{ marginTop: 12 }}>{receipt}</p> : null}
                {error ? <p className="warn" style={{ marginTop: 12 }}>{error}</p> : null}
              </aside>
            </div>
          )}
        </main>
      </div>
    </div>
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
    <div className="stage">
      <div className="shop plain">
        <Rail />
        <main className="main">
          <h1>Your orders</h1>
          <p className="quiet">Latest orders on this account.</p>
          {error ? <p className="warn">{error}</p> : null}
          <div className="grid" style={{ marginTop: 16 }}>
            {view.map((order) => (
              <article className="offer" key={order.id}>
                <div className="kicker">{order.orderNumber}</div>
                <h3>{order.offerName}</h3>
                <div className="price">{money.format(order.totalAmount)}</div>
                <div className="chips">{order.lines.map((line) => <span className="chip" key={line.sku}>{line.productName}</span>)}</div>
                <p className="quiet">{order.status}</p>
              </article>
            ))}
          </div>
          {rows.length === 0 ? <p className="quiet">No orders yet.</p> : null}
          <Pager page={current} pages={pages} from={rows.length ? start + 1 : 0} to={Math.min(start + pageSize, rows.length)} total={rows.length} onPage={setPage} />
        </main>
      </div>
    </div>
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
    <div className="auth-wrap">
      <section className="auth">
        <Link className="mark" to="/offers" aria-label="Heddle home">H</Link>
        <h1>{mode === "signup" ? "Create your account" : "Welcome back"}</h1>
        <p className="quiet">{mode === "signup" ? "Use it to purchase a set." : "Sign in to purchase a set."}</p>
        <form onSubmit={(event) => void submit(event)}>
          {mode === "signup" ? <label className="field">Full name<input value={fullName} onChange={(event) => setFullName(event.target.value)} required /></label> : null}
          <label className="field">Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          {mode === "signup" ? <label className="field">Phone<input value={phone} onChange={(event) => setPhone(event.target.value)} /></label> : null}
          <label className="field">Password<input type="password" minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
          {error ? <p className="warn">{error}</p> : null}
          <button className="primary" type="submit" disabled={busy}>{mode === "signup" ? "Create account" : "Sign in"}</button>
        </form>
        <p className="switch">
          {mode === "signup" ? <Link to={`/signin?next=${encodeURIComponent(next)}`}>Already have an account? Sign in</Link> : <Link to={`/signup?next=${encodeURIComponent(next)}`}>New here? Create an account</Link>}
        </p>
      </section>
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
