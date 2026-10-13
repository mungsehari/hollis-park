import type { ShipMethod, StoreData, Width } from "./types"

/** Same-origin by default (Vite proxy locally, CloudFront /api/* in production). */
const BASE =
  (import.meta.env.VITE_API_BASE as string | undefined)?.replace(/\/$/, "") ??
  ""

export class ApiError extends Error {
  readonly status: number
  readonly fields: Record<string, string>
  constructor(
    message: string,
    status: number,
    fields: Record<string, string> = {}
  ) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    res = await fetch(BASE + path, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
      },
    })
  } catch {
    throw new ApiError(
      "We could not reach the store. Check your connection and try again.",
      0
    )
  }
  const body = await res.json().catch(() => ({}))
  if (!res.ok)
    throw new ApiError(
      body.message || "Something went wrong. Please try again.",
      res.status,
      body.fields || {}
    )
  return body as T
}

const post = <T>(path: string, data: unknown) =>
  request<T>(path, { method: "POST", body: JSON.stringify(data) })

export const api = {
  store: () => request<StoreData>("/api/store"),
  validatePromo: (code: string) =>
    post<{ code: string; rate: number }>("/api/promo/validate", { code }),
  placeOrder: (order: {
    email: string
    first: string
    last: string
    address: string
    city: string
    pin: string
    ship: ShipMethod
    promo: string | null
    items: { id: string; size: number; width: Width; qty: number }[]
  }) =>
    post<{
      orderNumber: string
      status: string
      paymentStatus: string
      subtotal: number
      discount: number
      shipping: number
      tax: number
      total: number
      payment?: {
        keyId: string
        razorpayOrderId: string
        amountPaise: number
        currency: string
        testMode: boolean
      }
    }>("/api/orders", order),
  confirmPayment: (
    orderNumber: string,
    p: {
      razorpayOrderId: string
      razorpayPaymentId: string
      razorpaySignature: string
    }
  ) =>
    post<{ orderNumber: string; status: string; paymentStatus: string }>(
      `/api/orders/${encodeURIComponent(orderNumber)}/payment`,
      p
    ),
  contact: (msg: {
    name: string
    email: string
    topic: string
    order?: string
    slot?: string
    message: string
  }) => post<{ status: string }>("/api/contact", msg),
  newsletter: (email: string, topics: string[]) =>
    post<{
      status: string
      promoCode: string | null
      promoRate: number | null
    }>("/api/newsletter", { email, topics }),
}

/** Links out to a marketplace. They pass through the store so each click is counted, then redirect. */
export const go = {
  product: (slug: string, marketplace: "amazon" | "flipkart") =>
    `${BASE}/api/go/product/${encodeURIComponent(slug)}/${marketplace}`,
  pick: (id: string) => `${BASE}/api/go/pick/${encodeURIComponent(id)}`,
}
