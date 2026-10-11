import { AnimatePresence, motion } from "motion/react"
import { useState, type FormEvent } from "react"
import { Link } from "react-router"
import { ArrowRightIcon, CheckIcon, LockIcon, RotateCcwIcon, ShoppingBagIcon, XIcon } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { api, ApiError } from "@/lib/api"
import { countWord, pct, totals } from "@/lib/format"
import { useShop } from "@/state/store"
import { BagLine, FreeDeliveryMeter } from "@/components/store/bag-sheet"
import { PageShell } from "@/components/store/page"

export function TotalsList({ t, shipLabel }: { t: { subtotal: number; discount: number; shipping: number; tax: number; total: number }; shipLabel: string }) {
  const { money, promo, data } = useShop()
  const { taxMode, taxRate, taxName } = data.config
  const row = (label: string, value: string, extra = "") => <div className={`flex justify-between ${extra}`}><dt className="text-muted-foreground">{label}</dt><dd className="tabular-nums">{value}</dd></div>
  return (
    <dl className="space-y-2.5 text-sm">
      {row("Subtotal", money(t.subtotal))}
      {t.discount > 0 && row(`Discount (${promo?.code})`, `-${money(t.discount)}`, "text-success")}
      {row(shipLabel, t.shipping ? money(t.shipping) : "Free")}
      {t.tax > 0 && row(taxMode === "included" ? `Includes ${Math.round(taxRate * 100)}% ${taxName}` : `Estimated ${taxName}`, money(t.tax))}
      <div className="flex justify-between border-t pt-3 text-base font-semibold"><dt>Total</dt><dd className="tabular-nums">{money(t.total)}</dd></div>
    </dl>
  )
}

export function Cart() {
  const { data, cart, byId, promo, setPromo } = useShop()
  const [code, setCode] = useState(promo?.code ?? "")
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const t = totals(cart, byId, data.config, promo)

  const apply = async (e: FormEvent) => {
    e.preventDefault()
    if (!code.trim()) { setError("Enter a discount code."); return }
    setBusy(true)
    setError(null)
    try {
      const r = await api.validatePromo(code.trim())
      setPromo(r)
      setCode(r.code)
      toast.success(`${r.code} applied`, { description: `${pct(r.rate)} off your order.` })
    } catch (err) {
      setError(err instanceof ApiError && err.status === 422 ? "That code is not valid. Check the code from your newsletter email." : "Could not check the code. Try again.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <PageShell title="Your bag" trail={[["Home", "/"], ["Bag", null]]}>
      {!cart.length ? (
        <div className="flex flex-col items-center gap-3 rounded-3xl border border-dashed py-20 text-center">
          <span className="flex size-14 items-center justify-center rounded-full bg-muted"><ShoppingBagIcon className="size-6" /></span>
          <h2 className="font-heading text-2xl font-extrabold uppercase">Your bag is empty</h2>
          <p className="max-w-sm text-muted-foreground">Browse the {countWord(data.products.length)} models, or use the size guide to find your fit before you start.</p>
          <div className="mt-2 flex gap-2"><Button className="rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Shop all pairs</Button><Button variant="outline" className="rounded-full" nativeButton={false} render={<Link to="/size-guide" />}>Find my size</Button></div>
        </div>
      ) : (
        <div className="grid gap-10 lg:grid-cols-[1.6fr_1fr]">
          <div>
            <FreeDeliveryMeter subtotal={t.subtotal} />
            <ul className="mt-2 divide-y"><AnimatePresence initial={false}>{cart.map((l) => <BagLine key={l.key} l={l} />)}</AnimatePresence></ul>
          </div>
          <aside className="h-fit space-y-6 rounded-3xl border bg-card p-6 lg:sticky lg:top-28" aria-labelledby="sum">
            <h2 id="sum" className="font-heading text-xl font-extrabold uppercase">Summary</h2>
            <TotalsList t={t} shipLabel="Standard delivery" />
            <form onSubmit={apply} noValidate>
              <label htmlFor="promo" className="mb-1.5 block text-sm font-medium">Discount code</label>
              <div className="flex gap-2">
                <Input id="promo" value={code} placeholder="Newsletter code" autoComplete="off" aria-invalid={!!error} aria-describedby={error ? "promo-err" : undefined}
                  onChange={(e) => { setCode(e.target.value.toUpperCase()); setError(null) }} className="h-11 rounded-full px-4 font-mono" />
                <Button type="submit" variant="outline" className="h-11 rounded-full" disabled={busy}>Apply</Button>
              </div>
              <AnimatePresence>
                {error && <motion.p id="promo-err" initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="mt-2 text-sm text-destructive">{error}</motion.p>}
                {promo && !error && (
                  <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mt-2 flex items-center gap-1.5 text-sm text-success">
                    <CheckIcon className="size-4" />{promo.code} applied: {pct(promo.rate)} off
                    <button type="button" aria-label="Remove discount code" onClick={() => { setPromo(null); setCode("") }} className="ml-auto rounded-full p-1 text-muted-foreground hover:bg-muted"><XIcon className="size-3.5" /></button>
                  </motion.p>
                )}
              </AnimatePresence>
            </form>
            <Button size="lg" className="h-14 w-full rounded-full text-base" nativeButton={false} render={<Link to="/checkout" />}>Checkout<ArrowRightIcon /></Button>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-center gap-2"><RotateCcwIcon className="size-4" />60-day wear test on every pair</li>
              <li className="flex items-center gap-2"><LockIcon className="size-4" />No online payment is taken yet</li>
            </ul>
          </aside>
        </div>
      )}
    </PageShell>
  )
}
