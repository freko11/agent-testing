import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'
import type { OrderSide, OrderStatus } from '../trade/api'

export type SignalCall = 'BUY' | 'SELL' | 'HOLD'

export type SignalRuleId =
  | 'NO_VOLUME_DATA'
  | 'VOLUME_DRIED_UP'
  | 'VOLATILITY_TOO_EXTREME'
  | 'BULLISH_UNANIMOUS'
  | 'BULLISH_MAJORITY'
  | 'BEARISH_UNANIMOUS'
  | 'BEARISH_MAJORITY'
  | 'CONFLICTING_SIGNALS'
  | 'NO_STRONG_SIGNAL'

/** A single audit-trail row (E6-F3-S3) — mirrors AuditEntryResponse field-for-field. */
export interface AuditEntry {
  id: number
  tickerSymbol: string
  assetType: 'STOCK' | 'CRYPTO'
  side: OrderSide
  call: SignalCall
  matchedRule: SignalRuleId
  matchedRuleRationale: string
  ruleTableVersion: string
  holdTermMinDays: number | null
  holdTermMaxDays: number | null
  resultStatus: OrderStatus
  rejectionReason: string | null
  entryPrice: string | null
  loggedAt: string
}

export interface AuditEntryPage {
  content: AuditEntry[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function fetchAuditEntries(page = 0, size = 25): Promise<AuditEntryPage> {
  const response = await apiFetch(`/api/orders/audit-entries?page=${page}&size=${size}`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as AuditEntryPage
}
