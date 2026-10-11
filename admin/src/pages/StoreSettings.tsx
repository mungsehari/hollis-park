import { useMemo, useState, type FormEvent } from "react"
import { PlusIcon, TicketPercentIcon, Trash2Icon } from "lucide-react"
import { toast } from "sonner"
import { AdminGrid, type GridColumn } from "@/components/admin/grid"
import { Empty, ErrorNote, PageHeader } from "@/components/admin/shared"
import { Frame, FrameDescription, FrameHeader, FramePanel, FrameTitle } from "@/components/reui/frame"
import { NumberField, NumberFieldDecrement, NumberFieldGroup, NumberFieldIncrement, NumberFieldInput } from "@/components/reui/number-field"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldGroup, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { api, ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { date, money } from "@/lib/format"
import type { Promo, Settings as S } from "@/lib/types"
import { useApi } from "@/lib/use-api"

function Amount({ id, label, hint, value, onChange, step = 1, currency = true }: {
  id: string; label: string; hint?: string; value: number; onChange: (n: number) => void; step?: number; currency?: boolean
}) {
  return (
    <Field>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <NumberField id={id} value={value} min={0} step={currency ? 1 : 0.01} largeStep={step} onValueChange={(v) => onChange(v ?? 0)}
        format={currency ? { style: "currency", currency: "INR", maximumFractionDigits: 0 } : { maximumFractionDigits: 2 }}>
        <NumberFieldGroup><NumberFieldDecrement /><NumberFieldInput /><NumberFieldIncrement /></NumberFieldGroup>
      </NumberField>
      {hint && <FieldDescription>{hint}</FieldDescription>}
    </Field>
  )
}

export function Settings() {
  const { can } = useAuth()
  const readOnly = !can("ADMIN")
  const { data, error: loadError, setData } = useApi<S>("/api/admin/settings")
  const [s, setS] = useState<S | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [synced, setSynced] = useState<S | null>(null)
  if (data && data !== synced) {
    setSynced(data)
    setS({ ...data, taxRate: Number(data.taxRate) })
  }

  if (loadError) return <ErrorNote error={loadError} />
  if (!s) return <Skeleton className="h-96" />

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      setData(await api.put<S>("/api/admin/settings", s))
      toast.success("Saved. The store uses the new values within 30 seconds.")
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save.")
    } finally {
      setBusy(false)
    }
  }

  const pct = Math.round(s.taxRate * 10000) / 100
  return (
    <form onSubmit={save} className="space-y-6">
      <PageHeader title="Tax and delivery" description="Applied to the bag, checkout and every new order."
        actions={!readOnly && <Button type="submit" disabled={busy}>{busy ? "Saving" : "Save changes"}</Button>} />
      {readOnly && <p className="text-sm text-muted-foreground">Only admins can change these settings.</p>}
      <ErrorNote error={error} />
      <fieldset disabled={readOnly} className="grid gap-4 lg:grid-cols-2">
        <Frame>
          <FrameHeader><FrameTitle>Tax</FrameTitle><FrameDescription>For India, prices normally include GST.</FrameDescription></FrameHeader>
          <FramePanel>
            <FieldGroup>
              <FieldSet>
                <FieldLegend variant="label">How prices show tax</FieldLegend>
                <RadioGroup value={s.taxMode} onValueChange={(v) => setS({ ...s, taxMode: v as S["taxMode"] })}>
                  <Field orientation="horizontal"><RadioGroupItem value="included" id="tm-included" /><FieldLabel htmlFor="tm-included" className="font-normal">Included in prices (shows "Includes {pct}% {s.taxName}")</FieldLabel></Field>
                  <Field orientation="horizontal"><RadioGroupItem value="added" id="tm-added" /><FieldLabel htmlFor="tm-added" className="font-normal">Added at checkout</FieldLabel></Field>
                </RadioGroup>
              </FieldSet>
              <div className="grid gap-4 sm:grid-cols-2">
                <Amount id="tax-rate" label="Rate (%)" value={pct} step={0.5} currency={false} onChange={(n) => setS({ ...s, taxRate: Math.min(99.99, n) / 100 })}
                  hint={pct === 0 ? "0 hides the tax line." : undefined} />
                <Field><FieldLabel htmlFor="tax-name">Name</FieldLabel><Input id="tax-name" value={s.taxName} onChange={(e) => setS({ ...s, taxName: e.target.value })} required /></Field>
              </div>
            </FieldGroup>
          </FramePanel>
        </Frame>
        <Frame>
          <FrameHeader><FrameTitle>Delivery</FrameTitle><FrameDescription>Store pickup is always free.</FrameDescription></FrameHeader>
          <FramePanel>
            <FieldGroup>
              <Amount id="ship-fee" label="Standard delivery" step={10} value={s.shipFee} onChange={(n) => setS({ ...s, shipFee: n })} />
              <Amount id="free-from" label="Free standard delivery from" step={500} value={s.freeShipFrom} onChange={(n) => setS({ ...s, freeShipFrom: n })}
                hint={`A bag of ${money(s.freeShipFrom)} or more ships free.`} />
              <Amount id="express-fee" label="Express delivery" step={10} value={s.expressFee} onChange={(n) => setS({ ...s, expressFee: n })} />
            </FieldGroup>
          </FramePanel>
        </Frame>
      </fieldset>
    </form>
  )
}

