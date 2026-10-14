import { motion } from "motion/react"
import { Link } from "react-router"
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import { ArrowUpRightIcon, InboxIcon, PackageIcon, ShoppingBagIcon, TrendingUpIcon } from "lucide-react"
import { PageHeader, StatusBadge, ErrorNote } from "@/components/admin/shared"
import { Badge } from "@/components/reui/badge"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { dateTime, money, shortDay, STATUS } from "@/lib/format"
import type { Dashboard as D, OrderStatus } from "@/lib/types"
import { useApi } from "@/lib/use-api"
import { CountUp, Stagger, StaggerItem } from "@/components/admin/motion"

const chartConfig = { revenue: { label: "Revenue", color: "var(--chart-1)" } } satisfies ChartConfig

function Kpi({ title, value, format, hint, icon, to }: { title: string; value: number; format?: (n: number) => string; hint: string; icon: React.ReactNode; to?: string }) {
  const body = (
    <Card className="h-full transition-[border-color,box-shadow,transform] duration-300 hover:-translate-y-0.5 hover:border-ring/40 hover:shadow-md">
      <CardHeader>
        <CardDescription>{title}</CardDescription>
        <CardTitle className="font-heading text-2xl font-extrabold tabular-nums"><CountUp value={value} format={format} /></CardTitle>
        <CardAction className="text-muted-foreground [&_svg]:size-4">{icon}</CardAction>
      </CardHeader>
      <CardContent className="-mt-2 text-xs text-muted-foreground">{hint}</CardContent>
    </Card>
  )
  return to ? <Link to={to} className="block">{body}</Link> : body
}

