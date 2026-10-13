export type Role = "ADMIN" | "STAFF"
export type OrderStatus = "PENDING_PAYMENT" | "RECEIVED" | "CONFIRMED" | "SHIPPED" | "DELIVERED" | "CANCELLED"

export interface Me { id: string; email: string; name: string; role: Role }

export interface Page<T> { items: T[]; total: number; page: number; size: number }

export interface OrderSummary {
  id: string; orderNumber: string; name: string; email: string; city: string; items: number
  total: number; status: OrderStatus; paymentStatus?: string; ship: string; createdAt: string
}

export interface OrderLine { productId: string; name: string; size: number; width: string; qty: number; unitPrice: number; lineTotal: number; image?: string }
export interface StatusEvent { status: OrderStatus; at: string; by: string; note?: string }

export interface OrderDetail {
  id: string; orderNumber: string; status: OrderStatus; nextStatuses: OrderStatus[]
  email: string; first: string; last: string; address: string; city: string; pin: string; ship: string; promo?: string
  subtotal: number; discount: number; shipping: number; tax: number; total: number
  adminNote?: string; items: OrderLine[]; history: StatusEvent[]; createdAt: string; updatedAt?: string
  paymentStatus?: string; razorpayPaymentId?: string; paidAt?: string
}

export interface Product {
  id: string; name: string; collection: string; line: string; colourway: string; swatch: string; colours: string[]
  price: number; wide: { extra: number; soldOut: number[] } | null; rating: number; reviews: number; flag: string | null
  rank: number; added: string; img: string; gallery: string[]; context: string | null; contextCaption?: string; alt: string
  blurb: string; desc: string; specs: [string, string, string][]; materials: [string, string][]
  fit: { verdict: string; text: string }; care: string; soldOut: number[]; low: number[]; keywords: string
  amazonUrl?: string | null; flipkartUrl?: string | null
}
export interface ProductAdmin { product: Product; active: boolean; updatedAt?: string }

export interface Message {
  id: string; name: string; email: string; topic: string; orderNumber?: string; slot?: string
  message: string; status: "NEW" | "HANDLED"; createdAt: string
}
export interface Subscriber { id: string; email: string; topics: string[]; createdAt: string }
export interface Staff { id: string; email: string; name: string; role: Role; active: boolean; createdAt: string; lastLoginAt?: string }
export interface Settings { taxMode: "included" | "added"; taxRate: number; taxName: string; freeShipFrom: number; shipFee: number; expressFee: number }
export interface Promo { code: string; rate: number; active: boolean; createdAt: string }

export interface Dashboard {
  ordersToday: number; revenueToday: number; orders7d: number; revenue7d: number; revenue30d: number; averageOrder30d: number
  openOrders: number; newMessages: number; subscribers: number; activeProducts: number
  statusCounts: Record<OrderStatus, number>
  salesByDay: { date: string; orders: number; revenue: number }[]
  topProducts: { id: string; name: string; qty: number; revenue: number }[]
  recentOrders: OrderSummary[]
  marketplaceClicks30d?: Record<Marketplace, number>
  topOutbound30d?: { name: string; marketplace: string; clicks: number }[]
}

export interface StoreInfo {
  site: { name: string; store: string; orderPrefix: string }
  collections: { id: string; name: string }[]
  sizes: number[]
  colours: Record<string, { label: string; hex: string }>
}

export type Marketplace = "AMAZON" | "FLIPKART"

/** An affiliate product shown under "Care for your pair" in the store. */
export interface CarePick {
  id: string | null; title: string; blurb?: string | null; image?: string | null
  marketplace: Marketplace; url: string; sortOrder: number; active: boolean
}

export interface MediaItem { id: string; url: string; originalName?: string; contentType: string; sizeBytes: number; alt?: string; createdAt: string }

export interface CollectionItem {
  id: string; name: string; tagline?: string; intro?: string; image?: string | null; alt?: string
  choose: string[]; guide: { icon: string; t: string; p: string }[]; sortOrder: number; active: boolean
}

export interface HomeContent {
  hero: { eyebrow?: string; title: string; subtitle?: string; image?: string | null; imageAlt?: string
    ctaLabel?: string; ctaLink?: string; secondaryLabel?: string; secondaryLink?: string }
  banner: { enabled: boolean; text?: string; link?: string }
  spotlightProductId?: string
}
