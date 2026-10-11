import { AnimatePresence, motion } from "motion/react"
import { useState, type FormEvent } from "react"
import { CheckIcon } from "lucide-react"
import { cn } from "cn"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { api, ApiError } from "@/lib/api"
import { EMAIL_RE, pct } from "@/lib/format"

export function NewsletterForm({ tone = "light", topics = [] }: { tone?: "light" | "dark"; topics?: string[] }) {
  const [email, setEmail] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState<{ code: string | null; rate: number | null } | null>(null)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!EMAIL_RE.test(email.trim())) { setError("Enter an email address like name@example.com."); return }
    setBusy(true)
    setError(null)
    try {
      const r = await api.newsletter(email.trim(), topics)
      setDone({ code: r.promoCode, rate: r.promoRate })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not subscribe. Try again.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <AnimatePresence mode="wait">
      {done ? (
        <motion.div key="done" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className={cn("flex items-start gap-3 rounded-2xl p-4", tone === "dark" ? "bg-white/10" : "bg-accent")} role="status">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-sage text-white"><CheckIcon className="size-4" /></span>
          <div>
            <p className="font-semibold">You are on the list.</p>
            {done.code && <p className={cn("mt-1 text-sm", tone === "dark" ? "text-white/75" : "text-muted-foreground")}>Use <code className="font-mono font-semibold">{done.code}</code> in your bag for {pct(done.rate ?? 0)} off your first pair.</p>}
          </div>
        </motion.div>
      ) : (
        <motion.form key="form" exit={{ opacity: 0, y: -8 }} onSubmit={submit} noValidate className="space-y-2">
          <label htmlFor={`news-${tone}`} className="sr-only">Email address</label>
          <div className="flex gap-2">
            <Input
              id={`news-${tone}`} type="email" autoComplete="email" placeholder="you@example.com" value={email}
              onChange={(e) => { setEmail(e.target.value); setError(null) }}
              aria-invalid={!!error} aria-describedby={error ? `news-${tone}-err` : undefined}
              className={cn("h-11 rounded-full px-4", tone === "dark" && "border-white/20 bg-white/10 text-white placeholder:text-white/50")}
            />
            <Button type="submit" size="lg" disabled={busy} className={cn("h-11 rounded-full px-5", tone === "dark" && "bg-white text-sage-ink hover:bg-white/90")}>
              {busy ? "Joining" : "Subscribe"}
            </Button>
          </div>
          {error && <p id={`news-${tone}-err`} className={cn("text-sm", tone === "dark" ? "text-red-200" : "text-destructive")}>{error}</p>}
        </motion.form>
      )}
    </AnimatePresence>
  )
}
