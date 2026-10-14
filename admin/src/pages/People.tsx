import { useMemo, useState, type FormEvent } from "react"
import { CheckIcon, DownloadIcon, InboxIcon, MailIcon, RotateCcwIcon, UserPlusIcon, UsersIcon } from "lucide-react"
import { toast } from "sonner"
import { AdminGrid, type GridColumn } from "@/components/admin/grid"
import { Empty, ErrorNote, PageHeader } from "@/components/admin/shared"
import { DataGridColumnHeader } from "@/components/reui/data-grid/data-grid-column-header"
import { Badge } from "@/components/reui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { api, ApiError, qs } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { dateTime } from "@/lib/format"
import type { Message, Page, Staff as StaffT, Subscriber } from "@/lib/types"
import { useApi } from "@/lib/use-api"

const TOPIC: Record<string, string> = { sizing: "Sizing", order: "Order", fitting: "Fitting", resole: "Resole", other: "Other" }

/* ---------------- Messages ---------------- */

export function Messages() {
  const [status, setStatus] = useState("NEW")
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 20 })
  const [open, setOpen] = useState<Message | null>(null)
  const { data, error, loading, reload } = useApi<Page<Message>>(
    `/api/v1/admin/messages${qs({ status: status === "ALL" ? "" : status, page: pagination.pageIndex, size: pagination.pageSize })}`)

  const mark = async (m: Message, next: "NEW" | "HANDLED") => {
    try {
      await api.patch(`/api/v1/admin/messages/${m.id}`, { status: next })
      toast.success(next === "HANDLED" ? "Marked as handled" : "Moved back to new")
      setOpen(null)
      reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not update.")
    }
  }

  const columns = useMemo<GridColumn<Message>[]>(() => [
    { id: "createdAt", accessorKey: "createdAt", header: ({ column }) => <DataGridColumnHeader column={column} title="Received" />, size: 170,
      cell: ({ row }) => <span className="text-muted-foreground">{dateTime(row.original.createdAt)}</span> },
    { id: "name", accessorKey: "name", header: ({ column }) => <DataGridColumnHeader column={column} title="From" />, size: 220,
      cell: ({ row }) => <div><div className="font-medium">{row.original.name}</div><div className="text-xs text-muted-foreground">{row.original.email}</div></div> },
    { id: "topic", accessorKey: "topic", header: ({ column }) => <DataGridColumnHeader column={column} title="Topic" />, size: 110,
      cell: ({ row }) => <Badge variant="secondary" radius="full">{TOPIC[row.original.topic] ?? row.original.topic}</Badge> },
    { id: "message", accessorKey: "message", header: "Message", size: 380,
      cell: ({ row }) => <span className="line-clamp-2 text-sm">{row.original.slot ? `Fitting ${row.original.slot}. ` : ""}{row.original.orderNumber ? `${row.original.orderNumber}: ` : ""}{row.original.message}</span> },
    { id: "status", accessorKey: "status", header: ({ column }) => <DataGridColumnHeader column={column} title="Status" />, size: 100,
      cell: ({ row }) => row.original.status === "NEW" ? <Badge variant="warning" radius="full">New</Badge> : <Badge variant="outline" radius="full">Handled</Badge> },
  ], [])

  return (
    <>
      <PageHeader title="Messages" description="Contact form messages and fitting requests. Reply by email, then mark them handled." />
      <Tabs value={status} onValueChange={(v) => { setStatus(String(v)); setPagination((p) => ({ ...p, pageIndex: 0 })) }}>
        <TabsList><TabsTrigger value="NEW">New</TabsTrigger><TabsTrigger value="HANDLED">Handled</TabsTrigger><TabsTrigger value="ALL">All</TabsTrigger></TabsList>
      </Tabs>
      <ErrorNote error={error} />
      <AdminGrid data={data?.content ?? []} columns={columns} getRowId={(m) => String(m.id)} isLoading={loading && !data} onRowClick={setOpen}
        emptyMessage={<Empty icon={<InboxIcon />} title={status === "NEW" ? "Inbox zero" : "No messages"}>{status === "NEW" ? "Every message has been handled." : undefined}</Empty>}
        server={{ total: data?.totalElements ?? 0, pagination, onPaginationChange: setPagination }} />

      <Dialog open={!!open} onOpenChange={(o) => { if (!o) setOpen(null) }}>
        <DialogContent className="sm:max-w-lg">
          {open && <>
            <DialogHeader>
              <DialogTitle>{open.name}</DialogTitle>
              <DialogDescription>{TOPIC[open.topic] ?? open.topic} · {dateTime(open.createdAt)}</DialogDescription>
            </DialogHeader>
            <div className="space-y-3 text-sm">
              {open.orderNumber && <p><span className="text-muted-foreground">Order:</span> <span className="font-mono">{open.orderNumber}</span></p>}
              {open.slot && <p><span className="text-muted-foreground">Fitting slot:</span> {open.slot}</p>}
              <p className="rounded-lg bg-muted p-3 whitespace-pre-wrap">{open.message}</p>
            </div>
            <DialogFooter>
              <Button variant="outline" nativeButton={false} render={<a href={`mailto:${open.email}?subject=${encodeURIComponent("Re: your message to Hollis Park")}`} />}><MailIcon />Reply by email</Button>
              {open.status === "NEW"
                ? <Button onClick={() => mark(open, "HANDLED")}><CheckIcon />Mark handled</Button>
                : <Button variant="outline" onClick={() => mark(open, "NEW")}><RotateCcwIcon />Move back to new</Button>}
            </DialogFooter>
          </>}
        </DialogContent>
      </Dialog>
    </>
  )
}

