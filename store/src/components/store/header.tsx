import { AnimatePresence, motion } from "motion/react"
import { useEffect, useState } from "react"
import { Link, NavLink, useLocation } from "react-router"
import { ArrowRightIcon, HeartIcon, MenuIcon, SearchIcon, ShoppingBagIcon } from "lucide-react"
import { cn } from "cn"
import { Button } from "@/components/ui/button"
import {
  NavigationMenu, NavigationMenuContent, NavigationMenuItem, NavigationMenuLink, NavigationMenuList, NavigationMenuTrigger,
} from "@/components/ui/navigation-menu"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { useShop } from "@/state/store"
import { BrandMark } from "./brand"

function useScrolled(threshold = 24) {
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    const on = () => setScrolled(window.scrollY > threshold)
    on()
    window.addEventListener("scroll", on, { passive: true })
    return () => window.removeEventListener("scroll", on)
  }, [threshold])
  return scrolled
}

export function AnnouncementBar() {
  const { data } = useShop()
  const b = data.home.banner
  if (!b.enabled || !b.text) return null
  const items = Array.from({ length: 8 }, (_, i) => (
    <span key={i} className="flex shrink-0 items-center gap-6 px-6">
      {b.text}
      <span className="size-1 rounded-full bg-sage" aria-hidden="true" />
    </span>
  ))
  const body = (
    <div className="flex w-max marquee [--marquee-duration:45s] hover:[animation-play-state:paused]">
      {items}{items}
    </div>
  )
  return (
    <div className="relative z-50 overflow-hidden bg-sage-ink py-2 text-xs font-medium text-white/90">
      <span className="sr-only">{b.text}</span>
      <div aria-hidden="true">{b.link ? <Link to={b.link} tabIndex={-1}>{body}</Link> : body}</div>
    </div>
  )
}

const NAV = [
  { to: "/collections", label: "Collections" },
  { to: "/size-guide", label: "Size guide" },
  { to: "/about", label: "Our story" },
]

