import { useEffect, useMemo, useState } from "react"
import { useNavigate, useSearchParams } from "react-router"
import { SearchIcon, ShoppingBagIcon } from "lucide-react"
import { AdminGrid, type GridColumn } from "@/components/admin/grid"
import { Empty, ErrorNote, PageHeader, StatusBadge } from "@/components/admin/shared"
import { DataGridColumnHeader } from "@/components/reui/data-grid/data-grid-column-header"
import { Badge } from "@/components/reui/badge"
import { Input } from "@/components/ui/input"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { qs } from "@/lib/api"
import { dateTime, money, SHIP, STATUS } from "@/lib/format"
import type { OrderStatus, OrderSummary, Page } from "@/lib/types"
import { useApi } from "@/lib/use-api"

export function Orders() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const status = params.get("status") ?? ""
  const [q, setQ] = useState(params.get("q") ?? "")
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 20 })

  useEffect(() => {
    const t = setTimeout(() => {
      setParams((p) => { if (q) p.set("q", q); else p.delete("q"); return p }, { replace: true })
      setPagination((p) => ({ ...p, pageIndex: 0 }))
    }, 250)
    return () => clearTimeout(t)
  }, [q, setParams])

  const path = `/api/v1/admin/orders${qs({ status, q: params.get("q"), page: pagination.pageIndex, size: pagination.pageSize })}`
  const { data, error, loading } = useApi<Page<OrderSummary>>(path)

  const columns = useMemo<GridColumn<OrderSummary>[]>(() => [
    { id: "orderNumber", accessorKey: "orderNumber", header: ({ column }) => <DataGridColumnHeader column={column} title="Order" />,
      cell: ({ row }) => <span className="font-mono font-medium whitespace-nowrap">{row.original.orderNumber}</span>, size: 120 },
    { id: "createdAt", accessorKey: "createdAt", header: ({ column }) => <DataGridColumnHeader column={column} title="Placed" />,
      cell: ({ row }) => <span className="text-muted-foreground">{dateTime(row.original.createdAt)}</span>, size: 180 },
    { id: "name", accessorKey: "name", header: ({ column }) => <DataGridColumnHeader column={column} title="Customer" />,
      cell: ({ row }) => <div><div className="font-medium">{row.original.name}</div><div className="text-xs text-muted-foreground">{row.original.email}</div></div>, size: 240 },
    { id: "city", accessorKey: "city", header: ({ column }) => <DataGridColumnHeader column={column} title="City" />, size: 120 },
    { id: "ship", accessorKey: "ship", header: ({ column }) => <DataGridColumnHeader column={column} title="Delivery" />,
      cell: ({ row }) => SHIP[row.original.ship] ?? row.original.ship, size: 120 },
    { id: "items", accessorKey: "items", header: ({ column }) => <DataGridColumnHeader column={column} title="Pairs" />, size: 70 },
    { id: "status", accessorKey: "status", header: ({ column }) => <DataGridColumnHeader column={column} title="Status" />,
      cell: ({ row }) => <StatusBadge status={row.original.status} />, size: 150 },
    { id: "payment", accessorKey: "paymentStatus", header: ({ column }) => <DataGridColumnHeader column={column} title="Payment" />,
      cell: ({ row }) => row.original.paymentStatus === "PAID" ? <Badge variant="success" radius="full">Paid</Badge>
        : row.original.paymentStatus === "PENDING" ? <Badge variant="outline" radius="full">Pending</Badge> : <span className="text-muted-foreground">Not online</span>, size: 120 },
    { id: "total", accessorKey: "total", header: ({ column }) => <DataGridColumnHeader column={column} title="Total" />,
      cell: ({ row }) => <span className="tabular-nums">{money(row.original.total)}</span>, size: 110 },
  ], [])

  return (
    <>
      <PageHeader title="Orders" description="Orders are recorded without payment. Move them forward as you confirm, ship and deliver." />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="-mx-1 max-w-full overflow-x-auto px-1">
        <Tabs value={status || "ALL"} onValueChange={(v) => { setParams((p) => { if (v === "ALL") p.delete("status"); else p.set("status", String(v)); return p }); setPagination((p) => ({ ...p, pageIndex: 0 })) }}>
          <TabsList>
            <TabsTrigger value="ALL">All</TabsTrigger>
            {(Object.keys(STATUS) as OrderStatus[]).map((s) => <TabsTrigger key={s} value={s}>{STATUS[s].label}</TabsTrigger>)}
          </TabsList>
        </Tabs>
        </div>
        <div className="relative w-full sm:w-72">
          <SearchIcon className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input className="pl-8" placeholder="Order number, name, email or city" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search orders" />
        </div>
      </div>
      <ErrorNote error={error} />
      <AdminGrid
        data={data?.content ?? []}
        columns={columns}
        getRowId={(o) => o.orderNumber}
        isLoading={loading && !data}
        onRowClick={(o) => navigate(`/orders/${o.orderNumber}`)}
        emptyMessage={<Empty icon={<ShoppingBagIcon />} title="No orders here">{q || status ? "Try another search or status." : "Orders placed in the store appear here."}</Empty>}
        server={{ total: data?.totalElements ?? 0, pagination, onPaginationChange: setPagination }}
      />
    </>
  )
}
