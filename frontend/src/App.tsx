import { useEffect, useState } from 'react'
import { fetchCompanies, type Company } from './api/companies.ts'
import { PricePanel } from './components/PricePanel.tsx'

function App() {
  const [companies, setCompanies] = useState<Company[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null)

  useEffect(() => {
    fetchCompanies()
      .then((list) => {
        setCompanies(list)
        setSelected(list[0]?.ticker ?? null)
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : String(e)))
  }, [])

  return (
    <main>
      <header>
        <h1>RatioLab</h1>
        <p>Fundamentalanalyse und Unternehmensvergleich</p>
      </header>

      {error && <p role="alert">Unternehmensliste konnte nicht geladen werden: {error}</p>}

      {companies && (
        <nav aria-label="Unternehmen" className="company-nav">
          {companies.map((company) => (
            <button
              key={company.ticker}
              type="button"
              aria-pressed={company.ticker === selected}
              onClick={() => setSelected(company.ticker)}
            >
              {company.ticker}
            </button>
          ))}
        </nav>
      )}

      {selected && <PricePanel key={selected} ticker={selected} />}
    </main>
  )
}

export default App