export function PromoCodes() {
  const { data, error, loading, reload } = useApi<Promo[]>("/api/admin/promo-codes")
  const [adding, setAdding] = useState(false)
  const [deleting, setDeleting] = useState<Promo | null>(null)

  const patch = async (p: Promo, body: Partial<Promo>, message: string) => {
    try { await api.patch(`/api/admin/promo-codes/${p.code}`, body); toast.success(message); reload() }
    catch (err) { toast.error(err instanceof ApiError ? err.message : "Could not update.") }
  }

  const columns = useMemo<GridColumn<Promo>[]>(() => [
    { id: "code", accessorKey: "code", header: "Code", size: 200, cell: ({ row }) => <span className="font-mono font-medium">{row.original.code}</span> },
    { id: "rate", accessorKey: "rate", header: "Discount", size: 120, cell: ({ row }) => `${Math.round(Number(row.original.rate) * 100)}% off` },
    { id: "active", accessorKey: "active", header: "Active", size: 110,
      cell: ({ row }) => <Switch checked={row.original.active} aria-label={`${row.original.code} active`}
        onCheckedChange={(on) => patch(row.original, { active: on }, on ? "Code turned on" : "Code turned off")} /> },
    { id: "createdAt", accessorKey: "createdAt", header: "Created", size: 140, cell: ({ row }) => <span className="text-muted-foreground">{date(row.original.createdAt)}</span> },
    { id: "delete", header: "", size: 70,
      cell: ({ row }) => <Button variant="ghost" size="icon" aria-label={`Delete ${row.original.code}`} onClick={() => setDeleting(row.original)}><Trash2Icon /></Button> },
  ], []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <PageHeader title="Promo codes" description="The first active code is the one newsletter subscribers receive."
        actions={<Button onClick={() => setAdding(true)}><PlusIcon />New code</Button>} />
      <ErrorNote error={error} />
      <AdminGrid data={data ?? []} columns={columns} getRowId={(p) => p.code} isLoading={loading} emptyMessage={<Empty icon={<TicketPercentIcon />} title="No promo codes" />} />
      <PromoDialog open={adding} onOpenChange={setAdding} onDone={reload} />
      <AlertDialog open={!!deleting} onOpenChange={(o) => { if (!o) setDeleting(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete {deleting?.code}?</AlertDialogTitle>
            <AlertDialogDescription>Shoppers can no longer use it. Orders that already used it keep their discount. To pause a code instead, switch it off.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep it</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={async () => {
              if (!deleting) return
              try { await api.del(`/api/admin/promo-codes/${deleting.code}`); toast.success("Code deleted"); setDeleting(null); reload() }
              catch (err) { toast.error(err instanceof ApiError ? err.message : "Could not delete.") }
            }}>Delete code</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

function PromoDialog({ open, onOpenChange, onDone }: { open: boolean; onOpenChange: (o: boolean) => void; onDone: () => void }) {
  const [code, setCode] = useState("")
  const [percent, setPercent] = useState(10)
  const [error, setError] = useState<string | null>(null)
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      await api.post("/api/admin/promo-codes", { code, rate: percent / 100 })
      toast.success(`${code.toUpperCase()} created`)
      setCode("")
      onOpenChange(false)
      onDone()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not create.")
    }
  }
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <form onSubmit={submit} className="grid gap-5">
          <DialogHeader><DialogTitle>New promo code</DialogTitle><DialogDescription>A percentage off the bag subtotal.</DialogDescription></DialogHeader>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="p-code">Code</FieldLabel>
              <Input id="p-code" required pattern="[A-Za-z0-9]{3,40}" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="DIWALI20" />
              <FieldDescription>Letters and digits only.</FieldDescription>
            </Field>
            <Amount id="p-rate" label="Discount (%)" value={percent} currency={false} onChange={(n) => setPercent(Math.max(1, Math.min(90, n)))} />
          </FieldGroup>
          <ErrorNote error={error} />
          <DialogFooter><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit">Create code</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
