const PROVIDER_NAMES: Record<string, string> = {
  ALPHA_VANTAGE: 'Alpha Vantage',
}

export function providerName(provider: string | null): string {
  return provider ? (PROVIDER_NAMES[provider] ?? provider) : 'unbekannt'
}

/** 2026-11-06 → 06.11.2026 (a trading date, no time zone conversion). */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}.${month}.${year}`
}

/** ISO timestamp → local date and time, e.g. 07.11.2026, 01:53. */
export function formatTimestamp(isoTimestamp: string): string {
  return new Intl.DateTimeFormat('de-DE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(isoTimestamp))
}
