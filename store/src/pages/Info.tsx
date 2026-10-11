import { motion } from "motion/react"
import { useEffect, useState, type FormEvent } from "react"
import { Link, useSearchParams } from "react-router"
import { ArrowRightIcon, ClockIcon, MailIcon, MapPinIcon, MessageCircleIcon, PackageIcon, PhoneIcon, RotateCcwIcon, StoreIcon, TruckIcon } from "lucide-react"
import { toast } from "sonner"
import { Radio } from "@base-ui/react/radio"
import { RadioGroup } from "@base-ui/react/radio-group"
import { cn } from "cn"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import { api, ApiError } from "@/lib/api"
import { addWorkingDays, EMAIL_RE } from "@/lib/format"
import { useShop } from "@/state/store"
import { SectionTitle } from "@/components/store/brand"
import { CountUp, Reveal, Stagger, StaggerItem } from "@/components/store/motion"
import { NewsletterForm } from "@/components/store/newsletter-form"
import { PageShell } from "@/components/store/page"

export function Delivery() {
  const { data, money } = useShop()
  const { config, site } = data
  const cards = [
    { icon: TruckIcon, t: "Standard", price: money(config.shipFee), note: `free over ${money(config.freeShipFrom)}`, d: "3-5 working days across India, tracked from dispatch." },
    { icon: ClockIcon, t: "Express", price: money(config.expressFee), d: "Next working day when ordered by 2pm, Monday to Thursday." },
    { icon: StoreIcon, t: "Store pickup", price: "Free", d: `Ready in about two hours at 41 Quill Lane. We hold it for seven days.` },
    { icon: PackageIcon, t: "Outside India", price: money(1999), d: "7-10 working days to selected countries. Import duties and taxes are payable on delivery." },
  ]
  return (
    <PageShell title="Delivery and returns" trail={[["Home", "/"], ["Delivery and returns", null]]}
      lead={`Orders placed before 2pm on a working day leave ${site.store} the same afternoon. Every box includes a prepaid return label.`}>
      <Stagger className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map(({ icon: Icon, t, price, note, d }) => (
          <StaggerItem key={t} className="rounded-3xl border bg-card p-6">
            <span className="flex size-12 items-center justify-center rounded-full bg-accent"><Icon className="size-5" /></span>
            <h2 className="mt-5 text-xl font-semibold">{t}</h2>
            <p className="mt-1 font-heading text-3xl font-extrabold tabular-nums">{price} {note && <span className="font-sans text-sm font-normal text-muted-foreground">{note}</span>}</p>
            <p className="mt-3 text-muted-foreground">{d}</p>
          </StaggerItem>
        ))}
      </Stagger>
      <div className="mt-16 grid gap-10 lg:grid-cols-2">
        <div><SectionTitle>The 60-day<br />wear test</SectionTitle><p className="mt-5 text-lg text-muted-foreground">Wear them properly: outside, on runs, on the commute. If they are not right within 60 days of delivery, return them free for a refund or a different size.</p></div>
        <ol className="space-y-3">
          {[[MessageCircleIcon, "Start a return", "Send your order number through the contact form. We confirm within one working day."], [PackageIcon, "Pack and label", "Use any box. Stick on the prepaid label from your original parcel."], [RotateCcwIcon, "Refund in 3 days", "Refunds go back to the original payment method within three working days of arrival."]].map(([Icon, t, d], i) => {
            const I = Icon as typeof PackageIcon
            return <Reveal key={t as string} delay={i * 0.08}><li className="flex gap-4 rounded-3xl border bg-card p-5"><span className="flex size-11 shrink-0 items-center justify-center rounded-full bg-accent"><I className="size-5" /></span><div><h3 className="font-semibold">{t as string}</h3><p className="text-sm text-muted-foreground">{d as string}</p></div></li></Reveal>
          })}
        </ol>
      </div>
      <Reveal className="mt-16 grid gap-8 rounded-[2rem] bg-accent p-8 sm:p-12 lg:grid-cols-[1.4fr_1fr] lg:items-center">
        <div><SectionTitle>Resole service</SectionTitle><p className="mt-4 max-w-xl text-lg text-foreground/80">When the tread wears through, send the pair back. We remove the old outsole, clean the upper and fit a new sole in the original compound.</p>
          <dl className="mt-6 flex flex-wrap gap-x-10 gap-y-4"><div><dt className="text-sm text-muted-foreground">Price</dt><dd className="font-heading text-2xl font-extrabold tabular-nums">{money(3499)}</dd></div><div><dt className="text-sm text-muted-foreground">Turnaround</dt><dd className="font-heading text-2xl font-extrabold">About 10 days</dd></div><div><dt className="text-sm text-muted-foreground">Eligible</dt><dd className="font-heading text-2xl font-extrabold">All but Drift Knit</dd></div></dl></div>
        <Button size="lg" className="h-14 rounded-full px-7 text-base lg:justify-self-end" nativeButton={false} render={<Link to="/contact?topic=resole" />}>Request a resole<ArrowRightIcon /></Button>
      </Reveal>
    </PageShell>
  )
}

