import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type TradingMode = 'PAPER' | 'LIVE'

/**
 * The global paper/live switch's current state (E6-F1-S1) plus progress toward the two independent gates
 * that unlock LIVE mode: the paper-trade threshold (E6-F1-S2, `paperTradeThresholdMet`) and the one-time
 * risk-consent acknowledgment (E6-F1-S3, `riskConsentGiven`/`riskConsentGivenAt`). `liveModeUnlocked` is
 * true only once both gates pass. Mirrors TradingModeResponse field-for-field.
 */
export interface TradingModeState {
  mode: TradingMode
  changedAt: string | null
  successfulPaperTrades: number
  paperTradeThreshold: number
  paperTradeThresholdMet: boolean
  riskConsentGiven: boolean
  riskConsentGivenAt: string | null
  liveModeUnlocked: boolean
}

export async function fetchTradingMode(): Promise<TradingModeState> {
  const response = await apiFetch('/api/trading-mode')

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as TradingModeState
}

export async function switchTradingMode(mode: TradingMode): Promise<TradingModeState> {
  const response = await apiFetch('/api/trading-mode', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mode }),
  })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as TradingModeState
}

export async function giveRiskConsent(): Promise<TradingModeState> {
  const response = await apiFetch('/api/trading-mode/risk-consent', { method: 'POST' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as TradingModeState
}
