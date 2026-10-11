/** Razorpay Checkout (checkout.js) loaded on demand, only when a payment is actually started. */
export interface RazorpaySuccess { razorpay_payment_id: string; razorpay_order_id: string; razorpay_signature: string }

interface RazorpayOptions {
  key: string
  amount: number
  currency: string
  order_id: string
  name: string
  description?: string
  prefill?: { name?: string; email?: string }
  notes?: Record<string, string>
  theme?: { color?: string }
  handler: (r: RazorpaySuccess) => void
  modal?: { ondismiss?: () => void }
}
interface RazorpayInstance {
  open: () => void
  on: (event: "payment.failed", cb: (r: { error?: { description?: string } }) => void) => void
}
declare global { interface Window { Razorpay?: new (o: RazorpayOptions) => RazorpayInstance } }

const SRC = "https://checkout.razorpay.com/v1/checkout.js"
let loading: Promise<void> | null = null

export function loadRazorpay(): Promise<void> {
  if (window.Razorpay) return Promise.resolve()
  loading ??= new Promise<void>((resolve, reject) => {
    const s = document.createElement("script")
    s.src = SRC
    s.async = true
    s.onload = () => resolve()
    s.onerror = () => { loading = null; s.remove(); reject(new Error("Could not load the payment window. Check your connection and try again.")) }
    document.head.appendChild(s)
  })
  return loading
}

export async function openRazorpay(opts: {
  keyId: string; razorpayOrderId: string; amountPaise: number; currency: string
  storeName: string; description: string; customer: { name: string; email: string }; orderNumber: string
  onSuccess: (r: RazorpaySuccess) => void; onDismiss: () => void; onFailed: (message: string) => void
}) {
  await loadRazorpay()
  if (!window.Razorpay) throw new Error("The payment window is not available.")
  const rzp = new window.Razorpay({
    key: opts.keyId,
    amount: opts.amountPaise,
    currency: opts.currency,
    order_id: opts.razorpayOrderId,
    name: opts.storeName,
    description: opts.description,
    prefill: opts.customer,
    notes: { order: opts.orderNumber },
    theme: { color: "#5E725B" },
    handler: opts.onSuccess,
    modal: { ondismiss: opts.onDismiss },
  })
  rzp.on("payment.failed", (r) => opts.onFailed(r.error?.description || "The payment did not go through."))
  rzp.open()
}
