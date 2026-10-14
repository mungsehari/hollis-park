/**
 * Same-origin API client. Admin requests carry the session cookie and the CSRF token Spring issues.
 * Every JSON answer is the envelope {success, status, message, data[, requestId]}; callers get `data`,
 * failures become an ApiError with the server's message (and per-field messages for validation).
 */
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
  csrfLoaded ??= fetch("/api/v1/auth/csrf", { credentials: "same-origin" }).then(() => undefined).finally(() => { csrfLoaded = null })
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
  const env = await res.json().catch(() => ({}))
  if (!res.ok || env.success === false) {
    if (res.status === 401 && !path.startsWith("/api/v1/auth/login")) onUnauthorized()
    const fields: Record<string, string> = res.status === 400 && env.data && typeof env.data === "object" ? env.data : {}
    const detail = Object.values(fields).join(" ")
    const message = env.message || "Something went wrong."
    throw new ApiError(detail ? `${message} ${detail}` : message, res.status, fields)
  }
  return env.data as T
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
  const env = await res.json().catch(() => ({}))
  if (!res.ok || env.success === false) {
    if (res.status === 401) onUnauthorized()
    throw new ApiError(env.message || "The upload failed.", res.status)
  }
  return env.data as T
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
