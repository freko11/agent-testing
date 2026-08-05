import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { fetchKillSwitchState } from '../killswitch/api'
import { MarketDataError } from '../marketdata/api'
import type { SignalResponse } from '../signal/api'
import { placeOrder, type PlaceOrderPayload, type TradeOrderResponse } from './api'
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
  | { kind: 'confirming'; payload: PlaceOrderPayload }
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
  onOrderPlaced?: () => void
}

/**
 * Amount/leverage/take-profit/stop-loss input, validated against broker limits before
 * "Trade" is enabled (E5-F1-S1). Leverage is only shown for crypto, bounded to
 * 1x-MAX_CRYPTO_LEVERAGE; stock orders hide the field entirely and stay at the
 * hardcoded 1x default (E5-F1-S2). Only rendered for a BUY/SELL call — a HOLD has no
 * direction to size an entry for. Submitting a valid form opens a confirmation dialog
 * with the order summary (E5-F2-S2) rather than firing the order immediately — only
 * the dialog's own "Confirm trade" button calls placeOrder; Cancel or dismissing the
 * dialog (Esc) returns to the form with no API call. The backend always re-derives
 * direction/price from a fresh signal computation rather than trusting this form's
 * (possibly stale) snapshot, so a submission can still fail with SIGNAL_NOT_ACTIONABLE
 * if the call flipped to HOLD between lookup and confirm.
 */
function TradeForm({ signal, onOrderPlaced }: TradeFormProps) {
  const [values, setValues] = useState<TradeFormValues>(DEFAULT_VALUES)
  const [submitState, setSubmitState] = useState<SubmitState>({ kind: 'idle' })
  const [killSwitchEngaged, setKillSwitchEngaged] = useState(false)
  // A field's error is only worth showing once the user has actually interacted with it (blur)
  // or tried to submit — otherwise every field starts in a red "Enter an amount greater than 0"
  // state before anyone's typed anything, which reads as broken rather than helpful.
  const [touched, setTouched] = useState<Partial<Record<keyof TradeFormValues, boolean>>>({})
  const [submitAttempted, setSubmitAttempted] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)

  const { call, ticker, indicators } = signal

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (submitState.kind === 'confirming') {
      if (!dialog.open) dialog.showModal()
    } else if (dialog.open) {
      dialog.close()
    }
  }, [submitState.kind])

  // Independent fetch on mount, same pattern TradingModeBanner uses for its own state — this
  // component remounts per looked-up ticker (see TickerMetrics' `key`), so it reflects
  // whatever the kill switch's state is at the time of each lookup. Purely a proactive UX
  // disable; the backend's own KILL_SWITCH_ENGAGED 403 is still the real enforcement boundary.
  useEffect(() => {
    fetchKillSwitchState()
      .then((state) => setKillSwitchEngaged(state.state === 'ENGAGED'))
      .catch(() => {
        // Best-effort UX hint only — leave the form enabled and let a real submit attempt
        // surface the backend's authoritative 403 if the kill switch turns out to be engaged.
      })
  }, [])

  if (call === 'HOLD') return null

  const errors = validateTradeForm(values, ticker.assetType, call, indicators.price)
  const isValid = Object.keys(errors).length === 0

  function updateField(field: keyof TradeFormValues) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setSubmitState({ kind: 'idle' })
      setValues((current) => ({ ...current, [field]: event.target.value }))
    }
  }

  function blurField(field: keyof TradeFormValues) {
    return () => setTouched((current) => ({ ...current, [field]: true }))
  }

  function shouldShowError(field: keyof TradeFormValues): boolean {
    return Boolean(errors[field]) && (touched[field] === true || submitAttempted)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitAttempted(true)
    if (!isValid) return

    setSubmitState({
      kind: 'confirming',
      payload: {
        amountUsd: Number(values.amountUsd),
        leverage: Number(values.leverage),
        takeProfitPrice: Number(values.takeProfitPrice),
        stopLossPrice: Number(values.stopLossPrice),
      },
    })
  }

  function handleCancelConfirm() {
    setSubmitState({ kind: 'idle' })
  }

  async function handleConfirm() {
    if (submitState.kind !== 'confirming') return
    const { payload } = submitState

    setSubmitState({ kind: 'submitting' })
    try {
      const response = await placeOrder(ticker.symbol, payload)
      setSubmitState({ kind: 'result', response })
      onOrderPlaced?.()
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
        <input
          inputMode="decimal"
          value={values.amountUsd}
          onChange={updateField('amountUsd')}
          onBlur={blurField('amountUsd')}
        />
      </label>
      {shouldShowError('amountUsd') && (
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
              onBlur={blurField('leverage')}
            />
          </label>
          {shouldShowError('leverage') && (
            <p className="trade-form__error" role="alert">
              {errors.leverage}
            </p>
          )}
        </>
      )}

      <label>
        Take-profit price
        <input
          inputMode="decimal"
          value={values.takeProfitPrice}
          onChange={updateField('takeProfitPrice')}
          onBlur={blurField('takeProfitPrice')}
        />
      </label>
      {shouldShowError('takeProfitPrice') && (
        <p className="trade-form__error" role="alert">
          {errors.takeProfitPrice}
        </p>
      )}

      <label>
        Stop-loss price
        <input
          inputMode="decimal"
          value={values.stopLossPrice}
          onChange={updateField('stopLossPrice')}
          onBlur={blurField('stopLossPrice')}
        />
      </label>
      {shouldShowError('stopLossPrice') && (
        <p className="trade-form__error" role="alert">
          {errors.stopLossPrice}
        </p>
      )}

      {killSwitchEngaged && (
        <p className="trade-form__error" role="alert">
          The kill switch is engaged — new trades are blocked until it's cleared.
        </p>
      )}
      <button
        type="submit"
        disabled={!isValid || killSwitchEngaged || submitState.kind === 'submitting' || submitState.kind === 'confirming'}
      >
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

      <dialog ref={dialogRef} className="trade-confirm-dialog" onCancel={handleCancelConfirm}>
        {submitState.kind === 'confirming' && (
          <div className="trade-confirm-dialog__content">
            <h4>
              Confirm {call} order — {ticker.symbol}
            </h4>
            <dl className="trade-confirm-dialog__summary">
              <dt>Amount</dt>
              <dd>${submitState.payload.amountUsd}</dd>
              {ticker.assetType === 'CRYPTO' && (
                <>
                  <dt>Leverage</dt>
                  <dd>{submitState.payload.leverage}x</dd>
                </>
              )}
              <dt>Take-profit</dt>
              <dd>{submitState.payload.takeProfitPrice}</dd>
              <dt>Stop-loss</dt>
              <dd>{submitState.payload.stopLossPrice}</dd>
            </dl>
            <div className="trade-confirm-dialog__actions">
              <button type="button" onClick={handleCancelConfirm}>
                Cancel
              </button>
              <button type="button" onClick={handleConfirm} autoFocus>
                Confirm trade
              </button>
            </div>
          </div>
        )}
      </dialog>
    </form>
  )
}

export default TradeForm