/* ---------------- Subscribers ---------------- */

export function Subscribers() {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 50 })
  const { data, error, loading } = useApi<Page<Subscriber>>(`/api/v1/admin/subscribers${qs({ page: pagination.pageIndex, size: pagination.pageSize })}`)
  const columns = useMemo<GridColumn<Subscriber>[]>(() => [
    { id: "email", accessorKey: "email", header: ({ column }) => <DataGridColumnHeader column={column} title="Email" />, size: 300 },
    { id: "topics", accessorFn: (s) => s.topics?.join(" "), header: "Interested in", size: 300,
      cell: ({ row }) => <div className="flex flex-wrap gap-1">{(row.original.topics ?? []).map((t) => <Badge key={t} variant="secondary" radius="full">{t}</Badge>)}</div> },
    { id: "createdAt", accessorKey: "createdAt", header: ({ column }) => <DataGridColumnHeader column={column} title="Subscribed" />, size: 180,
      cell: ({ row }) => <span className="text-muted-foreground">{dateTime(row.original.createdAt)}</span> },
  ], [])
  return (
    <>
      <PageHeader title="Subscribers" description={`${data?.totalElements ?? 0} people get the monthly note.`}
        actions={<Button variant="outline" nativeButton={false} render={<a href="/api/v1/admin/subscribers/export" download />}><DownloadIcon />Export CSV</Button>} />
      <ErrorNote error={error} />
      <AdminGrid data={data?.content ?? []} columns={columns} getRowId={(s) => String(s.id)} isLoading={loading && !data}
        emptyMessage={<Empty icon={<MailIcon />} title="No subscribers yet" />}
        server={{ total: data?.totalElements ?? 0, pagination, onPaginationChange: setPagination }} />
    </>
  )
}

/* ---------------- Staff (ADMIN) ---------------- */

export function Staff() {
  const { me } = useAuth()
  const { data, error, loading, reload } = useApi<StaffT[]>("/api/v1/admin/staff")
  const [adding, setAdding] = useState(false)
  const [resetFor, setResetFor] = useState<StaffT | null>(null)

  const update = async (u: StaffT, body: Partial<Pick<StaffT, "role" | "active">>, message: string) => {
    try {
      await api.patch(`/api/v1/admin/staff/${u.id}`, body)
      toast.success(message)
      reload()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not update.")
    }
  }

  const columns = useMemo<GridColumn<StaffT>[]>(() => [
    { id: "name", accessorKey: "name", header: ({ column }) => <DataGridColumnHeader column={column} title="Name" />, size: 240,
      cell: ({ row }) => <div><div className="font-medium">{row.original.name}{row.original.email === me?.email && <span className="ml-1 text-xs text-muted-foreground">(you)</span>}</div><div className="text-xs text-muted-foreground">{row.original.email}</div></div> },
    { id: "role", accessorKey: "role", header: ({ column }) => <DataGridColumnHeader column={column} title="Role" />, size: 150,
      cell: ({ row }) => (
        <Select value={row.original.role} onValueChange={(v) => update(row.original, { role: v as StaffT["role"] }, "Role changed")}
          items={[{ value: "ADMIN", label: "Admin" }, { value: "STAFF", label: "Staff" }]}>
          <SelectTrigger size="sm" className="w-28" aria-label={`Role for ${row.original.name}`}><SelectValue /></SelectTrigger>
          <SelectContent><SelectItem value="ADMIN">Admin</SelectItem><SelectItem value="STAFF">Staff</SelectItem></SelectContent>
        </Select>
      ) },
    { id: "active", accessorKey: "active", header: "Can sign in", size: 120,
      cell: ({ row }) => <Switch checked={row.original.active} aria-label={`${row.original.name} can sign in`}
        onCheckedChange={(on) => update(row.original, { active: on }, on ? "Access restored" : "Access removed")} /> },
    { id: "lastLoginAt", accessorKey: "lastLoginAt", header: ({ column }) => <DataGridColumnHeader column={column} title="Last sign-in" />, size: 180,
      cell: ({ row }) => <span className="text-muted-foreground">{row.original.lastLoginAt ? dateTime(row.original.lastLoginAt) : "Never"}</span> },
    { id: "actions", header: "", size: 150,
      cell: ({ row }) => <Button variant="outline" size="sm" onClick={() => setResetFor(row.original)}>Reset password</Button> },
  ], [me]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <PageHeader title="Staff" description="Admins manage everything. Staff handle orders and messages, and can view products and settings."
        actions={<Button onClick={() => setAdding(true)}><UserPlusIcon />Add staff</Button>} />
      <Card><CardContent className="grid gap-3 text-sm sm:grid-cols-2">
        <p><Badge variant="invert" radius="full">Admin</Badge> <span className="ml-1 text-muted-foreground">Orders, messages, products, promo codes, tax and delivery, staff.</span></p>
        <p><Badge variant="secondary" radius="full">Staff</Badge> <span className="ml-1 text-muted-foreground">Orders, messages and subscribers. Read-only products and settings.</span></p>
      </CardContent></Card>
      <ErrorNote error={error} />
      <AdminGrid data={data ?? []} columns={columns} getRowId={(u) => String(u.id)} isLoading={loading} emptyMessage={<Empty icon={<UsersIcon />} title="No staff" />} />
      <StaffDialog open={adding} onOpenChange={setAdding} onDone={reload} />
      <ResetDialog user={resetFor} onClose={() => setResetFor(null)} />
    </>
  )
}

