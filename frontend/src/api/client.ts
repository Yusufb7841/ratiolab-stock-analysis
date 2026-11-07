/** The only place that makes HTTP requests to the RatioLab backend. */
export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, { headers: { Accept: 'application/json' } })
  if (!response.ok) {
    let message = `HTTP ${response.status}`
    try {
      const body = (await response.json()) as { message?: string }
      if (body.message) {
        message = body.message
      }
    } catch {
      // The error response has no JSON body; keep the HTTP status as message.
    }
    throw new Error(message)
  }
  return (await response.json()) as T
}
