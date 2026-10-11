import { AnimatePresence, motion } from "motion/react"
import { Link } from "react-router"
import { ArrowRightIcon, ShoppingBagIcon, Trash2Icon, TruckIcon } from "lucide-react"
import { NumberField, NumberFieldDecrement, NumberFieldGroup, NumberFieldIncrement, NumberFieldInput } from "@/components/reui/number-field"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { POPULAR } from "@/lib/catalog"
import { totals, unitPrice } from "@/lib/format"
import type { CartLine } from "@/lib/types"
import { useShop } from "@/state/store"

export function FreeDeliveryMeter({ subtotal }: { subtotal: number }) {
  const { data, money } = useShop()
  const goal = data.config.freeShipFrom
  const left = Math.max(0, goal - subtotal)
  return (
    <div className="rounded-xl bg-muted p-3.5">
      <p className="flex items-center gap-2 text-sm">
        <TruckIcon className="size-4 text-sage-deep" />
        {left > 0 ? <span>You are <b className="tabular-nums">{money(left)}</b> away from free delivery</span> : <span><b>Free delivery</b> is included</span>}
      </p>
      <div className="mt-2.5 h-1.5 overflow-hidden rounded-full bg-background">
        <motion.div className="h-full rounded-full bg-sage-deep" initial={false} animate={{ width: `${Math.min(100, (subtotal / goal) * 100)}%` }} transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }} />
      </div>
    </div>
  )
}

export function BagLine({ l, compact = false }: { l: CartLine; compact?: boolean }) {
  const { byId, money, setQty, data } = useShop()
  const p = byId[l.id]
  if (!p) return null
  return (
    <motion.li layout initial={{ opacity: 0, x: 16 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40, transition: { duration: 0.25 } }} className="flex gap-4 py-4">
      <Link to={`/catalog/${p.id}`} className="shrink-0 overflow-hidden rounded-xl bg-muted" tabIndex={-1} aria-hidden="true">
        <img src={p.img} alt="" className={compact ? "size-20 object-cover" : "size-24 object-cover sm:size-28"} />
      </Link>
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex justify-between gap-3">
          <div className="min-w-0">
            <Link to={`/catalog/${p.id}`} className="block truncate font-heading font-extrabold uppercase hover:underline">{p.name}</Link>
            <p className="text-sm text-muted-foreground">{p.colourway} · {data.config.sizeLabel} {l.size}{l.width === "wide" ? " Wide" : ""}</p>
          </div>
          <p className="font-semibold tabular-nums">{money(unitPrice(p, l.width) * l.qty)}</p>
        </div>
        <div className="mt-auto flex items-center justify-between pt-3">
          <NumberField value={l.qty} min={1} max={9} onValueChange={(v) => setQty(l.key, v ?? 1)} size="sm" className="w-28" aria-label={`Quantity of ${p.name}`}>
            <NumberFieldGroup><NumberFieldDecrement /><NumberFieldInput /><NumberFieldIncrement /></NumberFieldGroup>
          </NumberField>
          <Button variant="ghost" size="sm" className="text-muted-foreground" onClick={() => setQty(l.key, 0)} aria-label={`Remove ${p.name}`}>
            <Trash2Icon />Remove
          </Button>
        </div>
      </div>
    </motion.li>
  )
}

export function BagSheet() {
  const { bagOpen, setBagOpen, cart, byId, data, promo, money } = useShop()
  const t = totals(cart, byId, data.config, promo)
  return (
    <Sheet open={bagOpen} onOpenChange={setBagOpen}>
      <SheetContent side="right" className="flex w-full flex-col gap-0 sm:max-w-md">
        <SheetHeader className="border-b">
          <SheetTitle className="font-heading text-lg font-extrabold uppercase">Your bag {t.items > 0 && <span className="text-muted-foreground tabular-nums">({t.items})</span>}</SheetTitle>
        </SheetHeader>
        {cart.length ? (
          <>
            <div className="flex-1 overflow-y-auto px-4">
              <div className="pt-4"><FreeDeliveryMeter subtotal={t.subtotal} /></div>
              <ul className="divide-y"><AnimatePresence initial={false}>{cart.map((l) => <BagLine key={l.key} l={l} compact />)}</AnimatePresence></ul>
            </div>
            <SheetFooter className="gap-2.5 border-t">
              <div className="flex items-baseline justify-between"><span className="text-muted-foreground">Subtotal <span className="text-xs">(delivery and codes next)</span></span><span className="text-xl font-semibold tabular-nums">{money(t.subtotal)}</span></div>
              <Button size="lg" className="h-12 rounded-full text-base" nativeButton={false} render={<Link to="/checkout" onClick={() => setBagOpen(false)} />}>
                Checkout <ArrowRightIcon />
              </Button>
              <Link to="/cart" onClick={() => setBagOpen(false)} className="text-center text-sm font-medium underline underline-offset-4">View full bag</Link>
            </SheetFooter>
          </>
        ) : (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
            <motion.span initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="flex size-16 items-center justify-center rounded-full bg-muted"><ShoppingBagIcon className="size-7" /></motion.span>
            <p className="font-heading text-xl font-extrabold uppercase">Your bag is empty</p>
            <p className="text-sm text-muted-foreground">Start with a bestseller, or find your size first.</p>
            <div className="mt-4 grid w-full gap-2">
              {POPULAR.bestsellers(data.products).slice(0, 2).map((p) => (
                <Link key={p.id} to={`/catalog/${p.id}`} onClick={() => setBagOpen(false)} className="flex items-center gap-3 rounded-xl border p-2 text-left transition-colors hover:bg-muted">
                  <img src={p.img} alt="" className="size-14 rounded-lg object-cover" />
                  <span className="flex-1"><b className="block">{p.name}</b><span className="text-sm text-muted-foreground">{money(p.price)}</span></span>
                  <ArrowRightIcon className="mr-2 size-4" />
                </Link>
              ))}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}
