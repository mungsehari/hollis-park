import { useState, type FormEvent } from "react"
import { motion } from "motion/react"
import { Navigate, useLocation, useNavigate } from "react-router"
import { ErrorNote } from "@/components/admin/shared"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import loginHero from "@/assets/login-hero.webp"

export function Login() {
  const { me, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? "/"
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (me) return <Navigate to={from} replace />

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    setBusy(true)
    setError(null)
    try {
      await login(String(f.get("email")), String(f.get("password")))
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not sign in.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-[#8D9A89] lg:block">
        <motion.img src={loginHero} alt="" initial={{ scale: 1.08, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ duration: 1.4, ease: [0.22, 1, 0.36, 1] }} className="absolute inset-0 size-full object-cover object-right" />
        {/* keeps the white heading readable on light parts of the photo */}
        <div className="absolute inset-x-0 top-0 h-64 bg-gradient-to-b from-black/45 to-transparent" />
        <div className="relative p-10 text-right text-white">
          <motion.p initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.3 }} className="font-mono text-xs tracking-[0.2em] uppercase">Hollis Park</motion.p>
          <p className="mt-4 font-heading text-6xl leading-[0.95] font-extrabold tracking-tight uppercase">
            {["Store", "admin"].map((w, i) => (
              <span key={w} className="block overflow-hidden">
                <motion.span className="block" initial={{ y: "110%" }} animate={{ y: 0 }} transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1], delay: 0.45 + i * 0.1 }}>{w}</motion.span>
              </span>
            ))}
          </p>
        </div>
      </div>
      <div className="flex items-center justify-center p-6">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1], delay: 0.2 }} className="w-full max-w-sm">
        <Card className="w-full">
          <CardHeader>
            <CardTitle className="font-heading text-xl font-extrabold uppercase">Sign in</CardTitle>
            <CardDescription>Orders, products and messages for the Hollis Park store.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={submit} className="grid gap-5">
              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="email">Email</FieldLabel>
                  <Input id="email" name="email" type="email" autoComplete="username" required autoFocus />
                </Field>
                <Field>
                  <FieldLabel htmlFor="password">Password</FieldLabel>
                  <Input id="password" name="password" type="password" autoComplete="current-password" required />
                </Field>
              </FieldGroup>
              <ErrorNote error={error} />
              <Button type="submit" size="lg" disabled={busy}>{busy ? "Signing in" : "Sign in"}</Button>
            </form>
          </CardContent>
        </Card>
        </motion.div>
      </div>
    </div>
  )
}
