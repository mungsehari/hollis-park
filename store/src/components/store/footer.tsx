import { motion } from "motion/react"
import { Link } from "react-router"
import { toast } from "sonner"
import { useShop } from "@/state/store"
import { BrandMark } from "./brand"
import { NewsletterForm } from "./newsletter-form"

export function Footer() {
  const { data } = useShop()
  const { name, store } = data.site
  const cols: [string, [string, string][]][] = [
    ["Shop", [["All sneakers", "/catalog"], ...data.collections.map((c) => [c.name, `/collections/${c.id}`] as [string, string]), ["Saved pairs", "/saved"], ...((data.picks ?? []).length > 0 ? [["Care picks", "/care"] as [string, string]] : [])]],
    ["Help", [["Size guide", "/size-guide"], ["Delivery and returns", "/delivery"], ["Questions", "/faq"], ["Contact", "/contact"]]],
    ["Company", [["Our story", "/about"], ["Our lines", "/about#lines"], ["Monthly note", "/newsletter"], [`Visit ${store}`, "/contact?topic=fitting"]]],
  ]
  return (
    <footer className="mt-24 overflow-hidden bg-sage-ink text-white">
      <div className="mx-auto max-w-[90rem] px-4 pt-16 sm:px-6 lg:px-10">
        <div className="grid gap-12 lg:grid-cols-[1.4fr_2fr]">
          <div className="max-w-md">
            <Link to="/" className="flex items-center gap-2.5"><BrandMark /><span className="font-heading text-lg font-extrabold uppercase">{name}</span></Link>
            <p className="mt-5 text-white/70">New pairs, restocks and one useful note a month. Subscribe and take 10% off your first pair.</p>
            <div className="mt-5"><NewsletterForm tone="dark" /></div>
          </div>
          <div className="grid grid-cols-2 gap-8 sm:grid-cols-3">
            {cols.map(([title, links]) => (
              <nav key={title} aria-label={title}>
                <h2 className="font-mono text-[11px] tracking-[0.2em] text-white/50 uppercase">{title}</h2>
                <ul className="mt-4 space-y-2.5">
                  {links.map(([label, to]) => (
                    <li key={label}><Link to={to} className="text-white/85 transition-colors hover:text-white">{label}</Link></li>
                  ))}
                </ul>
              </nav>
            ))}
          </div>
        </div>
        <motion.p
          aria-hidden="true"
          initial={{ y: "40%", opacity: 0 }}
          whileInView={{ y: 0, opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 1.1, ease: [0.22, 1, 0.36, 1] }}
          className="mt-16 font-heading text-[19vw] leading-[0.8] font-extrabold tracking-tighter whitespace-nowrap text-sage uppercase lg:text-[15.5rem]"
        >
          {name}
        </motion.p>
        <div className="flex flex-col gap-3 border-t border-white/10 py-6 text-sm text-white/60 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <p>&copy; 2026 {name}. All rights reserved.</p>
            <p className="text-xs text-white/45">Some links go to Amazon or Flipkart; we may earn a small commission at no extra cost to you.</p>
          </div>
          <nav className="flex gap-5" aria-label="Legal">
            <Link to="/legal" className="hover:text-white">Privacy</Link>
            <Link to="/legal?tab=terms" className="hover:text-white">Terms of sale</Link>
            <button type="button" className="hover:text-white" onClick={() => toast("Add your Instagram link in footer.tsx")}>Instagram</button>
          </nav>
        </div>
      </div>
    </footer>
  )
}
