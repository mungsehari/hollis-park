import { Link } from "react-router"
import { AffiliateNote, PickGrid } from "@/components/store/marketplace"
import { PageShell } from "@/components/store/page"
import { Button } from "@/components/ui/button"
import { useShop } from "@/state/store"

/** All care picks: the extras we recommend with the shoes, bought on Amazon or Flipkart. */
export function Care() {
  const { data } = useShop()
  const picks = data.picks ?? []
  return (
    <PageShell
      wide
      eyebrow="Picks"
      title="Care for your pair"
      lead="Socks, cleaners and insoles we use with our own pairs. They open on Amazon or Flipkart in a new tab."
      trail={[["Home", "/"], ["Care picks", null]]}
    >
      {picks.length === 0 ? (
        <div className="rounded-3xl bg-accent p-10 text-center">
          <p className="text-lg">No picks right now. Check back soon.</p>
          <Button className="mt-6 rounded-full" nativeButton={false} render={<Link to="/catalog" />}>Shop sneakers</Button>
        </div>
      ) : (
        <>
          <PickGrid picks={picks} />
          <AffiliateNote className="mt-8" />
        </>
      )}
    </PageShell>
  )
}
