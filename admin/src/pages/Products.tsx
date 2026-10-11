import { useMemo, useState } from "react"
import { Link, useNavigate } from "react-router"
import { PackageIcon, PlusIcon } from "lucide-react"
import { AdminGrid, type GridColumn } from "@/components/admin/grid"
import { Empty, ErrorNote, PageHeader } from "@/components/admin/shared"
import { DataGridColumnHeader } from "@/components/reui/data-grid/data-grid-column-header"
import { Badge } from "@/components/reui/badge"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useAuth } from "@/lib/auth"
import { imageUrl, money } from "@/lib/format"
import type { ProductAdmin, StoreInfo } from "@/lib/types"
import { useApi } from "@/lib/use-api"

export function Products() {
  const { data, error, loading } = useApi<ProductAdmin[]>("/api/admin/products")
  const { data: store } = useApi<StoreInfo>("/api/store")
  const { can } = useAuth()
  const navigate = useNavigate()
  const [view, setView] = useState<"live" | "archived" | "all">("live")
  const coll = useMemo(() => Object.fromEntries((store?.collections ?? []).map((c) => [c.id, c.name])), [store])

  const rows = (data ?? []).filter((p) => (view === "all" ? true : view === "live" ? p.active : !p.active))

  const columns = useMemo<GridColumn<ProductAdmin>[]>(() => [
    { id: "name", accessorFn: (r) => r.product.name, header: ({ column }) => <DataGridColumnHeader column={column} title="Product" />, size: 280,
      cell: ({ row }) => {
        const p = row.original.product
        return (
          <div className="flex items-center gap-3">
            <img src={imageUrl(p.gallery?.[0] ?? p.img)} alt="" className="size-11 rounded-lg bg-muted object-cover" />
            <div className="min-w-0"><div className="truncate font-medium">{p.name}</div><div className="truncate text-xs text-muted-foreground">{p.colourway}</div></div>
          </div>
        )
      } },
    { id: "collection", accessorFn: (r) => r.product.collection, header: ({ column }) => <DataGridColumnHeader column={column} title="Collection" />, size: 130,
      cell: ({ row }) => coll[row.original.product.collection] ?? row.original.product.collection },
    { id: "price", accessorFn: (r) => r.product.price, header: ({ column }) => <DataGridColumnHeader column={column} title="Price" />, size: 110,
      cell: ({ row }) => <span className="tabular-nums">{money(row.original.product.price)}</span> },
    { id: "stock", accessorFn: (r) => r.product.soldOut.length, header: ({ column }) => <DataGridColumnHeader column={column} title="Stock" />, size: 190,
      cell: ({ row }) => {
        const p = row.original.product
        return p.soldOut.length || p.low.length
          ? <span className="text-sm">{p.soldOut.length ? `${p.soldOut.length} sold out` : ""}{p.soldOut.length && p.low.length ? ", " : ""}{p.low.length ? `${p.low.length} low` : ""}</span>
          : <span className="text-sm text-muted-foreground">All sizes in stock</span>
      } },
    { id: "rank", accessorFn: (r) => r.product.rank, header: ({ column }) => <DataGridColumnHeader column={column} title="Order" />, size: 80 },
    { id: "status", accessorFn: (r) => (r.active ? 1 : 0), header: ({ column }) => <DataGridColumnHeader column={column} title="Status" />, size: 110,
      cell: ({ row }) => row.original.active ? <Badge variant="success" radius="full">Live</Badge> : <Badge variant="outline" radius="full">Archived</Badge> },
  ], [coll])

  return (
    <>
      <PageHeader
        title="Products"
        description="What the store sells. Archive a product to hide it without losing its order history."
        actions={can("ADMIN") && <Button nativeButton={false} render={<Link to="/products/new" />}><PlusIcon />New product</Button>}
      />
      <Tabs value={view} onValueChange={(v) => setView(v as typeof view)}>
        <TabsList>
          <TabsTrigger value="live">Live {data ? `(${data.filter((p) => p.active).length})` : ""}</TabsTrigger>
          <TabsTrigger value="archived">Archived {data ? `(${data.filter((p) => !p.active).length})` : ""}</TabsTrigger>
          <TabsTrigger value="all">All</TabsTrigger>
        </TabsList>
      </Tabs>
      <ErrorNote error={error} />
      <AdminGrid
        data={rows}
        columns={columns}
        getRowId={(r) => r.product.id}
        isLoading={loading}
        onRowClick={(r) => navigate(`/products/${r.product.id}`)}
        emptyMessage={<Empty icon={<PackageIcon />} title={view === "archived" ? "Nothing archived" : "No products"} />}
      />
    </>
  )
}
