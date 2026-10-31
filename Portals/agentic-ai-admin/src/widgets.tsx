import { useEffect } from "react"

export function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose()
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [onClose])

  return (
    <div className="modal-back" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()}>
        <header>
          <h2>{title}</h2>
          <button className="icon-btn" type="button" onClick={onClose} aria-label="Close">×</button>
        </header>
        {children}
      </div>
    </div>
  )
}

export function Pagination({ page, pages, from, to, total, onPage }: {
  page: number
  pages: number
  from: number
  to: number
  total: number
  onPage: (page: number) => void
}) {
  return (
    <div className="pager">
      <span>{total === 0 ? "No records in this view" : `${from}–${to} of ${total}`}</span>
      <div>
        <button className="btn ghost tiny" type="button" disabled={page <= 1} onClick={() => onPage(page - 1)}>Previous</button>
        <span>Page {page} of {pages}</span>
        <button className="btn ghost tiny" type="button" disabled={page >= pages} onClick={() => onPage(page + 1)}>Next</button>
      </div>
    </div>
  )
}

export function Pill({ value }: { value: string }) {
  return <span className={`pill ${value}`}>{value.charAt(0) + value.slice(1).toLowerCase()}</span>
}

export function Banner({ text }: { text: string | null }) {
  if (!text) return null
  return <div className="banner" role="alert">{text}</div>
}

export function Toast({ text }: { text: string | null }) {
  if (!text) return null
  return <div className="toast" role="status">{text}</div>
}
