import { useEffect, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchTradingMode, switchTradingMode, type TradingMode, type TradingModeState } from './api'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function otherMode(mode: TradingMode): TradingMode {
  return mode === 'PAPER' ? 'LIVE' : 'PAPER'
}

/**
 * Persistent global paper/live mode banner (E6-F1-S1) — the most prominent
 * position on the dashboard, above even the page title, since it reflects an
 * app-wide state that affects every trade. Switching to LIVE is expected to
 * fail today (403 LIVE_MODE_NOT_YET_AVAILABLE, per TradingModeService's
 * temporary guard) — that failure is rendered via the backend's own message,
 * with no client-side special-casing, so this component needs no changes
 * once E6-F1-S2/S3 replace the guard with a real threshold/consent check.
 */
function TradingModeBanner() {
  const [state, setState] = useState<TradingModeState | null>(null)
  const [switching, setSwitching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchTradingMode()
      .then((result) => setState(result))
      .catch((reason) => setError(describeError(reason, 'Could not load the current trading mode.')))
  }, [])

  async function handleToggle() {
    if (!state) return
    const target = otherMode(state.mode)
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

  if (!state) {
    return error ? (
      <p role="alert" className="trading-mode-banner__error">
        {error}
      </p>
    ) : null
  }

  return (
    <div className={`trading-mode-banner trading-mode-banner--${state.mode.toLowerCase()}`} role="status">
      <span className="trading-mode-banner__mode">{state.mode} mode</span>
      <span className="trading-mode-banner__since">
        {state.changedAt ? `Since ${new Date(state.changedAt).toLocaleString()}` : 'Default — never changed'}
      </span>
      <button type="button" onClick={handleToggle} disabled={switching}>
        {switching ? 'Switching…' : `Switch to ${otherMode(state.mode)}`}
      </button>
      {error && <p className="trading-mode-banner__error">{error}</p>}
    </div>
  )
}

export default TradingModeBanner