export function Faq() {
  const { data } = useShop()
  const [cat, setCat] = useState(data.faqCats[0]?.[0] ?? "")
  return (
    <PageShell title="Questions, answered" trail={[["Home", "/"], ["Questions", null]]} lead={`The questions the ${data.site.store} team hears most, grouped by topic. If yours is not here, ask us directly.`}>
      <div className="grid gap-10 lg:grid-cols-[1.6fr_1fr]">
        <div>
          <Tabs value={cat} onValueChange={setCat}><TabsList className="h-auto flex-wrap">{data.faqCats.map(([id, label]) => <TabsTrigger key={id} value={id}>{label}</TabsTrigger>)}</TabsList></Tabs>
          <motion.div key={cat} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="mt-4">
            <Accordion>
              {data.faq.filter((f) => f.cat === cat).map((f) => (
                <AccordionItem key={f.q} value={f.q}><AccordionTrigger className="py-5 text-left text-lg font-medium">{f.q}</AccordionTrigger><AccordionContent className="max-w-prose text-base text-muted-foreground">{f.a}</AccordionContent></AccordionItem>
              ))}
            </Accordion>
          </motion.div>
        </div>
        <aside className="h-fit rounded-3xl bg-accent p-7 lg:sticky lg:top-28">
          <h2 className="font-heading text-2xl font-extrabold uppercase">Still deciding?</h2>
          <p className="mt-3 text-foreground/80">Send a photo of your current pair and your usual size. We will suggest a model and size within one working day.</p>
          <Button className="mt-5 rounded-full" nativeButton={false} render={<Link to="/contact?topic=sizing" />}>Ask about sizing</Button>
        </aside>
      </div>
    </PageShell>
  )
}

const TOPICS = [{ value: "sizing", label: "Sizing help" }, { value: "order", label: "An existing order" }, { value: "fitting", label: "Book a fitting" }, { value: "resole", label: "Resole a pair" }, { value: "other", label: "Something else" }]

