import { motion } from "motion/react"
import { useEffect, type ReactNode } from "react"
import { useLocation } from "react-router"
import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"
import { BagSheet } from "./bag-sheet"
import { Footer } from "./footer"
import { AnnouncementBar, Header } from "./header"
import { SearchDialog } from "./search-dialog"

export function Layout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [pathname])

  return (
    <TooltipProvider delay={150}>
      <a href="#main" className="sr-only z-[100] rounded-full bg-foreground px-4 py-2 text-background focus:not-sr-only focus:fixed focus:top-3 focus:left-3">Skip to content</a>
      <AnnouncementBar />
      <Header />
      {/* Enter animation only: the new page is never held back by an exit animation. */}
      <motion.main
        id="main"
        key={pathname}
        tabIndex={-1}
        className="outline-none"
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
      >
        {children}
      </motion.main>
      <Footer />
      <BagSheet />
      <SearchDialog />
      <Toaster position="bottom-center" />
    </TooltipProvider>
  )
}
