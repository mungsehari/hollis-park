import { BrowserRouter, Route, Routes } from "react-router"
import { AppShell } from "@/components/admin/app-shell"
import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"
import { AuthProvider, RequireAuth } from "@/lib/auth"
import { CarePicks } from "@/pages/CarePicks"
import { Collections, Homepage, Media } from "@/pages/Content"
import { Dashboard } from "@/pages/Dashboard"
import { Login } from "@/pages/Login"
import { OrderDetail } from "@/pages/OrderDetail"
import { Orders } from "@/pages/Orders"
import { Messages, Staff, Subscribers } from "@/pages/People"
import { ProductEdit } from "@/pages/ProductEdit"
import { Products } from "@/pages/Products"
import { PromoCodes, Settings } from "@/pages/StoreSettings"

export function App() {
  return (
    <BrowserRouter basename="/admin">
      <AuthProvider>
        <TooltipProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<RequireAuth><AppShell /></RequireAuth>}>
              <Route index element={<Dashboard />} />
              <Route path="orders" element={<Orders />} />
              <Route path="orders/:number" element={<OrderDetail />} />
              <Route path="products" element={<Products />} />
              <Route path="products/new" element={<RequireAuth role="ADMIN"><ProductEdit /></RequireAuth>} />
              <Route path="products/:id" element={<ProductEdit />} />
              <Route path="home" element={<Homepage />} />
              <Route path="collections" element={<Collections />} />
              <Route path="media" element={<Media />} />
              <Route path="care-picks" element={<CarePicks />} />
              <Route path="messages" element={<Messages />} />
              <Route path="subscribers" element={<Subscribers />} />
              <Route path="settings" element={<Settings />} />
              <Route path="promo-codes" element={<RequireAuth role="ADMIN"><PromoCodes /></RequireAuth>} />
              <Route path="staff" element={<RequireAuth role="ADMIN"><Staff /></RequireAuth>} />
              <Route path="*" element={<Dashboard />} />
            </Route>
          </Routes>
          <Toaster position="top-right" richColors />
        </TooltipProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