export function Contact() {
  const { data } = useShop()
  const { store, orderPrefix } = data.site
  const [params] = useSearchParams()
  const initial = params.get("topic")
  const [topic, setTopic] = useState(TOPICS.some((t) => t.value === initial) ? initial! : "sizing")
  const [f, setF] = useState({ name: "", email: "", order: "", message: "" })
  const [slot, setSlot] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState<{ name: string; slot: string | null } | null>(null)

  const slots: { day: string; t: string }[] = []
  let d = new Date()
  while (slots.length < 8) { d = addWorkingDays(d, 1); const day = d.toLocaleDateString(data.config.locale, { weekday: "short", day: "numeric", month: "short" }); ["10:30", "16:00"].forEach((t) => slots.push({ day, t })) }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const er: Record<string, string> = {}
    if (!f.name.trim()) er.name = "Tell us what to call you."
    if (!EMAIL_RE.test(f.email.trim())) er.email = "Enter an email like name@example.com."
    if (topic === "order" && !new RegExp(`^${orderPrefix}[0-9]{5}$`).test(f.order.trim())) er.order = `Order numbers look like ${orderPrefix}24817.`
    if (topic === "fitting" && !slot) er.slot = "Choose a fitting slot."
    if (f.message.trim().length < 10) er.message = "Add a little more detail, at least 10 characters."
    setErrors(er)
    if (Object.keys(er).length) return
    setBusy(true)
    try {
      await api.contact({ name: f.name.trim(), email: f.email.trim(), topic, message: f.message.trim(), order: topic === "order" ? f.order.trim() : undefined, slot: topic === "fitting" ? slot ?? undefined : undefined })
      setDone({ name: f.name.trim().split(" ")[0], slot: topic === "fitting" ? slot : null })
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not send. Please try again.")
    } finally { setBusy(false) }
  }

  const err = (k: string) => errors[k] && <p className="mt-1.5 text-sm text-destructive">{errors[k]}</p>
  return (
    <PageShell title={`Talk to ${store}`} trail={[["Home", "/"], ["Contact", null]]} lead={`Sizing help, an order, a fitting or a resole. A person at the ${store} store reads every message and replies within one working day.`}>
      <div className="grid gap-8 lg:grid-cols-[1.4fr_1fr]">
        {done ? (
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="rounded-3xl bg-accent p-8 sm:p-12">
            <h2 className="font-heading text-3xl font-extrabold uppercase">Thanks, {done.name}.</h2>
            <p className="mt-3 text-lg text-foreground/80">{done.slot ? <>We have your request for a fitting on <b>{done.slot}</b> at {store}. </> : null}The team replies within one working day.</p>
            <div className="mt-6 flex gap-3"><Button variant="outline" className="rounded-full bg-background" onClick={() => { setDone(null); setF({ name: "", email: "", order: "", message: "" }); setSlot(null) }}>Write another message</Button><Button className="rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Browse the shop</Button></div>
          </motion.div>
        ) : (
          <form onSubmit={submit} noValidate className="grid gap-5 rounded-3xl border bg-card p-6 sm:grid-cols-2 sm:p-8">
            <div><Label htmlFor="c-name" className="mb-1.5">Name</Label><Input id="c-name" value={f.name} aria-invalid={!!errors.name} onChange={(e) => setF({ ...f, name: e.target.value })} className="h-12 rounded-xl px-4" autoComplete="name" />{err("name")}</div>
            <div><Label htmlFor="c-email" className="mb-1.5">Email address</Label><Input id="c-email" type="email" value={f.email} aria-invalid={!!errors.email} onChange={(e) => setF({ ...f, email: e.target.value })} className="h-12 rounded-xl px-4" autoComplete="email" />{err("email")}</div>
            <div className="sm:col-span-2"><Label className="mb-1.5">Topic</Label>
              <Select value={topic} onValueChange={(v) => setTopic(String(v))} items={TOPICS}><SelectTrigger className="h-12 w-full rounded-xl" aria-label="Topic"><SelectValue /></SelectTrigger><SelectContent>{TOPICS.map((o) => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent></Select></div>
            {topic === "order" && <motion.div initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} className="sm:col-span-2"><Label htmlFor="c-order" className="mb-1.5">Order number</Label><Input id="c-order" value={f.order} placeholder={`${orderPrefix}24817`} aria-invalid={!!errors.order} onChange={(e) => setF({ ...f, order: e.target.value.toUpperCase() })} className="h-12 rounded-xl px-4 font-mono" />{err("order")}</motion.div>}
            {topic === "fitting" && (
              <motion.div initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} className="sm:col-span-2">
                <p id="slot-label" className="mb-1.5 text-sm font-medium">Preferred fitting slot</p>
                <RadioGroup aria-labelledby="slot-label" value={slot} onValueChange={(v) => setSlot(String(v))} className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                  {slots.map((s) => { const v = `${s.day}, ${s.t}`; return (
                    <Radio.Root key={v} value={v} nativeButton render={<button type="button" />} className="rounded-xl border p-3 text-left transition-colors hover:border-foreground data-checked:border-foreground data-checked:bg-foreground data-checked:text-background">
                      <b className="block text-sm">{s.day}</b><span className="text-sm opacity-70 tabular-nums">{s.t}</span>
                    </Radio.Root>) })}
                </RadioGroup>{err("slot")}
              </motion.div>
            )}
            <div className="sm:col-span-2"><Label htmlFor="c-msg" className="mb-1.5">Message</Label><Textarea id="c-msg" rows={5} value={f.message} aria-invalid={!!errors.message} onChange={(e) => setF({ ...f, message: e.target.value })} placeholder="Your usual size, the model you are considering, anything that has not fitted before." className="rounded-xl p-4" />{err("message")}</div>
            <div className="sm:col-span-2"><Button type="submit" size="lg" disabled={busy} className="h-14 rounded-full px-8 text-base">{busy ? "Sending" : "Send message"}</Button></div>
          </form>
        )}
        <aside className="h-fit space-y-5 rounded-3xl border bg-card p-6">
          <h2 className="font-heading text-2xl font-extrabold uppercase">{store} store</h2>
          <ul className="space-y-4 text-sm">
            <li className="flex gap-3"><MapPinIcon className="mt-0.5 size-4 shrink-0" />41 Quill Lane, {store}</li>
            <li className="flex gap-3"><ClockIcon className="mt-0.5 size-4 shrink-0" />Mon to Fri 10:00-19:00<br />Sat 10:00-18:00, Sun 11:00-17:00</li>
            <li className="flex gap-3"><PhoneIcon className="mt-0.5 size-4 shrink-0" /><span className="tabular-nums">+1 (503) 555-0192</span></li>
            <li className="flex gap-3"><MailIcon className="mt-0.5 size-4 shrink-0" />hello@hollispark.example</li>
          </ul>
        </aside>
      </div>
    </PageShell>
  )
}

