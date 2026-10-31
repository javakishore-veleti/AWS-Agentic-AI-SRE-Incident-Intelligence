import { FormEvent, useEffect, useMemo, useState } from "react"
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom"
import { OfferArt } from "./art"
import { api, customersApi, localDay, money, offersApi, ordersApi, when, type Offer, type ShopOrder } from "./api"
import { clearSession, saveSession, useSession } from "./session"

function AccountChip() {
  const session = useSession()
  if (!session) return <Link className="signin-link" to="/signin">Sign in</Link>
  const initials = session.customer.fullName.split(" ").map((part) => part[0]).slice(0, 2).join("")
  return (
    <Link className="who" to="/account">
      <span className="avatar">{initials}</span>
      <span>
        <strong>{session.customer.fullName}</strong>
        <small>Your account</small>
      </span>
    </Link>
  )
}

export function ShopPage() {
  const [offers, setOffers] = useState<Offer[]>([])
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [draftPiece, setDraftPiece] = useState("")
  const [draftCategory, setDraftCategory] = useState("")
  const [draftMax, setDraftMax] = useState("")
  const [piece, setPiece] = useState("")
  const [category, setCategory] = useState("")
  const [maxPrice, setMaxPrice] = useState("")
  const [page, setPage] = useState(1)

  useEffect(() => {
    api<Offer[]>(`${offersApi}/api/offers/active`)
      .then(setOffers)
      .catch((reason: Error) => setError(reason.message))
  }, [])

  useEffect(() => { setPage(1) }, [search, piece, category, maxPrice])

  const categories = useMemo(
    () => Array.from(new Set(offers.flatMap((offer) => offer.products.map((product) => product.category)))).sort(),
    [offers],
  )
  const filtered = offers.filter((offer) => {
    if (search && !offer.name.toLowerCase().includes(search.trim().toLowerCase())) return false
    if (piece && !offer.products.some((product) => product.name.toLowerCase().includes(piece.trim().toLowerCase()))) return false
    if (category && !offer.products.some((product) => product.category === category)) return false
    if (maxPrice && offer.offerPrice > Number(maxPrice)) return false
    return true
  })
  const pageSize = 3
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const current = Math.min(page, pages)
  const start = (current - 1) * pageSize
  const view = filtered.slice(start, start + pageSize)

  return (
    <section>
      <header className="shop-head">
        <div>
          <h1>Heddle</h1>
          <p>House goods, gathered into offers.</p>
        </div>
        <label className="search">
          <span aria-hidden="true">⌕</span>
          <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search offers" aria-label="Search offers" />
        </label>
        <AccountChip />
      </header>
      {error ? <div className="banner">{error}</div> : null}
      <div className="layout">
        <form className="filters" onSubmit={(event) => {
          event.preventDefault()
          setPiece(draftPiece)
          setCategory(draftCategory)
          setMaxPrice(draftMax)
        }}>
          <h2>Refine</h2>
          <label>Piece inside
            <input value={draftPiece} onChange={(event) => setDraftPiece(event.target.value)} placeholder="Carafe, lamp, board" />
          </label>
          <label>Category
            <select value={draftCategory} onChange={(event) => setDraftCategory(event.target.value)}>
              <option value="">Any</option>
              {categories.map((item) => <option key={item}>{item}</option>)}
            </select>
          </label>
          <label>Price up to
            <input type="number" min="0" step="1" value={draftMax} onChange={(event) => setDraftMax(event.target.value)} placeholder="150" />
          </label>
          <button className="btn orange" type="submit">Show offers</button>
          <button className="btn text" type="button" onClick={() => {
            setDraftPiece(""); setDraftCategory(""); setDraftMax(""); setPiece(""); setCategory(""); setMaxPrice(""); setSearch("")
          }}>Clear</button>
        </form>
        <div>
          <p className="result-line">Showing {filtered.length === 0 ? 0 : start + 1}–{Math.min(start + pageSize, filtered.length)} of {filtered.length} offers</p>
          <div className="grid">
            {view.map((offer) => (
              <Link className="offer-card" key={offer.id} to={`/offers/${offer.id}`}>
                <OfferArt code={offer.code} />
                <div className="body">
                  <span className="kicker">{offer.products[0]?.category ?? "Set"}</span>
                  <h3>{offer.name}</h3>
                  <div className="chips">{offer.products.map((product) => <span className="chip" key={product.id}>{product.name}</span>)}</div>
                  <div className="card-foot">
                    <span className="price">{money(offer.offerPrice)}</span>
                    <span className="btn text">View offer</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
          {filtered.length === 0 ? <div className="banner">No offers match this search.</div> : null}
          <div className="pager">
            <span>Latest offers, paged here</span>
            <div>
              <button type="button" disabled={current <= 1} onClick={() => setPage(current - 1)}>Previous</button>
              <span>Page {current} of {pages}</span>
              <button type="button" disabled={current >= pages} onClick={() => setPage(current + 1)}>Next</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export function OfferPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const session = useSession()
  const [offer, setOffer] = useState<Offer | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [receipt, setReceipt] = useState<ShopOrder | null>(null)
  const [buying, setBuying] = useState(false)

  useEffect(() => {
    if (!id) return
    api<Offer>(`${offersApi}/api/offers/${id}`).then(setOffer).catch((reason: Error) => setError(reason.message))
  }, [id])

  async function purchase() {
    if (!offer) return
    if (!session) {
      navigate(`/signin?next=/offers/${offer.id}`)
      return
    }
    setBuying(true)
    setError(null)
    try {
      const order = await api<ShopOrder>(`${ordersApi}/api/orders`, {
        method: "POST",
        body: JSON.stringify({ offerId: offer.id }),
      }, session.token)
      setReceipt(order)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Purchase failed.")
    } finally {
      setBuying(false)
    }
  }

  if (!offer && !error) return <p>Loading offer…</p>
  if (!offer) return <div className="banner">{error}</div>
  const pieceTotal = offer.products.reduce((sum, product) => sum + Number(product.unitPrice), 0)

  return (
    <section>
      <header className="shop-head">
        <div>
          <Link className="btn text" to="/">Back to offers</Link>
          <h1>{offer.name}</h1>
        </div>
        <div />
        <AccountChip />
      </header>
      <div className="detail">
        <div className="detail-art"><OfferArt code={offer.code} /></div>
        <div>
          <p>{offer.description}</p>
          <div className="piece-list">
            {offer.products.map((product) => (
              <div className="piece" key={product.id}>
                <span><strong>{product.name}</strong><div className="result-line">{product.sku} · {product.category}</div></span>
                <span>{money(product.unitPrice)}</span>
              </div>
            ))}
          </div>
          <p className="result-line">Pieces together {money(pieceTotal)}</p>
          <p className="price">Offer price {money(offer.offerPrice)}</p>
          {error ? <div className="banner">{error}</div> : null}
          {receipt ? (
            <div className="receipt">
              <div>Order placed</div>
              <strong>{receipt.orderNumber}</strong>
              <p>{receipt.offerName} · {money(receipt.totalAmount)}</p>
              <Link className="btn text" to="/orders">See your orders</Link>
            </div>
          ) : (
            <button className="btn orange" type="button" disabled={buying || offer.status !== "ACTIVE"} onClick={() => void purchase()}>
              {session ? (buying ? "Placing order…" : "Purchase offer") : "Sign in to purchase"}
            </button>
          )}
        </div>
      </div>
    </section>
  )
}

export function OrdersPage() {
  const session = useSession()
  const navigate = useNavigate()
  const [orders, setOrders] = useState<ShopOrder[]>([])
  const [error, setError] = useState<string | null>(null)
  const [from, setFrom] = useState("")
  const [to, setTo] = useState("")
  const [page, setPage] = useState(1)

  useEffect(() => {
    if (!session) {
      navigate("/signin?next=/orders")
      return
    }
    api<ShopOrder[]>(`${ordersApi}/api/orders/mine`, {}, session.token)
      .then(setOrders)
      .catch((reason: Error) => setError(reason.message))
  }, [session, navigate])

  const filtered = orders.filter((order) => {
    const day = localDay(order.placedAt)
    if (from && day < from) return false
    if (to && day > to) return false
    return true
  })
  const pageSize = 4
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const current = Math.min(page, pages)
  const view = filtered.slice((current - 1) * pageSize, current * pageSize)

  return (
    <section>
      <header className="shop-head">
        <div><h1>Your orders</h1><p>Placed from offers you purchased.</p></div>
        <div />
        <AccountChip />
      </header>
      {error ? <div className="banner">{error}</div> : null}
      <div className="filters" style={{ marginBottom: 14 }}>
        <h2>Dates</h2>
        <label>From<input type="date" value={from} onChange={(event) => { setFrom(event.target.value); setPage(1) }} /></label>
        <label>To<input type="date" value={to} onChange={(event) => { setTo(event.target.value); setPage(1) }} /></label>
      </div>
      <div className="order-list">
        {view.map((order) => (
          <article className="order-card" key={order.id}>
            <div>
              <strong>{order.orderNumber}</strong>
              <div>{order.offerName}</div>
              <div className="chips">{order.lines.map((line) => <span className="chip" key={line.sku}>{line.productName}</span>)}</div>
            </div>
            <div>
              <div className="price">{money(order.totalAmount)}</div>
              <div className="result-line">{when(order.placedAt)}</div>
              <span className="pill">{order.status}</span>
            </div>
          </article>
        ))}
      </div>
      {filtered.length === 0 ? <p>No orders in this date range.</p> : null}
      <div className="pager">
        <span>{filtered.length} orders</span>
        <div>
          <button type="button" disabled={current <= 1} onClick={() => setPage(current - 1)}>Previous</button>
          <span>Page {current} of {pages}</span>
          <button type="button" disabled={current >= pages} onClick={() => setPage(current + 1)}>Next</button>
        </div>
      </div>
    </section>
  )
}

export function AccountPage() {
  const session = useSession()
  const navigate = useNavigate()
  if (!session) return <Navigate to="/signin?next=/account" replace />
  return (
    <section className="auth-wrap">
      <div className="auth">
        <h1>{session.customer.fullName}</h1>
        <p>{session.customer.email}</p>
        <p>{session.customer.phone}</p>
        <Link className="btn orange" to="/orders">Your orders</Link>
        <button className="btn text" type="button" onClick={() => { clearSession(); navigate("/") }}>Sign out</button>
      </div>
    </section>
  )
}

export function SignInPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const next = params.get("next") || "/"

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const session = await api<{ token: string; customer: { id: string; fullName: string; email: string; phone: string; status: string } }>(
        `${customersApi}/api/auth/signin`,
        { method: "POST", body: JSON.stringify({ email, password }) },
      )
      saveSession(session)
      navigate(next)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Sign in failed.")
    }
  }

  return (
    <section className="auth-wrap">
      <form className="auth" onSubmit={(event) => void submit(event)}>
        <h1>Sign in</h1>
        <p>Use the email on your Heddle account.</p>
        {error ? <div className="banner">{error}</div> : null}
        <label>Email<input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
        <label>Password<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <button className="btn orange" type="submit">Sign in</button>
        <p className="switch">New here? <Link to={`/signup?next=${encodeURIComponent(next)}`}>Create an account</Link></p>
      </form>
    </section>
  )
}

export function SignUpPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [fullName, setFullName] = useState("")
  const [email, setEmail] = useState("")
  const [phone, setPhone] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const next = params.get("next") || "/"

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const session = await api<{ token: string; customer: { id: string; fullName: string; email: string; phone: string; status: string } }>(
        `${customersApi}/api/auth/signup`,
        { method: "POST", body: JSON.stringify({ fullName, email, phone, password }) },
      )
      saveSession(session)
      navigate(next)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Sign up failed.")
    }
  }

  return (
    <section className="auth-wrap">
      <form className="auth" onSubmit={(event) => void submit(event)}>
        <h1>Create your account</h1>
        <p>Purchase offers and keep the orders with your name.</p>
        {error ? <div className="banner">{error}</div> : null}
        <label>Full name<input required value={fullName} onChange={(event) => setFullName(event.target.value)} /></label>
        <label>Email<input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
        <label>Phone<input value={phone} onChange={(event) => setPhone(event.target.value)} /></label>
        <label>Password<input required type="password" minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <button className="btn orange" type="submit">Create account</button>
        <p className="switch">Already shopping? <Link to={`/signin?next=${encodeURIComponent(next)}`}>Sign in</Link></p>
      </form>
    </section>
  )
}
