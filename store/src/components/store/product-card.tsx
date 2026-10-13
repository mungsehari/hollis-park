import { AnimatePresence, motion } from "motion/react"
import { useRef, useState } from "react"
import { Link } from "react-router"
import { HeartIcon, PlusIcon, StarIcon, XIcon } from "lucide-react"
import { toast } from "sonner"
import { cn } from "cn"
import { Badge } from "@/components/reui/badge"
import { inStock } from "@/lib/format"
import type { Product } from "@/lib/types"
import { useShop } from "@/state/store"
import { FLIGHT_MS, flyToBag } from "./fly-to-bag"
import { EASE, FadeImg } from "./motion"

/** Product tile: the second photo fades in on hover, the heart saves, quick add picks a size in place. */
export function ProductCard({ p, className, priority = false, ref }: { p: Product; className?: string; priority?: boolean; ref?: React.Ref<HTMLElement> }) {
  const { saved, toggleSaved, collById, money, data, addToCart, setBagOpen } = useShop()
  const [picking, setPicking] = useState(false)
  const on = saved.includes(p.id)
  const second = p.gallery[1]
  const photoRef = useRef<HTMLDivElement>(null)

  const quickAdd = (s: number) => {
    const flew = flyToBag(p.img, photoRef.current)
    addToCart(p.id, s, "standard")
    setPicking(false)
    toast.success(`${p.name}, ${data.config.sizeLabel} ${s} added to your bag`)
    window.setTimeout(() => setBagOpen(true), flew ? FLIGHT_MS + 150 : 0)
  }

  return (
    <motion.article
      ref={ref}
      layout="position"
      exit={{ opacity: 0, scale: 0.96, transition: { duration: 0.25 } }}
      className={cn("group relative", className)}
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "0px 0px -8% 0px" }}
      transition={{ duration: 0.6, ease: EASE }}
    >
      <div ref={photoRef} className="relative overflow-hidden rounded-2xl bg-muted">
        <Link to={`/catalog/${p.id}`} className="block" aria-label={p.name}>
          <FadeImg
            src={p.img} alt={p.alt} width={1254} height={1254} loading={priority ? "eager" : "lazy"} decoding="async"
            className="aspect-square w-full object-cover group-hover:scale-[1.04]"
          />
          {second && (
            <img src={second} alt="" aria-hidden="true" loading="lazy" decoding="async"
              className="absolute inset-0 size-full object-cover opacity-0 transition-opacity duration-500 group-hover:opacity-100" />
          )}
        </Link>
        {p.flag && <Badge variant="invert" radius="full" size="lg" className="pointer-events-none absolute top-3 left-3">{p.flag}</Badge>}
        <motion.button
          type="button"
          whileTap={{ scale: 0.85 }}
          onClick={() => toggleSaved(p.id)}
          aria-pressed={on}
          aria-label={`${on ? "Remove" : "Save"} ${p.name}`}
          className="absolute top-3 right-3 flex size-10 items-center justify-center rounded-full bg-white/90 shadow-sm backdrop-blur transition-colors hover:bg-white"
        >
          <motion.span key={String(on)} initial={{ scale: 0.6 }} animate={{ scale: 1 }} transition={{ type: "spring", stiffness: 500, damping: 18 }}>
            <HeartIcon className={cn("size-[18px]", on && "fill-destructive text-destructive")} />
          </motion.span>
        </motion.button>

        <div className="absolute inset-x-3 bottom-3 hidden translate-y-3 opacity-0 transition-all duration-300 group-focus-within:translate-y-0 group-focus-within:opacity-100 group-hover:translate-y-0 group-hover:opacity-100 sm:block">
          <AnimatePresence mode="wait" initial={false}>
            {picking ? (
              <motion.div key="sizes" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 8 }}
                className="rounded-xl bg-white/95 p-2.5 shadow-lg backdrop-blur" role="group" aria-label={`Choose a size for ${p.name}`}>
                <div className="mb-2 flex items-center justify-between px-1 text-xs font-medium">
                  <span>Select size ({data.config.sizeLabel})</span>
                  <button type="button" onClick={() => setPicking(false)} aria-label="Close sizes" className="rounded-full p-1 hover:bg-muted"><XIcon className="size-3.5" /></button>
                </div>
                <div className="grid grid-cols-5 gap-1">
                  {data.sizes.map((s) => {
                    const ok = inStock(p, s)
                    return (
                      <button key={s} type="button" disabled={!ok}
                        aria-label={ok ? `${data.config.sizeLabel} ${s}` : `${data.config.sizeLabel} ${s}, sold out`}
                        onClick={() => quickAdd(s)}
                        className="h-8 rounded-md border text-xs font-medium tabular-nums transition-colors hover:border-foreground hover:bg-foreground hover:text-background disabled:cursor-not-allowed disabled:text-muted-foreground/60 disabled:line-through disabled:hover:bg-transparent disabled:hover:text-muted-foreground/60">
                        {s}
                      </button>
                    )
                  })}
                </div>
              </motion.div>
            ) : (
              <motion.button key="quick" type="button" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                onClick={() => setPicking(true)}
                className="flex h-11 w-full items-center justify-center gap-1.5 rounded-xl bg-white/95 text-sm font-medium shadow-lg backdrop-blur transition-colors hover:bg-foreground hover:text-background">
                <PlusIcon className="size-4" />Quick add
              </motion.button>
            )}
          </AnimatePresence>
        </div>
      </div>

      <div className="mt-3.5 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-mono text-[11px] tracking-[0.15em] text-muted-foreground uppercase">{collById[p.collection]?.name}</p>
          <h3 className="mt-1 truncate font-heading text-lg font-extrabold uppercase"><Link to={`/catalog/${p.id}`} className="after:absolute after:inset-0 after:content-[''] focus-visible:outline-none">{p.name}</Link></h3>
          <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
            <span className="size-3 rounded-full border" style={{ background: p.swatch }} aria-hidden="true" />{p.colourway}
          </p>
        </div>
        <div className="shrink-0 text-right">
          <p className="font-semibold tabular-nums">{money(p.price)}</p>
          <p className="mt-1 flex items-center justify-end gap-1 text-xs text-muted-foreground"><StarIcon className="size-3 fill-current text-foreground" />{p.rating}</p>
        </div>
      </div>
    </motion.article>
  )
}