export function About() {
  const { data, money } = useShop()
  const { site } = data
  const stats: [string, number, string?][] = [["Founded", 2019, "year"], ["Models in the range", data.products.length], ["Pairs resoled in 2025", 3140], ["Average rating", 4.8]]
  return (
    <PageShell title="Fewer pairs, worn longer" trail={[["Home", "/"], ["About", null]]} wide
      lead={`${site.name} started in 2019 with one running shoe and a repair bench. We still make a small range, publish the numbers behind every pair, and fix them when they wear out.`}>
      <Stagger className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map(([k, v, kind]) => (
          <StaggerItem key={k} className="rounded-3xl bg-accent p-6"><p className="text-sm text-foreground/70">{k}</p>
            <p className="mt-2 font-heading text-5xl font-extrabold tabular-nums">{kind === "year" ? v : <CountUp value={v} format={(n) => (v % 1 ? n.toFixed(1) : Math.round(n).toLocaleString("en-IN"))} />}</p></StaggerItem>
        ))}
      </Stagger>
      <div className="mt-14 grid gap-6 lg:grid-cols-3">
        {[
          ["Why only a few models", ["Most sneaker walls are the same four shoes in forty colours. We would rather make two good answers for each activity and keep every size in stock, so the pair you need is on the shelf the day you need it.", "Each model gets a year of wear testing by our own team and a panel of forty runners, players and commuters before it ships."]],
          ["Why we resole", ["The upper of a well-made shoe usually outlasts its sole by years. Our models are cemented with a heat-release adhesive so the outsole can come off cleanly and a new one can go on.", `A resole costs ${money(3499)}, takes about ten working days and uses the same rubber compound as the original.`]],
          ["What we measure", ["Weight, drop and stack height for every model, measured on a US 9.", "Fit notes written from returns data, not from the design brief.", "Reviews collected 30 days after delivery, once the shoes have been worn."]],
        ].map(([t, ps], i) => (
          <Reveal key={t as string} delay={i * 0.08} className="rounded-3xl border bg-card p-7"><h2 className="font-heading text-2xl font-extrabold uppercase">{t as string}</h2>{(ps as string[]).map((p) => <p key={p} className="mt-3 text-muted-foreground">{p}</p>)}</Reveal>
        ))}
      </div>
      <section className="mt-20" aria-labelledby="so-far"><SectionTitle><span id="so-far">So far</span></SectionTitle>
        <ol className="mt-8 divide-y border-y">
          {[["2019", "Tide, the first model.", "600 pairs, sold from a folding table at weekend races."], ["2021", "The resole bench opens.", "Every model redesigned so the outsole can be replaced."], ["2023", `${site.store} store.`, "Every model, every size, and a gait camera by the window."], ["2026", "A small range, four activities.", "Drift Knit and Loop Strap join the range."]].map(([y, t, d], i) => (
            <Reveal key={y} as="li" delay={i * 0.05} className="grid gap-2 py-6 sm:grid-cols-[8rem_1fr]"><span className="font-heading text-3xl font-extrabold tabular-nums">{y}</span><p><b>{t}</b> <span className="text-muted-foreground">{d}</span></p></Reveal>
          ))}
        </ol>
      </section>
      <section id="lines" className="mt-20 scroll-mt-28" aria-labelledby="lines-t"><SectionTitle><span id="lines-t">Three lines, one standard</span></SectionTitle>
        <div className="mt-8 grid gap-4 md:grid-cols-3">
          {data.lines.map((l) => (
            <Reveal key={l.id} className="rounded-3xl border bg-card p-6"><p className="font-mono text-[11px] tracking-[0.15em] text-muted-foreground uppercase">{l.for}</p>
              <h3 className="mt-2 font-heading text-2xl font-extrabold uppercase">{l.name}</h3><p className="mt-2 text-muted-foreground">{l.text}</p>
              <div className="mt-5 flex -space-x-3">{l.products.map((id) => data.products.find((p) => p.id === id)).filter(Boolean).map((p) => <img key={p!.id} src={p!.img} alt={p!.name} title={p!.name} className="size-14 rounded-full border-2 border-card object-cover" />)}</div></Reveal>
          ))}
        </div>
      </section>
    </PageShell>
  )
}

