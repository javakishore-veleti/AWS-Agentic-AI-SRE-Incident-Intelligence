import { useEffect, useState } from "react"
import type { Session } from "./api"

const KEY = "heddle.session"

export function loadSession(): Session | null {
  const raw = localStorage.getItem(KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function saveSession(session: Session) {
  localStorage.setItem(KEY, JSON.stringify(session))
  window.dispatchEvent(new Event("heddle-session"))
}

export function clearSession() {
  localStorage.removeItem(KEY)
  window.dispatchEvent(new Event("heddle-session"))
}

export function useSession() {
  const [session, setSession] = useState<Session | null>(() => loadSession())
  useEffect(() => {
    const sync = () => setSession(loadSession())
    window.addEventListener("heddle-session", sync)
    window.addEventListener("storage", sync)
    return () => {
      window.removeEventListener("heddle-session", sync)
      window.removeEventListener("storage", sync)
    }
  }, [])
  return session
}
