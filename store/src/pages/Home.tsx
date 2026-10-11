import { motion, useScroll, useTransform } from "motion/react"
import { useRef, useState } from "react"
import { Link } from "react-router"
import { ArrowRightIcon, ArrowUpRightIcon, HammerIcon, RotateCcwIcon, ShieldCheckIcon, TruckIcon } from "lucide-react"
import { Badge } from "@/components/reui/badge"
import { Rating } from "@/components/reui/rating"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Button } from "@/components/ui/button"
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from "@/components/ui/carousel"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { POPULAR, VALUE_LIMIT } from "@/lib/catalog"
import { useShop } from "@/state/store"
import { Eyebrow, SectionTitle } from "@/components/store/brand"
import { CountUp, EASE, Reveal, SplitWords, Stagger, StaggerItem } from "@/components/store/motion"
import { useTitle } from "@/components/store/page"
import { NewsletterForm } from "@/components/store/newsletter-form"
import { ProductCard } from "@/components/store/product-card"

function Hero() {
  const { data } = useShop()
  const { hero } = data.home
  const ref = useRef<HTMLElement>(null)
  const { scrollYProgress } = useScroll({ target: ref, offset: ["start start", "end start"] })
  const y = useTransform(scrollYProgress, [0, 1], ["0%", "18%"])
  const scale = useTransform(scrollYProgress, [0, 1], [1, 1.08])
  const fade = useTransform(scrollYProgress, [0, 0.7], [1, 0])

  return (
    <section ref={ref} className="relative isolate flex min-h-[100svh] items-end overflow-hidden bg-sage text-white" aria-labelledby="hero-title">
      {hero.image && (
        <motion.img
          src={hero.image} alt={hero.imageAlt ?? ""} fetchPriority="high" decoding="async"
          style={{ y, scale }}
          initial={{ opacity: 0, scale: 1.12 }} animate={{ opacity: 1 }}
          transition={{ duration: 1.4, ease: EASE }}
          className="absolute inset-0 -z-10 size-full object-cover object-[70%_30%] lg:object-[85%_25%]"
        />
      )}
      <div className="absolute inset-0 -z-10 bg-gradient-to-t from-black/55 via-black/10 to-black/25 lg:bg-gradient-to-r lg:from-black/45 lg:via-black/5 lg:to-transparent" />
      <motion.div style={{ opacity: fade }} className="mx-auto w-full max-w-[90rem] px-4 pt-32 pb-14 sm:px-6 lg:px-10 lg:pb-24">
        {hero.eyebrow && (
          <motion.p initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2, duration: 0.6 }}
            className="mb-5 flex items-center gap-3 font-mono text-xs tracking-[0.22em] uppercase">
            <span className="h-px w-8 bg-white" />{hero.eyebrow}
          </motion.p>
        )}
        <h1 id="hero-title" className="max-w-4xl font-heading text-[clamp(3.4rem,10vw,8.5rem)] leading-[0.88] font-extrabold tracking-tight text-balance uppercase">
          <SplitWords text={hero.title} delay={0.25} />
        </h1>
        {hero.subtitle && (
          <motion.p initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.7, duration: 0.7, ease: EASE }} className="mt-6 max-w-lg text-lg text-white/90">
            {hero.subtitle}
          </motion.p>
        )}
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.85, duration: 0.7, ease: EASE }} className="mt-8 flex flex-wrap gap-3">
          {hero.ctaLabel && (
            <Button size="lg" nativeButton={false} render={<Link to={hero.ctaLink || "/catalog"} />} className="h-12 rounded-full bg-white px-7 text-base text-sage-ink hover:bg-white/90">
              {hero.ctaLabel}<ArrowRightIcon />
            </Button>
          )}
          {hero.secondaryLabel && (
            <Button size="lg" variant="outline" nativeButton={false} render={<Link to={hero.secondaryLink || "/size-guide"} />}
              className="h-12 rounded-full border-white/50 bg-white/10 px-7 text-base text-white backdrop-blur hover:bg-white/20 hover:text-white">
              {hero.secondaryLabel}
            </Button>
          )}
        </motion.div>
      </motion.div>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.3 }} className="absolute bottom-6 left-1/2 hidden -translate-x-1/2 lg:block" aria-hidden="true">
        <motion.span animate={{ y: [0, 8, 0] }} transition={{ repeat: Infinity, duration: 2.2, ease: "easeInOut" }} className="block h-10 w-px bg-white/60" />
      </motion.div>
    </section>
  )
}