function StaffDialog({ open, onOpenChange, onDone }: { open: boolean; onOpenChange: (o: boolean) => void; onDone: () => void }) {
  const [role, setRole] = useState("STAFF")
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    setBusy(true)
    setError(null)
    try {
      await api.post("/api/v1/admin/staff", { name: f.get("name"), email: f.get("email"), password: f.get("password"), role })
      toast.success("Staff account created. Share the password with them privately.")
      onOpenChange(false)
      onDone()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not create the account.")
    } finally {
      setBusy(false)
    }
  }
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={submit} className="grid gap-5">
          <DialogHeader><DialogTitle>Add a staff member</DialogTitle><DialogDescription>They sign in at /admin with this email and password.</DialogDescription></DialogHeader>
          <FieldGroup>
            <Field><FieldLabel htmlFor="s-name">Name</FieldLabel><Input id="s-name" name="name" required /></Field>
            <Field><FieldLabel htmlFor="s-email">Email</FieldLabel><Input id="s-email" name="email" type="email" required /></Field>
            <Field>
              <FieldLabel>Role</FieldLabel>
              <Select value={role} onValueChange={(v) => setRole(String(v))} items={[{ value: "STAFF", label: "Staff" }, { value: "ADMIN", label: "Admin" }]}>
                <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                <SelectContent><SelectItem value="STAFF">Staff</SelectItem><SelectItem value="ADMIN">Admin</SelectItem></SelectContent>
              </Select>
            </Field>
            <Field>
              <FieldLabel htmlFor="s-pass">Temporary password</FieldLabel>
              <Input id="s-pass" name="password" type="password" autoComplete="new-password" minLength={10} required />
              <FieldDescription>At least 10 characters. They can change it after signing in.</FieldDescription>
            </Field>
          </FieldGroup>
          <ErrorNote error={error} />
          <DialogFooter><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? "Creating" : "Create account"}</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function ResetDialog({ user, onClose }: { user: StaffT | null; onClose: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!user) return
    const f = new FormData(e.currentTarget)
    try {
      await api.post(`/api/v1/admin/staff/${user.id}/password`, { password: f.get("password") })
      toast.success(`New password set for ${user.name}`)
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not reset.")
    }
  }
  return (
    <Dialog open={!!user} onOpenChange={(o) => { if (!o) { setError(null); onClose() } }}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={submit} className="grid gap-5">
          <DialogHeader><DialogTitle>Reset password</DialogTitle><DialogDescription>Set a new password for {user?.name}. Share it with them privately.</DialogDescription></DialogHeader>
          <Field><FieldLabel htmlFor="r-pass">New password</FieldLabel><Input id="r-pass" name="password" type="password" autoComplete="new-password" minLength={10} required /></Field>
          <ErrorNote error={error} />
          <DialogFooter><Button type="button" variant="outline" onClick={onClose}>Cancel</Button><Button type="submit">Set password</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
