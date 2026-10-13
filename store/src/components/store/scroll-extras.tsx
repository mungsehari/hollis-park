import { AnimatePresence, motion, useScroll, useSpring } from "motion/react"
import { useEffect, useState } from "react"
import { ArrowUpIcon } from "lucide-react"

/** Thin bar along the top that fills as the page is read. */
export function ScrollProgress() {
  const { scrollYProgress } = useScroll()
  const scaleX = useSpring(scrollYProgress, { stiffness: 140, damping: 30, restDelta: 0.001 })
  return <motion.div aria-hidden="true" style={{ scaleX }} className="fixed inset-x-0 top-0 z-[60] h-[3px] origin-left bg-sage" />
}

/** Round button that rises in after the visitor has scrolled a good way down. */
export function BackToTop() {
  const [show, setShow] = useState(false)
  useEffect(() => {
    const on = () => setShow(window.scrollY > 900)
    on()
    window.addEventListener("scroll", on, { passive: true })
    return () => window.removeEventListener("scroll", on)
  }, [])
  return (
    <AnimatePresence>
      {show && (
        <motion.button
          type="button"
          aria-label="Back to top"
          onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
          initial={{ opacity: 0, y: 24, scale: 0.8 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 24, scale: 0.8 }}
          whileHover={{ y: -3 }}
          whileTap={{ scale: 0.92 }}
          transition={{ type: "spring", stiffness: 380, damping: 26 }}
          className="fixed right-4 bottom-24 z-40 flex size-12 items-center justify-center rounded-full bg-foreground text-background shadow-xl sm:right-6 lg:bottom-8"
        >
          <ArrowUpIcon className="size-5" />
        </motion.button>
      )}
    </AnimatePresence>
  )
}