function Trust() {
  const { data, money } = useShop()
  const items = [
    { icon: TruckIcon, t: `Free delivery over ${money(data.config.freeShipFrom)}`, d: "Tracked, 3-5 working days across India" },
    { icon: RotateCcwIcon, t: "60-day wear test", d: "Wear them outside, return them free" },
    { icon: HammerIcon, t: `Resole for ${money(3499)}`, d: "A new outsole when the tread wears out" },
    { icon: ShieldCheckIcon, t: "Two sizes, one free return", d: "Order both, keep the pair that fits" },
  ]
  return (
    <Stagger className="mx-auto grid max-w-[90rem] gap-px overflow-hidden border-b bg-border sm:grid-cols-2 lg:grid-cols-4">
      {items.map(({ icon: Icon, t, d }) => (
        <StaggerItem key={t} className="flex items-start gap-4 bg-background p-6 lg:p-8">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-full bg-accent"><Icon className="size-5" /></span>
          <div><p className="font-semibold">{t}</p><p className="mt-0.5 text-sm text-muted-foreground">{d}</p></div>
        </StaggerItem>
      ))}
    </Stagger>
  )
}

function Popular() {
  const { data, money } = useShop()
  const [set, setSet] = useState<"bestsellers" | "new" | "value">("bestsellers")
  return (
    <section className="mx-auto max-w-[90rem] px-4 py-20 sm:px-6 lg:px-10 lg:py-28" aria-labelledby="popular">
      <div className="flex flex-wrap items-end justify-between gap-6">
        <div>
          <Eyebrow className="mb-3">The range</Eyebrow>
          <SectionTitle><span id="popular">Popular profiles</span></SectionTitle>
        </div>
        <div className="flex flex-wrap items-center gap-4">
          <Tabs value={set} onValueChange={(v) => setSet(v as typeof set)}>
            <TabsList>
              <TabsTrigger value="bestsellers">Bestsellers</TabsTrigger>
              <TabsTrigger value="new">New in</TabsTrigger>
              <TabsTrigger value="value">Under {money(VALUE_LIMIT)}</TabsTrigger>
            </TabsList>
          </Tabs>
          <Link to="/catalog" className="group hidden items-center gap-1.5 font-medium sm:flex">Shop all {data.products.length}<ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-1" /></Link>
        </div>
      </div>
      <motion.div key={set} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.4 }} className="mt-10 grid grid-cols-2 gap-x-4 gap-y-10 lg:grid-cols-4 lg:gap-x-6">
        {POPULAR[set](data.products).map((p) => <ProductCard key={p.id} p={p} />)}
      </motion.div>
    </section>
  )
}

function Collections() {
  const { data } = useShop()
  const list = data.collections.slice(0, 4)
  return (
    <section className="mx-auto max-w-[90rem] px-4 pb-20 sm:px-6 lg:px-10 lg:pb-28" aria-labelledby="activity">
      <div className="flex items-end justify-between gap-6">
        <SectionTitle><span id="activity">Made for your<br />kind of day</span></SectionTitle>
        <Link to="/collections" className="group hidden items-center gap-1.5 font-medium sm:flex">All collections<ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-1" /></Link>
      </div>
      <Stagger className="mt-10 grid gap-4 md:grid-cols-4 md:grid-rows-2 lg:gap-5">
        {list.map((c, i) => (
          <StaggerItem key={c.id} className={i === 0 ? "md:col-span-2 md:row-span-2" : i === 3 ? "md:col-span-2" : ""}>
            <Link to={`/collections/${c.id}`} className="group relative block h-full min-h-72 overflow-hidden rounded-3xl bg-muted">
              {c.image && <img src={c.image} alt={c.alt ?? ""} loading="lazy" className="absolute inset-0 size-full object-cover transition-transform duration-1000 ease-out group-hover:scale-105" />}
              <span className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />
              <span className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-4 p-6 text-white">
                <span>
                  <span className="block font-heading text-3xl font-extrabold uppercase lg:text-4xl">{c.name}</span>
                  <span className="mt-1 block max-w-xs text-sm text-white/80">{c.tagline}</span>
                </span>
                <span className="flex size-11 shrink-0 items-center justify-center rounded-full bg-white text-sage-ink transition-transform duration-500 group-hover:-translate-y-1 group-hover:translate-x-1"><ArrowUpRightIcon className="size-5" /></span>
              </span>
            </Link>
          </StaggerItem>
        ))}
      </Stagger>
    </section>
  )
}

