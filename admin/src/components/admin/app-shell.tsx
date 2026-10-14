import { MotionConfig, motion } from "motion/react"
import { useState, type FormEvent, type ReactNode } from "react"
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router"
import {
  ChevronsUpDownIcon, ExternalLinkIcon, InboxIcon, KeyRoundIcon, LayoutDashboardIcon, LogOutIcon, MailIcon, MoonIcon,
  ImageIcon, LayoutPanelTopIcon, LayersIcon, PackageIcon, SettingsIcon, ShoppingBagIcon, SparklesIcon, SunIcon, TicketPercentIcon, UsersIcon,
} from "lucide-react"
import { toast } from "sonner"
import { useTheme } from "@/components/theme-provider"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel, SidebarHeader, SidebarInset, SidebarMenu,
  SidebarMenuBadge, SidebarMenuButton, SidebarMenuItem, SidebarProvider, SidebarTrigger,
} from "@/components/ui/sidebar"
import { api, ApiError } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { initials } from "@/lib/format"
import type { Role } from "@/lib/types"
import { ErrorNote } from "./shared"

interface NavItem { to: string; label: string; icon: ReactNode; role?: Role; end?: boolean }

const SHOP: NavItem[] = [
  { to: "/", label: "Dashboard", icon: <LayoutDashboardIcon />, end: true },
  { to: "/orders", label: "Orders", icon: <ShoppingBagIcon /> },
  { to: "/products", label: "Products", icon: <PackageIcon /> },
  { to: "/messages", label: "Messages", icon: <InboxIcon /> },
  { to: "/subscribers", label: "Subscribers", icon: <MailIcon /> },
]
const CONTENT: NavItem[] = [
  { to: "/home", label: "Homepage", icon: <LayoutPanelTopIcon /> },
  { to: "/collections", label: "Collections", icon: <LayersIcon /> },
  { to: "/media", label: "Media", icon: <ImageIcon /> },
  { to: "/care-picks", label: "Care picks", icon: <SparklesIcon /> },
]
const STORE: NavItem[] = [
  { to: "/promo-codes", label: "Promo codes", icon: <TicketPercentIcon />, role: "ADMIN" },
  { to: "/settings", label: "Tax and delivery", icon: <SettingsIcon /> },
  { to: "/staff", label: "Staff", icon: <UsersIcon />, role: "ADMIN" },
]

function BrandMark() {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" className="size-8 shrink-0 text-[#8D9A89]">
      <circle cx="16" cy="16" r="16" fill="currentColor" />
      <path d="M9.5 21.5 17 10.5M15.5 21.5 23 10.5" stroke="#fff" strokeWidth="3" strokeLinecap="round" fill="none" />
    </svg>
  )
}

function NavGroup({ label, items, badges }: { label: string; items: NavItem[]; badges?: Record<string, number> }) {
  const { can } = useAuth()
  const { pathname } = useLocation()
  const visible = items.filter((i) => !i.role || can(i.role))
  if (!visible.length) return null
  return (
    <SidebarGroup>
      <SidebarGroupLabel>{label}</SidebarGroupLabel>
      <SidebarMenu>
        {visible.map((i) => {
          const active = i.end ? pathname === i.to : pathname.startsWith(i.to)
          return (
            <SidebarMenuItem key={i.to}>
              <SidebarMenuButton isActive={active} tooltip={i.label} render={<NavLink to={i.to} end={i.end} />}>
                {i.icon}
                <span>{i.label}</span>
              </SidebarMenuButton>
              {badges?.[i.to] ? <SidebarMenuBadge>{badges[i.to]}</SidebarMenuBadge> : null}
            </SidebarMenuItem>
          )
        })}
      </SidebarMenu>
    </SidebarGroup>
  )
}

function PasswordDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    if (f.get("next") !== f.get("confirm")) { setError("The new passwords do not match."); return }
    setBusy(true)
    setError(null)
    try {
      await api.post("/api/v1/auth/password", { current: f.get("current"), next: f.get("next") })
      toast.success("Password changed")
      onOpenChange(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not change the password.")
    } finally {
      setBusy(false)
    }
  }
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <form onSubmit={submit} className="grid gap-5">
          <DialogHeader>
            <DialogTitle>Change your password</DialogTitle>
            <DialogDescription>Use at least 10 characters. You stay signed in on this device.</DialogDescription>
          </DialogHeader>
          <FieldGroup>
            <Field><FieldLabel htmlFor="pw-current">Current password</FieldLabel><Input id="pw-current" name="current" type="password" autoComplete="current-password" required /></Field>
            <Field>
              <FieldLabel htmlFor="pw-next">New password</FieldLabel>
              <Input id="pw-next" name="next" type="password" autoComplete="new-password" minLength={10} required />
              <FieldDescription>At least 10 characters.</FieldDescription>
            </Field>
            <Field><FieldLabel htmlFor="pw-confirm">Repeat the new password</FieldLabel><Input id="pw-confirm" name="confirm" type="password" autoComplete="new-password" minLength={10} required /></Field>
          </FieldGroup>
          <ErrorNote error={error} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={busy}>{busy ? "Saving" : "Change password"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function UserMenu() {
  const { me, logout } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const [pwOpen, setPwOpen] = useState(false)
  if (!me) return null
  const dark = theme === "dark"
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger render={<SidebarMenuButton size="lg" className="data-popup-open:bg-sidebar-accent" />}>
          <Avatar className="size-8 rounded-lg"><AvatarFallback className="rounded-lg bg-primary text-primary-foreground">{initials(me.name)}</AvatarFallback></Avatar>
          <div className="grid flex-1 text-left text-sm leading-tight">
            <span className="truncate font-medium">{me.name}</span>
            <span className="truncate text-xs text-muted-foreground">{me.role === "ADMIN" ? "Admin" : "Staff"}</span>
          </div>
          <ChevronsUpDownIcon className="ml-auto size-4" />
        </DropdownMenuTrigger>
        <DropdownMenuContent side="top" align="start" className="min-w-56">
          <DropdownMenuGroup>
            <DropdownMenuLabel className="font-normal">
              <div className="text-sm font-medium">{me.name}</div>
              <div className="text-xs text-muted-foreground">{me.email}</div>
            </DropdownMenuLabel>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={() => setPwOpen(true)}><KeyRoundIcon />Change password</DropdownMenuItem>
          <DropdownMenuItem onClick={() => setTheme(dark ? "light" : "dark")}>{dark ? <SunIcon /> : <MoonIcon />}{dark ? "Light mode" : "Dark mode"}</DropdownMenuItem>
          <DropdownMenuItem render={<a href="/" target="_blank" rel="noreferrer" />}><ExternalLinkIcon />Open the store</DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive" onClick={async () => { await logout(); navigate("/login") }}><LogOutIcon />Sign out</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <PasswordDialog open={pwOpen} onOpenChange={setPwOpen} />
    </>
  )
}

export function AppShell({ badges }: { badges?: Record<string, number> }) {
  const { pathname } = useLocation()
  return (
    <MotionConfig reducedMotion="user">
    <SidebarProvider>
      <Sidebar collapsible="icon" variant="inset">
        <SidebarHeader>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton size="lg" render={<Link to="/" />}>
                <BrandMark />
                <div className="grid flex-1 text-left leading-tight">
                  <span className="font-heading text-sm font-extrabold tracking-tight uppercase">Hollis Park</span>
                  <span className="text-xs text-muted-foreground">Store admin</span>
                </div>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarHeader>
        <SidebarContent>
          <NavGroup label="Shop" items={SHOP} badges={badges} />
          <NavGroup label="Content" items={CONTENT} />
          <NavGroup label="Store" items={STORE} />
        </SidebarContent>
        <SidebarFooter>
          <SidebarMenu><SidebarMenuItem><UserMenu /></SidebarMenuItem></SidebarMenu>
        </SidebarFooter>
      </Sidebar>
      <SidebarInset>
        <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-1 h-4" />
          <span className="text-sm text-muted-foreground">Hollis Park admin</span>
        </header>
        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          {/* Each page slides in softly; no exit animation, so navigation never waits. */}
          <motion.div
            key={pathname}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
            className="mx-auto w-full max-w-7xl space-y-6"
          >
            <Outlet />
          </motion.div>
        </main>
      </SidebarInset>
    </SidebarProvider>
    </MotionConfig>
  )
}
