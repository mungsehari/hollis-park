import { lazy, Suspense } from "react"
import { Route, Routes, useLocation } from "react-router"
import { Layout } from "@/components/store/layout"
import { PageSkeleton } from "@/components/store/skeletons"
import { Home } from "@/pages/Home"

/* Everything except the home page loads on demand, so the first paint stays light. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const named = <T extends object>(load: () => Promise<T>, key: keyof T) =>
  lazy(() => load().then((m) => ({ default: m[key] as unknown as React.ComponentType })))
const Catalog = named(() => import("@/pages/Catalog"), "Catalog")
const Product = named(() => import("@/pages/Product"), "Product")
const Collections = named(() => import("@/pages/Collections"), "Collections")
const Collection = named(() => import("@/pages/Collections"), "Collection")
const Saved = named(() => import("@/pages/Collections"), "Saved")
const Cart = named(() => import("@/pages/Cart"), "Cart")
const Checkout = named(() => import("@/pages/Checkout"), "Checkout")
const SizeGuide = named(() => import("@/pages/SizeGuide"), "SizeGuide")
const Delivery = named(() => import("@/pages/Info"), "Delivery")
const Faq = named(() => import("@/pages/Info"), "Faq")
const Contact = named(() => import("@/pages/Info"), "Contact")
const About = named(() => import("@/pages/Info"), "About")
const Newsletter = named(() => import("@/pages/Info"), "Newsletter")
const Legal = named(() => import("@/pages/Info"), "Legal")
const Care = named(() => import("@/pages/Care"), "Care")
const NotFound = named(() => import("@/pages/NotFound"), "NotFound")

export function App() {
  const location = useLocation()
  return (
    <Layout>
      <Suspense fallback={<PageSkeleton />}>
      <Routes location={location}>
        <Route path="/" element={<Home />} />
        <Route path="/catalog" element={<Catalog />} />
        <Route path="/catalog/:id" element={<Product />} />
        <Route path="/collections" element={<Collections />} />
        <Route path="/collections/:id" element={<Collection />} />
        <Route path="/saved" element={<Saved />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/checkout" element={<Checkout />} />
        <Route path="/size-guide" element={<SizeGuide />} />
        <Route path="/delivery" element={<Delivery />} />
        <Route path="/faq" element={<Faq />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="/about" element={<About />} />
        <Route path="/newsletter" element={<Newsletter />} />
        <Route path="/legal" element={<Legal />} />
        <Route path="/care" element={<Care />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
      </Suspense>
    </Layout>
  )
}

export default App