export function Header() {
  const { data, cart, saved, bagBump, setBagOpen, setSearchOpen } = useShop()
  const { pathname } = useLocation()
  const scrolled = useScrolled()
  const [menuOpen, setMenuOpen] = useState(false)
  const onHero = pathname === "/" && !scrolled
  const items = cart.reduce((n, l) => n + l.qty, 0)

  /* Close the mobile menu when the route changes (adjusting state while rendering, not in an effect). */
  const [menuPath, setMenuPath] = useState(pathname)
  if (menuPath !== pathname) { setMenuPath(pathname); setMenuOpen(false) }

  const iconBtn = cn("relative rounded-full", onHero ? "text-white hover:bg-white/15 hover:text-white" : "")

  return (
    <header
      className={cn(
        "sticky top-0 z-40 transition-[background-color,box-shadow,color] duration-500",
        pathname === "/" && "-mb-[var(--header-h)]",
        onHero ? "bg-transparent text-white" : "bg-background/85 text-foreground shadow-[0_1px_0_var(--border)] backdrop-blur-xl",
      )}
      style={{ ["--header-h" as string]: "4.5rem" }}
    >
      <div className="mx-auto flex h-[4.5rem] max-w-[90rem] items-center gap-3 px-4 sm:px-6 lg:px-10">
        <Button variant="ghost" size="icon-lg" className={cn(iconBtn, "lg:hidden")} aria-label="Open menu" onClick={() => setMenuOpen(true)}>
          <MenuIcon />
        </Button>
        <Link to="/" className="flex items-center gap-2.5" aria-label={`${data.site.name} home`}>
          <BrandMark />
          <span className="font-heading text-lg font-extrabold tracking-tight uppercase">{data.site.name}</span>
        </Link>

        <NavigationMenu className="mx-auto hidden lg:flex">
          <NavigationMenuList>
            <NavigationMenuItem>
              <NavigationMenuTrigger className={cn("bg-transparent", onHero && "text-white hover:bg-white/15 hover:text-white data-popup-open:bg-white/15")}>Shop</NavigationMenuTrigger>
              <NavigationMenuContent>
                <div className="grid w-[46rem] grid-cols-[14rem_1fr] gap-4 p-3">
                  <div className="flex flex-col justify-between rounded-xl bg-muted p-5">
                    <div>
                      <p className="font-heading text-xl font-extrabold uppercase">All {data.products.length} pairs</p>
                      <p className="mt-2 text-sm text-muted-foreground">Running, court, training and everyday. Every model has a 60-day wear test.</p>
                    </div>
                    <NavigationMenuLink render={<Link to="/catalog" />} className="mt-6 inline-flex flex-row items-center gap-2 font-medium">
                      Shop all <ArrowRightIcon className="size-4" />
                    </NavigationMenuLink>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    {data.collections.slice(0, 4).map((c) => (
                      <NavigationMenuLink key={c.id} render={<Link to={`/collections/${c.id}`} />} className="group relative block overflow-hidden rounded-xl p-0">
                        {c.image && <img src={c.image} alt="" className="aspect-[16/10] w-full object-cover transition-transform duration-700 group-hover:scale-105" />}
                        <span className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                        <span className="absolute bottom-3 left-3 font-heading text-sm font-extrabold text-white uppercase">{c.name}</span>
                      </NavigationMenuLink>
                    ))}
                  </div>
                </div>
              </NavigationMenuContent>
            </NavigationMenuItem>
            {NAV.map((n) => (
              <NavigationMenuItem key={n.to}>
                <NavigationMenuLink
                  render={<NavLink to={n.to} />}
                  className={cn("rounded-lg px-3 py-2 text-sm font-medium", onHero ? "text-white hover:bg-white/15 hover:text-white" : "hover:bg-muted")}
                >
                  {n.label}
                </NavigationMenuLink>
              </NavigationMenuItem>
            ))}
          </NavigationMenuList>
        </NavigationMenu>

        <div className="ml-auto flex items-center gap-1 lg:ml-0">
          <button
            type="button"
            onClick={() => setSearchOpen(true)}
            className={cn(
              "hidden h-10 w-56 items-center gap-2 rounded-full border px-3.5 text-sm transition-colors md:flex",
              onHero ? "border-white/30 text-white/80 hover:bg-white/10" : "border-border bg-card text-muted-foreground hover:border-ring/40",
            )}
          >
            <SearchIcon className="size-4" />
            <span className="flex-1 text-left">Search</span>
            <kbd className={cn("rounded border px-1.5 font-mono text-[10px]", onHero ? "border-white/30" : "border-border")}>/</kbd>
          </button>
          <Button variant="ghost" size="icon-lg" className={cn(iconBtn, "md:hidden")} aria-label="Search" onClick={() => setSearchOpen(true)}>
            <SearchIcon />
          </Button>
          <Button variant="ghost" size="icon-lg" className={cn(iconBtn, "hidden sm:inline-flex")} aria-label={`Saved pairs, ${saved.length}`} nativeButton={false} render={<Link to="/saved" />}>
            <HeartIcon />
            {saved.length > 0 && <span className="absolute top-1 right-1 size-2 rounded-full bg-sage" />}
          </Button>
          <Button variant="ghost" size="icon-lg" className={iconBtn} aria-label={`Open bag, ${items} ${items === 1 ? "item" : "items"}`} onClick={() => setBagOpen(true)}>
            <ShoppingBagIcon />
            <AnimatePresence>
              {items > 0 && (
                <motion.span
                  key={bagBump}
                  initial={{ scale: 0.4, opacity: 0 }}
                  animate={{ scale: [1.35, 1], opacity: 1 }}
                  transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
                  className="absolute -top-0.5 -right-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-sage px-1 text-[11px] font-semibold text-white tabular-nums"
                >
                  {items}
                </motion.span>
              )}
            </AnimatePresence>
          </Button>
        </div>
      </div>

      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent side="left" className="w-full max-w-sm">
          <SheetHeader><SheetTitle className="font-heading text-lg font-extrabold uppercase">Menu</SheetTitle></SheetHeader>
          <nav className="flex flex-col gap-1 px-4" aria-label="Mobile">
            {[{ to: "/catalog", label: "Shop all" }, ...NAV, { to: "/saved", label: "Saved pairs" }].map((n, i) => (
              <motion.div key={n.to} initial={{ opacity: 0, x: -16 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.05 * i, duration: 0.4 }}>
                <Link to={n.to} className="flex items-center justify-between border-b py-4 font-heading text-2xl font-extrabold uppercase">
                  {n.label}<ArrowRightIcon className="size-5" />
                </Link>
              </motion.div>
            ))}
            <div className="mt-6 grid grid-cols-2 gap-3">
              {data.collections.map((c) => (
                <Link key={c.id} to={`/collections/${c.id}`} className="relative overflow-hidden rounded-xl">
                  {c.image && <img src={c.image} alt="" className="aspect-[4/3] w-full object-cover" />}
                  <span className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                  <span className="absolute bottom-2 left-2.5 text-sm font-semibold text-white">{c.name}</span>
                </Link>
              ))}
            </div>
          </nav>
        </SheetContent>
      </Sheet>
    </header>
  )
}
