import { AnimatePresence, motion } from "motion/react"
import { useEffect, useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router"
import { FootprintsIcon, SlidersHorizontalIcon, XIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Slider } from "@/components/ui/slider"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { SORTS } from "@/lib/catalog"
import { inStock, matchesQuery } from "@/lib/format"
import type { Product } from "@/lib/types"
import { emptyFilters, useShop, type Filters } from "@/state/store"
import { PageShell } from "@/components/store/page"
import { ProductCard } from "@/components/store/product-card"

const SORT_OPTIONS = [
  { value: "featured", label: "Featured" },
  { value: "new", label: "Newest" },
  { value: "price-asc", label: "Price: low to high" },
  { value: "price-desc", label: "Price: high to low" },
  { value: "rating", label: "Top rated" },
]

export function Catalog() {
  const { data, money, collById, filters, setFilters } = useShop()
  const [params] = useSearchParams()
  const [sheet, setSheet] = useState(false)
  const { sizeLabel } = data.config
  const prices = data.products.map((p) => p.price)
  const lo = Math.floor(Math.min(...prices) / 500) * 500
  const hi = Math.ceil(Math.max(...prices) / 500) * 500

  /* ?q= and ?c= (from search and collection pages) start a fresh filter set. */
  useEffect(() => {
    const q = params.get("q"), c = params.get("c")
    if (q === null && c === null) return
    const next = emptyFilters(filters.sort)
    if (q) next.q = q
    if (c && collById[c]) next.collection = c
    setFilters(next)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params])

  const results = useMemo(() => {
    const f = filters
    return data.products
      .filter((p) => {
        if (f.collection !== "all" && p.collection !== f.collection) return false
        if (f.priceMax !== null && p.price > f.priceMax) return false
        if (f.colours.length && !p.colours.some((c) => f.colours.includes(c))) return false
        if (f.wide && !p.wide) return false
        if (f.sizes.length && !f.sizes.some((s) => inStock(p, s, f.wide ? "wide" : "standard"))) return false
        if (f.q && !matchesQuery(p, f.q, data.colours)) return false
        return true
      })
      .sort(SORTS[filters.sort])
  }, [data, filters])

  const set = (patch: Partial<Filters>) => setFilters({ ...filters, ...patch })
  const reset = () => setFilters(emptyFilters(filters.sort))

  const chips: { key: string; label: string; remove: () => void }[] = []
  if (filters.q) chips.push({ key: "q", label: `"${filters.q}"`, remove: () => set({ q: "" }) })
  if (filters.collection !== "all") chips.push({ key: "c", label: collById[filters.collection]?.name ?? "", remove: () => set({ collection: "all" }) })
  filters.sizes.forEach((s) => chips.push({ key: `s${s}`, label: `${sizeLabel} ${s}`, remove: () => set({ sizes: filters.sizes.filter((x) => x !== s) }) }))
  if (filters.priceMax !== null) chips.push({ key: "p", label: `Up to ${money(filters.priceMax)}`, remove: () => set({ priceMax: null }) })
  filters.colours.forEach((c) => chips.push({ key: `col${c}`, label: data.colours[c]?.label ?? c, remove: () => set({ colours: filters.colours.filter((x) => x !== c) }) }))
  if (filters.wide) chips.push({ key: "w", label: "Wide", remove: () => set({ wide: false }) })

  const panel = (
    <div className="space-y-7">
      <fieldset>
        <legend className="mb-3 font-mono text-[11px] tracking-[0.18em] text-muted-foreground uppercase">Size in stock ({sizeLabel})</legend>
        <ToggleGroup multiple variant="outline" spacing={1.5} className="flex-wrap" value={filters.sizes.map(String)}
          onValueChange={(v: string[]) => set({ sizes: v.map(Number) })}>
          {data.sizes.map((s) => <ToggleGroupItem key={s} value={String(s)} aria-label={`${sizeLabel} ${s}`} className="h-9 min-w-11 rounded-lg tabular-nums data-pressed:bg-foreground data-pressed:text-background">{s}</ToggleGroupItem>)}
        </ToggleGroup>
      </fieldset>
      <fieldset>
        <legend className="mb-3 font-mono text-[11px] tracking-[0.18em] text-muted-foreground uppercase">Price</legend>
        <Slider
          min={lo} max={hi} step={500}
          value={[filters.priceMax ?? hi]}
          onValueChange={(v) => { const n = Array.isArray(v) ? v[0] : v; set({ priceMax: n >= hi ? null : n }) }}
          aria-label="Maximum price"
        />
        <p className="mt-3 text-sm tabular-nums text-muted-foreground">Up to <b className="text-foreground">{money(filters.priceMax ?? hi)}</b></p>
      </fieldset>
      <fieldset>
        <legend className="mb-3 font-mono text-[11px] tracking-[0.18em] text-muted-foreground uppercase">Colour</legend>
        <ToggleGroup multiple variant="outline" spacing={1.5} className="flex-wrap" value={filters.colours} onValueChange={(v: string[]) => set({ colours: v })}>
          {Object.entries(data.colours).map(([id, c]) => (
            <ToggleGroupItem key={id} value={id} className="h-9 gap-2 rounded-full px-3 data-pressed:border-foreground data-pressed:bg-accent">
              <span className="size-3.5 rounded-full border" style={{ background: c.hex }} />{c.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </fieldset>
      <div className="flex items-center gap-2.5">
        <Checkbox id="wide" checked={filters.wide} onCheckedChange={(c) => set({ wide: c === true })} />
        <Label htmlFor="wide">Available in Wide</Label>
      </div>
    </div>
  )

  return (
    <PageShell
      title="Shop all" trail={[["Home", "/"], ["Shop all", null]]} wide
      lead={`${data.products.length} models for running, court, training and everyday wear. Free delivery over ${money(data.config.freeShipFrom)} and a 60-day wear test on every pair.`}
    >
      <div className="sticky top-[4.5rem] z-30 -mx-4 flex flex-wrap items-center justify-between gap-3 border-b bg-background/90 px-4 py-3 backdrop-blur-xl sm:-mx-6 sm:px-6 lg:-mx-10 lg:px-10">
        <ToggleGroup variant="outline" spacing={1.5} className="flex-wrap" value={[filters.collection]} onValueChange={(v: string[]) => set({ collection: v[0] ?? "all" })}>
          {[["all", "All"], ...data.collections.map((c) => [c.id, c.name])].map(([id, label]) => (
            <ToggleGroupItem key={id} value={id} className="h-9 rounded-full px-4 data-pressed:bg-foreground data-pressed:text-background">{label}</ToggleGroupItem>
          ))}
        </ToggleGroup>
        <div className="flex items-center gap-2">
          <Button variant="outline" className="h-9 rounded-full lg:hidden" onClick={() => setSheet(true)}><SlidersHorizontalIcon />Filters{chips.length > 0 && ` (${chips.length})`}</Button>
          <Select value={filters.sort} onValueChange={(v) => set({ sort: String(v) })} items={SORT_OPTIONS}>
            <SelectTrigger className="h-9 w-48 rounded-full" aria-label="Sort by"><SelectValue /></SelectTrigger>
            <SelectContent>{SORT_OPTIONS.map((o) => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
          </Select>
        </div>
      </div>

      <div className="mt-8 grid gap-10 lg:grid-cols-[17rem_1fr]">
        <aside className="hidden lg:block" aria-label="Product filters">
          <div className="sticky top-40">{panel}</div>
        </aside>
        <div>
          <div className="mb-6 flex flex-wrap items-center gap-2" aria-live="polite">
            <p className="mr-2 text-sm text-muted-foreground"><b className="text-foreground tabular-nums">{results.length}</b> {results.length === 1 ? "pair" : "pairs"}</p>
            <AnimatePresence>
              {chips.map((c) => (
                <motion.button key={c.key} layout initial={{ opacity: 0, scale: 0.8 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.8 }}
                  type="button" onClick={c.remove} aria-label={`Remove filter ${c.label}`}
                  className="flex items-center gap-1.5 rounded-full bg-accent px-3 py-1 text-sm hover:bg-accent/70">{c.label}<XIcon className="size-3.5" /></motion.button>
              ))}
            </AnimatePresence>
            {chips.length > 1 && <button type="button" onClick={reset} className="text-sm underline underline-offset-4">Clear all</button>}
          </div>

          {results.length ? (
            <div className="grid grid-cols-2 gap-x-4 gap-y-10 xl:grid-cols-3 lg:gap-x-6">
              {results.map((p: Product) => <ProductCard key={p.id} p={p} />)}
            </div>
          ) : (
            <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="flex flex-col items-center gap-3 rounded-3xl border border-dashed py-20 text-center">
              <span className="flex size-14 items-center justify-center rounded-full bg-muted"><FootprintsIcon className="size-6" /></span>
              <h2 className="font-heading text-2xl font-extrabold uppercase">No pairs match yet</h2>
              <p className="max-w-sm text-muted-foreground">
                {filters.sizes.length ? `None of our models has ${sizeLabel} ${filters.sizes.join(", ")} in stock with these filters. Try another size or remove the colour.`
                  : filters.q ? `Nothing matches "${filters.q}". Try a colour like moss, clay or fog.` : "Try removing a filter or widening the price range."}
              </p>
              <div className="mt-2 flex gap-2"><Button className="rounded-full" onClick={reset}>Clear filters</Button><Button variant="outline" className="rounded-full" nativeButton={false} render={<Link to="/size-guide" />}>Find my size</Button></div>
            </motion.div>
          )}
        </div>
      </div>

      <Sheet open={sheet} onOpenChange={setSheet}>
        <SheetContent side="bottom" className="max-h-[88svh] overflow-y-auto rounded-t-3xl">
          <SheetHeader><SheetTitle className="font-heading text-lg font-extrabold uppercase">Filters</SheetTitle></SheetHeader>
          <div className="px-4 pb-2">{panel}</div>
          <SheetFooter className="flex-row gap-2">
            <Button variant="outline" className="h-12 flex-1 rounded-full" onClick={reset}>Clear all</Button>
            <Button className="h-12 flex-[1.4] rounded-full" onClick={() => setSheet(false)}>{results.length ? `Show ${results.length} ${results.length === 1 ? "pair" : "pairs"}` : "No matches yet"}</Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>
    </PageShell>
  )
}
