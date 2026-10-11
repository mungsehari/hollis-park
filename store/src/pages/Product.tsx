import { AnimatePresence, motion } from "motion/react"
import { useEffect, useRef, useState } from "react"
import { Link, useParams } from "react-router"
import { Radio } from "@base-ui/react/radio"
import { RadioGroup } from "@base-ui/react/radio-group"
import { CheckIcon, HeartIcon, InfoIcon, RulerIcon, ShoppingBagIcon, StoreIcon, TruckIcon, RotateCcwIcon } from "lucide-react"
import { toast } from "sonner"
import { cn } from "cn"
import { Badge } from "@/components/reui/badge"
import { Rating } from "@/components/reui/rating"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Button } from "@/components/ui/button"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { etaText, inStock, unitPrice } from "@/lib/format"
import type { Product as P, Width } from "@/lib/types"
import { useShop } from "@/state/store"
import { Eyebrow, SectionTitle } from "@/components/store/brand"
import { EASE, Reveal } from "@/components/store/motion"
import { Crumbs, useTitle } from "@/components/store/page"
import { ProductCard } from "@/components/store/product-card"
import { NotFound } from "./NotFound"

export function Product() {
  const { id = "" } = useParams()
  const { byId } = useShop()
  const p = byId[id]
  return p ? <ProductView key={p.id} p={p} /> : <NotFound />
}

function Gallery({ p }: { p: P }) {
  const photos = p.gallery.length ? p.gallery : [p.img]
  const [i, setI] = useState(0)
  const [zoom, setZoom] = useState<{ x: number; y: number } | null>(null)
  const fine = typeof window !== "undefined" && window.matchMedia("(hover: hover) and (pointer: fine)").matches
  return (
    <div className="grid gap-3 lg:grid-cols-[5rem_1fr]">
      <div className="order-2 flex gap-2 overflow-x-auto lg:order-1 lg:flex-col" role="tablist" aria-label={`${p.name} photos`}>
        {photos.map((src, n) => (
          <button key={src + n} type="button" role="tab" aria-selected={i === n} aria-label={`Photo ${n + 1}`} onClick={() => setI(n)}
            className={cn("relative size-16 shrink-0 overflow-hidden rounded-xl bg-muted ring-2 ring-transparent transition lg:size-20", i === n && "ring-foreground")}>
            <img src={src} alt="" className="size-full object-cover" loading="lazy" />
          </button>
        ))}
      </div>
      <div
        className="relative order-1 aspect-square overflow-hidden rounded-3xl bg-muted lg:order-2"
        onPointerMove={fine ? (e) => { const r = e.currentTarget.getBoundingClientRect(); setZoom({ x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 }) } : undefined}
        onPointerLeave={fine ? () => setZoom(null) : undefined}
      >
        <AnimatePresence mode="popLayout" initial={false}>
          <motion.img
            key={photos[i]} src={photos[i]} alt={i === 0 ? p.alt : `${p.name}, photo ${i + 1}`} width={1254} height={1254} decoding="async"
            initial={{ opacity: 0, scale: 1.03 }} animate={{ opacity: 1, scale: zoom ? 1.8 : 1 }} exit={{ opacity: 0 }}
            transition={{ opacity: { duration: 0.35 }, scale: { duration: zoom ? 0.25 : 0.5, ease: EASE } }}
            style={{ transformOrigin: zoom ? `${zoom.x}% ${zoom.y}%` : "50% 50%" }}
            className="absolute inset-0 size-full cursor-zoom-in object-cover"
          />
        </AnimatePresence>
        {p.flag && <Badge variant="invert" radius="full" size="lg" className="absolute top-4 left-4">{p.flag}</Badge>}
      </div>
    </div>
  )
}

