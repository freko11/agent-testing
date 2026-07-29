import { useState, type ChangeEvent, type FormEvent } from 'react'
import type { SignalResponse } from '../signal/api'
import { MAX_CRYPTO_LEVERAGE, validateTradeForm, type TradeFormValues } from './validation'

const DEFAULT_VALUES: TradeFormValues = {
  amountUsd: '',
  leverage: '1',
  takeProfitPrice: '',
  stopLossPrice: '',
}

interface TradeFormProps {
  signal: SignalResponse
}

/**
 * Amount/leverage/take-profit/stop-loss input, validated against broker limits before
 * "Trade" is enabled (E5-F1-S1). Leverage is only shown for crypto, bounded to
 * 1x-MAX_CRYPTO_LEVERAGE; stock orders hide the field entirely and stay at the
 * hardcoded 1x default (E5-F1-S2). Only rendered for a BUY/SELL call — a HOLD has no
 * direction to size an entry for. Submitting doesn't call a broker yet: bracket-order
 * construction and adapter routing are E5-F2-S1's scope, so this only proves the
 * validated payload is ready to hand off once that wiring lands.
 */
function TradeForm({ signal }: TradeFormProps) {
  const [values, setValues] = useState<TradeFormValues>(DEFAULT_VALUES)
  const [submitted, setSubmitted] = useState(false)

  const { call, ticker, indicators } = signal

  if (call === 'HOLD') return null

  const errors = validateTradeForm(values, ticker.assetType, call, indicators.price)
  const isValid = Object.keys(errors).length === 0

  function updateField(field: keyof TradeFormValues) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setSubmitted(false)
      setValues((current) => ({ ...current, [field]: event.target.value }))
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!isValid) return
    setSubmitted(true)
  }

  return (
    <form className="trade-form" onSubmit={handleSubmit}>
      <h3>
        Trade {ticker.symbol} ({call})
      </h3>
      <label>
        Amount (USD)
        <input inputMode="decimal" value={values.amountUsd} onChange={updateField('amountUsd')} />
      </label>
      {errors.amountUsd && (
        <p className="trade-form__error" role="alert">
          {errors.amountUsd}
        </p>
      )}

      {ticker.assetType === 'CRYPTO' && (
        <>
          <label>
            Leverage (1x-{MAX_CRYPTO_LEVERAGE}x)
            <input
              type="number"
              min={1}
              max={MAX_CRYPTO_LEVERAGE}
              step={1}
              value={values.leverage}
              onChange={updateField('leverage')}
            />
          </label>
          {errors.leverage && (
            <p className="trade-form__error" role="alert">
              {errors.leverage}
            </p>
          )}
        </>
      )}

      <label>
        Take-profit price
        <input inputMode="decimal" value={values.takeProfitPrice} onChange={updateField('takeProfitPrice')} />
      </label>
      {errors.takeProfitPrice && (
        <p className="trade-form__error" role="alert">
          {errors.takeProfitPrice}
        </p>
      )}

      <label>
        Stop-loss price
        <input inputMode="decimal" value={values.stopLossPrice} onChange={updateField('stopLossPrice')} />
      </label>
      {errors.stopLossPrice && (
        <p className="trade-form__error" role="alert">
          {errors.stopLossPrice}
        </p>
      )}

      <button type="submit" disabled={!isValid}>
        Trade
      </button>
      {submitted && (
        <p className="trade-form__note">Order details captured — submitting this to the broker lands in E5-F2-S1.</p>
      )}
    </form>
  )
}

export default TradeForm
