import { useMemo, useState } from "react"
import { useNavigate } from "react-router"
import { ArrowUpRightIcon, SearchIcon } from "lucide-react"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog"
import { PAGES, POPULAR } from "@/lib/catalog"
import { matchesQuery } from "@/lib/format"
import { useShop } from "@/state/store"

export function SearchDialog() {
  const { searchOpen, setSearchOpen, data, collById, money } = useShop()
  const [q, setQ] = useState("")
  const navigate = useNavigate()
  const term = q.trim().toLowerCase()

  const products = useMemo(() => (term ? data.products.filter((p) => matchesQuery(p, term, data.colours)) : POPULAR.bestsellers(data.products)), [term, data])
  const colls = term ? data.collections.filter((c) => c.name.toLowerCase().includes(term)) : []
  const pages = term ? PAGES.filter(([t, , d]) => (t + " " + d).toLowerCase().includes(term)) : PAGES.slice(0, 3)

  const go = (to: string) => { setSearchOpen(false); setQ(""); navigate(to) }

  return (
    <Dialog open={searchOpen} onOpenChange={(o) => { setSearchOpen(o); if (!o) setQ("") }}>
      <DialogContent className="top-[12%] translate-y-0 overflow-hidden p-0 sm:max-w-2xl" showCloseButton={false}>
        <DialogTitle className="sr-only">Search the store</DialogTitle>
        <DialogDescription className="sr-only">Search sneakers, collections and help pages</DialogDescription>
        <Command shouldFilter={false} className="rounded-none">
          <CommandInput
            placeholder="Search models, colours or pages" value={q} onValueChange={setQ}
            onKeyDown={(e) => { if (e.key === "Enter" && term && !document.querySelector("[cmdk-item][data-selected=true]")) go(`/catalog?q=${encodeURIComponent(q.trim())}`) }}
          />
          <CommandList className="max-h-[60vh]">
            <CommandEmpty>No results for "{q}". Try moss, suede, trail or wide.</CommandEmpty>
            {products.length > 0 && (
              <CommandGroup heading={term ? "Sneakers" : "Most worn"}>
                {products.map((p) => (
                  <CommandItem key={p.id} value={`p-${p.id}`} onSelect={() => go(`/catalog/${p.id}`)} className="gap-3 py-2">
                    <img src={p.img} alt="" className="size-12 rounded-lg bg-muted object-cover" />
                    <span className="flex-1"><b className="block font-medium">{p.name}</b><span className="text-xs text-muted-foreground">{collById[p.collection]?.name} · {p.colourway}</span></span>
                    <span className="text-sm tabular-nums">{money(p.price)}</span>
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {colls.length > 0 && (
              <CommandGroup heading="Collections">
                {colls.map((c) => (
                  <CommandItem key={c.id} value={`c-${c.id}`} onSelect={() => go(`/collections/${c.id}`)} className="gap-3 py-2">
                    {c.image && <img src={c.image} alt="" className="size-12 rounded-lg object-cover" />}
                    <span><b className="block font-medium">{c.name}</b><span className="text-xs text-muted-foreground">{c.tagline}</span></span>
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {pages.length > 0 && (
              <CommandGroup heading={term ? "Pages" : "Help"}>
                {pages.map(([t, r, d]) => (
                  <CommandItem key={r} value={`g-${r}`} onSelect={() => go(`/${r}`)} className="gap-3">
                    <ArrowUpRightIcon className="size-4" /><span><b className="block font-medium">{t}</b><span className="text-xs text-muted-foreground">{d}</span></span>
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
            {term && (
              <CommandGroup>
                <CommandItem value="all" onSelect={() => go(`/catalog?q=${encodeURIComponent(q.trim())}`)} className="gap-3">
                  <SearchIcon className="size-4" />See all results for "{q.trim()}"
                </CommandItem>
              </CommandGroup>
            )}
          </CommandList>
        </Command>
      </DialogContent>
    </Dialog>
  )
}
