import { apiFetch } from '../auth/api'
import { parseMarketDataError, type TickerSummary } from '../marketdata/api'

export type Broker = 'ALPACA' | 'BINANCE'

export type MovingAverageRelation = 'SHORT_ABOVE_LONG' | 'SHORT_BELOW_LONG' | 'EQUAL'

export interface MacdResult {
  line: number
  signal: number
  histogram: number
}

export interface MovingAverageResult {
  shortPeriod: number
  shortMa: number
  longPeriod: number
  longMa: number
  relation: MovingAverageRelation
}

export interface IndicatorResponse {
  ticker: TickerSummary
  source: Broker
  asOf: string
  price: number
  rsi: number
  macd: MacdResult
  movingAverage: MovingAverageResult
  volatility: number | null
  volume: number | null
  volumeTrend: number | null
}

export type SignalCall = 'BUY' | 'SELL' | 'HOLD'

export interface HoldTerm {
  minDays: number
  maxDays: number
  label: string
  rationale: string
  tableVersion: string
}

export interface SignalResponse {
  ticker: TickerSummary
  call: SignalCall
  matchedRule: string
  ruleRationale: string
  ruleTableVersion: string
  holdTerm: HoldTerm | null
  indicators: IndicatorResponse
}

/**
 * Same structured-error contract as fetchPriceHistory (MarketDataExceptionHandler
 * backs both endpoints) — reuses MarketDataError/MarketDataErrorCode from
 * marketdata/api rather than duplicating the error-parsing branch.
 */
export async function fetchSignal(symbol: string): Promise<SignalResponse> {
  const response = await apiFetch(`/api/tickers/${encodeURIComponent(symbol)}/signal`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as SignalResponse
}
