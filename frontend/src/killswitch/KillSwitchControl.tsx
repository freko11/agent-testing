import { useEffect, useRef, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import {
  clearKillSwitch,
  engageKillSwitch,
  fetchKillSwitchState,
  type KillSwitchCancelSummary,
  type KillSwitchResponse,
} from './api'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function describeSummary(summary: KillSwitchCancelSummary): string {
  if (summary.attempted === 0) {
    return 'No open orders to cancel.'
  }
  const base = `Cancelled ${summary.cancelled} of ${summary.attempted} open orders.`
  return summary.failed > 0 ? `${base} ${summary.failed} failed — check order history.` : base
}

/**
 * The global kill switch (E6-F2-S2) — a single control that immediately blocks new order
 * submissions and best-effort cancels every open order on both adapters. Engaging is
 * deliberately one click with no confirmation dialog, unlike TradingModeBanner's LIVE-mode
 * consent flow — a confirm step would defeat "stop everything instantly". Clearing (which
 * re-opens live risk) does use the confirm-dialog idiom, mirroring TradeForm/TradingModeBanner.
 */
function KillSwitchControl() {
  const [state, setState] = useState<KillSwitchResponse | null>(null)
  const [cancelSummary, setCancelSummary] = useState<KillSwitchCancelSummary | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmingClear, setConfirmingClear] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    fetchKillSwitchState()
      .then((result) => setState(result))
      .catch((reason) => setError(describeError(reason, 'Could not load the kill switch state.')))
  }, [])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (confirmingClear) {
      if (!dialog.open) dialog.showModal()
    } else if (dialog.open) {
      dialog.close()
    }
  }, [confirmingClear])

  async function handleEngage() {
    setBusy(true)
    setError(null)
    try {
      const result = await engageKillSwitch()
      setState(result.killSwitch)
      setCancelSummary(result.cancelSummary)
    } catch (reason) {
      setError(describeError(reason, 'Could not engage the kill switch.'))
    } finally {
      setBusy(false)
    }
  }

  function handleRequestClear() {
    setError(null)
    setConfirmingClear(true)
  }

  function handleCancelClear() {
    setConfirmingClear(false)
  }

  async function handleConfirmClear() {
    setBusy(true)
    setError(null)
    try {
      const result = await clearKillSwitch()
      setState(result)
      setCancelSummary(null)
      setConfirmingClear(false)
    } catch (reason) {
      setError(describeError(reason, 'Could not clear the kill switch.'))
    } finally {
      setBusy(false)
    }
  }

  if (!state) {
    return error ? (
      <p role="alert" className="kill-switch__error">
        {error}
      </p>
    ) : null
  }

  const engaged = state.state === 'ENGAGED'

  return (
    <div className={`kill-switch kill-switch--${state.state.toLowerCase()}`} role="status">
      {engaged ? (
        <>
          <span className="kill-switch__status">Kill switch ENGAGED — new trades are blocked.</span>
          {state.changedBy && (
            <span className="kill-switch__since">
              By {state.changedBy}
              {state.changedAt ? ` at ${new Date(state.changedAt).toLocaleString()}` : ''}
            </span>
          )}
          {cancelSummary && <span className="kill-switch__summary">{describeSummary(cancelSummary)}</span>}
          <button type="button" onClick={handleRequestClear} disabled={busy}>
            Clear kill switch
          </button>
        </>
      ) : (
        <button type="button" className="kill-switch__engage" onClick={handleEngage} disabled={busy}>
          {busy ? 'Engaging…' : 'Kill switch'}
        </button>
      )}
      {error && <p className="kill-switch__error" role="alert">{error}</p>}

      <dialog ref={dialogRef} className="trade-confirm-dialog" onCancel={handleCancelClear}>
        {confirmingClear && (
          <div className="trade-confirm-dialog__content">
            <h4>Clear the kill switch?</h4>
            <p>
              New order submissions will be unblocked immediately. Only clear this once you've reviewed the
              situation that caused you to engage it.
            </p>
            <div className="trade-confirm-dialog__actions">
              <button type="button" onClick={handleCancelClear}>
                Cancel
              </button>
              <button type="button" onClick={handleConfirmClear} disabled={busy} autoFocus>
                {busy ? 'Clearing…' : 'Clear kill switch'}
              </button>
            </div>
          </div>
        )}
      </dialog>
    </div>
  )
}

export default KillSwitchControl