export function Newsletter() {
  const { data } = useShop()
  const [topics, setTopics] = useState<string[]>(["running", "everyday"])
  const toggle = (t: string, on: boolean) => setTopics((s) => (on ? [...s, t] : s.filter((x) => x !== t)))
  return (
    <PageShell title="The monthly note" trail={[["Home", "/"], ["Newsletter", null]]}
      lead="One email a month: what is new, what is back in stock, and one practical note on fit, care or training. Subscribers get 10% off their first pair.">
      <div className="grid gap-10 lg:grid-cols-2">
        <div className="space-y-6 rounded-3xl border bg-card p-6 sm:p-8">
          <fieldset><legend className="mb-3 text-sm font-medium">What should we write about?</legend>
            <div className="grid grid-cols-2 gap-3">
              {[["running", "Running"], ["court", "Court"], ["training", "Training"], ["everyday", "Everyday"]].map(([v, l]) => (
                <label key={v} className="flex cursor-pointer items-center gap-3 rounded-xl border p-3 has-[[data-checked]]:border-foreground has-[[data-checked]]:bg-muted"><Checkbox checked={topics.includes(v)} onCheckedChange={(c) => toggle(v, c === true)} />{l}</label>
              ))}
            </div></fieldset>
          <NewsletterForm topics={topics} />
        </div>
        <section aria-labelledby="issues"><h2 id="issues" className="font-mono text-[11px] tracking-[0.2em] text-muted-foreground uppercase">Recent issues</h2>
          <ol className="mt-4 divide-y">
            {[["Sep 2026", "Breaking in leather court shoes without the blisters", "Three evenings, thick socks and a shoe tree."], ["Aug 2026", "Drift Knit is here", "Why we went deeper on foam for the first time, and who should stick with Tide One instead."], ["Jul 2026", "How often should you replace running shoes?", "Mileage is a poor guide. What to check on the outsole and midsole, and when a resole makes sense."]].map(([d, t, p]) => (
              <Reveal as="li" key={t} className="py-5"><p className="text-sm text-muted-foreground">{d}</p><h3 className="mt-1 text-xl font-semibold">{t}</h3><p className="mt-1 text-muted-foreground">{p}</p></Reveal>
            ))}
          </ol></section>
      </div>
      <span className="sr-only">{data.site.name}</span>
    </PageShell>
  )
}

export function Legal() {
  const [params, setParams] = useSearchParams()
  const tabs = [["privacy", "Privacy"], ["terms", "Terms of sale"], ["cookies", "Cookies"]]
  const tab = tabs.some(([id]) => id === params.get("tab")) ? params.get("tab")! : "privacy"
  useEffect(() => { window.scrollTo({ top: 0 }) }, [])
  const body: Record<string, [string, string][]> = {
    privacy: [["What we collect", "Your name, email, delivery address and order history, so we can deliver, handle returns and resoles, and answer your questions. Payment details are handled by our payment provider and never stored by us."], ["What we do not do", "We do not sell or rent your details. We do not use third-party advertising trackers. We only email you about marketing if you subscribe."], ["Your choices", "Ask us for a copy of your data, a correction or deletion at any time through the contact form. We respond within 30 days."]],
    terms: [["Orders and prices", "Prices are in Indian rupees (INR) and include GST where it applies. An order is accepted when we send the dispatch email."], ["Returns", "Pairs can be returned within 60 days of delivery for a refund or exchange, including pairs that have been worn outside, as long as they are not damaged beyond normal wear."], ["Resoles", "Resoles are guaranteed against sole separation for 12 months. We will tell you before starting if an upper is too worn to resole, and return it free."]],
    cookies: [["Essential only", "The store uses storage in your browser for your bag and saved pairs. There are no analytics or advertising cookies."], ["Your control", "You can clear this at any time by clearing site data in your browser settings. Your bag will be emptied."]],
  }
  return (
    <PageShell title="Privacy and terms" trail={[["Home", "/"], ["Privacy and terms", null]]} lead="Plain-language summaries of the policies that apply to this store. Last updated September 2026.">
      <Tabs value={tab} onValueChange={(v) => setParams({ tab: String(v) }, { replace: true, preventScrollReset: true })}>
        <TabsList>{tabs.map(([id, l]) => <TabsTrigger key={id} value={id}>{l}</TabsTrigger>)}</TabsList>
      </Tabs>
      <motion.div key={tab} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="mt-8 max-w-2xl space-y-6">
        {body[tab].map(([h, p]) => <div key={h}><h2 className="font-heading text-2xl font-extrabold uppercase">{h}</h2><p className={cn("mt-2 text-lg text-muted-foreground")}>{p}</p></div>)}
      </motion.div>
    </PageShell>
  )
}
