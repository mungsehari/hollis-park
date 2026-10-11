import { useMemo, useState, type FormEvent } from "react"
import { Link } from "react-router"
import { ArrowDownIcon, ArrowUpIcon, ImageIcon, LayoutPanelTopIcon, PlusIcon, Trash2Icon } from "lucide-react"
import { toast } from "sonner"
import { DropZone, ImageField } from "@/components/admin/image-picker"
import { Empty, ErrorNote, PageHeader } from "@/components/admin/shared"
import { Badge } from "@/components/reui/badge"
import { Frame, FrameDescription, FrameHeader, FramePanel, FrameTitle } from "@/components/reui/frame"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { api, ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { date } from "@/lib/format"
import type { CollectionItem, HomeContent, MediaItem, Page, ProductAdmin } from "@/lib/types"
import { useApi } from "@/lib/use-api"

const kb = (n: number) => (n > 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB`)

/* ---------------- Media library ---------------- */

export function Media() {
  const { can } = useAuth()
  const admin = can("ADMIN")
  const { data, error, loading, reload } = useApi<Page<MediaItem>>("/api/admin/media?size=120")
  const [deleting, setDeleting] = useState<MediaItem | null>(null)

  return (
    <>
      <PageHeader title="Media" description="Every photo used on the store. Upload once, use it on products, collections and the homepage." />
      <ErrorNote error={error} />
      {admin && <DropZone onUploaded={reload} />}
      {loading && !data ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">{Array.from({ length: 10 }, (_, i) => <Skeleton key={i} className="aspect-square rounded-2xl" />)}</div>
      ) : data && data.items.length ? (
        <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
          {data.items.map((m) => (
            <li key={m.id} className="group overflow-hidden rounded-2xl border bg-card">
              <div className="relative aspect-square bg-muted"><img src={m.url} alt={m.alt ?? ""} loading="lazy" className="size-full object-cover" /></div>
              <div className="flex items-center justify-between gap-2 p-3">
                <div className="min-w-0"><p className="truncate text-sm font-medium" title={m.originalName}>{m.originalName ?? "image"}</p><p className="text-xs text-muted-foreground">{kb(m.sizeBytes)} · {date(m.createdAt)}</p></div>
                {admin && <Button variant="ghost" size="icon-sm" aria-label={`Delete ${m.originalName ?? "image"}`} onClick={() => setDeleting(m)}><Trash2Icon /></Button>}
              </div>
            </li>
          ))}
        </ul>
      ) : <Empty icon={<ImageIcon />} title="No images yet">Upload your first photo above.</Empty>}

      <AlertDialog open={!!deleting} onOpenChange={(o) => { if (!o) setDeleting(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this image?</AlertDialogTitle>
            <AlertDialogDescription>It is removed from storage for good. An image that a product, collection or the homepage still uses cannot be deleted.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep it</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={async () => {
              if (!deleting) return
              try { await api.del(`/api/admin/media/${deleting.id}`); toast.success("Image deleted"); setDeleting(null); reload() }
              catch (err) { toast.error(err instanceof ApiError ? err.message : "Could not delete."); setDeleting(null) }
            }}>Delete image</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

/* ---------------- Collections ---------------- */

const blank = (): CollectionItem => ({ id: "", name: "", tagline: "", intro: "", image: null, alt: "", choose: [], guide: [], sortOrder: 99, active: true })

export function Collections() {
  const { can } = useAuth()
  const admin = can("ADMIN")
  const { data, error, loading, reload } = useApi<CollectionItem[]>("/api/admin/collections")
  const { data: products } = useApi<ProductAdmin[]>("/api/admin/products")
  const [editing, setEditing] = useState<{ item: CollectionItem; isNew: boolean } | null>(null)
  const counts = useMemo(() => {
    const m: Record<string, number> = {}
    products?.filter((p) => p.active).forEach((p) => { m[p.product.collection] = (m[p.product.collection] ?? 0) + 1 })
    return m
  }, [products])

  const toggle = async (c: CollectionItem, active: boolean) => {
    try { await api.patch(`/api/admin/collections/${c.id}`, { active }); toast.success(active ? "Collection is live" : "Collection hidden from the store"); reload() }
    catch (err) { toast.error(err instanceof ApiError ? err.message : "Could not update.") }
  }

  return (
    <>
      <PageHeader title="Collections" description="The activity groups on the homepage, menu and shop filters."
        actions={admin && <Button onClick={() => setEditing({ item: blank(), isNew: true })}><PlusIcon />New collection</Button>} />
      <ErrorNote error={error} />
      {loading && !data ? <div className="grid gap-4 sm:grid-cols-2"><Skeleton className="h-48" /><Skeleton className="h-48" /></div> : (
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {(data ?? []).map((c) => (
            <li key={c.id} className="overflow-hidden rounded-2xl border bg-card">
              <button type="button" onClick={() => setEditing({ item: structuredClone(c), isNew: false })} className="block w-full text-left">
                <div className="relative aspect-[16/9] bg-muted">
                  {c.image ? <img src={c.image} alt="" className="size-full object-cover" /> : <div className="flex size-full items-center justify-center text-muted-foreground"><ImageIcon className="size-8" /></div>}
                  {!c.active && <Badge variant="outline" radius="full" className="absolute top-3 left-3 bg-background">Hidden</Badge>}
                </div>
                <div className="p-4"><h2 className="font-heading text-lg font-extrabold uppercase">{c.name}</h2><p className="line-clamp-1 text-sm text-muted-foreground">{c.tagline}</p>
                  <p className="mt-2 text-xs text-muted-foreground">{counts[c.id] ?? 0} live products</p></div>
              </button>
              {admin && (
                <div className="flex items-center justify-between border-t px-4 py-2.5 text-sm">
                  <label className="flex items-center gap-2"><Switch checked={c.active} onCheckedChange={(on) => toggle(c, on)} aria-label={`${c.name} live`} />Show in store</label>
                  <Button variant="ghost" size="sm" onClick={() => setEditing({ item: structuredClone(c), isNew: false })}>Edit</Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      <CollectionDialog state={editing} readOnly={!admin} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />
    </>
  )
}

function CollectionDialog({ state, readOnly, onClose, onSaved }: { state: { item: CollectionItem; isNew: boolean } | null; readOnly: boolean; onClose: () => void; onSaved: () => void }) {
  const [draft, setDraft] = useState<CollectionItem | null>(null)
  const [sourceKey, setSourceKey] = useState<unknown>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  if (state !== sourceKey) { setSourceKey(state); setDraft(state ? structuredClone(state.item) : null); setError(null) }
  if (!state || !draft) return null
  const set = <K extends keyof CollectionItem>(k: K, v: CollectionItem[K]) => setDraft({ ...draft, [k]: v })

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (state.isNew) await api.post("/api/admin/collections", draft)
      else await api.put(`/api/admin/collections/${draft.id}`, draft)
      toast.success(state.isNew ? "Collection created" : "Collection saved")
      onSaved()
    } catch (err) { setError(err instanceof ApiError ? err.message : "Could not save.") } finally { setBusy(false) }
  }
  const move = (i: number, d: number) => { const g = [...draft.guide]; [g[i], g[i + d]] = [g[i + d], g[i]]; set("guide", g) }

  return (
    <Dialog open onOpenChange={(o) => { if (!o) onClose() }}>
      <DialogContent className="max-h-[92svh] overflow-y-auto sm:max-w-2xl">
        <form onSubmit={save} className="grid gap-6">
          <DialogHeader><DialogTitle>{state.isNew ? "New collection" : draft.name}</DialogTitle><DialogDescription>Shown on the homepage tiles, the Shop menu and the collection page.</DialogDescription></DialogHeader>
          <fieldset disabled={readOnly} className="grid gap-6">
            <ImageField label="Collection photo" value={draft.image} onChange={(u) => set("image", u)} readOnly={readOnly} aspect="aspect-[16/9]" />
            <FieldGroup>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field><FieldLabel htmlFor="col-name">Name</FieldLabel><Input id="col-name" required value={draft.name} onChange={(e) => set("name", e.target.value)} /></Field>
                <Field>
                  <FieldLabel htmlFor="col-id">Web address</FieldLabel>
                  <Input id="col-id" required disabled={!state.isNew} pattern="[a-z0-9]+(-[a-z0-9]+)*" value={draft.id} placeholder="hiking" onChange={(e) => set("id", e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, "-"))} />
                </Field>
              </div>
              <Field><FieldLabel htmlFor="col-tag">Tagline</FieldLabel><Input id="col-tag" value={draft.tagline ?? ""} onChange={(e) => set("tagline", e.target.value)} /></Field>
              <Field><FieldLabel htmlFor="col-intro">Introduction</FieldLabel><Textarea id="col-intro" rows={3} value={draft.intro ?? ""} onChange={(e) => set("intro", e.target.value)} /></Field>
              <Field><FieldLabel htmlFor="col-alt">Photo description</FieldLabel><Input id="col-alt" value={draft.alt ?? ""} onChange={(e) => set("alt", e.target.value)} /></Field>
            </FieldGroup>

            <div>
              <p className="mb-2 text-sm font-medium">Which one? (one line per model)</p>
              <div className="space-y-2">
                {draft.choose.map((c, i) => (
                  <div key={i} className="flex gap-2"><Input aria-label={`Choice ${i + 1}`} value={c} onChange={(e) => set("choose", draft.choose.map((x, n) => (n === i ? e.target.value : x)))} />
                    <Button type="button" variant="ghost" size="icon" aria-label="Remove" onClick={() => set("choose", draft.choose.filter((_, n) => n !== i))}><Trash2Icon /></Button></div>
                ))}
                <Button type="button" variant="outline" size="sm" onClick={() => set("choose", [...draft.choose, ""])}><PlusIcon />Add a line</Button>
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-medium">What to look for</p>
              <div className="space-y-3">
                {draft.guide.map((g, i) => (
                  <div key={i} className="grid gap-2 rounded-xl border p-3">
                    <div className="flex gap-2">
                      <Input aria-label="Tip title" placeholder="Title" value={g.t} onChange={(e) => set("guide", draft.guide.map((x, n) => (n === i ? { ...x, t: e.target.value } : x)))} />
                      <Input aria-label="Icon" className="w-32" placeholder="icon" value={g.icon} onChange={(e) => set("guide", draft.guide.map((x, n) => (n === i ? { ...x, icon: e.target.value } : x)))} />
                    </div>
                    <Textarea aria-label="Tip text" rows={2} value={g.p} onChange={(e) => set("guide", draft.guide.map((x, n) => (n === i ? { ...x, p: e.target.value } : x)))} />
                    <div className="flex justify-end gap-1">
                      <Button type="button" variant="ghost" size="icon-sm" aria-label="Move up" disabled={i === 0} onClick={() => move(i, -1)}><ArrowUpIcon /></Button>
                      <Button type="button" variant="ghost" size="icon-sm" aria-label="Move down" disabled={i === draft.guide.length - 1} onClick={() => move(i, 1)}><ArrowDownIcon /></Button>
                      <Button type="button" variant="ghost" size="icon-sm" aria-label="Remove tip" onClick={() => set("guide", draft.guide.filter((_, n) => n !== i))}><Trash2Icon /></Button>
                    </div>
                  </div>
                ))}
                <Button type="button" variant="outline" size="sm" onClick={() => set("guide", [...draft.guide, { icon: "footprints", t: "", p: "" }])}><PlusIcon />Add a tip</Button>
                <p className="text-xs text-muted-foreground">Icons: footprints, ruler, clock, hammer, leaf, shield, heart.</p>
              </div>
            </div>

            <Field><FieldLabel htmlFor="col-order">Position</FieldLabel><Input id="col-order" type="number" min={0} className="w-28" value={draft.sortOrder} onChange={(e) => set("sortOrder", Number(e.target.value))} />
              <FieldDescription>Lower numbers come first in menus and on the homepage.</FieldDescription></Field>
          </fieldset>
          <ErrorNote error={error} />
          <DialogFooter><Button type="button" variant="outline" onClick={onClose}>{readOnly ? "Close" : "Cancel"}</Button>{!readOnly && <Button type="submit" disabled={busy}>{busy ? "Saving" : "Save collection"}</Button>}</DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/* ---------------- Homepage ---------------- */

export function Homepage() {
  const { can } = useAuth()
  const readOnly = !can("ADMIN")
  const { data, error: loadError, setData } = useApi<HomeContent>("/api/admin/home")
  const { data: products } = useApi<ProductAdmin[]>("/api/admin/products")
  const [h, setH] = useState<HomeContent | null>(null)
  const [synced, setSynced] = useState<HomeContent | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  if (data && data !== synced) { setSynced(data); setH(structuredClone(data)) }

  if (loadError) return <ErrorNote error={loadError} />
  if (!h) return <Skeleton className="h-96" />

  const hero = (k: keyof HomeContent["hero"], v: string | null) => setH({ ...h, hero: { ...h.hero, [k]: v } })
  const live = (products ?? []).filter((p) => p.active)
  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try { setData(await api.put<HomeContent>("/api/admin/home", h)); toast.success("Homepage saved. It is live now.") }
    catch (err) { setError(err instanceof ApiError ? err.message : "Could not save.") } finally { setBusy(false) }
  }

  return (
    <form onSubmit={save} className="space-y-6">
      <PageHeader title="Homepage" description="The first screen shoppers see, the banner above the menu and the featured pair."
        actions={<>
          <Button type="button" variant="outline" nativeButton={false} render={<a href="/" target="_blank" rel="noreferrer" />}>View the store</Button>
          {!readOnly && <Button type="submit" disabled={busy}>{busy ? "Saving" : "Save changes"}</Button>}
        </>} />
      {readOnly && <p className="text-sm text-muted-foreground">Only admins can change the homepage.</p>}
      <ErrorNote error={error} />
      <fieldset disabled={readOnly} className="grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <div className="space-y-4">
          <Frame>
            <FrameHeader><FrameTitle>Hero</FrameTitle><FrameDescription>The full-screen photo and headline.</FrameDescription></FrameHeader>
            <FramePanel>
              <FieldGroup>
                <ImageField label="Hero photo" value={h.hero.image} onChange={(u) => hero("image", u)} readOnly={readOnly} aspect="aspect-[16/9]" />
                <Field><FieldLabel htmlFor="h-alt">Photo description</FieldLabel><Input id="h-alt" value={h.hero.imageAlt ?? ""} onChange={(e) => hero("imageAlt", e.target.value)} /></Field>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field><FieldLabel htmlFor="h-eyebrow">Small label</FieldLabel><Input id="h-eyebrow" value={h.hero.eyebrow ?? ""} onChange={(e) => hero("eyebrow", e.target.value)} placeholder="New season" /></Field>
                  <Field><FieldLabel htmlFor="h-title">Headline</FieldLabel><Input id="h-title" required value={h.hero.title} onChange={(e) => hero("title", e.target.value)} /></Field>
                </div>
                <Field><FieldLabel htmlFor="h-sub">Subheading</FieldLabel><Textarea id="h-sub" rows={2} value={h.hero.subtitle ?? ""} onChange={(e) => hero("subtitle", e.target.value)} /></Field>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field><FieldLabel htmlFor="h-cta">Main button</FieldLabel><Input id="h-cta" value={h.hero.ctaLabel ?? ""} onChange={(e) => hero("ctaLabel", e.target.value)} /></Field>
                  <Field><FieldLabel htmlFor="h-cta-link">Goes to</FieldLabel><Input id="h-cta-link" value={h.hero.ctaLink ?? ""} onChange={(e) => hero("ctaLink", e.target.value)} placeholder="/catalog" /></Field>
                  <Field><FieldLabel htmlFor="h-s">Second button</FieldLabel><Input id="h-s" value={h.hero.secondaryLabel ?? ""} onChange={(e) => hero("secondaryLabel", e.target.value)} /></Field>
                  <Field><FieldLabel htmlFor="h-s-link">Goes to</FieldLabel><Input id="h-s-link" value={h.hero.secondaryLink ?? ""} onChange={(e) => hero("secondaryLink", e.target.value)} placeholder="/size-guide" /></Field>
                </div>
              </FieldGroup>
            </FramePanel>
          </Frame>
        </div>
        <div className="space-y-4">
          <Frame>
            <FrameHeader><FrameTitle>Announcement banner</FrameTitle><FrameDescription>The scrolling line above the menu.</FrameDescription></FrameHeader>
            <FramePanel>
              <FieldGroup>
                <Field orientation="horizontal"><Switch id="b-on" checked={h.banner.enabled} onCheckedChange={(on) => setH({ ...h, banner: { ...h.banner, enabled: on } })} /><FieldLabel htmlFor="b-on">Show the banner</FieldLabel></Field>
                <Field><FieldLabel htmlFor="b-text">Text</FieldLabel><Input id="b-text" maxLength={140} value={h.banner.text ?? ""} onChange={(e) => setH({ ...h, banner: { ...h.banner, text: e.target.value } })} /></Field>
                <Field><FieldLabel htmlFor="b-link">Links to</FieldLabel><Input id="b-link" value={h.banner.link ?? ""} onChange={(e) => setH({ ...h, banner: { ...h.banner, link: e.target.value } })} placeholder="/delivery" /></Field>
              </FieldGroup>
            </FramePanel>
          </Frame>
          <Frame>
            <FrameHeader><FrameTitle>Featured pair</FrameTitle><FrameDescription>The large "most worn this season" block.</FrameDescription></FrameHeader>
            <FramePanel>
              <Select value={h.spotlightProductId ?? ""} onValueChange={(v) => setH({ ...h, spotlightProductId: String(v) })}
                items={live.map((p) => ({ value: p.product.id, label: p.product.name }))}>
                <SelectTrigger className="w-full" aria-label="Featured product"><SelectValue placeholder="Choose a product" /></SelectTrigger>
                <SelectContent>{live.map((p) => <SelectItem key={p.product.id} value={p.product.id}>{p.product.name}</SelectItem>)}</SelectContent>
              </Select>
            </FramePanel>
          </Frame>
          <div className="flex items-center gap-2 rounded-xl bg-muted p-4 text-sm text-muted-foreground"><LayoutPanelTopIcon className="size-4 shrink-0" />Photos come from the <Link to="/media" className="underline underline-offset-4">media library</Link>.</div>
        </div>
      </fieldset>
    </form>
  )
}