export function Dashboard() {
  const { data: d, error, loading } = useApi<D>("/api/v1/admin/dashboard")
  if (error) return <ErrorNote error={error} />
  if (loading || !d) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-9 w-48" />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-32" />)}</div>
        <Skeleton className="h-80" />
      </div>
    )
  }
  const chartData = d.salesByDay.map((p) => ({ ...p, day: shortDay(p.date), revenue: Number(p.revenue) }))
  return (
    <>
      <PageHeader title="Dashboard" description="The last 30 days. Cancelled orders are left out of revenue." />
      <Stagger className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StaggerItem><Kpi title="Revenue today" value={Number(d.revenueToday)} format={money} hint={`${d.ordersToday} ${d.ordersToday === 1 ? "order" : "orders"} today`} icon={<TrendingUpIcon />} /></StaggerItem>
        <StaggerItem><Kpi title="Last 7 days" value={Number(d.revenue7d)} format={money} hint={`${d.orders7d} orders, ${money(d.revenue30d)} in 30 days`} icon={<ShoppingBagIcon />} /></StaggerItem>
        <StaggerItem><Kpi title="Orders to handle" value={d.openOrders} hint="Received or confirmed, not yet shipped" icon={<PackageIcon />} to="/orders?status=RECEIVED" /></StaggerItem>
        <StaggerItem><Kpi title="New messages" value={d.newMessages} hint={`${d.subscribers} newsletter subscribers`} icon={<InboxIcon />} to="/messages" /></StaggerItem>
      </Stagger>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Revenue, last 14 days</CardTitle>
            <CardDescription>Average order over 30 days: {money(d.averageOrder30d)}</CardDescription>
          </CardHeader>
          <CardContent>
            <ChartContainer config={chartConfig} className="aspect-auto h-64 w-full">
              <BarChart data={chartData} margin={{ left: 4, right: 4 }}>
                <CartesianGrid vertical={false} />
                <XAxis dataKey="day" tickLine={false} axisLine={false} tickMargin={8} minTickGap={16} />
                <YAxis tickLine={false} axisLine={false} width={64} tickFormatter={(v: number) => (v >= 1000 ? `₹${Math.round(v / 1000)}k` : `₹${v}`)} />
                <ChartTooltip content={<ChartTooltipContent formatter={(v) => money(Number(v))} />} />
                <Bar dataKey="revenue" fill="var(--color-revenue)" radius={6} />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Orders by status</CardTitle><CardDescription>All time</CardDescription></CardHeader>
          <CardContent className="space-y-3">
            {(Object.keys(STATUS) as OrderStatus[]).map((s) => (
              <Link key={s} to={`/orders?status=${s}`} className="flex items-center justify-between rounded-lg px-2 py-1.5 hover:bg-muted">
                <StatusBadge status={s} />
                <span className="font-medium tabular-nums">{d.statusCounts[s] ?? 0}</span>
              </Link>
            ))}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Latest orders</CardTitle>
            <CardAction><Link to="/orders" className="text-sm font-medium underline-offset-4 hover:underline">All orders</Link></CardAction>
          </CardHeader>
          <CardContent>
            {d.recentOrders.length ? (
              <Table>
                <TableHeader><TableRow><TableHead>Order</TableHead><TableHead>Customer</TableHead><TableHead>Status</TableHead><TableHead className="text-right">Total</TableHead></TableRow></TableHeader>
                <TableBody>
                  {d.recentOrders.map((o) => (
                    <TableRow key={o.id}>
                      <TableCell><Link to={`/orders/${o.orderNumber}`} className="font-mono font-medium hover:underline">{o.orderNumber}</Link><div className="text-xs text-muted-foreground">{dateTime(o.createdAt)}</div></TableCell>
                      <TableCell>{o.name}<div className="text-xs text-muted-foreground">{o.city}</div></TableCell>
                      <TableCell><StatusBadge status={o.status} /></TableCell>
                      <TableCell className="text-right tabular-nums">{money(o.total)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : <p className="py-8 text-center text-sm text-muted-foreground">No orders yet.</p>}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Top sellers</CardTitle><CardDescription>Pairs sold, last 30 days</CardDescription></CardHeader>
          <CardContent className="space-y-3">
            {d.topProducts.length ? d.topProducts.map((p, i) => (
              <Link key={p.id} to={`/products/${p.id}`} className="flex items-center gap-3 rounded-lg px-2 py-1.5 hover:bg-muted">
                <Badge variant="secondary" radius="full" size="lg">{i + 1}</Badge>
                <span className="flex-1 truncate">{p.name}</span>
                <span className="text-sm text-muted-foreground tabular-nums">{p.qty} sold</span>
              </Link>
            )) : <p className="py-6 text-center text-sm text-muted-foreground">Nothing sold yet.</p>}
            <p className="pt-2 text-xs text-muted-foreground">{d.activeProducts} products live in the store.</p>
          </CardContent>
        </Card>
      </div>

      <MarketplaceCard d={d} />
    </>
  )
}

/** Clicks out to Amazon and Flipkart (product "Also on" buttons and care picks), last 30 days. */
function MarketplaceCard({ d }: { d: D }) {
  const clicks = d.marketplaceClicks30d ?? { AMAZON: 0, FLIPKART: 0 }
  const total = (clicks.AMAZON ?? 0) + (clicks.FLIPKART ?? 0)
  const bars: [string, number, string][] = [["Amazon", clicks.AMAZON ?? 0, "bg-[#FF9900]"], ["Flipkart", clicks.FLIPKART ?? 0, "bg-[#2874F0]"]]
  return (
    <Card>
      <CardHeader>
        <CardTitle>Marketplace clicks</CardTitle>
        <CardDescription>People sent to Amazon and Flipkart, last 30 days</CardDescription>
        <CardAction><Link to="/care-picks" className="text-sm font-medium underline-offset-4 hover:underline">Care picks</Link></CardAction>
      </CardHeader>
      <CardContent className="grid gap-8 lg:grid-cols-2">
        <div className="space-y-4">
          <p className="font-heading text-4xl font-extrabold tabular-nums"><CountUp value={total} /> <span className="text-base font-normal text-muted-foreground">clicks</span></p>
          {bars.map(([label, n, color]) => (
            <div key={label}>
              <div className="mb-1.5 flex justify-between text-sm"><span>{label}</span><span className="tabular-nums text-muted-foreground">{n}</span></div>
              <div className="h-2.5 overflow-hidden rounded-full bg-muted">
                <motion.div className={`h-full rounded-full ${color}`} initial={{ width: 0 }} animate={{ width: total ? `${(n / total) * 100}%` : "0%" }} transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1], delay: 0.2 }} />
              </div>
            </div>
          ))}
        </div>
        <div>
          <p className="mb-2 text-sm font-medium">Most followed</p>
          {d.topOutbound30d && d.topOutbound30d.length ? (
            <ul className="space-y-1.5">
              {d.topOutbound30d.map((t, i) => (
                <li key={t.name + t.marketplace} className="flex items-center gap-3 rounded-lg px-2 py-1.5 hover:bg-muted">
                  <Badge variant="secondary" radius="full" size="lg">{i + 1}</Badge>
                  <span className="flex-1 truncate">{t.name}</span>
                  <span className="flex items-center gap-1 text-sm text-muted-foreground tabular-nums">{t.clicks}<ArrowUpRightIcon className="size-3.5" />{t.marketplace}</span>
                </li>
              ))}
            </ul>
          ) : <p className="py-6 text-center text-sm text-muted-foreground">No clicks yet. Add Amazon / Flipkart links to products or create care picks.</p>}
        </div>
      </CardContent>
    </Card>
  )
}
