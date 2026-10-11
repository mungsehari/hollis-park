import { useCallback, useEffect, useState } from "react"
import { api, ApiError } from "./api"

/**
 * Loads a GET endpoint. `reload` re-fetches; `setData` applies the result of a write.
 * Data from the previous request stays visible while the next one loads.
 */
export function useApi<T>(path: string | null) {
  const [tick, setTick] = useState(0)
  const key = path ? `${path}#${tick}` : null
  const [state, setState] = useState<{ key: string | null; data: T | null; error: string | null }>({ key: null, data: null, error: null })

  useEffect(() => {
    if (!key || !path) return
    let live = true
    api.get<T>(path)
      .then((d) => { if (live) setState({ key, data: d, error: null }) })
      .catch((e) => { if (live) setState((s) => ({ key, data: s.data, error: e instanceof ApiError ? e.message : "Could not load." })) })
    return () => { live = false }
  }, [key, path])

  const reload = useCallback(() => setTick((n) => n + 1), [])
  const setData = useCallback((data: T) => setState((s) => ({ ...s, data })), [])
  return { data: state.data, error: state.error, loading: key !== null && state.key !== key, reload, setData }
}
