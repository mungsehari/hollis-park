import { useCallback, useRef, useState, type DragEvent } from "react"
import { CheckIcon, ImageIcon, ImagePlusIcon, Loader2Icon, UploadCloudIcon } from "lucide-react"
import { toast } from "sonner"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { api, ApiError } from "@/lib/api"
import type { MediaItem, Page } from "@/lib/types"
import { useApi } from "@/lib/use-api"

export const ACCEPT = "image/jpeg,image/png,image/webp,image/avif"
const MAX = 8 * 1024 * 1024

export async function uploadFiles(files: File[], alt?: string): Promise<MediaItem[]> {
  const out: MediaItem[] = []
  for (const f of files) {
    if (!ACCEPT.split(",").includes(f.type)) { toast.error(`${f.name}: use a JPG, PNG, WebP or AVIF image.`); continue }
    if (f.size > MAX) { toast.error(`${f.name}: images can be up to 8 MB.`); continue }
    try {
      out.push(await api.upload<MediaItem>("/api/v1/admin/media", f, alt ? { alt } : {}))
    } catch (err) {
      toast.error(`${f.name}: ${err instanceof ApiError ? err.message : "upload failed"}`)
    }
  }
  return out
}

/** A drop target that uploads what is dropped or chosen, then hands the new images back. */
export function DropZone({ onUploaded, multiple = true, compact = false }: { onUploaded: (items: MediaItem[]) => void; multiple?: boolean; compact?: boolean }) {
  const input = useRef<HTMLInputElement>(null)
  const [over, setOver] = useState(false)
  const [busy, setBusy] = useState(false)

  const run = useCallback(async (list: FileList | File[]) => {
    const files = Array.from(list).slice(0, multiple ? 12 : 1)
    if (!files.length) return
    setBusy(true)
    const done = await uploadFiles(files)
    setBusy(false)
    if (done.length) { toast.success(done.length === 1 ? "Image uploaded" : `${done.length} images uploaded`); onUploaded(done) }
  }, [multiple, onUploaded])

  const onDrop = (e: DragEvent) => { e.preventDefault(); setOver(false); void run(e.dataTransfer.files) }

  return (
    <div
      onDragOver={(e) => { e.preventDefault(); setOver(true) }}
      onDragLeave={() => setOver(false)}
      onDrop={onDrop}
      className={cn("flex flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed text-center transition-colors", compact ? "p-5" : "p-10", over ? "border-primary bg-accent" : "border-border bg-muted/40")}
    >
      {busy ? <Loader2Icon className="size-6 animate-spin text-muted-foreground" /> : <UploadCloudIcon className="size-6 text-muted-foreground" />}
      <p className="text-sm font-medium">{busy ? "Uploading" : `Drop ${multiple ? "images" : "an image"} here`}</p>
      <p className="text-xs text-muted-foreground">JPG, PNG, WebP or AVIF, up to 8 MB each</p>
      <Button type="button" variant="outline" size="sm" disabled={busy} onClick={() => input.current?.click()}>Choose from your computer</Button>
      <input ref={input} type="file" accept={ACCEPT} multiple={multiple} className="sr-only" tabIndex={-1} aria-label="Choose images"
        onChange={(e) => { if (e.target.files) void run(e.target.files); e.target.value = "" }} />
    </div>
  )
}

