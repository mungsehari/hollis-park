import { useState, type FormEvent } from "react"
import { AnimatePresence, motion } from "motion/react"
import { ExternalLinkIcon, PlusIcon, SparklesIcon, Trash2Icon } from "lucide-react"
import { toast } from "sonner"
import { ImageField } from "@/components/admin/image-picker"
import { Empty, ErrorNote, PageHeader } from "@/components/admin/shared"
import { Badge } from "@/components/reui/badge"
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
import type { CarePick, Marketplace } from "@/lib/types"
import { useApi } from "@/lib/use-api"

const LABEL: Record<Marketplace, string> = { AMAZON: "Amazon", FLIPKART: "Flipkart" }
const DOT: Record<Marketplace, string> = { AMAZON: "bg-[#FF9900]", FLIPKART: "bg-[#2874F0]" }
const HINT: Record<Marketplace, string> = {
  AMAZON: "An https link on amazon.in or an amzn.to short link (your Associates link with your tag).",
  FLIPKART: "An https link on flipkart.com or a fkrt.it short link (your affiliate link).",
}

const blank = (sortOrder: number): CarePick => ({ id: null, title: "", blurb: "", image: null, marketplace: "AMAZON", url: "", sortOrder, active: true })

/** Care picks: affiliate products (socks, cleaners, insoles) shown under "Care for your pair". */
export function CarePicks() {
  const { can } = useAuth()
  const admin = can("ADMIN")
  const { data, error, loading, reload } = useApi<CarePick[]>("/api/v1/admin/care-picks")
  const [editing, setEditing] = useState<CarePick | null>(null)
  const [removing, setRemoving] = useState<CarePick | null>(null)

  const toggle = async (p: CarePick, active: boolean) => {
    try { await api.put(`/api/v1/admin/care-picks/${p.id}`, { ...p, active }); toast.success(active ? "Pick is live" : "Pick hidden from the store"); reload() }
    catch (err) { toast.error(err instanceof ApiError ? err.message : "Could not update.") }
  }
  const remove = async () => {
    if (!removing) return
    try { await api.del(`/api/v1/admin/care-picks/${removing.id}`); toast.success("Pick removed"); setRemoving(null); reload() }
    catch (err) { toast.error(err instanceof ApiError ? err.message : "Could not remove.") }
  }

  return (
    <>
      <PageHeader
        title="Care picks"
        description="Socks, cleaners and insoles you recommend, bought on Amazon or Flipkart through your affiliate links. Every click is counted on the dashboard."
        actions={admin && <Button onClick={() => setEditing(blank((data?.length ?? 0) * 10))}><PlusIcon />New pick</Button>}
      />
      <ErrorNote error={error} />
      {loading && !data ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-64" />)}</div>
      ) : data && data.length === 0 ? (
        <Empty icon={<SparklesIcon />} title="No picks yet">Add one and it appears on the homepage and the Care picks page.</Empty>
      ) : (
        <motion.ul layout className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <AnimatePresence initial={false}>
            {(data ?? []).map((p, i) => (
              <motion.li
                key={p.id}
                layout
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0, transition: { delay: i * 0.04 } }}
                exit={{ opacity: 0, scale: 0.95 }}
                className="overflow-hidden rounded-2xl border bg-card transition-shadow hover:shadow-md"
              >
                <button type="button" onClick={() => setEditing(structuredClone(p))} className="block w-full text-left">
                  <div className="relative aspect-[16/9] bg-muted">
                    {p.image ? <img src={p.image} alt="" className="size-full object-cover" /> : <div className="flex size-full items-center justify-center text-muted-foreground"><SparklesIcon className="size-8" /></div>}
                    <span className="absolute top-3 left-3 flex items-center gap-1.5 rounded-full bg-background/90 px-2.5 py-1 text-xs font-medium">
                      <span className={`size-2 rounded-full ${DOT[p.marketplace]}`} />{LABEL[p.marketplace]}
                    </span>
                    {!p.active && <Badge variant="outline" radius="full" className="absolute top-3 right-3 bg-background">Hidden</Badge>}
                  </div>
                  <div className="p-4">
                    <h2 className="font-heading text-lg font-extrabold uppercase">{p.title}</h2>
                    <p className="line-clamp-2 text-sm text-muted-foreground">{p.blurb}</p>
                  </div>
                </button>
                <div className="flex items-center justify-between gap-2 border-t px-4 py-2.5 text-sm">
                  {admin ? (
                    <label className="flex items-center gap-2"><Switch checked={p.active} onCheckedChange={(on) => toggle(p, on)} aria-label={`${p.title} live`} />Show in store</label>
                  ) : <span className="text-muted-foreground">{p.active ? "Live" : "Hidden"}</span>}
                  <div className="flex gap-1">
                    <Button variant="ghost" size="icon-sm" aria-label="Open the link" render={<a href={p.url} target="_blank" rel="noreferrer" />} nativeButton={false}><ExternalLinkIcon /></Button>
                    {admin && <Button variant="ghost" size="icon-sm" aria-label={`Remove ${p.title}`} onClick={() => setRemoving(p)}><Trash2Icon /></Button>}
                  </div>
                </div>
              </motion.li>
            ))}
          </AnimatePresence>
        </motion.ul>
      )}

      <PickDialog pick={editing} readOnly={!admin} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload() }} />

      <AlertDialog open={!!removing} onOpenChange={(o) => { if (!o) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove {removing?.title}?</AlertDialogTitle>
            <AlertDialogDescription>It disappears from the store. Its past clicks stay on the dashboard.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter><AlertDialogCancel>Keep it</AlertDialogCancel><AlertDialogAction variant="destructive" onClick={remove}>Remove</AlertDialogAction></AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

function PickDialog({ pick, readOnly, onClose, onSaved }: { pick: CarePick | null; readOnly: boolean; onClose: () => void; onSaved: () => void }) {
  const [draft, setDraft] = useState<CarePick | null>(null)
  const [source, setSource] = useState<CarePick | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  if (pick !== source) { setSource(pick); setDraft(pick ? structuredClone(pick) : null); setError(null) }
  if (!pick || !draft) return null
  const set = <K extends keyof CarePick>(k: K, v: CarePick[K]) => setDraft({ ...draft, [k]: v })
  const isNew = !draft.id

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (isNew) await api.post("/api/v1/admin/care-picks", draft)
      else await api.put(`/api/v1/admin/care-picks/${draft.id}`, draft)
      toast.success(isNew ? "Pick added" : "Pick saved")
      onSaved()
    } catch (err) { setError(err instanceof ApiError ? err.message : "Could not save.") } finally { setBusy(false) }
  }

  return (
    <Dialog open onOpenChange={(o) => { if (!o) onClose() }}>
      <DialogContent className="max-h-[92svh] overflow-y-auto sm:max-w-xl">
        <form onSubmit={save} className="grid gap-6">
          <DialogHeader>
            <DialogTitle>{isNew ? "New care pick" : draft.title}</DialogTitle>
            <DialogDescription>Shown on the homepage and the Care picks page. It opens on {LABEL[draft.marketplace]} in a new tab.</DialogDescription>
          </DialogHeader>
          <fieldset disabled={readOnly} className="grid gap-6">
            <ImageField label="Photo (optional)" value={draft.image ?? null} onChange={(u) => set("image", u)} readOnly={readOnly} aspect="aspect-[4/3]" />
            <FieldGroup>
              <Field><FieldLabel htmlFor="pick-title">Title</FieldLabel><Input id="pick-title" required maxLength={120} value={draft.title} onChange={(e) => set("title", e.target.value)} placeholder="No-show cotton socks" /></Field>
              <Field><FieldLabel htmlFor="pick-blurb">Why you recommend it</FieldLabel><Textarea id="pick-blurb" rows={2} maxLength={300} value={draft.blurb ?? ""} onChange={(e) => set("blurb", e.target.value)} /></Field>
              <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
                <Field>
                  <FieldLabel>Marketplace</FieldLabel>
                  <Select value={draft.marketplace} onValueChange={(v) => set("marketplace", v as Marketplace)} items={[{ value: "AMAZON", label: "Amazon" }, { value: "FLIPKART", label: "Flipkart" }]}>
                    <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                    <SelectContent><SelectItem value="AMAZON">Amazon</SelectItem><SelectItem value="FLIPKART">Flipkart</SelectItem></SelectContent>
                  </Select>
                </Field>
                <Field><FieldLabel htmlFor="pick-order">Order</FieldLabel><Input id="pick-order" type="number" value={draft.sortOrder} onChange={(e) => set("sortOrder", Number(e.target.value) || 0)} /></Field>
              </div>
              <Field>
                <FieldLabel htmlFor="pick-url">Link</FieldLabel>
                <Input id="pick-url" type="url" required inputMode="url" value={draft.url} onChange={(e) => set("url", e.target.value.trim())} placeholder={draft.marketplace === "AMAZON" ? "https://amzn.to/..." : "https://www.flipkart.com/..."} />
                <FieldDescription>{HINT[draft.marketplace]}</FieldDescription>
              </Field>
              <Field orientation="horizontal"><Switch id="pick-active" checked={draft.active} onCheckedChange={(on) => set("active", on)} /><FieldLabel htmlFor="pick-active">Show in the store</FieldLabel></Field>
            </FieldGroup>
          </fieldset>
          <ErrorNote error={error} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>{readOnly ? "Close" : "Cancel"}</Button>
            {!readOnly && <Button type="submit" disabled={busy}>{busy ? "Saving" : "Save"}</Button>}
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
