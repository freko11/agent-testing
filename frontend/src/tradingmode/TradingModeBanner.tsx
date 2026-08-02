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
 * app-wide state that affects every trade. The LIVE toggle is proactively
 * disabled with an explanation whenever the paper-trade threshold (E6-F1-S2)
 * isn't yet met — a disabled button can't be driven by a failed click alone,
 * so `TradingModeState` carries the threshold-progress fields needed here.
 * The backend's own 403 message is still shown as a defense-in-depth
 * fallback (e.g. stale client state), but is no longer the primary UX.
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

  const target = otherMode(state.mode)
  const blockedByThreshold = target === 'LIVE' && !state.liveModeUnlocked

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
    </div>
  )
}

export default TradingModeBanner
