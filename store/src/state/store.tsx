import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react"
import { toast } from "sonner"
import { api, ApiError } from "@/lib/api"
import { makeMoney } from "@/lib/format"
import type { CartLine, Collection, Product, Promo, StoreData, Width } from "@/lib/types"

/* Bag and saved pairs live in this browser. Storage is wrapped so private windows still work. */
const storage = {
  get<T>(key: string, fallback: T): T {
    try {
      const raw = window.localStorage.getItem(key)
      return raw ? (JSON.parse(raw) as T) : fallback
    } catch {
      return fallback
    }
  },
  set(key: string, value: unknown) {
    try {
      window.localStorage.setItem(key, JSON.stringify(value))
    } catch {
      /* storage unavailable: memory only */
    }
  },
}
const KEY = "hp"

export interface Filters {
  q: string
  collection: string
  sizes: number[]
  priceMax: number | null
  colours: string[]
  wide: boolean
  sort: string
}
export const emptyFilters = (sort = "featured"): Filters => ({ q: "", collection: "all", sizes: [], priceMax: null, colours: [], wide: false, sort })

interface Shop {
  data: StoreData
  byId: Record<string, Product>
  collById: Record<string, Collection>
  money: (n: number) => string
  cart: CartLine[]
  saved: string[]
  promo: Promo | null
  bagBump: number
  addToCart: (id: string, size: number, width: Width, qty?: number) => void
  setQty: (key: string, qty: number) => void
  clearCart: () => void
  toggleSaved: (id: string) => void
  setPromo: (p: Promo | null) => void
  bagOpen: boolean
  setBagOpen: (o: boolean) => void
  searchOpen: boolean
  setSearchOpen: (o: boolean) => void
  filters: Filters
  setFilters: (f: Filters) => void
}

const ShopContext = createContext<Shop | null>(null)

export function useShop() {
  const ctx = useContext(ShopContext)
  if (!ctx) throw new Error("useShop outside StoreProvider")
  return ctx
}

function ShopProvider({ data, children }: { data: StoreData; children: ReactNode }) {
  const byId = useMemo(() => Object.fromEntries(data.products.map((p) => [p.id, p])), [data])
  const collById = useMemo(() => Object.fromEntries(data.collections.map((c) => [c.id, c])), [data])
  const money = useMemo(() => makeMoney(data.config), [data])

  const [cart, setCart] = useState<CartLine[]>(() => storage.get<CartLine[]>(KEY + "-cart", []).filter((l) => byId[l.id]))
  const [saved, setSaved] = useState<string[]>(() => storage.get<string[]>(KEY + "-saved", []).filter((id) => byId[id]))
  const [promo, setPromo] = useState<Promo | null>(() => storage.get<Promo | null>(KEY + "-promo", null))
  const [bagBump, setBagBump] = useState(0)
  const [bagOpen, setBagOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [filters, setFilters] = useState<Filters>(() => emptyFilters())

  useEffect(() => storage.set(KEY + "-cart", cart), [cart])
  useEffect(() => storage.set(KEY + "-saved", saved), [saved])
  useEffect(() => storage.set(KEY + "-promo", promo), [promo])

  const addToCart = useCallback((id: string, size: number, width: Width, qty = 1) => {
    const key = `${id}|${size}|${width}`
    setCart((c) => {
      const line = c.find((l) => l.key === key)
      if (line) return c.map((l) => (l.key === key ? { ...l, qty: Math.min(l.qty + qty, 9) } : l))
      return [...c, { key, id, size, width, qty }]
    })
    setBagBump((n) => n + 1)
  }, [])

  const setQty = useCallback((key: string, qty: number) => {
    setCart((c) => (qty <= 0 ? c.filter((l) => l.key !== key) : c.map((l) => (l.key === key ? { ...l, qty: Math.min(qty, 9) } : l))))
  }, [])

  const clearCart = useCallback(() => {
    setCart([])
    setPromo(null)
  }, [])

  const toggleSaved = useCallback((id: string) => {
    const name = byId[id]?.name ?? "Pair"
    const had = saved.includes(id)
    setSaved((s) => (had ? s.filter((x) => x !== id) : [...s, id]))
    toast(had ? `${name} removed from saved` : `${name} saved`, { description: had ? undefined : "Find it under Saved pairs." })
  }, [byId, saved])

  /* "/" opens search from anywhere outside a text field. */
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement | null
      if (e.key === "/" && !/INPUT|TEXTAREA|SELECT/.test(t?.tagName ?? "") && !t?.isContentEditable) {
        e.preventDefault()
        setSearchOpen(true)
      }
    }
    document.addEventListener("keydown", onKey)
    return () => document.removeEventListener("keydown", onKey)
  }, [])

  const value: Shop = {
    data, byId, collById, money, cart, saved, promo, bagBump, addToCart, setQty, clearCart, toggleSaved, setPromo,
    bagOpen, setBagOpen, searchOpen, setSearchOpen, filters, setFilters,
  }
  return <ShopContext.Provider value={value}>{children}</ShopContext.Provider>
}

export function StoreProvider({ children, fallback }: { children: ReactNode; fallback: (s: { error: string | null; retry: () => void }) => ReactNode }) {
  const [data, setData] = useState<StoreData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let live = true
    api.store()
      .then((d) => { if (live) setData(d) })
      .catch((e: unknown) => { if (live) setError(e instanceof ApiError ? e.message : "The store could not load.") })
    return () => { live = false }
  }, [attempt])

  if (!data) return <>{fallback({ error, retry: () => { setError(null); setAttempt((n) => n + 1) } })}</>
  return <ShopProvider data={data}>{children}</ShopProvider>
}
