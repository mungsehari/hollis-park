import { AnimatePresence, motion } from "motion/react"
import { useEffect, useState } from "react"

interface Flight {
  id: number
  src: string
  from: DOMRect
  to: DOMRect
}

type Listener = (f: Flight) => void
const listeners = new Set<Listener>()
let nextId = 0

/** How long a flight takes; callers wait this long before opening the bag. */
export const FLIGHT_MS = 750

/**
 * Sends a copy of the product photo from `fromEl` in an arc into the header bag (the element marked
 * data-bag-target). Returns false when it did not fly (no element, bag hidden, or reduced motion),
 * so the caller can skip the wait.
 */
export function flyToBag(src: string, fromEl: Element | null | undefined): boolean {
  const target = document.querySelector("[data-bag-target]")
  if (!fromEl || !target || listeners.size === 0) return false
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return false
  const to = target.getBoundingClientRect()
  if (to.width === 0) return false
  const flight = { id: ++nextId, src, from: fromEl.getBoundingClientRect(), to }
  listeners.forEach((l) => l(flight))
  return true
}

/** Renders the flying photos. Mounted once in the layout. */
export function FlyToBagLayer() {
  const [flights, setFlights] = useState<Flight[]>([])
  useEffect(() => {
    const add: Listener = (f) => setFlights((all) => [...all, f])
    listeners.add(add)
    return () => { listeners.delete(add) }
  }, [])

  return (
    <div className="pointer-events-none fixed inset-0 z-[70]" aria-hidden="true">
      <AnimatePresence>
        {flights.map((f) => {
          const size = Math.min(f.from.width, f.from.height, 220)
          const startX = f.from.left + f.from.width / 2 - size / 2
          const startY = f.from.top + f.from.height / 2 - size / 2
          const endX = f.to.left + f.to.width / 2 - 14
          const endY = f.to.top + f.to.height / 2 - 14
          const peak = Math.min(startY, endY) - 90
          return (
            <motion.img
              key={f.id}
              src={f.src}
              alt=""
              className="absolute top-0 left-0 object-cover shadow-2xl"
              initial={{ x: startX, y: startY, width: size, height: size, borderRadius: 24, opacity: 1, rotate: 0 }}
              animate={{
                x: [startX, (startX + endX) / 2, endX],
                y: [startY, peak, endY],
                width: [size, size * 0.55, 28],
                height: [size, size * 0.55, 28],
                borderRadius: [24, 999, 999],
                rotate: [0, -8, 0],
                opacity: [1, 1, 0.6],
              }}
              exit={{ opacity: 0, scale: 0.4, transition: { duration: 0.15 } }}
              transition={{ duration: FLIGHT_MS / 1000, ease: [0.65, 0, 0.35, 1], times: [0, 0.45, 1] }}
              onAnimationComplete={() => setFlights((all) => all.filter((x) => x.id !== f.id))}
            />
          )
        })}
      </AnimatePresence>
    </div>
  )
}