function ProductView({ p }: { p: P }) {
  const { data, collById, money, saved, toggleSaved, addToCart, setBagOpen } = useShop()
  const { sizeLabel, freeShipFrom, shipFee, expressFee, locale } = data.config
  const coll = collById[p.collection]
  useTitle(`${p.name}, ${p.colourway} | ${data.site.name}`)
  const [width, setWidth] = useState<Width>("standard")
  const [size, setSize] = useState<number | null>(null)
  const [adding, setAdding] = useState<"idle" | "loading" | "done">("idle")
  const [shake, setShake] = useState(0)
  const [note, setNote] = useState<{ msg: string; kind: "info" | "error" | "ok" | "low" }>({ msg: p.fit.text, kind: "info" })
  const [stuck, setStuck] = useState(false)
  const addRef = useRef<HTMLButtonElement>(null)
  const gridRef = useRef<HTMLDivElement>(null)

  const reviews = data.reviews.filter((r) => r.product === p.id)
  const fitAvg = reviews.reduce((n, r) => n + r.fit, 0) / Math.max(1, reviews.length)
  const related = [...data.products.filter((x) => x.collection === p.collection && x.id !== p.id),
    ...data.products.filter((x) => x.collection !== p.collection).sort((a, b) => a.rank - b.rank)].slice(0, 4)
  const price = unitPrice(p, width)
  const chart = (s: number) => data.sizeChart.find((r) => r[0] === s)

  useEffect(() => {
    const el = addRef.current
    if (!el) return
    const io = new IntersectionObserver(([e]) => setStuck(!e.isIntersecting), { rootMargin: "0px 0px 100000px 0px" })
    io.observe(el)
    return () => io.disconnect()
  }, [])

  const pick = (s: number) => {
    if (!inStock(p, s, width)) {
      setNote({ msg: `${sizeLabel} ${s} is sold out${width === "wide" ? " in Wide" : ""}. We restock most sizes within three weeks.`, kind: "error" })
      return
    }
    setSize(s)
    const row = chart(s)
    const conv = row ? `UK ${row[1]}, EU ${row[2]}, ${row[3]} cm` : ""
    setNote(p.low.includes(s) && width === "standard"
      ? { msg: `Only a few pairs left in ${sizeLabel} ${s}. ${conv}.`, kind: "low" }
      : { msg: `In stock. ${sizeLabel} ${s} is ${conv}.`, kind: "ok" })
  }

  const changeWidth = (w: Width) => {
    setWidth(w)
    if (size !== null && !inStock(p, size, w)) {
      setNote({ msg: `${sizeLabel} ${size} is sold out in ${w === "wide" ? "Wide" : "Standard"}. Choose another size.`, kind: "error" })
      setSize(null)
    }
  }

  const add = async () => {
    if (size === null) {
      setNote({ msg: "Choose a size first. Not sure? Use the size guide.", kind: "error" })
      setShake((n) => n + 1)
      gridRef.current?.scrollIntoView({ behavior: "smooth", block: "center" })
      return
    }
    setAdding("loading")
    await new Promise((r) => setTimeout(r, 450))
    addToCart(p.id, size, width)
    setAdding("done")
    setBagOpen(true)
    toast.success(`${p.name}, ${sizeLabel} ${size} added to your bag`)
    setTimeout(() => setAdding("idle"), 1500)
  }

  const on = saved.includes(p.id)
  const noteIcon = note.kind === "ok" ? <CheckIcon className="size-4" /> : <InfoIcon className="size-4" />

  return (
    <div className="mx-auto max-w-[90rem] px-4 pt-8 pb-8 sm:px-6 lg:px-10">
      <Crumbs trail={[["Home", "/"], ["Shop", "/catalog"], [coll?.name ?? "", coll ? `/collections/${coll.id}` : null], [p.name, null]]} />
      <div className="mt-6 grid gap-8 lg:mt-8 lg:grid-cols-[1.15fr_1fr] lg:gap-14">
        <Gallery p={p} />
        <div className="lg:py-4">
          <div className="flex items-center justify-between gap-4">
            <Eyebrow>{coll?.name} / {p.line}</Eyebrow>
            <button type="button" className="flex items-center gap-2 text-sm" onClick={() => document.getElementById("reviews")?.scrollIntoView({ behavior: "smooth" })}>
              <Rating rating={p.rating} size="sm" /><span className="tabular-nums text-muted-foreground">{p.rating} ({p.reviews})</span>
            </button>
          </div>
          <h1 className="mt-3 font-heading text-5xl leading-[0.92] font-extrabold uppercase sm:text-6xl" tabIndex={-1}>{p.name}</h1>
          <p className="mt-3 flex items-center gap-2 text-muted-foreground"><span className="size-4 rounded-full border" style={{ background: p.swatch }} />{p.colourway}</p>
          <p className="mt-5 flex items-baseline gap-3">
            <motion.span key={price} initial={{ y: 8, opacity: 0 }} animate={{ y: 0, opacity: 1 }} className="text-3xl font-semibold tabular-nums">{money(price)}</motion.span>
            <span className="text-sm text-muted-foreground">{p.wide ? `Wide adds ${money(p.wide.extra)}` : "Standard width only"}</span>
          </p>
          <p className="mt-5 text-lg text-foreground/80">{p.desc}</p>

          {p.wide && (
            <div className="mt-7">
              <p className="mb-2 text-sm font-medium">Width <span className="font-normal text-muted-foreground">{width === "wide" ? "Wide, +4 mm across the forefoot" : "Standard"}</span></p>
              <ToggleGroup variant="outline" value={[width]} onValueChange={(v: string[]) => changeWidth((v[0] as Width) ?? width)} className="w-full">
                <ToggleGroupItem value="standard" className="h-11 flex-1 rounded-xl data-pressed:bg-foreground data-pressed:text-background">Standard</ToggleGroupItem>
                <ToggleGroupItem value="wide" className="h-11 flex-1 rounded-xl data-pressed:bg-foreground data-pressed:text-background">Wide +{money(p.wide.extra)}</ToggleGroupItem>
              </ToggleGroup>
            </div>
          )}

          <div className="mt-7">
            <div className="mb-2 flex items-center justify-between">
              <p id="size-label" className="text-sm font-medium">Size ({sizeLabel}) <span className="font-normal text-muted-foreground">{size === null ? "Select a size" : `${sizeLabel} ${size}${width === "wide" ? " Wide" : ""}`}</span></p>
              <Link to="/size-guide" className="flex items-center gap-1.5 text-sm underline underline-offset-4"><RulerIcon className="size-4" />Size guide</Link>
            </div>
            <RadioGroup
              key={shake}
              ref={gridRef}
              aria-labelledby="size-label"
              value={size === null ? null : String(size)}
              onValueChange={(v) => pick(Number(v))}
              className="grid grid-cols-5 gap-2"
              render={<motion.div animate={shake ? { x: [0, -8, 7, -4, 0] } : {}} transition={{ duration: 0.4 }} />}
            >
              {data.sizes.map((s) => {
                const ok = inStock(p, s, width)
                const low = ok && p.low.includes(s) && width === "standard"
                return (
                  <Radio.Root
                    key={s} value={String(s)} disabled={!ok} nativeButton render={<button type="button" />}
                    aria-label={`${sizeLabel} ${s}${ok ? (low ? ", only a few left" : "") : ", sold out"}`}
                    className={cn(
                      "relative h-12 rounded-xl border text-sm font-medium tabular-nums transition-all hover:border-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                      "data-checked:border-foreground data-checked:bg-foreground data-checked:text-background",
                      "data-disabled:cursor-not-allowed data-disabled:bg-muted data-disabled:text-muted-foreground/60 data-disabled:line-through data-disabled:hover:border-border",
                    )}
                  >
                    {s}
                    {low && <span className="absolute top-1.5 right-1.5 size-1.5 rounded-full bg-warning" />}
                  </Radio.Root>
                )
              })}
            </RadioGroup>
            <p className={cn("mt-3 flex items-start gap-2 text-sm", note.kind === "error" && "text-destructive", note.kind === "low" && "text-warning-foreground", note.kind === "ok" && "text-success", note.kind === "info" && "text-muted-foreground")} aria-live="polite">
              <span className="mt-0.5">{noteIcon}</span>{note.msg}
            </p>
          </div>

          <div className="mt-6 flex gap-3">
            <Button ref={addRef} size="lg" onClick={add} disabled={adding === "loading"} className={cn("h-14 flex-1 rounded-full text-base transition-colors", adding === "done" && "bg-success hover:bg-success")}>
              <AnimatePresence mode="wait" initial={false}>
                <motion.span key={adding + String(size === null)} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} className="flex items-center gap-2">
                  {adding === "loading" ? "Adding" : adding === "done" ? <><CheckIcon />Added to bag</> : size === null ? "Select a size" : <><ShoppingBagIcon />Add to bag · {money(price)}</>}
                </motion.span>
              </AnimatePresence>
            </Button>
            <Button variant="outline" size="icon-lg" className="size-14 rounded-full" aria-pressed={on} aria-label={`${on ? "Remove" : "Save"} ${p.name}`} onClick={() => toggleSaved(p.id)}>
              <HeartIcon className={cn(on && "fill-destructive text-destructive")} />
            </Button>
          </div>

          <ul className="mt-6 space-y-3 rounded-2xl bg-muted p-4 text-sm">
            <li className="flex gap-3"><TruckIcon className="mt-0.5 size-4 shrink-0" /><span><b>{etaText("standard", locale)}</b> with standard delivery. Free over {money(freeShipFrom)}.</span></li>
            <li className="flex gap-3"><RotateCcwIcon className="mt-0.5 size-4 shrink-0" /><span><b>60-day wear test.</b> Wear them outside, still free to return.</span></li>
            <li className="flex gap-3"><StoreIcon className="mt-0.5 size-4 shrink-0" /><span><b>In stock at {data.site.store}.</b> Pick up in about two hours.</span></li>
          </ul>

          <Accordion className="mt-6">
            <AccordionItem value="materials"><AccordionTrigger className="py-4 text-base font-medium">Materials</AccordionTrigger>
              <AccordionContent><ul className="divide-y text-sm">{p.materials.map(([k, v]) => <li key={k} className="flex justify-between gap-6 py-2"><span className="text-muted-foreground">{k}</span><span className="text-right">{v}</span></li>)}</ul></AccordionContent></AccordionItem>
            <AccordionItem value="fit"><AccordionTrigger className="py-4 text-base font-medium">Fit</AccordionTrigger><AccordionContent className="text-muted-foreground">{p.fit.text}</AccordionContent></AccordionItem>
            <AccordionItem value="care"><AccordionTrigger className="py-4 text-base font-medium">Care</AccordionTrigger><AccordionContent className="text-muted-foreground">{p.care}</AccordionContent></AccordionItem>
            <AccordionItem value="delivery"><AccordionTrigger className="py-4 text-base font-medium">Delivery and returns</AccordionTrigger>
              <AccordionContent className="text-muted-foreground">Standard delivery in 3-5 working days across India, {money(shipFee)} or free over {money(freeShipFrom)}. Express next working day for {money(expressFee)}. Return within 60 days of delivery, worn or not, with the prepaid label in the box.</AccordionContent></AccordionItem>
          </Accordion>
        </div>
      </div>

      <section className="mt-20" aria-labelledby="numbers">
        <SectionTitle><span id="numbers">The numbers</span></SectionTitle>
        <p className="mt-2 text-muted-foreground">Measured on a US 9, the same way for every model.</p>
        <div className="mt-8 grid grid-cols-2 gap-3 lg:grid-cols-4">
          {p.specs.map(([k, v, n], idx) => (
            <Reveal key={k} delay={idx * 0.06} className="rounded-3xl border bg-card p-6">
              <p className="font-mono text-[11px] tracking-[0.18em] text-muted-foreground uppercase">{k}</p>
              <p className="mt-2 font-heading text-4xl font-extrabold tabular-nums">{v}</p>
              <p className="mt-1 text-sm text-muted-foreground">{n}</p>
            </Reveal>
          ))}
        </div>
      </section>

      <section id="reviews" className="mt-20 grid gap-10 scroll-mt-28 lg:grid-cols-[1fr_1.6fr]" aria-labelledby="rev-title">
        <div className="lg:sticky lg:top-28 lg:self-start">
          <SectionTitle><span id="rev-title">Reviews</span></SectionTitle>
          <div className="mt-5 flex items-end gap-4"><span className="font-heading text-6xl font-extrabold tabular-nums">{p.rating}</span><span><Rating rating={p.rating} /><br /><span className="text-sm text-muted-foreground tabular-nums">{p.reviews} verified reviews</span></span></div>
          <div className="mt-8">
            <p className="text-sm font-medium">How it fits: {p.fit.verdict.toLowerCase()}</p>
            <div className="relative mt-3 h-2 rounded-full bg-muted"><motion.span initial={{ left: "50%" }} whileInView={{ left: `${50 + fitAvg * 42}%` }} viewport={{ once: true }} transition={{ duration: 0.9, ease: EASE }} className="absolute top-1/2 size-5 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-background bg-foreground shadow" /></div>
            <div className="mt-2 flex justify-between text-xs text-muted-foreground"><span>Runs small</span><span>True</span><span>Runs large</span></div>
          </div>
        </div>
        <div className="space-y-4">
          {reviews.map((r, i) => (
            <Reveal key={i} className="rounded-3xl border bg-card p-6">
              <div className="flex items-center justify-between"><Rating rating={r.rating} size="sm" /><Badge variant="secondary" radius="full">Verified, worn 30 days</Badge></div>
              <p className="mt-3 text-lg">{r.text}</p>
              <p className="mt-4 text-sm text-muted-foreground"><b className="text-foreground">{r.name}</b> · {r.place}, {r.date} · Bought {r.size}</p>
            </Reveal>
          ))}
        </div>
      </section>

      <section className="mt-20" aria-labelledby="related">
        <div className="flex items-end justify-between"><SectionTitle><span id="related">Keep exploring</span></SectionTitle><Link to="/catalog" className="hidden font-medium underline underline-offset-4 sm:block">Shop all</Link></div>
        <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-10 lg:grid-cols-4 lg:gap-x-6">{related.map((x) => <ProductCard key={x.id} p={x} />)}</div>
      </section>

      <AnimatePresence>
        {stuck && (
          <motion.div initial={{ y: "100%" }} animate={{ y: 0 }} exit={{ y: "100%" }} transition={{ duration: 0.4, ease: EASE }}
            className="fixed inset-x-0 bottom-0 z-30 flex items-center justify-between gap-3 border-t bg-background/95 px-4 py-3 backdrop-blur-xl lg:hidden">
            <span className="min-w-0"><b className="block truncate font-heading uppercase">{p.name}</b><span className="text-sm tabular-nums text-muted-foreground">{money(price)}</span></span>
            <Button className="h-11 rounded-full px-6" onClick={add}>{size === null ? "Choose size" : "Add to bag"}</Button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
