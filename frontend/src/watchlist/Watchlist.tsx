import { useEffect, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchWatchlist, removeFromWatchlist, type WatchlistEntry } from './api'

interface WatchlistProps {
  refreshKey: number
  onSelect: (symbol: string) => void
}

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

/** Saved tickers a user wants to revisit without retyping (E3-F3-S1). */
function Watchlist({ refreshKey, onSelect }: WatchlistProps) {
  const [entries, setEntries] = useState<WatchlistEntry[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [removingSymbol, setRemovingSymbol] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setError(null)
    fetchWatchlist()
      .then((result) => {
        if (!cancelled) setEntries(result)
      })
      .catch((reason) => {
        if (!cancelled) setError(describeError(reason, 'Could not load the watchlist.'))
      })
    return () => {
      cancelled = true
    }
  }, [refreshKey])

  async function handleRemove(symbol: string) {
    setRemovingSymbol(symbol)
    try {
      await removeFromWatchlist(symbol)
      setEntries((current) => (current ? current.filter((entry) => entry.ticker.symbol !== symbol) : current))
    } catch (reason) {
      setError(describeError(reason, 'Could not remove ticker from the watchlist.'))
    } finally {
      setRemovingSymbol(null)
    }
  }

  return (
    <section>
      <h2>Watchlist</h2>
      {error && <p role="alert">{error}</p>}
      {entries === null && !error && <p>Loading…</p>}
      {entries !== null && entries.length === 0 && <p>No saved tickers yet. Look one up and add it below.</p>}
      {entries !== null && entries.length > 0 && (
        <ul className="watchlist-list">
          {entries.map((entry) => (
            <li key={entry.id} className="watchlist-item">
              <button type="button" className="watchlist-item__symbol" onClick={() => onSelect(entry.ticker.symbol)}>
                {entry.ticker.symbol}
              </button>
              <span className="watchlist-item__asset-type">{entry.ticker.assetType}</span>
              <button
                type="button"
                onClick={() => handleRemove(entry.ticker.symbol)}
                disabled={removingSymbol === entry.ticker.symbol}
              >
                {removingSymbol === entry.ticker.symbol ? 'Removing…' : 'Remove'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export default Watchlist
