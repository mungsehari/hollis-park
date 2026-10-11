import { Link, useParams } from "react-router"
import { ArrowRightIcon, ArrowUpRightIcon, FootprintsIcon, HammerIcon, LeafIcon, ClockIcon, RulerIcon, ShieldCheckIcon, HeartIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Eyebrow, SectionTitle } from "@/components/store/brand"
import { Reveal, Stagger, StaggerItem } from "@/components/store/motion"
import { Crumbs, PageShell, useTitle } from "@/components/store/page"
import { ProductCard } from "@/components/store/product-card"
import { useShop } from "@/state/store"
import { NotFound } from "./NotFound"

const ICONS: Record<string, typeof FootprintsIcon> = { footprints: FootprintsIcon, hammer: HammerIcon, leaf: LeafIcon, clock: ClockIcon, ruler: RulerIcon, shield: ShieldCheckIcon, heart: HeartIcon }

export function Collections() {
  const { data, money } = useShop()
  return (
    <PageShell title="Four ways to move" trail={[["Home", "/"], ["Collections", null]]} wide
      lead="Each collection has a couple of models, chosen to cover the job without a wall of near-identical options. Start with the activity, then compare fit and feel.">
      <div className="space-y-6">
        {data.collections.map((c, idx) => {
          const ps = data.products.filter((p) => p.collection === c.id)
          return (
            <Reveal key={c.id} className="group grid overflow-hidden rounded-[2rem] border bg-card lg:grid-cols-2">
              <Link to={`/collections/${c.id}`} className={`relative block min-h-72 overflow-hidden bg-muted ${idx % 2 ? "lg:order-2" : ""}`} aria-label={c.name}>
                {c.image && <img src={c.image} alt={c.alt ?? ""} loading="lazy" className="absolute inset-0 size-full object-cover transition-transform duration-1000 group-hover:scale-105" />}
              </Link>
              <div className="flex flex-col justify-between gap-8 p-7 sm:p-10">
                <div>
                  <Eyebrow>{ps.length} models{ps.length ? `, from ${money(Math.min(...ps.map((p) => p.price)))}` : ""}</Eyebrow>
                  <h2 className="mt-3 font-heading text-4xl font-extrabold uppercase"><Link to={`/collections/${c.id}`} className="hover:underline">{c.name}</Link></h2>
                  <p className="mt-3 text-muted-foreground">{c.intro}</p>
                </div>
                <div className="flex flex-wrap gap-3">
                  {ps.map((p) => (
                    <Link key={p.id} to={`/catalog/${p.id}`} className="flex items-center gap-3 rounded-2xl border p-2 pr-4 transition-colors hover:bg-muted">
                      <img src={p.img} alt="" className="size-14 rounded-xl object-cover" /><span className="text-sm"><b className="block">{p.name}</b><span className="text-muted-foreground tabular-nums">{money(p.price)}</span></span>
                    </Link>
                  ))}
                </div>
              </div>
            </Reveal>
          )
        })}
      </div>

      <section className="mt-20" aria-labelledby="compare">
        <div className="flex items-end justify-between gap-4"><SectionTitle><span id="compare">Compare at a glance</span></SectionTitle><Link to="/size-guide" className="hidden items-center gap-1.5 font-medium sm:flex">Fit notes<ArrowRightIcon className="size-4" /></Link></div>
        <div className="mt-8 overflow-x-auto rounded-3xl border bg-card">
          <table className="w-full min-w-[40rem] text-left text-sm">
            <thead className="bg-muted font-mono text-[11px] tracking-[0.15em] text-muted-foreground uppercase">
              <tr>{["Model", "Collection", "Weight", "Drop", "Stack", "Fit", "Price"].map((h) => <th key={h} scope="col" className="px-5 py-3.5 font-medium">{h}</th>)}</tr>
            </thead>
            <tbody className="divide-y">
              {data.products.map((p) => (
                <tr key={p.id} className="transition-colors hover:bg-muted/60">
                  <th scope="row" className="px-5 py-4"><Link to={`/catalog/${p.id}`} className="font-heading text-base font-extrabold uppercase hover:underline">{p.name}</Link></th>
                  <td className="px-5 py-4">{data.collections.find((c) => c.id === p.collection)?.name}</td>
                  <td className="px-5 py-4 tabular-nums">{p.specs[0]?.[1]}</td><td className="px-5 py-4 tabular-nums">{p.specs[1]?.[1]}</td><td className="px-5 py-4 tabular-nums">{p.specs[2]?.[1]} mm</td>
                  <td className="px-5 py-4">{p.fit.verdict}</td><td className="px-5 py-4 font-medium tabular-nums">{money(p.price)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </PageShell>
  )
}

export function Collection() {
  const { id = "" } = useParams()
  const { data, collById } = useShop()
  const c = collById[id]
  useTitle(c ? `${c.name} sneakers | ${data.site.name}` : "Collection")
  if (!c) return <NotFound />
  const ps = data.products.filter((p) => p.collection === c.id)
  const others = data.collections.filter((x) => x.id !== c.id)
  return (
    <div className="mx-auto max-w-[90rem] px-4 pt-8 sm:px-6 lg:px-10">
      <Crumbs trail={[["Home", "/"], ["Collections", "/collections"], [c.name, null]]} />
      <header className="relative mt-6 overflow-hidden rounded-[2rem] bg-sage text-white">
        {c.image && <img src={c.image} alt={c.alt ?? ""} className="absolute inset-0 size-full object-cover" />}
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
        <div className="relative flex min-h-[26rem] flex-col justify-end p-8 sm:p-12 lg:min-h-[32rem]">
          <h1 className="font-heading text-6xl leading-[0.9] font-extrabold uppercase sm:text-8xl" tabIndex={-1}>{c.name}</h1>
          <p className="mt-4 max-w-xl text-lg text-white/90">{c.intro}</p>
        </div>
      </header>

      <section className="mt-16" aria-labelledby="models">
        <div className="flex items-end justify-between gap-4"><SectionTitle><span id="models">The models</span></SectionTitle><Link to={`/catalog?c=${c.id}`} className="hidden items-center gap-1.5 font-medium sm:flex">Filter in the shop<ArrowRightIcon className="size-4" /></Link></div>
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          {ps.map((p) => <ProductCard key={p.id} p={p} />)}
          {c.choose.length > 0 && (
            <Reveal className="rounded-3xl bg-accent p-6">
              <h3 className="font-heading text-2xl font-extrabold uppercase">Which one?</h3>
              <ul className="mt-5 space-y-4">
                {c.choose.map((t, i) => (
                  <li key={t} className="flex gap-3">{ps[i] && <img src={ps[i].img} alt="" className="size-14 shrink-0 rounded-xl object-cover" />}<p className="text-sm">{t}</p></li>
                ))}
              </ul>
              <Button variant="outline" className="mt-6 rounded-full bg-background" nativeButton={false} render={<Link to="/contact?topic=sizing" />}>Ask the {data.site.store} team</Button>
            </Reveal>
          )}
        </div>
      </section>

      {c.guide.length > 0 && (
        <section className="mt-20" aria-labelledby="look">
          <SectionTitle><span id="look">What to look for</span></SectionTitle>
          <Stagger className="mt-8 grid gap-4 md:grid-cols-3">
            {c.guide.map((g) => {
              const Icon = ICONS[g.icon] ?? FootprintsIcon
              return (
                <StaggerItem key={g.t} className="rounded-3xl border bg-card p-6">
                  <span className="flex size-12 items-center justify-center rounded-full bg-accent"><Icon className="size-5" /></span>
                  <h3 className="mt-5 text-xl font-semibold">{g.t}</h3><p className="mt-2 text-muted-foreground">{g.p}</p>
                </StaggerItem>
              )
            })}
          </Stagger>
        </section>
      )}

      {others.length > 0 && (
        <section className="mt-20" aria-labelledby="others">
          <SectionTitle><span id="others">Other collections</span></SectionTitle>
          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            {others.slice(0, 3).map((o) => (
              <Link key={o.id} to={`/collections/${o.id}`} className="group relative aspect-[4/3] overflow-hidden rounded-3xl bg-muted">
                {o.image && <img src={o.image} alt="" loading="lazy" className="absolute inset-0 size-full object-cover transition-transform duration-700 group-hover:scale-105" />}
                <span className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                <span className="absolute inset-x-5 bottom-4 flex items-center justify-between font-heading text-xl font-extrabold text-white uppercase">{o.name}<ArrowUpRightIcon /></span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

export function Saved() {
  const { saved, byId } = useShop()
  const list = saved.map((id) => byId[id]).filter(Boolean)
  return (
    <PageShell title="Saved pairs" trail={[["Home", "/"], ["Saved", null]]} wide lead="Pairs you marked with the heart stay here while you compare. Saved items live in this browser only.">
      {list.length ? (
        <div className="grid grid-cols-2 gap-x-4 gap-y-10 lg:grid-cols-4 lg:gap-x-6">{list.map((p) => <ProductCard key={p.id} p={p} />)}</div>
      ) : (
        <div className="flex flex-col items-center gap-3 rounded-3xl border border-dashed py-20 text-center">
          <span className="flex size-14 items-center justify-center rounded-full bg-muted"><HeartIcon className="size-6" /></span>
          <h2 className="font-heading text-2xl font-extrabold uppercase">Nothing saved yet</h2>
          <p className="text-muted-foreground">Tap the heart on any pair to keep it here while you decide.</p>
          <Button className="mt-2 rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Browse all pairs</Button>
        </div>
      )}
    </PageShell>
  )
}
