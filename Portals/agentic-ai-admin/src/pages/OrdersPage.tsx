import { useEffect, useState } from "react"
import { loadOrders, removeOrder, saveOrder } from "../api"
import type { OrderStatus, ShopOrder } from "../types"
import { Field, Modal, Pager, money, pretty, usePaged, when } from "../ui"

export function OrdersPage() {
  const [rows, setRows] = useState<ShopOrder[]>([])
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [criteria, setCriteria] = useState({ customer: "", email: "", offer: "", status: "", from: "", to: "" })
  const [editing, setEditing] = useState<ShopOrder | null>(null)
  const [status, setStatus] = useState<OrderStatus>("PLACED")
  const [note, setNote] = useState("")
  const [formError, setFormError] = useState<string | null>(null)
  const [pendingDelete, setPendingDelete] = useState<ShopOrder | null>(null)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    setRows(await loadOrders(criteria))
  }

  useEffect(() => {
    let active = true
    loadOrders(criteria)
      .then((loaded) => {
        if (active) setRows(loaded)
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "Could not load orders.")
      })
    return () => {
      active = false
    }
  }, [criteria])

  const page = usePaged(rows, JSON.stringify(criteria))

  function openEdit(row: ShopOrder) {
    setEditing(row)
    setStatus(row.status)
    setNote(row.note ?? "")
    setFormError(null)
  }

  async function onSave() {
    if (!editing) return
    setBusy(true)
    setFormError(null)
    try {
      await saveOrder(editing.id, { status, note })
      setEditing(null)
      setNotice("Order updated.")
      await refresh()
    } catch (reason) {
      setFormError(reason instanceof Error ? reason.message : "Could not update the order.")
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!pendingDelete) return
    setBusy(true)
    try {
      await removeOrder(pendingDelete.id)
      setPendingDelete(null)
      setNotice("Order cancelled.")
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not cancel the order.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <p className="crumb">Commerce / Orders</p>
      <div className="page-head">
        <div>
          <h1>Orders</h1>
          <p className="lede">Latest 100 matching orders. From and to are inclusive UTC calendar days.</p>
        </div>
      </div>
      {notice ? <div className="banner">{notice}</div> : null}
      {error ? <div className="banner warn">{error}</div> : null}
      <div className="filters orders">
        <Field label="Customer"><input value={criteria.customer} onChange={(event) => setCriteria({ ...criteria, customer: event.target.value })} /></Field>
        <Field label="Email"><input value={criteria.email} onChange={(event) => setCriteria({ ...criteria, email: event.target.value })} /></Field>
        <Field label="Offer"><input value={criteria.offer} onChange={(event) => setCriteria({ ...criteria, offer: event.target.value })} /></Field>
        <Field label="Status">
          <select value={criteria.status} onChange={(event) => setCriteria({ ...criteria, status: event.target.value })}>
            <option value="">Any status</option>
            <option value="RESERVING">Reserving</option>
            <option value="RESERVATION_FAILED">Reservation failed</option>
            <option value="PLACED">Placed</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="FULFILLED">Fulfilled</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </Field>
        <Field label="Placed from"><input type="date" value={criteria.from} onChange={(event) => setCriteria({ ...criteria, from: event.target.value })} /></Field>
        <Field label="Placed to"><input type="date" value={criteria.to} onChange={(event) => setCriteria({ ...criteria, to: event.target.value })} /></Field>
      </div>
      <section className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Order</th><th>Customer</th><th>Offer</th><th>Placed</th><th>Total</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {page.view.map((row) => (
                <tr key={row.id}>
                  <td><strong>{row.orderNumber}</strong></td>
                  <td>{row.customerName}<div className="muted">{row.customerEmail}</div></td>
                  <td>{row.offerName}</td>
                  <td>{when(row.placedAt)}</td>
                  <td>{money.format(row.totalAmount)}</td>
                  <td><span className={`pill ${row.status}`}>{pretty(row.status)}</span></td>
                  <td className="actions">
                    <button className="text-btn" type="button" onClick={() => openEdit(row)}>Update</button>
                    <button className="danger" type="button" onClick={() => setPendingDelete(row)}>Cancel</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {page.total === 0 ? <div className="empty">No orders match these criteria.</div> : null}
        </div>
        <Pager {...page} onPage={page.setPage} />
      </section>
      {editing ? (
        <Modal title={editing.orderNumber} onClose={() => setEditing(null)}>
          <p className="lede">{editing.customerName} bought {editing.offerName} on {when(editing.placedAt)}.</p>
          <div className="chips" style={{ margin: "12px 0" }}>
            {editing.lines.map((line) => <span className="chip" key={line.sku}>{line.productName}</span>)}
          </div>
          <div className="form-grid">
            <Field label="Status">
              <select value={status} onChange={(event) => setStatus(event.target.value as OrderStatus)}>
                <option value="RESERVING">Reserving</option>
                <option value="RESERVATION_FAILED">Reservation failed</option>
                <option value="PLACED">Placed</option>
                <option value="CONFIRMED">Confirmed</option>
                <option value="FULFILLED">Fulfilled</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </Field>
            <div className="wide"><Field label="Note"><textarea value={note} onChange={(event) => setNote(event.target.value)} /></Field></div>
          </div>
          {formError ? <p className="form-error">{formError}</p> : null}
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setEditing(null)}>Close</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onSave()}>Save update</button>
          </div>
        </Modal>
      ) : null}
      {pendingDelete ? (
        <Modal title="Cancel order" onClose={() => setPendingDelete(null)}>
          <p>Cancel {pendingDelete.orderNumber}? The order stays on record, and reserved stock is released.</p>
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setPendingDelete(null)}>Keep</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onDelete()}>Cancel order</button>
          </div>
        </Modal>
      ) : null}
    </>
  )
}
