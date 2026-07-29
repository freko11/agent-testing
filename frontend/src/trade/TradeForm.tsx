import { useState, type ChangeEvent, type FormEvent } from 'react'
import { MarketDataError } from '../marketdata/api'
import type { SignalResponse } from '../signal/api'
import { placeOrder, type TradeOrderResponse } from './api'
import { MAX_CRYPTO_LEVERAGE, validateTradeForm, type TradeFormValues } from './validation'

const DEFAULT_VALUES: TradeFormValues = {
  amountUsd: '',
  leverage: '1',
  takeProfitPrice: '',
  stopLossPrice: '',
}

const SUBMIT_ERROR_MESSAGES: Partial<Record<string, string>> = {
  SIGNAL_NOT_ACTIONABLE: 'The signal changed to HOLD before this order could be submitted. Look the ticker up again to see the current call.',
  BROKER_CREDENTIAL_NOT_CONFIGURED: 'No broker credentials are configured for this asset type yet — the order was not submitted.',
  MARKET_CLOSED: 'The market closed before this order could be submitted.',
}

type SubmitState =
  | { kind: 'idle' }
  | { kind: 'submitting' }
  | { kind: 'result'; response: TradeOrderResponse }
  | { kind: 'error'; message: string }

type ResultTone = 'success' | 'warning' | 'error'

function describeResult(response: TradeOrderResponse): { tone: ResultTone; text: string } {
  switch (response.status) {
    case 'FILLED':
    case 'PARTIALLY_FILLED':
      return {
        tone: 'success',
        text: `Order filled at ${response.filledPrice ?? '—'}. Broker order ID: ${response.brokerOrderId ?? '—'}.`,
      }
    case 'PARTIALLY_PROTECTED':
      return {
        tone: 'warning',
        text: `Position opened, but not fully protected: ${response.rejectionReason ?? 'a take-profit or stop-loss leg is missing.'} Check the broker's account view.`,
      }
    case 'SUBMISSION_UNKNOWN':
      return {
        tone: 'warning',
        text: `Status unknown — do not resubmit. ${response.rejectionReason ?? 'Verify manually via the broker before retrying.'}`,
      }
    case 'REJECTED':
    case 'FAILED':
      return {
        tone: 'error',
        text: `Order not placed: ${response.rejectionReason ?? 'the broker rejected the order.'}`,
      }
    default:
      return { tone: 'success', text: `Order submitted (status: ${response.status}).` }
  }
}

interface TradeFormProps {
  signal: SignalResponse
}

/**
 * Amount/leverage/take-profit/stop-loss input, validated against broker limits before
 * "Trade" is enabled (E5-F1-S1). Leverage is only shown for crypto, bounded to
 * 1x-MAX_CRYPTO_LEVERAGE; stock orders hide the field entirely and stay at the
 * hardcoded 1x default (E5-F1-S2). Only rendered for a BUY/SELL call — a HOLD has no
 * direction to size an entry for. Submitting fires the real bracket order immediately
 * (E5-F2-S1) — no confirmation step yet, that's E5-F2-S2's job. The backend always
 * re-derives direction/price from a fresh signal computation rather than trusting this
 * form's (possibly stale) snapshot, so a submission can still fail with
 * SIGNAL_NOT_ACTIONABLE if the call flipped to HOLD between lookup and click.
 */
function TradeForm({ signal }: TradeFormProps) {
  const [values, setValues] = useState<TradeFormValues>(DEFAULT_VALUES)
  const [submitState, setSubmitState] = useState<SubmitState>({ kind: 'idle' })

  const { call, ticker, indicators } = signal

  if (call === 'HOLD') return null

  const errors = validateTradeForm(values, ticker.assetType, call, indicators.price)
  const isValid = Object.keys(errors).length === 0

  function updateField(field: keyof TradeFormValues) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setSubmitState({ kind: 'idle' })
      setValues((current) => ({ ...current, [field]: event.target.value }))
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!isValid) return

    setSubmitState({ kind: 'submitting' })
    try {
      const response = await placeOrder(ticker.symbol, {
        amountUsd: Number(values.amountUsd),
        leverage: Number(values.leverage),
        takeProfitPrice: Number(values.takeProfitPrice),
        stopLossPrice: Number(values.stopLossPrice),
      })
      setSubmitState({ kind: 'result', response })
    } catch (reason) {
      const message =
        reason instanceof MarketDataError
          ? (SUBMIT_ERROR_MESSAGES[reason.code] ?? reason.message)
          : 'Something went wrong submitting the order. Please try again.'
      setSubmitState({ kind: 'error', message })
    }
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

      <button type="submit" disabled={!isValid || submitState.kind === 'submitting'}>
        {submitState.kind === 'submitting' ? 'Submitting…' : 'Trade'}
      </button>
      {submitState.kind === 'result' && (
        <p className={`trade-form__result trade-form__result--${describeResult(submitState.response).tone}`} role="status">
          {describeResult(submitState.response).text}
        </p>
      )}
      {submitState.kind === 'error' && (
        <p className="trade-form__result trade-form__result--error" role="alert">
          {submitState.message}
        </p>
      )}
    </form>
  )
}

export default TradeForm
