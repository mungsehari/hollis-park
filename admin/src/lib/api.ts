/** Same-origin API client. Admin requests carry the session cookie and the CSRF token Spring issues. */
export class ApiError extends Error {
  readonly status: number
  readonly fields: Record<string, string>
  constructor(message: string, status: number, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

const readCookie = (name: string) =>
  document.cookie.split("; ").find((c) => c.startsWith(name + "="))?.split("=")[1]

let csrfLoaded: Promise<void> | null = null
function ensureCsrf() {
  if (readCookie("XSRF-TOKEN")) return Promise.resolve()
  csrfLoaded ??= fetch("/api/auth/csrf", { credentials: "same-origin" }).then(() => undefined).finally(() => { csrfLoaded = null })
  return csrfLoaded
}

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => { onUnauthorized = fn }

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const write = method !== "GET"
  if (write) await ensureCsrf()
  let res: Response
  try {
    res = await fetch(path, {
      method,
      credentials: "same-origin",
      headers: {
        Accept: "application/json",
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(write ? { "X-XSRF-TOKEN": decodeURIComponent(readCookie("XSRF-TOKEN") ?? "") } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError("Cannot reach the server. Check your connection.", 0)
  }
  if (res.status === 204) return undefined as T
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    if (res.status === 401 && !path.startsWith("/api/auth/login")) onUnauthorized()
    const fields = data.fields ?? {}
    const detail = Object.values(fields).join(" ")
    throw new ApiError(detail ? `${data.message} ${detail}` : data.message || "Something went wrong.", res.status, fields)
  }
  return data as T
}

/** Uploads one image as multipart form data (the browser sets the boundary header). */
async function upload<T>(path: string, file: File, fields: Record<string, string> = {}): Promise<T> {
  await ensureCsrf()
  const body = new FormData()
  body.append("file", file)
  Object.entries(fields).forEach(([k, v]) => body.append(k, v))
  let res: Response
  try {
    res = await fetch(path, {
      method: "POST",
      credentials: "same-origin",
      headers: { Accept: "application/json", "X-XSRF-TOKEN": decodeURIComponent(readCookie("XSRF-TOKEN") ?? "") },
      body,
    })
  } catch {
    throw new ApiError("Cannot reach the server. Check your connection.", 0)
  }
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    if (res.status === 401) onUnauthorized()
    throw new ApiError(data.message || "The upload failed.", res.status)
  }
  return data as T
}

export const api = {
  upload,
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body ?? {}),
  put: <T>(path: string, body: unknown) => request<T>("PUT", path, body),
  patch: <T>(path: string, body: unknown) => request<T>("PATCH", path, body),
  del: <T>(path: string) => request<T>("DELETE", path),
}

export const qs = (params: Record<string, string | number | undefined | null>) => {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== "") p.set(k, String(v)) })
  const s = p.toString()
  return s ? "?" + s : ""
}
