import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type Broker = 'ALPACA' | 'BINANCE'
export type OrderSide = 'BUY' | 'SELL'

export type OrderStatus =
  | 'PENDING'
  | 'SUBMITTED'
  | 'FILLED'
  | 'PARTIALLY_FILLED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'FAILED'
  | 'PARTIALLY_PROTECTED'
  | 'SUBMISSION_UNKNOWN'

/** Numeric payload for POST /api/tickers/{symbol}/orders — the caller converts form-string values to numbers before sending, so JSON numbers (not numeric strings) hit the backend's BigDecimal fields. */
export interface PlaceOrderPayload {
  amountUsd: number
  leverage: number
  takeProfitPrice: number
  stopLossPrice: number
}

/**
 * Always a successful (2xx) response shape — a business rejection, a
 * partially-protected fill, and an infrastructure failure are all normal
 * values of `status`/`rejectionReason` here, never a thrown MarketDataError.
 * Only pre-flight failures (bad ticker, no actionable signal, invalid
 * request, no credential configured) throw, via parseMarketDataError like
 * every other endpoint.
 */
export interface TradeOrderResponse {
  orderId: number
  clientOrderId: string
  brokerOrderId: string | null
  broker: Broker
  side: OrderSide
  quantity: string
  status: OrderStatus
  filledPrice: string | null
  rejectionReason: string | null
  submittedAt: string | null
}

export async function placeOrder(symbol: string, payload: PlaceOrderPayload): Promise<TradeOrderResponse> {
  const response = await apiFetch(`/api/tickers/${encodeURIComponent(symbol)}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as TradeOrderResponse
}
