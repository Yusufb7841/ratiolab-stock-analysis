import { vi } from 'vitest'
import type { PriceSeries } from '../types/prices.ts'

/** Artificial test data: placeholder ticker TEST, no real company. */
export const TEST_SERIES: PriceSeries = {
  ticker: 'TEST',
  provider: 'ALPHA_VANTAGE',
  adjustment: 'UNADJUSTED',
  currency: 'USD',
  timeZone: 'US/Eastern',
  asOf: '2026-01-06',
  retrievedAt: '2026-01-07T09:00:00Z',
  points: [
    { date: '2026-01-05', open: '100.0000', high: '101.5000', low: '99.5000', close: '101.0000', volume: 1000 },
    { date: '2026-01-06', open: '101.0000', high: '102.2500', low: '100.5000', close: '102.0000', volume: 2000 },
  ],
}

export const EMPTY_SERIES: PriceSeries = {
  ticker: 'TEST',
  provider: null,
  adjustment: null,
  currency: null,
  timeZone: null,
  asOf: null,
  retrievedAt: null,
  points: [],
}

/** Replaces fetch with fixed responses per URL path. */
export function fakeFetch(routes: Record<string, { status?: number; body: unknown }>) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (path: string) => {
      const route = routes[path]
      if (!route) {
        return new Response(JSON.stringify({ code: 'NOT_FOUND', message: `No fake for ${path}` }), { status: 404 })
      }
      return new Response(JSON.stringify(route.body), {
        status: route.status ?? 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }),
  )
}
