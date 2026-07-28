import { apiFetch } from '../auth/api'
import { parseMarketDataError, type TickerSummary } from '../marketdata/api'

export interface WatchlistEntry {
  id: number
  ticker: TickerSummary
  addedAt: string
}

/** Same structured-error contract as the other endpoints — see parseMarketDataError. */
export async function fetchWatchlist(): Promise<WatchlistEntry[]> {
  const response = await apiFetch('/api/watchlist')

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as WatchlistEntry[]
}

/** Idempotent — adding an already-watchlisted symbol just returns the existing entry (200, not 201). */
export async function addToWatchlist(symbol: string): Promise<WatchlistEntry> {
  const response = await apiFetch('/api/watchlist', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol }),
  })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as WatchlistEntry
}

export async function removeFromWatchlist(symbol: string): Promise<void> {
  const response = await apiFetch(`/api/watchlist/${encodeURIComponent(symbol)}`, { method: 'DELETE' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }
}
