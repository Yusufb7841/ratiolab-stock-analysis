/** Daily price series as delivered by GET /api/v1/companies/{ticker}/prices. Decimals are strings. */
export type PricePoint = {
  date: string
  open: string
  high: string
  low: string
  close: string
  volume: number
  /** The import that stored this day (provenance). */
  importRunId: number
}

export type PriceSeries = {
  ticker: string
  provider: string | null
  adjustment: string | null
  currency: string | null
  timeZone: string | null
  /** Latest trading day in the series (YYYY-MM-DD), null if nothing has been imported. */
  asOf: string | null
  /** Import time (ISO 8601, UTC), null if nothing has been imported. */
  retrievedAt: string | null
  points: PricePoint[]
}
