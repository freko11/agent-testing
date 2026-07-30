import { apiFetch } from '../auth/api'

export interface Candle {
  timestamp: string
  open: string
  high: string
  low: string
  close: string
  volume: string
}

export interface TickerSummary {
  id: number
  symbol: string
  assetType: 'STOCK' | 'CRYPTO'
  exchange: string | null
}

export interface PriceHistoryResponse {
  ticker: TickerSummary
  source: string
  candles: Candle[]
}

export type MarketDataErrorCode =
  | 'TICKER_NOT_REGISTERED'
  | 'ASSET_TYPE_CONFLICT'
  | 'NO_PRICE_DATA'
  | 'MARKET_CLOSED'
  | 'INVALID_REQUEST'
  | 'MARKET_DATA_RATE_LIMITED'
  | 'MARKET_DATA_UNAVAILABLE'
  | 'INSUFFICIENT_PRICE_HISTORY'
  | 'SIGNAL_NOT_ACTIONABLE'
  | 'BROKER_CREDENTIAL_NOT_CONFIGURED'
  | 'ORDER_NOT_FOUND'
  | 'ORDER_REFRESH_UNAVAILABLE'
  | 'LIVE_MODE_NOT_YET_AVAILABLE'

export class MarketDataError extends Error {
  code: MarketDataErrorCode | 'UNKNOWN'

  constructor(code: MarketDataErrorCode | 'UNKNOWN', message: string) {
    super(message)
    this.name = 'MarketDataError'
    this.code = code
  }
}

/**
 * Parses the structured error body MarketDataExceptionHandler (and, as of
 * E3-F3-S1, WatchlistController) produce on a non-2xx response, so callers
 * across marketdata/signal/chart/watchlist all branch on the same
 * MarketDataErrorCode instead of each re-parsing the response body.
 */
export async function parseMarketDataError(response: Response): Promise<MarketDataError> {
  let code: MarketDataErrorCode | 'UNKNOWN' = 'UNKNOWN'
  let message = `Request failed with status ${response.status}`
  try {
    const body = (await response.json()) as { error?: string; message?: string }
    if (body.error) code = body.error as MarketDataErrorCode
    if (body.message) message = body.message
  } catch {
    // Response body wasn't JSON (e.g. a network-level failure) — fall back to the defaults above.
  }
  return new MarketDataError(code, message)
}

/**
 * Throws MarketDataError with the backend's structured error code (see
 * MarketDataExceptionHandler) so callers can render a specific message per
 * failure kind instead of a single generic "something went wrong" — the
 * whole point of E2-F1-S2.
 */
export async function fetchPriceHistory(symbol: string): Promise<PriceHistoryResponse> {
  const response = await apiFetch(`/api/tickers/${encodeURIComponent(symbol)}/price-history`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as PriceHistoryResponse
}
