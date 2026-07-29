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
