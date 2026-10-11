import { motion } from "motion/react"
import { useState, type FormEvent } from "react"
import { Link } from "react-router"
import { ArrowRightIcon } from "lucide-react"
import { cn } from "cn"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { inStock } from "@/lib/format"
import { useShop } from "@/state/store"
import { SectionTitle } from "@/components/store/brand"
import { Reveal } from "@/components/store/motion"
import { PageShell } from "@/components/store/page"

export function SizeGuide() {
  const { data, byId } = useShop()
  const { sizeChart: chart, sizes, config } = data
  const [cm, setCm] = useState("")
  const [model, setModel] = useState("any")
  const [error, setError] = useState(false)
  const [r, setR] = useState<{ us: number; uk: number; eu: number; note: string; pid: string | null; pname: string | null } | null>(null)
  const options = [{ value: "any", label: "Any model" }, ...data.products.map((p) => ({ value: p.id, label: p.name }))]

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const n = parseFloat(cm)
    if (!(n >= 21 && n <= 32)) { setError(true); return }
    setError(false)
    let idx = chart.findIndex((row) => row[3] >= n)
    const tooBig = idx === -1
    if (tooBig) idx = chart.length - 1
    const p = byId[model]
    let adj = 0
    let advice = "Most of our models are true to size. Check the fit note for the model you choose."
    if (p) {
      if (/large/i.test(p.fit.verdict)) { adj = -1; advice = `${p.name} runs half a size large, so we have taken half a size off.` }
      else if (/small/i.test(p.fit.verdict)) { adj = 1; advice = `${p.name} runs slightly small, so we have added half a size.` }
      else advice = `${p.name} is true to size. ${p.fit.text.split(". ").slice(1).join(". ")}`
    }
    idx = Math.max(0, Math.min(chart.length - 1, idx + adj))
    const [us, uk, eu] = chart[idx]
    const stock = p ? (inStock(p, us) ? `${config.sizeLabel} ${us} is in stock.` : `${config.sizeLabel} ${us} is sold out right now; we restock most sizes within three weeks.`) : ""
    const edge = tooBig ? `Our largest size is ${config.sizeLabel} ${sizes[sizes.length - 1]}. ` : n < 24 ? `Our smallest size is ${config.sizeLabel} ${sizes[0]}. ` : ""
    setR({ us, uk, eu, note: `${edge}${advice} ${stock}`, pid: p?.id ?? null, pname: p?.name ?? null })
  }

  return (
    <PageShell title="A better fit starts here" trail={[["Home", "/"], ["Size guide", null]]}
      lead="Measure once, in the evening, in the socks you run or walk in. Then check the fit note for the model you have in mind.">
      <div className="grid gap-8 lg:grid-cols-[1fr_1.2fr]">
        <form onSubmit={submit} noValidate className="space-y-5 rounded-3xl border bg-card p-6 sm:p-8">
          <h2 className="font-heading text-2xl font-extrabold uppercase">Find your size</h2>
          <div>
            <label htmlFor="cm" className="mb-1.5 block text-sm font-medium">Foot length in cm</label>
            <Input id="cm" type="number" inputMode="decimal" step="0.1" min={21} max={32} placeholder="For example 26.5" value={cm}
              aria-invalid={error} aria-describedby="cm-hint" onChange={(e) => { setCm(e.target.value); setError(false) }} className="h-12 rounded-xl px-4" />
            <p id="cm-hint" className={cn("mt-1.5 text-sm", error ? "text-destructive" : "text-muted-foreground")}>{error ? "Enter a length between 21 and 32 cm." : "Heel to the tip of your longest toe, on your longer foot."}</p>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium">Model</label>
            <Select value={model} onValueChange={(v) => setModel(String(v))} items={options}>
              <SelectTrigger className="h-12 w-full rounded-xl" aria-label="Model"><SelectValue /></SelectTrigger>
              <SelectContent>{options.map((o) => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
            </Select>
          </div>
          <Button type="submit" size="lg" className="h-12 rounded-full px-7">Show my size</Button>
          <div aria-live="polite">
            {r ? (
              <motion.div key={`${r.us}${r.pid}`} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="rounded-2xl bg-accent p-5">
                <dl className="grid grid-cols-3 gap-3 text-center">
                  {[["US", r.us], ["UK", r.uk], ["EU", r.eu]].map(([k, v]) => <div key={k} className="rounded-xl bg-background py-3"><dt className="font-mono text-[11px] tracking-[0.15em] text-muted-foreground">{k}</dt><dd className="font-heading text-3xl font-extrabold tabular-nums">{v}</dd></div>)}
                </dl>
                <p className="mt-4 text-sm">{r.note}</p>
                {r.pid && <Link to={`/catalog/${r.pid}`} className="mt-3 inline-flex items-center gap-1.5 font-medium underline underline-offset-4">Shop {r.pname}<ArrowRightIcon className="size-4" /></Link>}
              </motion.div>
            ) : <p className="text-sm text-muted-foreground">Your recommended US, UK and EU sizes appear here.</p>}
          </div>
        </form>
        <ol className="grid gap-4">
          {[
            ["Stand on paper", "Heel against a wall, weight on the foot, in the socks you plan to wear."],
            ["Mark heel and toe", "Hold the pen upright. Mark the back of the heel and the tip of the longest toe."],
            ["Measure the longer foot", "Measure between the marks on both feet and use the larger number."],
          ].map(([t, d], i) => (
            <Reveal key={t} delay={i * 0.08}><li className="flex gap-5 rounded-3xl border bg-card p-6">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-foreground font-heading text-xl font-extrabold text-background">{i + 1}</span>
              <div><h3 className="text-xl font-semibold">{t}</h3><p className="mt-1 text-muted-foreground">{d}</p></div></li></Reveal>
          ))}
        </ol>
      </div>

      <section className="mt-20" aria-labelledby="chart">
        <SectionTitle><span id="chart">Size chart</span></SectionTitle>
        <p className="mt-2 text-muted-foreground">Unisex sizing. US women's is 1.5 sizes above US men's.</p>
        <div className="mt-8 overflow-x-auto rounded-3xl border bg-card">
          <table className="w-full min-w-[30rem] text-left text-sm">
            <thead className="bg-muted font-mono text-[11px] tracking-[0.15em] text-muted-foreground uppercase"><tr>{["US men", "US women", "UK", "EU", "Foot length"].map((h) => <th key={h} scope="col" className="px-5 py-3.5 font-medium">{h}</th>)}</tr></thead>
            <tbody className="divide-y">
              {chart.map(([us, uk, eu, c]) => (
                <tr key={us} className={cn("tabular-nums transition-colors", r?.us === us && "bg-accent font-semibold")}>
                  <th scope="row" className="px-5 py-3">{us}</th><td className="px-5 py-3">{us + 1.5}</td><td className="px-5 py-3">{uk}</td><td className="px-5 py-3">{eu}</td><td className="px-5 py-3">{c} cm</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="mt-20" aria-labelledby="fit">
        <SectionTitle><span id="fit">Fit by model</span></SectionTitle>
        <div className="mt-8 grid gap-4 md:grid-cols-2">
          {data.products.map((p) => (
            <Reveal key={p.id}><article className="flex gap-4 rounded-3xl border bg-card p-4">
              <img src={p.img} alt="" className="size-20 shrink-0 rounded-2xl object-cover" loading="lazy" />
              <div><Link to={`/catalog/${p.id}`} className="font-heading text-lg font-extrabold uppercase hover:underline">{p.name}</Link>
                <span className="ml-2 rounded-full bg-accent px-2.5 py-0.5 text-xs font-medium">{p.fit.verdict}</span><p className="mt-1 text-sm text-muted-foreground">{p.fit.text}</p></div>
            </article></Reveal>
          ))}
        </div>
      </section>
    </PageShell>
  )
}