function Spotlight() {
  const { data, byId, money } = useShop()
  const p = byId[data.home.spotlightProductId ?? ""] ?? data.products[0]
  if (!p) return null
  return (
    <section className="mx-auto max-w-[90rem] px-4 pb-20 sm:px-6 lg:px-10 lg:pb-28" aria-labelledby="spotlight">
      <div className="grid overflow-hidden rounded-[2rem] bg-accent lg:grid-cols-2">
        <Reveal className="relative bg-muted">
          <img src={p.img} alt={p.alt} loading="lazy" className="size-full min-h-80 object-cover" />
        </Reveal>
        <div className="flex flex-col justify-center gap-6 p-8 sm:p-12 lg:p-16">
          <Eyebrow>Most worn this season</Eyebrow>
          <h2 id="spotlight" className="font-heading text-5xl leading-[0.92] font-extrabold uppercase lg:text-6xl">{p.name}</h2>
          <p className="max-w-md text-lg text-foreground/80">{p.blurb} {p.desc.split(". ")[0]}.</p>
          <dl className="grid grid-cols-3 gap-4 border-y border-foreground/10 py-5">
            {p.specs.slice(0, 3).map(([k, v]) => (
              <div key={k}><dt className="font-mono text-[11px] tracking-[0.15em] text-muted-foreground uppercase">{k}</dt><dd className="mt-1 font-heading text-2xl font-extrabold tabular-nums">{v}</dd></div>
            ))}
          </dl>
          <div className="flex flex-wrap items-center gap-4">
            <Button size="lg" className="h-12 rounded-full px-7 text-base" nativeButton={false} render={<Link to={`/catalog/${p.id}`} />}>
              Choose your size · {money(p.price)}
            </Button>
            <Link to={`/collections/${p.collection}`} className="group inline-flex items-center gap-1.5 font-medium">Compare<ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-1" /></Link>
          </div>
        </div>
      </div>
    </section>
  )
}

function Promise() {
  const { data, money } = useShop()
  return (
    <section className="bg-sage-ink py-20 text-white lg:py-28" aria-labelledby="promise">
      <div className="mx-auto max-w-[90rem] px-4 sm:px-6 lg:px-10">
        <div className="max-w-2xl">
          <Eyebrow className="mb-3 text-white/50">Our promise</Eyebrow>
          <SectionTitle><span id="promise">Try them properly.</span></SectionTitle>
          <p className="mt-5 text-lg text-white/70">Every pair ships with the same three promises, whether you order online or try them on at {data.site.store}.</p>
        </div>
        <Stagger className="mt-12 grid gap-4 md:grid-cols-3">
          {[
            { n: 60, suffix: " days", t: "Wear test", d: "Run in them, wear them to work, get them muddy. If they are not right within 60 days, send them back free." },
            { n: 2, suffix: " sizes", t: "One free return", d: "Between sizes? Order both. Keep the pair that fits and send the other back with the prepaid label." },
            { n: 3499, prefix: "₹", t: "Resole service", d: `When the tread wears through, we fit a new outsole in about ten working days for ${money(3499)}.` },
          ].map((c) => (
            <StaggerItem key={c.t} className="rounded-3xl bg-white/[0.06] p-7 ring-1 ring-white/10 transition-colors hover:bg-white/10">
              <p className="font-heading text-5xl font-extrabold tabular-nums">{c.prefix}<CountUp value={c.n} format={(x) => Math.round(x).toLocaleString("en-IN")} />{c.suffix}</p>
              <h3 className="mt-4 text-xl font-semibold">{c.t}</h3>
              <p className="mt-2 text-white/70">{c.d}</p>
            </StaggerItem>
          ))}
        </Stagger>
      </div>
    </section>
  )
}

