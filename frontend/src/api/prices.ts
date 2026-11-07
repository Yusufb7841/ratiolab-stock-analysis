import type { PriceSeries } from '../types/prices.ts'
import { getJson } from './client.ts'

export function fetchPrices(ticker: string): Promise<PriceSeries> {
  return getJson<PriceSeries>(`/api/v1/companies/${encodeURIComponent(ticker)}/prices`)
}
