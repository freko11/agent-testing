import type { TickerSummary } from '../marketdata/api'

export type TradeDirection = 'BUY' | 'SELL'

/**
 * Mirrors BinanceFuturesTradingAdapter.MAX_LEVERAGE (backend's real adapter-enforced
 * ceiling) — the crypto-side broker limit this form validates against. Stock orders are
 * capped at 1x by this app's own DB check constraint (no leveraged stock orders), enforced
 * below rather than mirrored as a separate constant since there's only one valid value.
 */
export const MAX_CRYPTO_LEVERAGE = 20

export interface TradeFormValues {
  amountUsd: string
  leverage: string
  takeProfitPrice: string
  stopLossPrice: string
}

export interface TradeFormErrors {
  amountUsd?: string
  leverage?: string
  takeProfitPrice?: string
  stopLossPrice?: string
}

/**
 * Pure numeric-range validation for the trade input form (E5-F1-S1). Asset-type-aware
 * field visibility (hiding leverage entirely for stocks) is E5-F1-S2's job — this only
 * decides whether the values entered are within broker-enforceable bounds.
 */
export function validateTradeForm(
  values: TradeFormValues,
  assetType: TickerSummary['assetType'],
  direction: TradeDirection,
  currentPrice: number,
): TradeFormErrors {
  const errors: TradeFormErrors = {}

  const amount = Number(values.amountUsd)
  if (!values.amountUsd.trim() || !Number.isFinite(amount) || amount <= 0) {
    errors.amountUsd = 'Enter an amount greater than 0.'
  }

  const leverage = Number(values.leverage)
  if (!values.leverage.trim() || !Number.isInteger(leverage)) {
    errors.leverage = 'Leverage must be a whole number.'
  } else if (assetType === 'STOCK' && leverage !== 1) {
    errors.leverage = 'Stock orders cannot use leverage (must be 1x).'
  } else if (assetType === 'CRYPTO' && (leverage < 1 || leverage > MAX_CRYPTO_LEVERAGE)) {
    errors.leverage = `Leverage must be between 1x and ${MAX_CRYPTO_LEVERAGE}x.`
  }

  const takeProfit = Number(values.takeProfitPrice)
  if (!values.takeProfitPrice.trim() || !Number.isFinite(takeProfit) || takeProfit <= 0) {
    errors.takeProfitPrice = 'Enter a take-profit price greater than 0.'
  } else if (direction === 'BUY' && takeProfit <= currentPrice) {
    errors.takeProfitPrice = `Take-profit must be above the current price (${currentPrice}).`
  } else if (direction === 'SELL' && takeProfit >= currentPrice) {
    errors.takeProfitPrice = `Take-profit must be below the current price (${currentPrice}).`
  }

  const stopLoss = Number(values.stopLossPrice)
  if (!values.stopLossPrice.trim() || !Number.isFinite(stopLoss) || stopLoss <= 0) {
    errors.stopLossPrice = 'Enter a stop-loss price greater than 0.'
  } else if (direction === 'BUY' && stopLoss >= currentPrice) {
    errors.stopLossPrice = `Stop-loss must be below the current price (${currentPrice}).`
  } else if (direction === 'SELL' && stopLoss <= currentPrice) {
    errors.stopLossPrice = `Stop-loss must be above the current price (${currentPrice}).`
  }

  return errors
}
