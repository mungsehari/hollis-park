import { cn } from "cn"

export function BrandMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" className={cn("size-8 shrink-0", className)}>
      <circle cx="16" cy="16" r="16" fill="var(--sage)" />
      <path d="M9.5 21.5 17 10.5M15.5 21.5 23 10.5" stroke="#fff" strokeWidth="3" strokeLinecap="round" fill="none" />
    </svg>
  )
}

/** Small uppercase label used above headings. */
export function Eyebrow({ children, className }: { children: React.ReactNode; className?: string }) {
  return <p className={cn("font-mono text-[11px] tracking-[0.2em] text-muted-foreground uppercase", className)}>{children}</p>
}

export function SectionTitle({ children, className }: { children: React.ReactNode; className?: string }) {
  return <h2 className={cn("font-heading text-3xl leading-[0.95] font-extrabold tracking-tight text-balance uppercase sm:text-4xl lg:text-5xl", className)}>{children}</h2>
}
