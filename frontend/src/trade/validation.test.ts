import { describe, expect, it } from 'vitest'
import { MAX_CRYPTO_LEVERAGE, validateTradeForm, type TradeFormValues } from './validation'

function values(overrides: Partial<TradeFormValues> = {}): TradeFormValues {
  return {
    amountUsd: '1000',
    leverage: '1',
    takeProfitPrice: '110',
    stopLossPrice: '90',
    ...overrides,
  }
}

describe('validateTradeForm', () => {
  it('accepts valid BUY values for a stock at 1x leverage', () => {
    expect(validateTradeForm(values(), 'STOCK', 'BUY', 100)).toEqual({})
  })

  it('accepts valid SELL values for crypto with take-profit below and stop-loss above price', () => {
    const result = validateTradeForm(
      values({ leverage: '5', takeProfitPrice: '90', stopLossPrice: '110' }),
      'CRYPTO',
      'SELL',
      100,
    )
    expect(result).toEqual({})
  })

  it('rejects a non-positive or non-numeric amount', () => {
    expect(validateTradeForm(values({ amountUsd: '0' }), 'CRYPTO', 'BUY', 100).amountUsd).toBeDefined()
    expect(validateTradeForm(values({ amountUsd: '-50' }), 'CRYPTO', 'BUY', 100).amountUsd).toBeDefined()
    expect(validateTradeForm(values({ amountUsd: 'abc' }), 'CRYPTO', 'BUY', 100).amountUsd).toBeDefined()
    expect(validateTradeForm(values({ amountUsd: '' }), 'CRYPTO', 'BUY', 100).amountUsd).toBeDefined()
  })

  it('rejects any leverage other than 1x for stocks', () => {
    expect(validateTradeForm(values({ leverage: '2' }), 'STOCK', 'BUY', 100).leverage).toBeDefined()
    expect(validateTradeForm(values({ leverage: '1' }), 'STOCK', 'BUY', 100).leverage).toBeUndefined()
  })

  it('bounds crypto leverage between 1x and the adapter max', () => {
    expect(validateTradeForm(values({ leverage: '0' }), 'CRYPTO', 'BUY', 100).leverage).toBeDefined()
    expect(
      validateTradeForm(values({ leverage: String(MAX_CRYPTO_LEVERAGE + 1) }), 'CRYPTO', 'BUY', 100).leverage,
    ).toBeDefined()
    expect(
      validateTradeForm(values({ leverage: String(MAX_CRYPTO_LEVERAGE) }), 'CRYPTO', 'BUY', 100).leverage,
    ).toBeUndefined()
  })

  it('rejects a fractional leverage value', () => {
    expect(validateTradeForm(values({ leverage: '1.5' }), 'CRYPTO', 'BUY', 100).leverage).toBeDefined()
  })

  it('requires take-profit above price on a BUY and below price on a SELL', () => {
    expect(validateTradeForm(values({ takeProfitPrice: '95' }), 'CRYPTO', 'BUY', 100).takeProfitPrice).toBeDefined()
    expect(validateTradeForm(values({ takeProfitPrice: '105' }), 'CRYPTO', 'SELL', 100).takeProfitPrice).toBeDefined()
  })

  it('requires stop-loss below price on a BUY and above price on a SELL', () => {
    expect(validateTradeForm(values({ stopLossPrice: '105' }), 'CRYPTO', 'BUY', 100).stopLossPrice).toBeDefined()
    expect(validateTradeForm(values({ stopLossPrice: '95' }), 'CRYPTO', 'SELL', 100).stopLossPrice).toBeDefined()
  })

  it('rejects a non-positive or non-numeric take-profit/stop-loss price', () => {
    expect(validateTradeForm(values({ takeProfitPrice: '0' }), 'CRYPTO', 'BUY', 100).takeProfitPrice).toBeDefined()
    expect(validateTradeForm(values({ stopLossPrice: 'abc' }), 'CRYPTO', 'BUY', 100).stopLossPrice).toBeDefined()
  })
})
