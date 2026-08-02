import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type KillSwitchState = 'ENGAGED' | 'CLEARED'

export interface KillSwitchResponse {
  state: KillSwitchState
  changedAt: string | null
  changedBy: string | null
}

export interface KillSwitchCancelSummary {
  attempted: number
  cancelled: number
  failed: number
  failureMessages: string[]
}

export interface EngageKillSwitchResponse {
  killSwitch: KillSwitchResponse
  cancelSummary: KillSwitchCancelSummary
}

export async function fetchKillSwitchState(): Promise<KillSwitchResponse> {
  const response = await apiFetch('/api/kill-switch')

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as KillSwitchResponse
}

export async function engageKillSwitch(): Promise<EngageKillSwitchResponse> {
  const response = await apiFetch('/api/kill-switch/engage', { method: 'POST' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as EngageKillSwitchResponse
}

export async function clearKillSwitch(): Promise<KillSwitchResponse> {
  const response = await apiFetch('/api/kill-switch/clear', { method: 'POST' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as KillSwitchResponse
}
