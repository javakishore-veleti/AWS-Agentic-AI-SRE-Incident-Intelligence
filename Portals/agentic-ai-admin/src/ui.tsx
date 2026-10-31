import { useEffect, useState, type ReactNode } from "react"

export const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" })

export function pretty(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}

export function when(iso: string) {
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" }).format(new Date(iso))
}

export function dayKey(iso: string) {
  const date = new Date(iso)
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

export function usePaged<T>(items: T[], resetKey: string, pageSize = 5) {
  const [page, setPage] = useState(1)
  useEffect(() => {
    setPage(1)
  }, [resetKey])
  const pages = Math.max(1, Math.ceil(items.length / pageSize))
  const current = Math.min(page, pages)
  const start = (current - 1) * pageSize
  return {
    page: current,
    pages,
    total: items.length,
    from: items.length === 0 ? 0 : start + 1,
    to: Math.min(start + pageSize, items.length),
    view: items.slice(start, start + pageSize),
    setPage,
  }
}

export function Pager(props: {
  page: number
  pages: number
  from: number
  to: number
  total: number
  onPage: (page: number) => void
}) {
  return (
    <div className="pager">
      <span>{props.total === 0 ? "No records in this view" : `${props.from}–${props.to} of ${props.total}`}</span>
      <div className="pager-controls">
        <button type="button" disabled={props.page <= 1} onClick={() => props.onPage(props.page - 1)}>Previous</button>
        <strong>Page {props.page} of {props.pages}</strong>
        <button type="button" disabled={props.page >= props.pages} onClick={() => props.onPage(props.page + 1)}>Next</button>
      </div>
    </div>
  )
}

export function Modal(props: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-back" onClick={props.onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={props.title} onClick={(event) => event.stopPropagation()}>
        <header>
          <h2>{props.title}</h2>
          <button type="button" className="text-btn" onClick={props.onClose}>Close</button>
        </header>
        {props.children}
      </div>
    </div>
  )
}

export function Field(props: { label: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{props.label}</span>
      {props.children}
    </label>
  )
}
