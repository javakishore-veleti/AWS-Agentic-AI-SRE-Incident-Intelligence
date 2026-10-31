import { useEffect, useState } from "react"
import { loadCustomers, removeCustomer, saveCustomer } from "../api"
import type { Customer, CustomerStatus } from "../types"
import { Field, Modal, Pager, pretty, usePaged } from "../ui"

const empty = { fullName: "", email: "", phone: "", status: "ACTIVE" as CustomerStatus, password: "" }

export function CustomersPage() {
  const [rows, setRows] = useState<Customer[]>([])
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [criteria, setCriteria] = useState({ name: "", email: "", status: "" })
  const [editing, setEditing] = useState<Customer | "new" | null>(null)
  const [form, setForm] = useState(empty)
  const [formError, setFormError] = useState<string | null>(null)
  const [pendingDelete, setPendingDelete] = useState<Customer | null>(null)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    setRows(await loadCustomers(criteria))
  }

  useEffect(() => {
    let active = true
    loadCustomers(criteria)
      .then((loaded) => {
        if (active) setRows(loaded)
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "Could not load customers.")
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

  function openEdit(row: Customer) {
    setForm({ fullName: row.fullName, email: row.email, phone: row.phone ?? "", status: row.status, password: "" })
    setFormError(null)
    setEditing(row)
  }

  async function onSave() {
    if (!form.fullName.trim() || !form.email.trim()) {
      setFormError("Name and email are required.")
      return
    }
    if ((editing === "new" || form.password) && form.password.length < 8) {
      setFormError("Password must be at least 8 characters.")
      return
    }
    setBusy(true)
    setFormError(null)
    try {
      await saveCustomer({
        fullName: form.fullName,
        email: form.email,
        phone: form.phone,
        status: form.status,
        password: form.password,
      }, editing === "new" || editing === null ? undefined : editing.id)
      setEditing(null)
      setNotice(editing === "new" ? "Customer added." : "Customer updated.")
      await refresh()
    } catch (reason) {
      setFormError(reason instanceof Error ? reason.message : "Could not save the customer.")
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!pendingDelete) return
    setBusy(true)
    try {
      await removeCustomer(pendingDelete.id)
      setPendingDelete(null)
      setNotice("Customer removed.")
      await refresh()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not remove the customer.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <p className="crumb">Commerce / Customers</p>
      <div className="page-head">
        <div>
          <h1>Customers</h1>
          <p className="lede">Latest 100 matching customers. Shop signups appear here too.</p>
        </div>
        <button className="primary" type="button" onClick={openNew}>New customer</button>
      </div>
      {notice ? <div className="banner">{notice}</div> : null}
      {error ? <div className="banner warn">{error}</div> : null}
      <div className="filters">
        <Field label="Name"><input value={criteria.name} onChange={(event) => setCriteria({ ...criteria, name: event.target.value })} /></Field>
        <Field label="Email"><input value={criteria.email} onChange={(event) => setCriteria({ ...criteria, email: event.target.value })} /></Field>
        <Field label="Status">
          <select value={criteria.status} onChange={(event) => setCriteria({ ...criteria, status: event.target.value })}>
            <option value="">Any status</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
        </Field>
      </div>
      <section className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Customer</th><th>Email</th><th>Phone</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {page.view.map((row) => (
                <tr key={row.id}>
                  <td><strong>{row.fullName}</strong></td>
                  <td>{row.email}</td>
                  <td>{row.phone || "—"}</td>
                  <td><span className={`pill ${row.status}`}>{pretty(row.status)}</span></td>
                  <td className="actions">
                    <button className="text-btn" type="button" onClick={() => openEdit(row)}>Edit</button>
                    <button className="danger" type="button" onClick={() => setPendingDelete(row)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {page.total === 0 ? <div className="empty">No customers match these criteria.</div> : null}
        </div>
        <Pager {...page} onPage={page.setPage} />
      </section>
      {editing ? (
        <Modal title={editing === "new" ? "New customer" : "Edit customer"} onClose={() => setEditing(null)}>
          <div className="form-grid">
            <Field label="Full name"><input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></Field>
            <Field label="Email"><input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></Field>
            <Field label="Phone"><input value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></Field>
            <Field label="Status">
              <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as CustomerStatus })}>
                <option value="ACTIVE">Active</option>
                <option value="SUSPENDED">Suspended</option>
              </select>
            </Field>
            <div className="wide">
              <Field label={editing === "new" ? "Password" : "New password"}>
                <input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder={editing === "new" ? "" : "Leave blank to keep the current password"} />
              </Field>
            </div>
          </div>
          {formError ? <p className="form-error">{formError}</p> : null}
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onSave()}>Save customer</button>
          </div>
        </Modal>
      ) : null}
      {pendingDelete ? (
        <Modal title="Remove customer" onClose={() => setPendingDelete(null)}>
          <p>Remove {pendingDelete.fullName}?</p>
          <div className="form-actions">
            <button className="ghost" type="button" onClick={() => setPendingDelete(null)}>Keep</button>
            <button className="primary" type="button" disabled={busy} onClick={() => void onDelete()}>Remove</button>
          </div>
        </Modal>
      ) : null}
    </>
  )
}
