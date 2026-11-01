import { useEffect, useState } from "react"
import { loadProducts, removeProduct, saveProduct } from "../api"
import type { Product, ProductStatus } from "../types"
import { Field, Modal, Pager, money, pretty, usePaged } from "../ui"

const empty = { sku: "", name: "", category: "", unitPrice: "", status: "ACTIVE" as ProductStatus, description: "" }

export function ProductsPage() {
  const [rows, setRows] = useState<Product[]>([])
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [criteria, setCriteria] = useState({ name: "", sku: "", category: "", status: "" })
  const [editing, setEditing] = useState<Product | "new" | null>(null)
  const [form, setForm] = useState(empty)
  const [formError, setFormError] = useState<string | null>(null)
  const [pendingDelete, setPendingDelete] = useState<Product | null>(null)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    setRows(await loadProducts(criteria))
  }

  useEffect(() => {
    let active = true
    loadProducts(criteria)
      .then((loaded) => {
        if (active) setRows(loaded)
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "Could not load products.")
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

  function openEdit(row: Product) {
    setForm({
      sku: row.sku,
      name: row.name,
      category: row.category,
      unitPrice: String(row.unitPrice),
      status: row.status,
      description: row.description ?? "",
    })
    setFormError(null)
    setEditing(row)
  }

  async function onSave() {
    if (!form.sku.trim() || !form.name.trim() || !form.category.trim() || form.unitPrice === "") {
      setFormError("SKU, name, category, and price are required.")
      return
    }
    setBusy(true)
    setFormError(null)
    try {
      await saveProduct({
        sku: form.sku,
        name: form.name,
        category: form.category,
        unitPrice: Number(form.unitPrice),
        status: form.status,
        description: form.description,
      }, editing === "new" || editing === null ? undefined : editing.id)
      setEditing(null)
      setNotice(editing === "new" ? "Product added." : "Product updated.")
      await refresh()
    } catch (reason) {
      setFormError(reason instanceof Error ? reason.message : "Could not save the product.")
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!pendingDelete) return
    setBusy(true)
    try {
      await removeProduct(pendingDelete.id)
      setPendingDelete(null)
      setNotice("Product removed.")
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not remove the product.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <p className="crumb">Catalog / Product catalog</p>
      <div className="page-head">
        <div>
          <h1>Product catalog</h1>
          <p className="lede">Latest 100 matching products. Pages stay in the browser.</p>
        </div>
        <button className="primary" type="button" onClick={openNew}>New product</button>
      </div>
      {notice ? <div className="banner">{notice}</div> : null}
      {error ? <div className="banner warn">{error}</div> : null}
      <div className="filters">
        <Field label="Name"><input value={criteria.name} onChange={(event) => setCriteria({ ...criteria, name: event.target.value })} /></Field>
        <Field label="SKU"><input value={criteria.sku} onChange={(event) => setCriteria({ ...criteria, sku: event.target.value })} /></Field>
        <Field label="Category"><input value={criteria.category} onChange={(event) => setCriteria({ ...criteria, category: event.target.value })} /></Field>
        <Field label="Status">
          <select value={criteria.status} onChange={(event) => setCriteria({ ...criteria, status: event.target.value })}>
            <option value="">Any status</option>
            <option value="ACTIVE">Active</option>
            <option value="DISCONTINUED">Discontinued</option>
          </select>
        </Field>
      </div>
      <section className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Product</th><th>SKU</th><th>Category</th><th>Price</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {page.view.map((row) => (
                <tr key={row.id}>
                  <td><strong>{row.name}</strong><div className="muted">{row.description}</div></td>
                  <td>{row.sku}</td>
                  <td>{row.category}</td>
                  <td>{money.format(row.unitPrice)}</td>
                  <td><span className={`pill ${row.status}`}>{pretty(row.status)}</span></td>
                  <td className="actions">
                    <button className="text-btn" type="button" onClick={() => openEdit(row)}>Edit</button>
                    <button className="danger" type="button" onClick={() => setPendingDelete(row)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {page.total === 0 ? <div className="empty">No products match these criteria.</div> : null}
        </div>
        <Pager {...page} onPage={page.setPage} />
      </section>
      {editing ? (
        <Modal title={editing === "new" ? "New product" : "Edit product"} onClose={() => setEditing(null)}>
          <div className="form-grid">
            <Field label="SKU"><input value={form.sku} onChange={(event) => setForm({ ...form, sku: event.target.value })} /></Field>
            <Field label="Name"><input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></Field>
            <Field label="Category"><input value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} /></Field>
            <Field label="Unit price"><input type="number" min="0" step="0.01" value={form.unitPrice} onChange={(event) => setForm({ ...form, unitPrice: event.target.value })} /></Field>
            <Field label="Status">
              <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as ProductStatus })}>
                <option value="ACTIVE">Active</option>
                <option value="DISCONTINUED">Discontinued</option>
              </select>
            </Field>
            <div className="wide"><Field label="Description"><textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></Field></div>
          </div>
          {formError ? <p className="form-error">{formError}</p> : null}
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onSave()}>Save product</button>
          </div>
        </Modal>
      ) : null}
      {pendingDelete ? (
        <Modal title="Remove product" onClose={() => setPendingDelete(null)}>
          <p>Remove {pendingDelete.name} from the catalog?</p>
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setPendingDelete(null)}>Keep</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onDelete()}>Remove</button>
          </div>
        </Modal>
      ) : null}
    </>
  )
}
