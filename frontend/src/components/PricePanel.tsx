import { useEffect, useState } from 'react'
import { fetchPrices } from '../api/prices.ts'
import type { PriceSeries } from '../types/prices.ts'
import { PriceChart } from './PriceChart.tsx'
import { formatDate, formatTimestamp, providerName } from './format.ts'

type State =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; series: PriceSeries }

type Props = {
  ticker: string
}

/**
 * Price chart of one company with loading, empty and error states.
 * Render it with key={ticker} so that switching companies starts from the loading state.
 */
export function PricePanel({ ticker }: Props) {
  const [state, setState] = useState<State>({ status: 'loading' })

  useEffect(() => {
    let active = true
    fetchPrices(ticker)
      .then((series) => {
        if (active) {
          setState({ status: 'loaded', series })
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setState({ status: 'error', message: error instanceof Error ? error.message : String(error) })
        }
      })
    return () => {
      active = false
    }
  }, [ticker])

  if (state.status === 'loading') {
    return <p role="status">Kursdaten werden geladen …</p>
  }
  if (state.status === 'error') {
    return <p role="alert">Kursdaten konnten nicht geladen werden: {state.message}</p>
  }

  const { series } = state
  if (series.points.length === 0 || !series.asOf || !series.retrievedAt) {
    return <p>Für {ticker} wurden noch keine Kursdaten importiert.</p>
  }

  return (
    <figure className="price-panel">
      <PriceChart ticker={series.ticker} points={series.points} />
      <figcaption>
        Tägliche Schlusskurse ({series.adjustment === 'UNADJUSTED' ? 'nicht bereinigt' : series.adjustment}) in{' '}
        {series.currency} · Quelle: {providerName(series.provider)} · letzter Handelstag: {formatDate(series.asOf)} ·
        importiert: {formatTimestamp(series.retrievedAt)}
      </figcaption>
    </figure>
  )
}
