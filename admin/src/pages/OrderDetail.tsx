import { useState } from "react"
import { Link, useParams } from "react-router"
import { ArrowLeftIcon, MailIcon, MapPinIcon, TruckIcon } from "lucide-react"
import { toast } from "sonner"
import { ErrorNote, PageHeader, StatusBadge } from "@/components/admin/shared"
import { Frame, FrameHeader, FramePanel, FrameTitle } from "@/components/reui/frame"
import {
  Timeline, TimelineContent, TimelineDate, TimelineHeader, TimelineIndicator, TimelineItem, TimelineSeparator, TimelineTitle,
} from "@/components/reui/timeline"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Field, FieldLabel } from "@/components/ui/field"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { api, ApiError } from "@/lib/api"
import { dateTime, imageUrl, money, SHIP, STATUS } from "@/lib/format"
import type { OrderDetail as O, OrderStatus } from "@/lib/types"
import { useApi } from "@/lib/use-api"

export function OrderDetail() {
  const { number = "" } = useParams()
  const { data: o, error, loading, setData } = useApi<O>(`/api/v1/admin/orders/${encodeURIComponent(number)}`)
  const [pending, setPending] = useState<OrderStatus | null>(null)
  const [note, setNote] = useState("")
  const [adminNote, setAdminNote] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (error) return <><BackLink /><ErrorNote error={error} /></>
  if (loading || !o) return <div className="space-y-4"><Skeleton className="h-9 w-64" /><Skeleton className="h-96" /></div>

  const save = async (body: Record<string, unknown>, message: string) => {
    setBusy(true)
    try {
      setData(await api.patch<O>(`/api/v1/admin/orders/${o.orderNumber}`, body))
      toast.success(message)
      return true
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not save.")
      return false
    } finally {
      setBusy(false)
    }
  }

  const noteValue = adminNote ?? o.adminNote ?? ""

  return (
    <>
      <BackLink />
      <PageHeader
        title={o.orderNumber}
        description={<>Placed {dateTime(o.createdAt)} · <StatusBadge status={o.status} /></>}
        actions={o.nextStatuses.length ? [...o.nextStatuses].sort((a) => (a === "CANCELLED" ? 1 : -1)).map((s) => (
          <Button key={s} variant={s === "CANCELLED" ? "destructive" : "default"} onClick={() => { setNote(""); setPending(s) }}>{STATUS[s].action}</Button>
        )) : <span className="text-sm text-muted-foreground">This order is {STATUS[o.status].label.toLowerCase()}. No further steps.</span>}
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Frame>
            <FrameHeader><FrameTitle>Items</FrameTitle></FrameHeader>
            <FramePanel className="p-0">
              <Table>
                <TableHeader><TableRow><TableHead>Pair</TableHead><TableHead>Size</TableHead><TableHead className="text-right">Qty</TableHead><TableHead className="text-right">Price</TableHead><TableHead className="text-right">Total</TableHead></TableRow></TableHeader>
                <TableBody>
                  {o.items.map((l, i) => (
                    <TableRow key={i}>
                      <TableCell>
                        <Link to={`/products/${l.productId}`} className="flex items-center gap-3 hover:underline">
                          {l.image ? <img src={imageUrl(l.image)} alt="" className="size-10 rounded-md bg-muted object-cover" /> : <span className="size-10 rounded-md bg-muted" />}
                          <span className="font-medium">{l.name}</span>
                        </Link>
                      </TableCell>
                      <TableCell>US {l.size}{l.width === "wide" ? " Wide" : ""}</TableCell>
                      <TableCell className="text-right tabular-nums">{l.qty}</TableCell>
                      <TableCell className="text-right tabular-nums">{money(l.unitPrice)}</TableCell>
                      <TableCell className="text-right tabular-nums">{money(l.lineTotal)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <dl className="grid gap-1.5 border-t p-4 text-sm">
                <Row label="Subtotal" value={money(o.subtotal)} />
                {Number(o.discount) > 0 && <Row label={`Discount (${o.promo})`} value={"-" + money(o.discount)} />}
                <Row label={`${SHIP[o.ship] ?? o.ship} delivery`} value={Number(o.shipping) ? money(o.shipping) : "Free"} />
                {Number(o.tax) > 0 && <Row label="Tax included" value={money(o.tax)} />}
                <Row label="Total" value={money(o.total)} strong />
                <p className="pt-1 text-xs text-muted-foreground">
                  {o.paymentStatus === "PAID" ? <>Paid online{o.paidAt ? ` on ${dateTime(o.paidAt)}` : ""}. Razorpay payment <span className="font-mono">{o.razorpayPaymentId}</span>.</>
                    : o.paymentStatus === "PENDING" ? "Waiting for the customer to pay. Do not ship until it shows as paid."
                    : "No payment was taken online for this order."}
                </p>
              </dl>
            </FramePanel>
          </Frame>

          <Frame>
            <FrameHeader><FrameTitle>Internal note</FrameTitle></FrameHeader>
            <FramePanel>
              <Field>
                <FieldLabel htmlFor="admin-note" className="sr-only">Internal note</FieldLabel>
                <Textarea id="admin-note" rows={3} placeholder="Only staff can see this. For example: customer asked to deliver after 6pm." value={noteValue} onChange={(e) => setAdminNote(e.target.value)} />
              </Field>
              <div className="mt-3 flex justify-end">
                <Button variant="outline" disabled={busy || noteValue === (o.adminNote ?? "")} onClick={async () => { if (await save({ adminNote: noteValue }, "Note saved")) setAdminNote(null) }}>Save note</Button>
              </div>
            </FramePanel>
          </Frame>
        </div>

        <div className="space-y-4">
          <Frame>
            <FrameHeader><FrameTitle>Customer</FrameTitle></FrameHeader>
            <FramePanel className="space-y-3 text-sm">
              <p className="font-medium">{o.first} {o.last}</p>
              <p className="flex items-start gap-2"><MailIcon className="mt-0.5 size-4 text-muted-foreground" /><a href={`mailto:${o.email}`} className="break-all hover:underline">{o.email}</a></p>
              <p className="flex items-start gap-2"><MapPinIcon className="mt-0.5 size-4 text-muted-foreground" /><span>{o.address}<br />{o.city} {o.pin}</span></p>
              <p className="flex items-start gap-2"><TruckIcon className="mt-0.5 size-4 text-muted-foreground" />{SHIP[o.ship] ?? o.ship}</p>
            </FramePanel>
          </Frame>
          <Frame>
            <FrameHeader><FrameTitle>History</FrameTitle></FrameHeader>
            <FramePanel>
              <Timeline defaultValue={o.history.length}>
                {o.history.map((h, i) => (
                  <TimelineItem key={i} step={i + 1}>
                    <TimelineHeader>
                      <TimelineSeparator />
                      <TimelineDate>{dateTime(h.at)}</TimelineDate>
                      <TimelineTitle>{STATUS[h.status]?.label ?? h.status}</TimelineTitle>
                      <TimelineIndicator />
                    </TimelineHeader>
                    <TimelineContent>
                      by {h.by}{h.note ? <span className="mt-1 block text-foreground">"{h.note}"</span> : null}
                    </TimelineContent>
                  </TimelineItem>
                ))}
              </Timeline>
            </FramePanel>
          </Frame>
        </div>
      </div>

      <AlertDialog open={!!pending} onOpenChange={(open) => { if (!open) setPending(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{pending ? STATUS[pending].action : ""}?</AlertDialogTitle>
            <AlertDialogDescription>
              {pending === "CANCELLED" ? "A cancelled order cannot be reopened." : `The order moves from ${STATUS[o.status].label.toLowerCase()} to ${pending ? STATUS[pending].label.toLowerCase() : ""}.`} The change is recorded in the history with your name.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <Field>
            <FieldLabel htmlFor="status-note">Note (optional)</FieldLabel>
            <Textarea id="status-note" rows={2} value={note} onChange={(e) => setNote(e.target.value)} placeholder={pending === "SHIPPED" ? "Courier and tracking number" : "Why, for the record"} />
          </Field>
          <AlertDialogFooter>
            <AlertDialogCancel>Back</AlertDialogCancel>
            <AlertDialogAction
              variant={pending === "CANCELLED" ? "destructive" : "default"}
              disabled={busy}
              onClick={async () => { if (pending && (await save({ status: pending, note }, `Order ${STATUS[pending].label.toLowerCase()}`))) setPending(null) }}
            >
              {pending ? STATUS[pending].action : ""}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return <div className={`flex justify-between ${strong ? "border-t pt-2 font-semibold" : ""}`}><dt className={strong ? "" : "text-muted-foreground"}>{label}</dt><dd className="tabular-nums">{value}</dd></div>
}

function BackLink() {
  return <Link to="/orders" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"><ArrowLeftIcon className="size-4" />Orders</Link>
}
