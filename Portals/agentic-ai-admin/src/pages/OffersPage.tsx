import { useEffect, useState } from "react"
import { loadOffers, loadProducts, removeOffer, saveOffer } from "../api"
import type { Offer, OfferStatus, Product } from "../types"
import { Field, Modal, Pager, money, pretty, usePaged } from "../ui"

const empty = { code: "", name: "", offerPrice: "", status: "ACTIVE" as OfferStatus, startsOn: "", endsOn: "", description: "", productIds: [] as string[] }

export function OffersPage() {
  const [rows, setRows] = useState<Offer[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [criteria, setCriteria] = useState({ name: "", code: "", product: "", status: "" })
  const [editing, setEditing] = useState<Offer | "new" | null>(null)
  const [form, setForm] = useState(empty)
  const [formError, setFormError] = useState<string | null>(null)
  const [pendingDelete, setPendingDelete] = useState<Offer | null>(null)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    const [offerRows, productRows] = await Promise.all([loadOffers(criteria), loadProducts({ name: "", sku: "", category: "", status: "" })])
    setRows(offerRows)
    setProducts(productRows)
  }

  useEffect(() => {
    let active = true
    Promise.all([loadOffers(criteria), loadProducts({ name: "", sku: "", category: "", status: "" })])
      .then(([offerRows, productRows]) => {
        if (!active) return
        setRows(offerRows)
        setProducts(productRows)
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "Could not load offers.")
      })
    return () => {
      active = false
    }
  }, [criteria])

  const page = usePaged(rows, JSON.stringify(criteria))

  function openNew() {
    setForm(empty)
    setFormError(null)
    setEditing("new")
  }

  function openEdit(row: Offer) {
    setForm({
      code: row.code,
      name: row.name,
      offerPrice: String(row.offerPrice),
      status: row.status,
      startsOn: row.startsOn ?? "",
      endsOn: row.endsOn ?? "",
      description: row.description ?? "",
      productIds: row.productIds,
    })
    setFormError(null)
    setEditing(row)
  }

  function toggleProduct(id: string) {
    setForm((current) => ({
      ...current,
      productIds: current.productIds.includes(id)
        ? current.productIds.filter((item) => item !== id)
        : [...current.productIds, id],
    }))
  }

  async function onSave() {
    if (!form.code.trim() || !form.name.trim() || form.offerPrice === "") {
      setFormError("Code, name, and offer price are required.")
      return
    }
    if (form.productIds.length === 0) {
      setFormError("Map the offer to at least one product.")
      return
    }
    setBusy(true)
    setFormError(null)
    try {
      await saveOffer({
        code: form.code,
        name: form.name,
        description: form.description,
        offerPrice: Number(form.offerPrice),
        status: form.status,
        startsOn: form.startsOn || null,
        endsOn: form.endsOn || null,
        productIds: form.productIds,
      }, editing === "new" || editing === null ? undefined : editing.id)
      setEditing(null)
      setNotice(editing === "new" ? "Offer published to the desk." : "Offer updated.")
      await refresh()
    } catch (reason) {
      setFormError(reason instanceof Error ? reason.message : "Could not save the offer.")
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!pendingDelete) return
    setBusy(true)
    try {
      await removeOffer(pendingDelete.id)
      setPendingDelete(null)
      setNotice("Offer removed.")
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not remove the offer.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <p className="crumb">Commerce / Offers</p>
      <div className="page-head">
        <div>
          <h1>Offers</h1>
          <p className="lede">Latest 100 matching offers, each mapped to catalog products.</p>
        </div>
        <button className="primary" type="button" onClick={openNew}>New offer</button>
      </div>
      {notice ? <div className="banner">{notice}</div> : null}
      {error ? <div className="banner warn">{error}</div> : null}
      <div className="filters">
        <Field label="Offer"><input value={criteria.name} onChange={(event) => setCriteria({ ...criteria, name: event.target.value })} /></Field>
        <Field label="Code"><input value={criteria.code} onChange={(event) => setCriteria({ ...criteria, code: event.target.value })} /></Field>
        <Field label="Product"><input value={criteria.product} onChange={(event) => setCriteria({ ...criteria, product: event.target.value })} /></Field>
        <Field label="Status">
          <select value={criteria.status} onChange={(event) => setCriteria({ ...criteria, status: event.target.value })}>
            <option value="">Any status</option>
            <option value="DRAFT">Draft</option>
            <option value="ACTIVE">Active</option>
            <option value="EXPIRED">Expired</option>
          </select>
        </Field>
      </div>
      <section className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Offer</th><th>Products</th><th>Price</th><th>Window</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {page.view.map((row) => (
                <tr key={row.id}>
                  <td><strong>{row.name}</strong><div className="muted">{row.code}</div></td>
                  <td><div className="chips">{row.products.map((product) => <span className="chip" key={product.id}>{product.name}</span>)}</div></td>
                  <td>{money.format(row.offerPrice)}</td>
                  <td className="muted">{row.startsOn ?? "Open"} – {row.endsOn ?? "Open"}</td>
                  <td><span className={`pill ${row.status}`}>{pretty(row.status)}</span></td>
                  <td className="actions">
                    <button className="text-btn" type="button" onClick={() => openEdit(row)}>Edit</button>
                    <button className="danger" type="button" onClick={() => setPendingDelete(row)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {page.total === 0 ? <div className="empty">No offers match these criteria.</div> : null}
        </div>
        <Pager {...page} onPage={page.setPage} />
      </section>
      {editing ? (
        <Modal title={editing === "new" ? "New offer" : "Edit offer"} onClose={() => setEditing(null)}>
          <div className="form-grid">
            <Field label="Code"><input value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} /></Field>
            <Field label="Name"><input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></Field>
            <Field label="Offer price"><input type="number" min="0.01" step="0.01" value={form.offerPrice} onChange={(event) => setForm({ ...form, offerPrice: event.target.value })} /></Field>
            <Field label="Status">
              <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as OfferStatus })}>
                <option value="DRAFT">Draft</option>
                <option value="ACTIVE">Active</option>
                <option value="EXPIRED">Expired</option>
              </select>
            </Field>
            <Field label="Starts"><input type="date" value={form.startsOn} onChange={(event) => setForm({ ...form, startsOn: event.target.value })} /></Field>
            <Field label="Ends"><input type="date" value={form.endsOn} onChange={(event) => setForm({ ...form, endsOn: event.target.value })} /></Field>
            <div className="wide"><Field label="Description"><textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></Field></div>
            <div className="wide">
              <Field label="Products in this offer">
                <div className="checks">
                  {products.map((product) => (
                    <label className="check" key={product.id}>
                      <input type="checkbox" checked={form.productIds.includes(product.id)} onChange={() => toggleProduct(product.id)} />
                      {product.name} · {product.sku}
                    </label>
                  ))}
                </div>
              </Field>
            </div>
          </div>
          {formError ? <p className="form-error">{formError}</p> : null}
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onSave()}>Save offer</button>
          </div>
        </Modal>
      ) : null}
      {pendingDelete ? (
        <Modal title="Remove offer" onClose={() => setPendingDelete(null)}>
          <p>Remove {pendingDelete.name}?</p>
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setPendingDelete(null)}>Keep</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onDelete()}>Remove</button>
          </div>
        </Modal>
      ) : null}
    </>
  )
}
