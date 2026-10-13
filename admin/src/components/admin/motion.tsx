import { motion, useInView, useMotionValue, useSpring, useTransform, type Variants } from "motion/react"
import { useEffect, useRef, type ReactNode } from "react"

/** Same calm easing as the store: short moves, soft landing, no bounce. */
export const EASE = [0.22, 1, 0.36, 1] as const

const item: Variants = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: EASE } },
}

/** Children marked <StaggerItem> appear one after another. */
export function Stagger({ children, className, gap = 0.06 }: { children: ReactNode; className?: string; gap?: number }) {
  return (
    <motion.div className={className} initial="hidden" animate="show" variants={{ hidden: {}, show: { transition: { staggerChildren: gap } } }}>
      {children}
    </motion.div>
  )
}

export function StaggerItem({ children, className }: { children: ReactNode; className?: string }) {
  return <motion.div className={className} variants={item}>{children}</motion.div>
}

/** Counts up from zero to `value` when it first comes into view. */
export function CountUp({ value, format = (n: number) => Math.round(n).toLocaleString("en-IN") }: { value: number; format?: (n: number) => string }) {
  const ref = useRef<HTMLSpanElement>(null)
  const inView = useInView(ref, { once: true })
  const mv = useMotionValue(0)
  const spring = useSpring(mv, { duration: 1200, bounce: 0 })
  const text = useTransform(spring, (n) => format(n))
  useEffect(() => { if (inView) mv.set(value) }, [inView, mv, value])
  return <motion.span ref={ref}>{text}</motion.span>
}
