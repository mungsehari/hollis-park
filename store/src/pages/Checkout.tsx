import { motion } from "motion/react"
import { useState, type FormEvent } from "react"
import { Link } from "react-router"
import { Radio } from "@base-ui/react/radio"
import { RadioGroup } from "@base-ui/react/radio-group"
import { CheckIcon, CreditCardIcon, LockIcon, ShieldCheckIcon } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { api, ApiError } from "@/lib/api"
import { EMAIL_RE, etaText, totals, unitPrice } from "@/lib/format"
import { openRazorpay } from "@/lib/razorpay"
import type { CartLine, ShipMethod } from "@/lib/types"
import { useShop } from "@/state/store"
import { PageShell } from "@/components/store/page"
import { TotalsList } from "./Cart"

interface Done { order: string; first: string; eta: string; total: number; items: CartLine[]; paid: boolean }
interface Awaiting {
  order: string; first: string; email: string; eta: string; total: number; items: CartLine[]
  payment: { keyId: string; razorpayOrderId: string; amountPaise: number; currency: string; testMode: boolean }
}

export function Checkout() {
  const { data, cart, byId, promo, money, clearCart } = useShop()
  const { config, site } = data
  const [ship, setShip] = useState<ShipMethod>("standard")
  const [form, setForm] = useState({ email: "", first: "", last: "", address: "", city: "", pin: "" })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState<Done | null>(null)
  const [awaiting, setAwaiting] = useState<Awaiting | null>(null)
  const [payBusy, setPayBusy] = useState(false)
  const [payNote, setPayNote] = useState<string | null>(null)
  const t = totals(cart, byId, config, promo, ship)
  const shipLabel = { standard: "Standard delivery", express: "Express delivery", pickup: "Store pickup" }[ship]
  const pinRe = new RegExp(`^${config.postcode.pattern}$`)

  const validate = () => {
    const e: Record<string, string> = {}
    if (!EMAIL_RE.test(form.email.trim())) e.email = "Enter an email so we can reach you about the order."
    if (!form.first.trim()) e.first = "Add a first name."
    if (!form.last.trim()) e.last = "Add a last name."
    if (!form.address.trim()) e.address = "Add the street and house number."
    if (!form.city.trim()) e.city = "Add a city."
    if (!pinRe.test(form.pin.trim())) e.pin = config.postcode.hint
    setErrors(e)
    return e
  }

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    const e = validate()
    if (Object.keys(e).length) {
      toast.error("A few details need attention")
      document.getElementById(`co-${Object.keys(e)[0]}`)?.focus()
      return
    }
    setBusy(true)
    try {
      const r = await api.placeOrder({
        ...form, email: form.email.trim(), pin: form.pin.trim(), ship, promo: promo?.code ?? null,
        items: cart.map(({ id, size, width, qty }) => ({ id, size, width, qty })),
      })
      const eta = ship === "pickup" ? "Ready in about 2 hours" : etaText(ship, config.locale)
      const items = cart.map((l) => ({ ...l }))
      if (r.payment) {
        // Payment is on: the order exists but is waiting. The bag is cleared only once it is paid.
        const next: Awaiting = { order: r.orderNumber, first: form.first.trim(), email: form.email.trim(), eta, total: r.total, items, payment: r.payment }
        setAwaiting(next)
        void pay(next)
      } else {
        setDone({ order: r.orderNumber, first: form.first.trim(), total: r.total, items, eta, paid: false })
        clearCart()
      }
    } catch (err) {
      if (err instanceof ApiError && err.fields) {
        const map: Record<string, string> = {}
        Object.entries(err.fields).forEach(([k, v]) => { map[k] = v })
        setErrors(map)
      }
      toast.error(err instanceof ApiError ? err.message : "Something went wrong. Please try again.")
    } finally {
      setBusy(false)
    }
  }

  const pay = async (a: Awaiting) => {
    setPayBusy(true)
    setPayNote(null)
    try {
      await openRazorpay({
        keyId: a.payment.keyId, razorpayOrderId: a.payment.razorpayOrderId, amountPaise: a.payment.amountPaise, currency: a.payment.currency,
        storeName: site.name, description: `Order ${a.order}`, orderNumber: a.order, customer: { name: a.first, email: a.email },
        onDismiss: () => { setPayBusy(false); setPayNote("The payment window was closed. Your order is saved, but it is not paid yet.") },
        onFailed: (m) => { setPayBusy(false); setPayNote(`${m} You can try again.`) },
        onSuccess: async (resp) => {
          try {
            await api.confirmPayment(a.order, { razorpayOrderId: resp.razorpay_order_id, razorpayPaymentId: resp.razorpay_payment_id, razorpaySignature: resp.razorpay_signature })
            setDone({ order: a.order, first: a.first, eta: a.eta, total: a.total, items: a.items, paid: true })
            setAwaiting(null)
            clearCart()
          } catch (err) {
            setPayBusy(false)
            setPayNote(err instanceof ApiError ? err.message : "We could not confirm the payment. If money was taken, it will be refunded.")
          }
        },
      })
    } catch (err) {
      setPayBusy(false)
      setPayNote(err instanceof Error ? err.message : "Could not open the payment window.")
    }
  }

  const field = (name: keyof typeof form, label: string, extra: React.ComponentProps<typeof Input> = {}, span = false) => (
    <div className={span ? "sm:col-span-2" : ""}>
      <label htmlFor={`co-${name}`} className="mb-1.5 block text-sm font-medium">{label}</label>
      <Input id={`co-${name}`} value={form[name]} aria-invalid={!!errors[name]} aria-describedby={errors[name] ? `co-${name}-err` : undefined}
        onChange={(e) => { setForm({ ...form, [name]: e.target.value }); if (errors[name]) setErrors({ ...errors, [name]: "" }) }} className="h-12 rounded-xl px-4" {...extra} />
      {errors[name] && <p id={`co-${name}-err`} className="mt-1.5 text-sm text-destructive">{errors[name]}</p>}
    </div>
  )

  if (done) {
    return (
      <PageShell title="Order received" trail={[["Home", "/"], ["Checkout", null]]}>
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="grid gap-8 lg:grid-cols-[1.4fr_1fr]">
          <div className="rounded-3xl bg-accent p-8 sm:p-12">
            <motion.span initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: "spring", stiffness: 300, damping: 16, delay: 0.15 }} className="flex size-14 items-center justify-center rounded-full bg-sage-deep text-white"><CheckIcon className="size-7" /></motion.span>
            <h2 className="mt-6 font-heading text-4xl font-extrabold uppercase">Thanks, {done.first}.</h2>
            <p className="mt-3 max-w-lg text-lg text-foreground/80">{done.paid
              ? `Payment received. Your order is with the ${site.store} team. Keep your order number for any questions.`
              : `Your order is with the ${site.store} team. Nothing was charged, because online payment is not connected yet. Keep your order number for any questions.`}</p>
            <dl className="mt-8 grid gap-4 sm:grid-cols-3">
              {[["Order", done.order], ["Delivery", done.eta], ["Total", money(done.total)]].map(([k, v]) => (
                <div key={k} className="rounded-2xl bg-background p-4"><dt className="font-mono text-[11px] tracking-[0.15em] text-muted-foreground uppercase">{k}</dt><dd className="mt-1 font-semibold tabular-nums">{v}</dd></div>
              ))}
            </dl>
            <div className="mt-8 flex gap-3"><Button className="rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Keep shopping</Button><Button variant="outline" className="rounded-full bg-background" nativeButton={false} render={<Link to="/delivery" />}>Returns and resoles</Button></div>
          </div>
          <aside className="rounded-3xl border bg-card p-6">
            <h2 className="font-heading text-xl font-extrabold uppercase">In this order</h2>
            <ul className="mt-4 space-y-3">
              {done.items.map((l) => { const p = byId[l.id]; return p && (
                <li key={l.key} className="flex items-center gap-3"><img src={p.img} alt="" className="size-14 rounded-xl object-cover" />
                  <span className="flex-1 text-sm"><b className="block">{p.name} × {l.qty}</b><span className="text-muted-foreground">{config.sizeLabel} {l.size}{l.width === "wide" ? " Wide" : ""}</span></span>
                  <span className="tabular-nums">{money(unitPrice(p, l.width) * l.qty)}</span></li>
              ) })}
            </ul>
          </aside>
        </motion.div>
      </PageShell>
    )
  }

  if (awaiting) {
    return (
      <PageShell title="Complete your payment" trail={[["Bag", "/cart"], ["Checkout", null]]}>
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="mx-auto max-w-xl rounded-3xl border bg-card p-8 text-center sm:p-12">
          <span className="mx-auto flex size-14 items-center justify-center rounded-full bg-accent"><CreditCardIcon className="size-6" /></span>
          <h2 className="mt-5 font-heading text-3xl font-extrabold uppercase">Order {awaiting.order}</h2>
          <p className="mt-2 text-lg text-muted-foreground">Pay <b className="text-foreground tabular-nums">{money(awaiting.total)}</b> to confirm your order.</p>
          {awaiting.payment.testMode && <p className="mt-4 rounded-xl bg-warning/15 p-3 text-sm text-warning-foreground">Demo mode: no real money is charged. Use Razorpay's test card or UPI.</p>}
          {payNote && <p role="alert" className="mt-4 text-sm text-destructive">{payNote}</p>}
          <Button size="lg" disabled={payBusy} onClick={() => void pay(awaiting)} className="mt-6 h-14 w-full rounded-full text-base">{payBusy ? "Waiting for payment" : "Pay now"}</Button>
          <p className="mt-4 flex items-center justify-center gap-2 text-sm text-muted-foreground"><LockIcon className="size-4" />Payment is handled by Razorpay. We never see your card details.</p>
        </motion.div>
      </PageShell>
    )
  }

  if (!cart.length) {
    return (
      <PageShell title="Checkout" trail={[["Bag", "/cart"], ["Checkout", null]]}>
        <div className="flex flex-col items-center gap-3 rounded-3xl border border-dashed py-20 text-center">
          <h2 className="font-heading text-2xl font-extrabold uppercase">Nothing to check out</h2>
          <p className="text-muted-foreground">Add a pair to your bag first.</p>
          <Button className="mt-2 rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Shop all pairs</Button>
        </div>
      </PageShell>
    )
  }

  const options: [ShipMethod, string, string, string][] = [
    ["standard", "Standard", `3-5 working days. ${etaText("standard", config.locale)}`, t.subtotal >= config.freeShipFrom ? "Free" : money(config.shipFee)],
    ["express", "Express", `${etaText("express", config.locale)} if ordered by 2pm`, money(config.expressFee)],
    ["pickup", `Pick up at ${site.store}`, "Ready in 2 hours, 41 Quill Lane", "Free"],
  ]

  return (
    <PageShell title="Checkout" trail={[["Bag", "/cart"], ["Checkout", null]]} lead={`Your order goes straight to the ${site.store} team. Online payment is not connected yet, so nothing is charged here.`}>
      <div className="grid gap-10 lg:grid-cols-[1.5fr_1fr]">
        <form onSubmit={submit} noValidate className="space-y-10">
          <section aria-labelledby="s1"><h2 id="s1" className="mb-5 flex items-center gap-3 font-heading text-xl font-extrabold uppercase"><span className="flex size-8 items-center justify-center rounded-full bg-foreground text-sm text-background">1</span>Contact</h2>
            <div className="grid gap-4">{field("email", "Email address", { type: "email", autoComplete: "email" })}</div></section>
          <section aria-labelledby="s2"><h2 id="s2" className="mb-5 flex items-center gap-3 font-heading text-xl font-extrabold uppercase"><span className="flex size-8 items-center justify-center rounded-full bg-foreground text-sm text-background">2</span>Delivery address</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {field("first", "First name", { autoComplete: "given-name" })}{field("last", "Last name", { autoComplete: "family-name" })}
              {field("address", "Street address", { autoComplete: "street-address" }, true)}{field("city", "City", { autoComplete: "address-level2" })}
              {field("pin", config.postcode.label, { autoComplete: "postal-code", inputMode: config.postcode.numeric ? "numeric" : "text" })}
            </div></section>
          <section aria-labelledby="s3"><h2 id="s3" className="mb-5 flex items-center gap-3 font-heading text-xl font-extrabold uppercase"><span className="flex size-8 items-center justify-center rounded-full bg-foreground text-sm text-background">3</span>Delivery method</h2>
            <RadioGroup value={ship} onValueChange={(v) => setShip(v as ShipMethod)} aria-label="Delivery method" className="grid gap-3">
              {options.map(([val, title, sub, price]) => (
                <label key={val} className="flex cursor-pointer items-center gap-4 rounded-2xl border p-4 transition-colors hover:border-foreground/40 has-[[data-checked]]:border-foreground has-[[data-checked]]:bg-muted has-[[data-checked]]:shadow-[inset_0_0_0_1px_var(--foreground)]">
                  <Radio.Root value={val} className="flex size-5 shrink-0 items-center justify-center rounded-full border-2 data-checked:border-foreground data-checked:after:size-2.5 data-checked:after:rounded-full data-checked:after:bg-foreground" aria-label={title} />
                  <span className="flex-1"><b className="block">{title}</b><span className="text-sm text-muted-foreground">{sub}</span></span>
                  <span className="font-semibold tabular-nums">{price}</span>
                </label>
              ))}
            </RadioGroup></section>
          <section aria-labelledby="s4"><h2 id="s4" className="mb-5 flex items-center gap-3 font-heading text-xl font-extrabold uppercase"><span className="flex size-8 items-center justify-center rounded-full bg-foreground text-sm text-background">4</span>Payment</h2>
            <div className="flex gap-4 rounded-2xl bg-muted p-5"><LockIcon className="mt-0.5 size-5 shrink-0" /><div>
              <p className="font-semibold">{data.payments?.enabled ? "Pay securely with Razorpay." : "Payment is not collected online yet."}</p>
              <p className="mt-1 text-sm text-muted-foreground">{data.payments?.enabled
                ? `Cards, UPI and net banking. The payment window opens after you place the order.${data.payments.testMode ? " Demo mode: no real money is charged." : ""}`
                : "When a payment provider is connected, its secure card form appears here. Until then the order is recorded and nothing is charged."}</p></div></div></section>
          <div>
            <Button type="submit" size="lg" disabled={busy} className="h-14 w-full rounded-full text-base">{busy ? "Placing order" : data.payments?.enabled ? "Place order and pay" : "Place order"}</Button>
            <p className="mt-3 flex items-center gap-2 text-sm text-muted-foreground"><ShieldCheckIcon className="size-4" />Prices and stock are checked again by the store before the order is saved.</p>
          </div>
        </form>
        <aside className="h-fit space-y-5 rounded-3xl border bg-card p-6 lg:sticky lg:top-28" aria-labelledby="osum">
          <h2 id="osum" className="font-heading text-xl font-extrabold uppercase">Order summary</h2>
          <ul className="space-y-3">
            {cart.map((l) => { const p = byId[l.id]; return p && (
              <li key={l.key} className="flex items-center gap-3">
                <span className="relative"><img src={p.img} alt="" className="size-14 rounded-xl object-cover" /><span className="absolute -top-1.5 -right-1.5 flex size-5 items-center justify-center rounded-full bg-foreground text-[11px] text-background">{l.qty}</span></span>
                <span className="flex-1 text-sm"><b className="block">{p.name}</b><span className="text-muted-foreground">{config.sizeLabel} {l.size}{l.width === "wide" ? " Wide" : ""}</span></span>
                <span className="tabular-nums">{money(unitPrice(p, l.width) * l.qty)}</span>
              </li>
            ) })}
          </ul>
          <TotalsList t={t} shipLabel={shipLabel} />
        </aside>
      </div>
    </PageShell>
  )
}
