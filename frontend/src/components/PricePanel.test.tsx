import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EMPTY_SERIES, TEST_SERIES, fakeFetch } from '../test/fakeApi.ts'
import { PricePanel } from './PricePanel.tsx'

// ECharts needs a real canvas; the chart itself is replaced by a placeholder in these tests.
vi.mock('./PriceChart.tsx', () => ({
  PriceChart: ({ ticker, points }: { ticker: string; points: unknown[] }) => (
    <div data-testid="price-chart">
      {ticker}: {points.length} Punkte
    </div>
  ),
}))

const PRICES = '/api/v1/companies/TEST/prices'

describe('PricePanel', () => {
  it('shows the chart with source, adjustment and data date after loading', async () => {
    fakeFetch({ [PRICES]: { body: TEST_SERIES } })

    render(<PricePanel ticker="TEST" />)

    expect(screen.getByRole('status')).toHaveTextContent('werden geladen')
    expect(await screen.findByTestId('price-chart')).toHaveTextContent('TEST: 2 Punkte')
    const caption = screen.getByText(/Quelle: Alpha Vantage/)
    expect(caption).toHaveTextContent('nicht bereinigt')
    expect(caption).toHaveTextContent('USD')
    expect(caption).toHaveTextContent('letzter Handelstag: 06.01.2026')
    expect(caption).toHaveTextContent('07.01.2026')
  })

  it('shows an empty state instead of a chart when nothing has been imported', async () => {
    fakeFetch({ [PRICES]: { body: EMPTY_SERIES } })

    render(<PricePanel ticker="TEST" />)

    expect(await screen.findByText('Für TEST wurden noch keine Kursdaten importiert.')).toBeInTheDocument()
    expect(screen.queryByTestId('price-chart')).not.toBeInTheDocument()
  })

  it('shows the error message from the API', async () => {
    fakeFetch({ [PRICES]: { status: 404, body: { code: 'UNKNOWN_TICKER', message: 'Ticker TEST is not supported' } } })

    render(<PricePanel ticker="TEST" />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Ticker TEST is not supported')
  })
})
