import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'
import type { Broker, OrderSide, OrderStatus } from '../trade/api'

export type EntryOrderType = 'MARKET' | 'LIMIT'
export type TradingMode = 'PAPER' | 'LIVE'

/** A single order row for the status/history view (E5-F3-S1) — mirrors OrderResponse field-for-field. */
export interface OrderSummary {
  id: number
  tickerSymbol: string
  assetType: 'STOCK' | 'CRYPTO'
  broker: Broker
  orderMode: TradingMode
  side: OrderSide
  quantity: string
  requestedAmountUsd: string | null
  leverage: string
  entryOrderType: EntryOrderType
  takeProfitPrice: string
  stopLossPrice: string
  entryPrice: string | null
  clientOrderId: string
  brokerOrderId: string | null
  status: OrderStatus
  rejectionReason: string | null
  submittedAt: string | null
  filledAt: string | null
  createdAt: string
  updatedAt: string
}

export async function fetchOrders(limit = 50): Promise<OrderSummary[]> {
  const response = await apiFetch(`/api/orders?limit=${limit}`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as OrderSummary[]
}

/** Manually re-polls the broker for one order's current status (no automatic/background polling exists — E5-F3-S1). */
export async function refreshOrder(id: number): Promise<OrderSummary> {
  const response = await apiFetch(`/api/orders/${id}/refresh`, { method: 'POST' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as OrderSummary
}

/** Parsed out of the `Content-Disposition` header the backend always sets on a successful export — a plain
 * regex, not a full RFC 6266 parser, since this app only ever generates its own simple `filename="..."` form. */
function filenameFromContentDisposition(header: string | null, fallback: string): string {
  const match = header?.match(/filename="([^"]+)"/)
  return match ? match[1] : fallback
}

/** Fetches the CSV export for a date range (E5-F3-S2) as a Blob rather than navigating to the endpoint directly —
 * a plain link navigation to a failing request (e.g. start-after-end) would land on raw JSON with no way to reuse
 * this app's existing typed MarketDataError handling. */
export async function exportOrdersCsv(start: string, end: string): Promise<{ filename: string; blob: Blob }> {
  const params = new URLSearchParams({ start, end })
  const response = await apiFetch(`/api/orders/export?${params}`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  const filename = filenameFromContentDisposition(
    response.headers.get('Content-Disposition'),
    `trade-history-${start}-to-${end}.csv`,
  )
  return { filename, blob: await response.blob() }
}
