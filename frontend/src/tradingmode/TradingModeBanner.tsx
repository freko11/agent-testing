import { useEffect, useRef, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import {
  fetchTradingMode,
  giveRiskConsent,
  switchTradingMode,
  type TradingMode,
  type TradingModeState,
} from './api'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function otherMode(mode: TradingMode): TradingMode {
  return mode === 'PAPER' ? 'LIVE' : 'PAPER'
}

/**
 * Persistent global paper/live mode banner (E6-F1-S1) — the most prominent
 * position on the dashboard, above even the page title, since it reflects an
 * app-wide state that affects every trade. Switching to LIVE is gated behind
 * two independent checks: the paper-trade threshold (E6-F1-S2) proactively
 * disables the button with an explanation, and the one-time risk-consent
 * acknowledgment (E6-F1-S3) — once the threshold is met — opens a disclaimer
 * dialog instead of switching immediately, mirroring TradeForm's confirm-dialog
 * pattern. Consent, once given, is recorded server-side and never asked again.
 * The backend's own 403 message is still shown as a defense-in-depth fallback
 * (e.g. stale client state), but is no longer the primary UX for either gate.
 */
function TradingModeBanner() {
  const [state, setState] = useState<TradingModeState | null>(null)
  const [switching, setSwitching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmingConsent, setConfirmingConsent] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    fetchTradingMode()
      .then((result) => setState(result))
      .catch((reason) => setError(describeError(reason, 'Could not load the current trading mode.')))
  }, [])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (confirmingConsent) {
      if (!dialog.open) dialog.showModal()
    } else if (dialog.open) {
      dialog.close()
    }
  }, [confirmingConsent])

  async function switchToLive() {
    setSwitching(true)
    setError(null)
    try {
      const result = await switchTradingMode('LIVE')
      setState(result)
    } catch (reason) {
      setError(describeError(reason, 'Could not switch to LIVE mode.'))
    } finally {
      setSwitching(false)
    }
  }

  async function handleToggle() {
    if (!state) return
    const target = otherMode(state.mode)
    if (target === 'LIVE' && !state.riskConsentGiven) {
      setError(null)
      setConfirmingConsent(true)
      return
    }
    if (target === 'LIVE') {
      await switchToLive()
      return
    }
    setSwitching(true)
    setError(null)
    try {
      const result = await switchTradingMode(target)
      setState(result)
    } catch (reason) {
      setError(describeError(reason, `Could not switch to ${target} mode.`))
    } finally {
      setSwitching(false)
    }
  }

  function handleCancelConsent() {
    setConfirmingConsent(false)
  }

  async function handleConfirmConsent() {
    setSwitching(true)
    setError(null)
    try {
      const consented = await giveRiskConsent()
      setState(consented)
      setConfirmingConsent(false)
      await switchToLive()
    } catch (reason) {
      setError(describeError(reason, 'Could not record risk-consent acknowledgment.'))
      setSwitching(false)
    }
  }

  if (!state) {
    return error ? (
      <p role="alert" className="trading-mode-banner__error">
        {error}
      </p>
    ) : null
  }

  const target = otherMode(state.mode)
  const blockedByThreshold = target === 'LIVE' && !state.paperTradeThresholdMet

  return (
    <div className={`trading-mode-banner trading-mode-banner--${state.mode.toLowerCase()}`} role="status">
      <span className="trading-mode-banner__mode">{state.mode} mode</span>
      <span className="trading-mode-banner__since">
        {state.changedAt ? `Since ${new Date(state.changedAt).toLocaleString()}` : 'Default — never changed'}
      </span>
      <button type="button" onClick={handleToggle} disabled={switching || blockedByThreshold}>
        {switching ? 'Switching…' : `Switch to ${target}`}
      </button>
      {blockedByThreshold && (
        <span className="trading-mode-banner__threshold">
          Live mode unlocks after {state.paperTradeThreshold} successful paper trades (
          {state.successfulPaperTrades} completed).
        </span>
      )}
      {error && <p className="trading-mode-banner__error">{error}</p>}

      <dialog ref={dialogRef} className="trade-confirm-dialog" onCancel={handleCancelConsent}>
        {confirmingConsent && (
          <div className="trade-confirm-dialog__content">
            <h4>Switch to LIVE mode?</h4>
            <p>
              LIVE mode places real orders with real money through your connected broker accounts. Bracket
              orders, leverage, and stop-loss/take-profit levels behave the same as in paper mode, but fills,
              losses, and fees are real and irreversible. Make sure you understand the risks before continuing.
            </p>
            <div className="trade-confirm-dialog__actions">
              <button type="button" onClick={handleCancelConsent}>
                Cancel
              </button>
              <button type="button" onClick={handleConfirmConsent} disabled={switching} autoFocus>
                {switching ? 'Switching…' : 'I understand and accept the risk'}
              </button>
            </div>
          </div>
        )}
      </dialog>
    </div>
  )
}

export default TradingModeBanner
