import type { OrderStatus } from "./types"

const whole = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 })
const paise = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const money = (n: number | null | undefined) => {
  const v = Number(n ?? 0)
  return Number.isInteger(v) ? whole.format(v) : paise.format(v)
}

export const dateTime = (iso?: string) =>
  iso ? new Date(iso).toLocaleString("en-IN", { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" }) : "—"
export const date = (iso?: string) => (iso ? new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "—")
export const shortDay = (iso: string) => new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short" })

export const STATUS: Record<OrderStatus, { label: string; variant: "secondary" | "info" | "warning" | "success" | "destructive" | "outline"; action: string }> = {
  PENDING_PAYMENT: { label: "Awaiting payment", variant: "outline", action: "Mark awaiting payment" },
  RECEIVED: { label: "Received", variant: "warning", action: "Mark received" },
  CONFIRMED: { label: "Confirmed", variant: "info", action: "Confirm order" },
  SHIPPED: { label: "Shipped", variant: "secondary", action: "Mark shipped" },
  DELIVERED: { label: "Delivered", variant: "success", action: "Mark delivered" },
  CANCELLED: { label: "Cancelled", variant: "destructive", action: "Cancel order" },
}

export const SHIP: Record<string, string> = { standard: "Standard", express: "Express", pickup: "Store pickup" }

export const initials = (name: string) => name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]!.toUpperCase()).join("")

export const imageUrl = (img: string) => (/^https?:\/\//.test(img) || img.startsWith("/") ? img : `/images/${img}.webp`)