function Reviews() {
  const { data, byId } = useShop()
  const count = data.products.reduce((n, p) => n + p.reviews, 0)
  const avg = count ? data.products.reduce((n, p) => n + p.rating * p.reviews, 0) / count : 0
  return (
    <section className="mx-auto max-w-[90rem] overflow-hidden px-4 py-20 sm:px-6 lg:px-10 lg:py-28" aria-labelledby="reviews">
      <Carousel opts={{ align: "start", dragFree: true }}>
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <SectionTitle><span id="reviews">What a month<br />of wear says</span></SectionTitle>
            <div className="mt-4 flex items-center gap-3 text-muted-foreground">
              <Rating rating={Math.round(avg * 10) / 10} size="sm" />
              <span><b className="text-foreground tabular-nums">{avg.toFixed(1)}</b> out of 5 from <span className="tabular-nums">{count.toLocaleString("en-IN")}</span> verified reviews</span>
            </div>
          </div>
          <div className="relative hidden h-10 w-24 sm:block"><CarouselPrevious className="left-0 size-10" /><CarouselNext className="right-0 size-10" /></div>
        </div>
        <CarouselContent className="mt-10 -ml-4">
          {data.reviews.map((r, i) => {
            const p = byId[r.product]
            return (
              <CarouselItem key={i} className="basis-[85%] pl-4 sm:basis-1/2 lg:basis-1/3">
                <figure className="flex h-full flex-col rounded-3xl border bg-card p-6">
                  <div className="flex items-center justify-between gap-3"><Rating rating={r.rating} size="sm" /><Badge variant="secondary" radius="full">Verified</Badge></div>
                  <blockquote className="mt-4 flex-1 text-lg leading-snug">{r.text}</blockquote>
                  <figcaption className="mt-6 flex items-center justify-between gap-4 text-sm">
                    <span><b className="block">{r.name}</b><span className="text-muted-foreground">{r.place}, {r.date}</span></span>
                    {p && (
                      <Link to={`/catalog/${p.id}`} className="flex items-center gap-2 text-right text-xs text-muted-foreground hover:text-foreground">
                        <span>{p.name}<br />{r.size}</span><img src={p.img} alt="" className="size-11 rounded-lg object-cover" />
                      </Link>
                    )}
                  </figcaption>
                </figure>
              </CarouselItem>
            )
          })}
        </CarouselContent>
      </Carousel>
    </section>
  )
}

function Faq() {
  const { data } = useShop()
  return (
    <section className="mx-auto grid max-w-[90rem] gap-10 px-4 pb-20 sm:px-6 lg:grid-cols-[1fr_1.4fr] lg:gap-20 lg:px-10 lg:pb-28" aria-labelledby="faq">
      <div>
        <SectionTitle><span id="faq">Before you<br />add to bag</span></SectionTitle>
        <p className="mt-5 max-w-sm text-lg text-muted-foreground">Sizing, delivery and returns, answered plainly. Anything else, the {data.site.store} team replies within one working day.</p>
        <Button variant="outline" size="lg" className="mt-6 h-12 rounded-full px-6" nativeButton={false} render={<Link to="/faq" />}>All questions<ArrowRightIcon /></Button>
      </div>
      <Accordion>
        {data.faq.filter((f) => f.home).map((f) => (
          <AccordionItem key={f.q} value={f.q}>
            <AccordionTrigger className="py-5 text-left text-lg font-medium">{f.q}</AccordionTrigger>
            <AccordionContent className="max-w-prose text-base text-muted-foreground">{f.a}</AccordionContent>
          </AccordionItem>
        ))}
      </Accordion>
    </section>
  )
}

export function Home() {
  const { data } = useShop()
  useTitle(`${data.site.name} | Considered sneakers for everyday miles`)
  return (
    <>
      <Hero />
      <Trust />
      <Popular />
      <Collections />
      <Spotlight />
      <Promise />
      <Reviews />
      <Faq />
      <section className="mx-auto max-w-[90rem] px-4 pb-8 sm:px-6 lg:px-10" aria-labelledby="news">
        <Reveal className="grid gap-8 rounded-[2rem] bg-accent p-8 sm:p-12 lg:grid-cols-2 lg:items-center lg:p-16">
          <div>
            <SectionTitle><span id="news">Good things,<br />occasionally.</span></SectionTitle>
            <p className="mt-4 text-lg text-foreground/80">New pairs, restocks and one useful note a month. Subscribe and take 10% off your first pair.</p>
          </div>
          <NewsletterForm />
        </Reveal>
      </section>
    </>
  )
}
