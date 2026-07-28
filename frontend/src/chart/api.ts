import { apiFetch } from '../auth/api'
import { parseMarketDataError, type Candle, type TickerSummary } from '../marketdata/api'

export type Broker = 'ALPACA' | 'BINANCE'

/** One walk-forward RSI/MA point, aligned by its own timestamp to the candle at the same day. */
export interface ChartIndicatorPoint {
  timestamp: string
  rsi: number
  maShort: number
  maLong: number
}

export interface ChartDataResponse {
  ticker: TickerSummary
  source: Broker
  candles: Candle[]
  indicators: ChartIndicatorPoint[]
}

/**
 * Same structured-error contract as fetchSignal/fetchPriceHistory (MarketDataExceptionHandler
 * backs all three endpoints) — reuses the shared parseMarketDataError helper rather than
 * duplicating the error-parsing branch. Unlike fetchSignal, this can never throw
 * INSUFFICIENT_PRICE_HISTORY — chart-data returns candles with an empty indicator series instead.
 */
export async function fetchChartData(symbol: string, limit = 200): Promise<ChartDataResponse> {
  const response = await apiFetch(`/api/tickers/${encodeURIComponent(symbol)}/chart-data?limit=${limit}`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as ChartDataResponse
}