/** Library grid for picking one or several images; also hosts the upload tab. */
export function MediaPicker({ open, onOpenChange, onPick, multiple = false, title = "Choose an image" }: {
  open: boolean; onOpenChange: (o: boolean) => void; onPick: (urls: string[]) => void; multiple?: boolean; title?: string
}) {
  const [tab, setTab] = useState("library")
  const [chosen, setChosen] = useState<string[]>([])
  const { data, loading, reload } = useApi<Page<MediaItem>>(open ? "/api/v1/admin/media?size=60" : null)

  const toggle = (url: string) => setChosen((c) => (multiple ? (c.includes(url) ? c.filter((x) => x !== url) : [...c, url]) : [url]))
  const confirm = (urls: string[]) => { onPick(urls); setChosen([]); onOpenChange(false) }

  return (
    <Dialog open={open} onOpenChange={(o) => { onOpenChange(o); if (!o) setChosen([]) }}>
      <DialogContent className="max-h-[88svh] overflow-hidden sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>Pick from images already uploaded, or add new ones.</DialogDescription>
        </DialogHeader>
        <Tabs value={tab} onValueChange={(v) => setTab(String(v))}>
          <TabsList><TabsTrigger value="library">Library {data ? `(${data.totalElements})` : ""}</TabsTrigger><TabsTrigger value="upload">Upload new</TabsTrigger></TabsList>
        </Tabs>
        <div className="-mx-1 max-h-[52svh] overflow-y-auto px-1">
          {tab === "upload" ? (
            <DropZone multiple={multiple} onUploaded={(items) => { reload(); if (multiple) { setChosen(items.map((i) => i.url)); setTab("library") } else confirm([items[0].url]) }} />
          ) : loading && !data ? (
            <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">{Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="aspect-square rounded-xl" />)}</div>
          ) : data && data.content.length ? (
            <ul className="grid grid-cols-3 gap-3 sm:grid-cols-4">
              {data.content.map((m) => {
                const on = chosen.includes(m.url)
                return (
                  <li key={m.id}>
                    <button type="button" onClick={() => toggle(m.url)} onDoubleClick={() => !multiple && confirm([m.url])} aria-pressed={on}
                      className={cn("group relative block aspect-square w-full overflow-hidden rounded-xl bg-muted ring-2 ring-transparent transition", on && "ring-primary")}>
                      <img src={m.url} alt={m.alt ?? m.originalName ?? ""} loading="lazy" className="size-full object-cover transition-transform group-hover:scale-105" />
                      {on && <span className="absolute top-2 right-2 flex size-6 items-center justify-center rounded-full bg-primary text-primary-foreground"><CheckIcon className="size-3.5" /></span>}
                    </button>
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="flex flex-col items-center gap-2 py-10 text-center text-sm text-muted-foreground"><ImageIcon className="size-8" />No images yet. Upload the first one.</div>
          )}
        </div>
        <div className="flex items-center justify-between gap-3 border-t pt-4">
          <p className="text-sm text-muted-foreground">{chosen.length ? `${chosen.length} selected` : "Click an image to select it"}</p>
          <div className="flex gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="button" disabled={!chosen.length} onClick={() => confirm(chosen)}>Use {chosen.length > 1 ? `${chosen.length} images` : "image"}</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

/** A single image field: preview, change and remove. */
export function ImageField({ value, onChange, label, aspect = "aspect-[4/3]", readOnly = false }: {
  value?: string | null; onChange: (url: string | null) => void; label: string; aspect?: string; readOnly?: boolean
}) {
  const [open, setOpen] = useState(false)
  return (
    <div>
      <p className="mb-1.5 text-sm font-medium">{label}</p>
      <div className={cn("relative overflow-hidden rounded-xl border bg-muted", aspect)}>
        {value ? <img src={value} alt="" className="size-full object-cover" /> : <div className="flex size-full items-center justify-center text-muted-foreground"><ImageIcon className="size-8" /></div>}
      </div>
      {!readOnly && (
        <div className="mt-2 flex gap-2">
          <Button type="button" variant="outline" size="sm" onClick={() => setOpen(true)}><ImagePlusIcon />{value ? "Change image" : "Choose image"}</Button>
          {value && <Button type="button" variant="ghost" size="sm" onClick={() => onChange(null)}>Remove</Button>}
        </div>
      )}
      <MediaPicker open={open} onOpenChange={setOpen} onPick={(u) => onChange(u[0] ?? null)} title={label} />
    </div>
  )
}
