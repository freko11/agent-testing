import { useEffect, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchSystemAlerts, type SystemAlertSummary } from './api'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

/**
 * A compact ops-alert indicator in the status strip, alongside TradingModeBanner/
 * KillSwitchControl — the app's existing place for cross-cutting state visible regardless of
 * active tab. Deliberately not folded into the Notifications tab (that domain is end-user/
 * ticker-scoped by schema design) or the Signal Health tab (a kill-switch trip isn't
 * signal-related, and shouldn't require opening a specific tab to notice).
 */
function SystemAlertStrip() {
  const [alerts, setAlerts] = useState<SystemAlertSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  function load() {
    setLoading(true)
    setError(null)
    fetchSystemAlerts()
      .then((result) => setAlerts(result))
      .catch((reason) => setError(describeError(reason, 'Could not load system alerts.')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const count = alerts?.length ?? 0

  return (
    <div className="system-alert-strip" role="status">
      <details className="system-alert-strip__details">
        <summary className="system-alert-strip__summary">
          <span className="system-alert-strip__label">System alerts</span>
          <span className={`system-alert-strip__pill${count > 0 ? ' system-alert-strip__pill--active' : ''}`}>
            {count}
          </span>
        </summary>
        <div className="system-alert-strip__panel">
          <button type="button" onClick={load} disabled={loading}>
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
          {error && <p role="alert">{error}</p>}
          {alerts && alerts.length === 0 && !error && <p>No system alerts.</p>}
          {alerts && alerts.length > 0 && (
            <ul className="system-alert-strip__list">
              {alerts.map((alert) => (
                <li key={alert.id}>
                  <span className="system-alert-strip__message">{alert.message}</span>
                  <span className="system-alert-strip__timestamp">{new Date(alert.createdAt).toLocaleString()}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </details>
    </div>
  )
}

export default SystemAlertStrip
