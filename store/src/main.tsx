import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { BrowserRouter } from "react-router"
import "./index.css"
import App from "./App.tsx"
import { ThemeProvider } from "@/components/theme-provider.tsx"
import { StoreProvider } from "@/state/store"

function Boot({ error, retry }: { error: string | null; retry: () => void }) {
  return (
    <main className="flex min-h-svh items-center justify-center px-6 text-center">
      {error ? (
        <div className="space-y-4">
          <h1 className="font-heading text-3xl font-extrabold uppercase">The store could not load</h1>
          <p className="text-muted-foreground">{error}</p>
          <button type="button" onClick={retry} className="h-11 rounded-full bg-foreground px-6 text-background">Try again</button>
        </div>
      ) : (
        <p className="font-mono text-xs tracking-[0.2em] text-muted-foreground uppercase" aria-live="polite">Loading the store</p>
      )}
    </main>
  )
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider defaultTheme="light" storageKey="hp-theme">
      <BrowserRouter>
        <StoreProvider fallback={(s) => <Boot {...s} />}>
          <App />
        </StoreProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>
)
