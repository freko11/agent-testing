import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type SystemAlertType = 'KILL_SWITCH_ENGAGED' | 'SIGNAL_DRIFT_DECAY'

/** Mirrors backend.alert.SystemAlertResponse. @JsonInclude(NON_NULL) on the backend means the
 * SIGNAL_DRIFT_DECAY-only fields are simply absent (not null) on a KILL_SWITCH_ENGAGED row. */
export interface SystemAlertSummary {
  id: number
  alertType: SystemAlertType
  message: string
  ruleTableVersion?: string
  direction?: string
  checkpoint?: 'MIN' | 'MID' | 'MAX'
  driftPct?: number
  createdAt: string
}

/** Plain DB read (no live-broker call), same class of endpoint as KillSwitchControl/
 * TradingModeBanner -- safe to fetch on mount, unlike monitoring/api.ts's two endpoints. */
export async function fetchSystemAlerts(limit = 20): Promise<SystemAlertSummary[]> {
  const response = await apiFetch(`/api/system-alerts?limit=${encodeURIComponent(limit)}`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as SystemAlertSummary[]
}
