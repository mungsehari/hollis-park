import { useEffect, type ReactNode } from "react"
import { Link } from "react-router"
import { ChevronRightIcon } from "lucide-react"
import { Eyebrow } from "./brand"
import { SplitWords } from "./motion"

export function useTitle(title: string) {
  useEffect(() => { document.title = title }, [title])
}

export function Crumbs({ trail }: { trail: [string, string | null][] }) {
  return (
    <nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-1.5 text-sm text-muted-foreground">
      {trail.map(([label, to], i) => (
        <span key={label} className="flex items-center gap-1.5">
          {i > 0 && <ChevronRightIcon className="size-3.5" />}
          {to ? <Link to={to} className="hover:text-foreground">{label}</Link> : <span aria-current="page" className="text-foreground">{label}</span>}
        </span>
      ))}
    </nav>
  )
}

/** Standard page shell: breadcrumbs, a big animated title and an optional lead paragraph. */
export function PageShell({ title, lead, eyebrow, trail, children, wide = false }: {
  title: string; lead?: ReactNode; eyebrow?: string; trail?: [string, string | null][]; children: ReactNode; wide?: boolean
}) {
  useTitle(`${title.replace(/\s+/g, " ")} | Hollis Park`)
  return (
    <div className={`mx-auto px-4 pt-8 pb-8 sm:px-6 lg:px-10 ${wide ? "max-w-[90rem]" : "max-w-7xl"}`}>
      {trail && <Crumbs trail={trail} />}
      <header className="mt-6 mb-10 grid gap-5 lg:mt-10 lg:mb-14 lg:grid-cols-[1.2fr_1fr] lg:items-end">
        <div>
          {eyebrow && <Eyebrow className="mb-3">{eyebrow}</Eyebrow>}
          <h1 className="font-heading text-5xl leading-[0.92] font-extrabold tracking-tight uppercase sm:text-6xl lg:text-7xl" tabIndex={-1}>
            <SplitWords text={title} />
          </h1>
        </div>
        {lead && <p className="max-w-xl text-lg text-muted-foreground lg:justify-self-end">{lead}</p>}
      </header>
      {children}
    </div>
  )
}
