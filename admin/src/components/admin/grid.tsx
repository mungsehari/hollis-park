import { useMemo, useState, type ReactNode } from "react"
import { useTable, type ColumnDef, type PaginationState } from "@tanstack/react-table"
import { DataGrid, DataGridContainer, dataGridFeatures, type DataGridFeatures } from "@/components/reui/data-grid/data-grid"
import { DataGridPagination } from "@/components/reui/data-grid/data-grid-pagination"
import { DataGridTable } from "@/components/reui/data-grid/data-grid-table"

export type GridColumn<T extends object> = ColumnDef<DataGridFeatures, T, any> // eslint-disable-line @typescript-eslint/no-explicit-any

/**
 * ReUI DataGrid with sorting and pagination. Pass `server` to page on the API
 * (orders, messages, subscribers); leave it out to page in the browser.
 */
export function AdminGrid<T extends object>({
  data, columns, getRowId, isLoading, emptyMessage, onRowClick, server, pageSize = 20,
}: {
  data: T[]
  columns: GridColumn<T>[]
  getRowId: (row: T) => string
  isLoading?: boolean
  emptyMessage?: ReactNode
  onRowClick?: (row: T) => void
  pageSize?: number
  server?: { total: number; pagination: PaginationState; onPaginationChange: (p: PaginationState) => void }
}) {
  const [localPagination, setLocalPagination] = useState<PaginationState>({ pageIndex: 0, pageSize })
  const pagination = server ? server.pagination : localPagination
  const memoColumns = useMemo(() => columns, [columns])

  const table = useTable<DataGridFeatures, T>({
    features: dataGridFeatures,
    columns: memoColumns,
    data,
    getRowId,
    state: { pagination },
    onPaginationChange: (updater) => {
      const next = typeof updater === "function" ? updater(pagination) : updater
      if (server) server.onPaginationChange(next)
      else setLocalPagination(next)
    },
    manualPagination: !!server,
    rowCount: server ? server.total : undefined,
  })

  return (
    <DataGrid<DataGridFeatures, T>
      table={table}
      recordCount={server ? server.total : data.length}
      isLoading={isLoading}
      emptyMessage={emptyMessage}
      onRowClick={onRowClick}
      tableLayout={{ headerBackground: true, headerSticky: true, rowBorder: true, width: "auto" }}
    >
      <div className="w-full space-y-2.5">
        <DataGridContainer className="rounded-xl border bg-card">
          <div className="overflow-x-auto">
            <DataGridTable />
          </div>
        </DataGridContainer>
        <DataGridPagination sizes={[10, 20, 50, 100]} />
      </div>
    </DataGrid>
  )
}
