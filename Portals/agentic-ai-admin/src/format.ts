import { useEffect, useState } from "react"

export function money(value: number) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value)
}

export function when(iso: string) {
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" }).format(new Date(iso))
}

export function localDay(iso: string) {
  const date = new Date(iso)
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

export function includes(value: string, query: string) {
  return value.toLowerCase().includes(query.trim().toLowerCase())
}

export function usePaged<T>(items: T[], pageSize: number, resetKey: string) {
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
