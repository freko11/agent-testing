import { useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchSignalDrift, FeatureDisabledError, type CheckpointDrift, type RuleTableVersionDrift, type SignalDriftReport } from './api'
import { decayTone } from './tone'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function pct(value: number): string {
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

function CheckpointRow({ checkpoint }: { checkpoint: CheckpointDrift }) {
  return (
    <tr>
      <td>{checkpoint.checkpoint}</td>
      <td>{checkpoint.scored}</td>
      <td>{pct(checkpoint.liveExpectancyPctAfterCosts)}</td>
      <td>{pct(checkpoint.baselineExpectancyPctAfterCosts)}</td>
      <td>{pct(checkpoint.driftPct)}</td>
      <td>
        <span className={`order-status order-status--${decayTone(checkpoint.possibleDecay)}`}>
          {checkpoint.possibleDecay ? 'Possible decay' : 'OK'}
        </span>
      </td>
      <td>{pct(checkpoint.liveExpectancyPctAfterCostsAndFunding)}</td>
      <td>{pct(checkpoint.baselineExpectancyPctAfterCostsAndFunding)}</td>
      <td>{pct(checkpoint.driftPctAfterFunding)}</td>
    </tr>
  )
}

function DirectionSection({ label, totalCalls, hasBaseline, checkpoints }: {
  label: string
  totalCalls: number
  hasBaseline: boolean
  checkpoints: CheckpointDrift[]
}) {
  return (
    <div className="signal-health-direction">
      <h4>
        {label} <span className="signal-health-direction__count">({totalCalls} calls)</span>
      </h4>
      {!hasBaseline && <p>No baseline for this rule-table version.</p>}
      {hasBaseline && checkpoints.length === 0 && <p>No calls yet for this direction.</p>}
      {hasBaseline && checkpoints.length > 0 && (
        <div className="signal-health-table-wrap">
          <table className="audit-trail-table">
            <thead>
              <tr>
                <th>Checkpoint</th>
                <th>Scored</th>
                <th>Live (after costs)</th>
                <th>Baseline</th>
                <th>Drift</th>
                <th>Status</th>
                <th>Live (w/ funding)</th>
                <th>Baseline (w/ funding)</th>
                <th>Drift (w/ funding)</th>
              </tr>
            </thead>
            <tbody>
              {checkpoints.map((checkpoint) => (
                <CheckpointRow key={checkpoint.checkpoint} checkpoint={checkpoint} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function VersionSection({ version }: { version: RuleTableVersionDrift }) {
  return (
    <div className="signal-health-version">
      <h3>
        Rule table {version.ruleTableVersion}
        {!version.hasBaseline && <span className="signal-health-version__badge"> — no baseline computed</span>}
      </h3>
      <DirectionSection label="BUY" totalCalls={version.buy.totalCalls} hasBaseline={version.hasBaseline}
        checkpoints={version.buy.checkpoints} />
      <DirectionSection label="SELL" totalCalls={version.sell.totalCalls} hasBaseline={version.hasBaseline}
        checkpoints={version.sell.checkpoints} />
    </div>
  )
}

/**
 * Renders GET /api/monitoring/signal-drift. Deliberately never fetches on mount -- the endpoint
 * recomputes from scratch on every call, including a real Alpaca/Binance fetch per distinct
 * ticker symbol, so this panel starts idle and only loads on an explicit click.
 */
function DriftReportPanel() {
  const [report, setReport] = useState<SignalDriftReport | null>(null)
  const [lookbackDaysInput, setLookbackDaysInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [disabled, setDisabled] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    setDisabled(false)
    try {
      const lookbackDays = lookbackDaysInput.trim() === '' ? undefined : Number(lookbackDaysInput)
      if (lookbackDays !== undefined && (!Number.isInteger(lookbackDays) || lookbackDays < 1)) {
        setError('Lookback days must be a positive whole number.')
        return
      }
      const result = await fetchSignalDrift(lookbackDays)
      setReport(result)
    } catch (reason) {
      if (reason instanceof FeatureDisabledError) {
        setDisabled(true)
      } else {
        setError(describeError(reason, 'Could not load the signal-drift report.'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <h2>Live signal drift</h2>
      <p>
        Re-scores the rule table's live performance against real forward market data. Recomputed fresh on every
        load — this hits the market-data API, so it's not auto-refreshed.
      </p>
      <div className="signal-health-controls">
        <label>
          Lookback days
          <input
            type="number"
            min={1}
            placeholder="default"
            value={lookbackDaysInput}
            onChange={(e) => setLookbackDaysInput(e.target.value)}
          />
        </label>
        <button type="button" onClick={load} disabled={loading}>
          {loading ? 'Loading…' : report ? 'Refresh' : 'Load report'}
        </button>
      </div>
      {disabled && <p>Signal-drift monitoring is disabled on this server.</p>}
      {error && <p role="alert">{error}</p>}
      {report && (
        <>
          <p className="signal-health-summary">
            {report.scoredAuditEntries} scored, {report.skippedAuditEntries} skipped, of{' '}
            {report.totalAuditEntriesConsidered} audit entries in the last {report.lookbackDays} days.
          </p>
          {report.versions.length === 0 && <p>No signal-drift data yet for this lookback window.</p>}
          {report.versions.map((version) => (
            <VersionSection key={version.ruleTableVersion} version={version} />
          ))}
        </>
      )}
    </section>
  )
}

export default DriftReportPanel
