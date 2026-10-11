import { useState, type FormEvent, type ReactNode } from "react"
import { Link, useNavigate, useParams } from "react-router"
import { ArchiveIcon, ArchiveRestoreIcon, ArrowLeftIcon, ArrowLeftToLineIcon, ExternalLinkIcon, ImagePlusIcon, PlusIcon, StarIcon, Trash2Icon, XIcon } from "lucide-react"
import { toast } from "sonner"
import { MediaPicker } from "@/components/admin/image-picker"
import { ErrorNote, PageHeader } from "@/components/admin/shared"
import { Badge } from "@/components/reui/badge"
import { Frame, FrameDescription, FrameHeader, FramePanel, FrameTitle } from "@/components/reui/frame"
import { NumberField, NumberFieldDecrement, NumberFieldGroup, NumberFieldIncrement, NumberFieldInput } from "@/components/reui/number-field"
import { Button } from "@/components/ui/button"
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { api, ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import type { Product, ProductAdmin, StoreInfo } from "@/lib/types"
import { useApi } from "@/lib/use-api"

const blank = (): Product => ({
  id: "", name: "", collection: "running", line: "", colourway: "", swatch: "linear-gradient(135deg, #ECE7DC 50%, #8D9A89 50%)",
  colours: [], price: 0, wide: null, rating: 0, reviews: 0, flag: null, rank: 50, added: new Date().toISOString().slice(0, 10),
  img: "", gallery: [], context: null, contextCaption: "", alt: "", blurb: "", desc: "", keywords: "", care: "",
  specs: [["Weight", "", "US 9, one shoe"], ["Drop", "", "Heel to forefoot"], ["Stack", "", "Millimetres of foam"], ["Best for", "", ""]],
  materials: [["Upper", ""], ["Midsole", ""], ["Outsole", ""]],
  fit: { verdict: "True to size", text: "" }, soldOut: [], low: [],
})

function Section({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  return (
    <Frame>
      <FrameHeader><FrameTitle>{title}</FrameTitle>{description && <FrameDescription>{description}</FrameDescription>}</FrameHeader>
      <FramePanel><FieldGroup>{children}</FieldGroup></FramePanel>
    </Frame>
  )
}

function Money({ id, value, onChange, min = 0 }: { id: string; value: number; onChange: (n: number) => void; min?: number }) {
  return (
    <NumberField id={id} value={value} min={min} step={1} largeStep={100} onValueChange={(v) => onChange(v ?? 0)} format={{ style: "currency", currency: "INR", maximumFractionDigits: 0 }}>
      <NumberFieldGroup>
        <NumberFieldDecrement />
        <NumberFieldInput />
        <NumberFieldIncrement />
      </NumberFieldGroup>
    </NumberField>
  )
}

export function ProductEdit() {
  const { id } = useParams()
  const isNew = !id
  const navigate = useNavigate()
  const { can } = useAuth()
  const readOnly = !can("ADMIN")
  const { data, error: loadError, loading, setData } = useApi<ProductAdmin>(isNew ? null : `/api/admin/products/${id}`)
  const { data: store } = useApi<StoreInfo>("/api/store")
  const [p, setP] = useState<Product | null>(isNew ? blank() : null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [picker, setPicker] = useState(false)

  // A fresh copy of the product becomes the editable draft whenever it (re)loads or saves.
  const [synced, setSynced] = useState<ProductAdmin | null>(null)
  if (data && data !== synced) {
    setSynced(data)
    setP(structuredClone(data.product))
  }

  if (loadError) return <><Back /><ErrorNote error={loadError} /></>
  if ((!isNew && loading) || !p) return <div className="space-y-4"><Skeleton className="h-9 w-64" /><Skeleton className="h-96" /></div>

  const set = <K extends keyof Product>(k: K, v: Product[K]) => setP({ ...p, [k]: v })
  const sizes = store?.sizes ?? []

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const body = { ...p, img: p.gallery[0] ?? p.img, flag: p.flag || null, context: p.context || null }
      const res = isNew ? await api.post<ProductAdmin>("/api/admin/products", body) : await api.put<ProductAdmin>(`/api/admin/products/${p.id}`, body)
      toast.success(isNew ? `${res.product.name} created` : "Changes saved")
      if (isNew) navigate(`/products/${res.product.id}`, { replace: true })
      else setData(res)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save.")
    } finally {
      setBusy(false)
    }
  }

  const toggleActive = async () => {
    if (!data) return
    try {
      const res = await api.patch<ProductAdmin>(`/api/admin/products/${p.id}`, { active: !data.active })
      setData(res)
      toast.success(res.active ? "Product is live again" : "Product archived and hidden from the store")
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not update.")
    }
  }

  return (
    <form onSubmit={save} className="space-y-6">
      <Back />
      <PageHeader
        title={isNew ? "New product" : p.name || "Product"}
        description={isNew ? "Fill in the details shoppers see on the product page." : <>{data?.active ? <Badge variant="success" radius="full">Live</Badge> : <Badge variant="outline" radius="full">Archived</Badge>} <span className="ml-1">/{p.id}</span></>}
        actions={<>
          {!isNew && data?.active && <Button variant="outline" nativeButton={false} render={<a href={`/catalog/${p.id}`} target="_blank" rel="noreferrer" />}><ExternalLinkIcon />View in store</Button>}
          {!isNew && !readOnly && <Button type="button" variant="outline" onClick={toggleActive}>{data?.active ? <><ArchiveIcon />Archive</> : <><ArchiveRestoreIcon />Restore</>}</Button>}
          {!readOnly && <Button type="submit" disabled={busy}>{busy ? "Saving" : isNew ? "Create product" : "Save changes"}</Button>}
        </>}
      />
      {readOnly && <p className="text-sm text-muted-foreground">You can view products. Only admins can change them.</p>}
      <ErrorNote error={error} />

      <fieldset disabled={readOnly} className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Section title="Basics">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field><FieldLabel htmlFor="name">Name</FieldLabel><Input id="name" required value={p.name} onChange={(e) => set("name", e.target.value)} /></Field>
              <Field>
                <FieldLabel htmlFor="pid">Web address</FieldLabel>
                <Input id="pid" required disabled={!isNew} value={p.id} pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="tide-two"
                  onChange={(e) => set("id", e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, "-"))} />
                <FieldDescription>{isNew ? "Lowercase and dashes. It cannot change later." : `/catalog/${p.id}`}</FieldDescription>
              </Field>
              <Field>
                <FieldLabel>Collection</FieldLabel>
                <Select value={p.collection} onValueChange={(v) => set("collection", String(v))} items={(store?.collections ?? []).map((c) => ({ value: c.id, label: c.name }))}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                  <SelectContent>{(store?.collections ?? []).map((c) => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}</SelectContent>
                </Select>
              </Field>
              <Field><FieldLabel htmlFor="line">Line</FieldLabel><Input id="line" value={p.line} onChange={(e) => set("line", e.target.value)} /></Field>
              <Field><FieldLabel htmlFor="colourway">Colourway</FieldLabel><Input id="colourway" value={p.colourway} onChange={(e) => set("colourway", e.target.value)} placeholder="Chalk / Clay" /></Field>
              <Field><FieldLabel htmlFor="flag">Label on the card</FieldLabel><Input id="flag" value={p.flag ?? ""} onChange={(e) => set("flag", e.target.value)} placeholder="Bestseller, New, ..." /></Field>
            </div>
            <Field><FieldLabel htmlFor="blurb">Short description</FieldLabel><Input id="blurb" value={p.blurb} onChange={(e) => set("blurb", e.target.value)} /></Field>
            <Field><FieldLabel htmlFor="desc">Full description</FieldLabel><Textarea id="desc" rows={4} value={p.desc} onChange={(e) => set("desc", e.target.value)} /></Field>
            <Field><FieldLabel htmlFor="care">Care</FieldLabel><Textarea id="care" rows={2} value={p.care} onChange={(e) => set("care", e.target.value)} /></Field>
            <Field>
              <FieldLabel htmlFor="keywords">Search words</FieldLabel>
              <Input id="keywords" value={p.keywords} onChange={(e) => set("keywords", e.target.value)} />
              <FieldDescription>Extra words shoppers might search for, separated by spaces.</FieldDescription>
            </Field>
          </Section>

          <Section title="Sizes and stock" description="Tap a size to mark it. Shoppers see sold-out sizes crossed out and low sizes flagged.">
            <Field>
              <FieldLabel>Sold out</FieldLabel>
              <ToggleGroup multiple variant="outline" spacing={1} className="flex-wrap" value={p.soldOut.map(String)} onValueChange={(v: string[]) => set("soldOut", v.map(Number).sort((a, b) => a - b))}>
                {sizes.map((s) => <ToggleGroupItem key={s} value={String(s)} aria-label={`US ${s} sold out`} className="min-w-11 data-pressed:bg-destructive data-pressed:text-white">{s}</ToggleGroupItem>)}
              </ToggleGroup>
            </Field>
            <Field>
              <FieldLabel>Only a few left</FieldLabel>
              <ToggleGroup multiple variant="outline" spacing={1} className="flex-wrap" value={p.low.map(String)} onValueChange={(v: string[]) => set("low", v.map(Number).sort((a, b) => a - b))}>
                {sizes.map((s) => <ToggleGroupItem key={s} value={String(s)} aria-label={`US ${s} low stock`} disabled={p.soldOut.includes(s)} className="min-w-11 data-pressed:bg-warning data-pressed:text-white">{s}</ToggleGroupItem>)}
              </ToggleGroup>
            </Field>
          </Section>

          <Section title="Specs" description="The four tiles on the product page.">
            {p.specs.map((row, i) => (
              <div key={i} className="grid gap-2 sm:grid-cols-[1fr_1fr_1.4fr]">
                {row.map((cell, j) => (
                  <Input key={j} aria-label={["Spec name", "Value", "Note"][j] + ` ${i + 1}`} value={cell}
                    onChange={(e) => set("specs", p.specs.map((r, ri) => (ri === i ? (r.map((c, ci) => (ci === j ? e.target.value : c)) as [string, string, string]) : r)))} />
                ))}
              </div>
            ))}
          </Section>

          <Section title="Materials">
            {p.materials.map(([k, v], i) => (
              <div key={i} className="flex gap-2">
                <Input aria-label={`Part ${i + 1}`} className="w-40" value={k} onChange={(e) => set("materials", p.materials.map((m, mi) => (mi === i ? [e.target.value, m[1]] : m)))} />
                <Input aria-label={`Material ${i + 1}`} value={v} onChange={(e) => set("materials", p.materials.map((m, mi) => (mi === i ? [m[0], e.target.value] : m)))} />
                <Button type="button" variant="ghost" size="icon" aria-label={`Remove ${k || "row"}`} onClick={() => set("materials", p.materials.filter((_, mi) => mi !== i))}><Trash2Icon /></Button>
              </div>
            ))}
            <div><Button type="button" variant="outline" size="sm" onClick={() => set("materials", [...p.materials, ["", ""]])}><PlusIcon />Add material</Button></div>
          </Section>
        </div>

        <div className="space-y-4">
          <Section title="Price">
            <Field><FieldLabel htmlFor="price">Price (standard width)</FieldLabel><Money id="price" value={p.price} onChange={(n) => set("price", n)} /></Field>
            <Field orientation="horizontal">
              <Switch id="wide" checked={!!p.wide} onCheckedChange={(on) => set("wide", on ? { extra: 799, soldOut: [] } : null)} />
              <FieldLabel htmlFor="wide">Also sold in Wide</FieldLabel>
            </Field>
            {p.wide && <Field><FieldLabel htmlFor="wide-extra">Wide costs extra</FieldLabel><Money id="wide-extra" value={p.wide.extra} onChange={(n) => set("wide", { ...p.wide!, extra: n })} /></Field>}
          </Section>

          <Section title="Photos" description="The first photo is the main one. Shoppers see the second photo when they hover a product card.">
            <ul className="grid grid-cols-3 gap-2">
              {p.gallery.map((url, i) => (
                <li key={url + i} className="group relative aspect-square overflow-hidden rounded-xl border bg-muted">
                  <img src={url} alt={i === 0 ? p.alt : ""} className="size-full object-cover" />
                  {i === 0 && <span className="absolute top-1.5 left-1.5 flex items-center gap-1 rounded-full bg-primary px-2 py-0.5 text-[10px] font-medium text-primary-foreground"><StarIcon className="size-3" />Main</span>}
                  {!readOnly && (
                    <div className="absolute inset-x-1.5 bottom-1.5 flex justify-between opacity-100 transition-opacity sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100">
                      <Button type="button" size="icon-xs" variant="secondary" aria-label="Make main photo" disabled={i === 0} onClick={() => set("gallery", [url, ...p.gallery.filter((_, n) => n !== i)])}><ArrowLeftToLineIcon /></Button>
                      <Button type="button" size="icon-xs" variant="secondary" aria-label="Remove photo" onClick={() => set("gallery", p.gallery.filter((_, n) => n !== i))}><XIcon /></Button>
                    </div>
                  )}
                </li>
              ))}
              {!readOnly && (
                <li>
                  <button type="button" onClick={() => setPicker(true)} className="flex aspect-square w-full flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed text-sm text-muted-foreground transition-colors hover:border-primary hover:text-foreground">
                    <ImagePlusIcon className="size-5" />Add photos
                  </button>
                </li>
              )}
            </ul>
            <MediaPicker open={picker} onOpenChange={setPicker} multiple title="Add product photos"
              onPick={(urls) => set("gallery", [...p.gallery, ...urls.filter((u) => !p.gallery.includes(u))])} />
            <Field><FieldLabel htmlFor="alt">Main photo description</FieldLabel><Textarea id="alt" rows={2} value={p.alt} onChange={(e) => set("alt", e.target.value)} />
              <FieldDescription>Read aloud by screen readers and shown if the image cannot load.</FieldDescription></Field>
          </Section>

          <Section title="Fit">
            <Field><FieldLabel htmlFor="fit-verdict">Verdict</FieldLabel><Input id="fit-verdict" required value={p.fit.verdict} onChange={(e) => set("fit", { ...p.fit, verdict: e.target.value })} placeholder="True to size" /></Field>
            <Field><FieldLabel htmlFor="fit-text">Fit note</FieldLabel><Textarea id="fit-text" rows={3} value={p.fit.text} onChange={(e) => set("fit", { ...p.fit, text: e.target.value })} /></Field>
          </Section>

          <Section title="Listing">
            <Field>
              <FieldLabel htmlFor="rank">Position in "Featured"</FieldLabel>
              <Input id="rank" type="number" min={1} value={p.rank} onChange={(e) => set("rank", Number(e.target.value))} />
              <FieldDescription>Lower numbers show first.</FieldDescription>
            </Field>
            <Field><FieldLabel htmlFor="added">Added on</FieldLabel><Input id="added" type="date" value={p.added} onChange={(e) => set("added", e.target.value)} /></Field>
          </Section>
        </div>
      </fieldset>
    </form>
  )
}

function Back() {
  return <Link to="/products" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"><ArrowLeftIcon className="size-4" />Products</Link>
}
