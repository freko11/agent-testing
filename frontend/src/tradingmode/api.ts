import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type TradingMode = 'PAPER' | 'LIVE'

/** The global paper/live switch's current state (E6-F1-S1) — mirrors TradingModeResponse field-for-field. */
export interface TradingModeState {
  mode: TradingMode
  changedAt: string | null
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
