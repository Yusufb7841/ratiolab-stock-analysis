import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from './App.tsx'
import { EMPTY_SERIES, fakeFetch } from './test/fakeApi.ts'

vi.mock('./components/PriceChart.tsx', () => ({
  PriceChart: () => <div data-testid="price-chart" />,
}))

describe('App', () => {
  it('shows the application name and one button per configured company', async () => {
    fakeFetch({
      '/api/v1/companies': { body: [{ ticker: 'TEST' }] },
      '/api/v1/companies/TEST/prices': { body: EMPTY_SERIES },
    })

    render(<App />)

    expect(screen.getByRole('heading', { name: 'RatioLab' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'TEST' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('shows an error when the company list cannot be loaded', async () => {
    fakeFetch({})

    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Unternehmensliste konnte nicht geladen werden')
  })
})
