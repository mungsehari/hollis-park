import { Link, useLocation } from "react-router"
import { Button } from "@/components/ui/button"
import { POPULAR } from "@/lib/catalog"
import { PageShell } from "@/components/store/page"
import { ProductCard } from "@/components/store/product-card"
import { useShop } from "@/state/store"

export function NotFound() {
  const { data } = useShop()
  const { pathname } = useLocation()
  return (
    <PageShell title="This pair wandered off" lead={<>The page <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-sm">{pathname}</code> does not exist or has moved.</>}>
      <div className="flex gap-3"><Button className="rounded-full" nativeButton={false} render={<Link to="/" />}>Back to home</Button><Button variant="outline" className="rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Shop all</Button></div>
      <h2 className="mt-16 mb-6 font-heading text-3xl font-extrabold uppercase">Most worn right now</h2>
      <div className="grid grid-cols-2 gap-x-4 gap-y-10 lg:grid-cols-4 lg:gap-x-6">{POPULAR.bestsellers(data.products).map((p) => <ProductCard key={p.id} p={p} />)}</div>
    </PageShell>
  )
}
