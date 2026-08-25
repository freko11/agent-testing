import { useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import {
  fetchWeightedVoteShadow,
  FeatureDisabledError,
  type CheckpointStats,
  type DirectionalOutcomeStats,
  type WeightedVoteBucketOutcome,
  type WeightedVoteShadowReport,
} from './api'
import { disagreementCountTone } from './tone'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function CheckpointStatsRow({ label, stats }: { label: string; stats: CheckpointStats }) {
  return (
    <tr>
      <td>{label}</td>
      <td>{stats.win}</td>
      <td>{stats.loss}</td>
      <td>{stats.wash}</td>
      <td>{stats.notScored}</td>
      <td>{stats.avgWinReturnPct.toFixed(2)}%</td>
      <td>{stats.avgLossReturnPct.toFixed(2)}%</td>
      <td>{stats.tpHit}</td>
      <td>{stats.slHit}</td>
      <td>{stats.horizonExpired}</td>
      <td>{stats.avgHoldingDays.toFixed(1)}</td>
    </tr>
  )
}

function CheckpointStatsTable({ rows }: { rows: { label: string; stats: CheckpointStats }[] }) {
  return (
    <div className="signal-health-table-wrap">
      <table className="audit-trail-table">
        <thead>
          <tr>
            <th>Checkpoint</th>
            <th>Win</th>
            <th>Loss</th>
            <th>Wash</th>
            <th>Not scored</th>
            <th>Avg win return</th>
            <th>Avg loss return</th>
            <th>TP hit</th>
            <th>SL hit</th>
            <th>Horizon expired</th>
            <th>Avg holding days</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <CheckpointStatsRow key={row.label} label={row.label} stats={row.stats} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function BucketOutcomeSection({ title, outcome }: { title: string; outcome: WeightedVoteBucketOutcome }) {
  return (
    <div className="signal-health-bucket">
      <h4>
        {title}{' '}
        <span className={`order-status order-status--${disagreementCountTone(outcome.count)}`}>
          {outcome.count} {outcome.count === 1 ? 'entry' : 'entries'}
        </span>
      </h4>
      {outcome.count === 0 ? (
        <p>0 disagreements.</p>
      ) : (
        <CheckpointStatsTable rows={[{ label: 'Reference horizon', stats: outcome.scoring }]} />
      )}
    </div>
  )
}

function DowngradeOutcomeSection({ outcome }: { outcome: { count: number; scoring: DirectionalOutcomeStats } }) {
  return (
    <div className="signal-health-bucket">
      <h4>
        Downgraded by weighted engine{' '}
        <span className={`order-status order-status--${disagreementCountTone(outcome.count)}`}>
          {outcome.count} {outcome.count === 1 ? 'entry' : 'entries'}
        </span>
      </h4>
      {outcome.count === 0 ? (
        <p>0 disagreements.</p>
      ) : (
        <CheckpointStatsTable
          rows={[
            { label: 'MIN', stats: outcome.scoring.min },
            { label: 'MID', stats: outcome.scoring.mid },
            { label: 'MAX', stats: outcome.scoring.max },
          ]}
        />
      )}
    </div>
  )
}

/**
 * Renders GET /api/monitoring/weighted-vote-shadow. Same manual-trigger-only rule as
 * DriftReportPanel: the endpoint recomputes fresh on every call (a DB scan + walk-forward
 * scoring), so it never fetches on mount.
 */
function WeightedVoteShadowPanel() {
  const [report, setReport] = useState<WeightedVoteShadowReport | null>(null)
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
      const result = await fetchWeightedVoteShadow(lookbackDays)
      setReport(result)
    } catch (reason) {
      if (reason instanceof FeatureDisabledError) {
        setDisabled(true)
      } else {
        setError(describeError(reason, 'Could not load the weighted-vote shadow report.'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <h2>Weighted-vote shadow scoring</h2>
      <p>
        Replays persisted signal calls through the (still unwired) weighted-vote engine and buckets where it would
        have disagreed with the live rule table. Recomputed fresh on every load, so it's not auto-refreshed.
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
      {disabled && <p>Weighted-vote shadow scoring is disabled on this server.</p>}
      {error && <p role="alert">{error}</p>}
      {report && (
        <>
          <p className="signal-health-summary">
            {report.agreeCount} agreed, {report.skippedEntries} skipped, of {report.totalEntriesConsidered} entries
            in the last {report.lookbackDays} days.
          </p>
          <BucketOutcomeSection title="Weighted-only BUY" outcome={report.weightedOnlyBuy} />
          <BucketOutcomeSection title="Weighted-only SELL" outcome={report.weightedOnlySell} />
          <DowngradeOutcomeSection outcome={report.downgradedByWeighted} />
          {report.knownLimitations.length > 0 && (
            <div className="signal-health-limitations">
              <h4>Known limitations</h4>
              <ul>
                {report.knownLimitations.map((limitation) => (
                  <li key={limitation}>{limitation}</li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  )
}

export default WeightedVoteShadowPanel
