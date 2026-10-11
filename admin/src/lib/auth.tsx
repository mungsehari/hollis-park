import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react"
import { Navigate, useLocation } from "react-router"
import { Spinner } from "@/components/ui/spinner"
import { api, setUnauthorizedHandler } from "./api"
import type { Me, Role } from "./types"

interface Auth {
  me: Me | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  can: (role: Role) => boolean
}

const AuthContext = createContext<Auth | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setUnauthorizedHandler(() => setMe(null))
    api.get<Me>("/api/auth/me").then(setMe).catch(() => setMe(null)).finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    setMe(await api.post<Me>("/api/auth/login", { email, password }))
  }, [])

  const logout = useCallback(async () => {
    await api.post("/api/auth/logout").catch(() => undefined)
    setMe(null)
  }, [])

  const can = useCallback((role: Role) => !!me && (me.role === "ADMIN" || me.role === role), [me])

  return <AuthContext.Provider value={{ me, loading, login, logout, can }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth outside AuthProvider")
  return ctx
}

export function RequireAuth({ children, role }: { children: ReactNode; role?: Role }) {
  const { me, loading, can } = useAuth()
  const location = useLocation()
  if (loading) {
    return <div className="flex min-h-svh items-center justify-center"><Spinner className="size-6" /></div>
  }
  if (!me) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  if (role && !can(role)) return <Navigate to="/" replace />
  return <>{children}</>
}
