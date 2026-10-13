import { motion } from "motion/react"
import { Link } from "react-router"
import { ArrowUpRightIcon, SparklesIcon } from "lucide-react"
import { cn } from "cn"
import { go } from "@/lib/api"
import type { CarePick, Marketplace, Product } from "@/lib/types"
import { useShop } from "@/state/store"
import { SectionTitle } from "./brand"
import { EASE, Stagger, StaggerItem } from "./motion"

/** Brand accent dots only (no logos): orange for Amazon, blue for Flipkart. */
const DOT: Record<Marketplace, string> = { AMAZON: "bg-[#FF9900]", FLIPKART: "bg-[#2874F0]" }

/** Shown wherever marketplace links appear. Affiliate programmes require it. */
export function AffiliateNote({ className }: { className?: string }) {
  return (
    <p className={cn("text-xs text-muted-foreground", className)}>
      Some links go to Amazon or Flipkart. We may earn a small commission when you buy there, at no extra cost to you.
    </p>
  )
}

const outLink = { target: "_blank", rel: "sponsored noopener" } as const

/** "Also on Amazon / Flipkart" under the product page's own Add to bag. Hidden when the pair has no marketplace links. */
export function AlsoOn({ p }: { p: Product }) {
  const links = [
    p.amazonUrl ? { m: "AMAZON" as const, label: "Amazon", href: go.product(p.id, "amazon") } : null,
    p.flipkartUrl ? { m: "FLIPKART" as const, label: "Flipkart", href: go.product(p.id, "flipkart") } : null,
  ].filter((x) => x !== null)
  if (links.length === 0) return null
  return (
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, ease: EASE, delay: 0.15 }} className="mt-4">
      <p className="text-sm text-muted-foreground">Prefer a marketplace? This pair is also on</p>
      <div className={cn("mt-2 grid gap-3", links.length > 1 && "grid-cols-2")}>
        {links.map((l) => (
          <motion.a
            key={l.m} href={l.href} {...outLink}
            whileHover={{ y: -2 }} whileTap={{ scale: 0.97 }}
            aria-label={`Buy ${p.name} on ${l.label} (opens in a new tab)`}
            className="group flex h-12 items-center justify-center gap-2 rounded-full border bg-card text-sm font-medium shadow-xs transition-colors hover:border-foreground"
          >
            <span className={cn("size-2.5 rounded-full", DOT[l.m])} aria-hidden="true" />
            {l.label}
            <ArrowUpRightIcon className="size-4 transition-transform duration-300 group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </motion.a>
        ))}
      </div>
      <AffiliateNote className="mt-2" />
    </motion.div>
  )
}

/** One care pick: the whole card is the link; it lifts, the photo zooms and the arrow nudges on hover. */
export function PickCard({ pick }: { pick: CarePick }) {
  return (
    <motion.a
      href={go.pick(pick.id)} {...outLink}
      whileHover={{ y: -6 }} transition={{ duration: 0.35, ease: EASE }}
      aria-label={`${pick.title} on ${pick.marketplaceLabel} (opens in a new tab)`}
      className="group flex h-full flex-col overflow-hidden rounded-3xl border bg-card shadow-xs transition-shadow hover:shadow-xl"
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-accent">
        {pick.image ? (
          <img src={pick.image} alt="" loading="lazy" decoding="async" className="size-full object-cover transition-transform duration-700 ease-out group-hover:scale-105" />
        ) : (
          <div className="flex size-full items-center justify-center bg-gradient-to-br from-accent via-muted to-sage/30">
            <SparklesIcon className="size-10 text-sage-deep transition-transform duration-700 group-hover:scale-110 group-hover:rotate-6" aria-hidden="true" />
          </div>
        )}
        <span className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-background/90 px-3 py-1 text-xs font-medium shadow-sm backdrop-blur">
          <span className={cn("size-2 rounded-full", DOT[pick.marketplace])} aria-hidden="true" />{pick.marketplaceLabel}
        </span>
      </div>
      <div className="flex flex-1 flex-col p-5">
        <h3 className="font-heading text-lg leading-tight font-extrabold uppercase">{pick.title}</h3>
        {pick.blurb && <p className="mt-2 flex-1 text-sm text-muted-foreground">{pick.blurb}</p>}
        <span className="mt-4 inline-flex items-center gap-1.5 text-sm font-medium">
          View on {pick.marketplaceLabel}
          <ArrowUpRightIcon className="size-4 transition-transform duration-300 group-hover:translate-x-1 group-hover:-translate-y-1" />
        </span>
      </div>
    </motion.a>
  )
}

/** Grid of picks that reveal one after another as they scroll in. */
export function PickGrid({ picks }: { picks: CarePick[] }) {
  return (
    <Stagger className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:gap-6" gap={0.1}>
      {picks.map((p) => <StaggerItem key={p.id} className="h-full"><PickCard pick={p} /></StaggerItem>)}
    </Stagger>
  )
}

/** Home page section. Hidden when the admin has no active picks. */
export function CarePicksSection() {
  const { data } = useShop()
  const picks = data.picks ?? []
  if (picks.length === 0) return null
  return (
    <section className="mx-auto max-w-[90rem] px-4 pb-20 sm:px-6 lg:px-10 lg:pb-28" aria-labelledby="care-picks">
      <div className="flex items-end justify-between gap-6">
        <div>
          <SectionTitle><span id="care-picks">Care for your pair</span></SectionTitle>
          <p className="mt-3 max-w-xl text-muted-foreground">The socks, cleaners and insoles we use with ours, from Amazon and Flipkart.</p>
        </div>
        <Link to="/care" className="hidden shrink-0 font-medium underline underline-offset-4 sm:block">See all picks</Link>
      </div>
      <div className="mt-8"><PickGrid picks={picks.slice(0, 4)} /></div>
      <AffiliateNote className="mt-6" />
    </section>
  )
}
