import { useEffect, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { statusTone } from '../order/statusTone'
import { fetchAuditEntries, type AuditEntryPage } from './api'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function holdTermLabel(minDays: number | null, maxDays: number | null): string {
  return minDays !== null && maxDays !== null ? `${minDays}-${maxDays}d` : '—'
}

/**
 * Audit-trail viewer (E6-F3-S3) — the dashboard's review surface for E6-F3-S1/S2's immutable
 * order-audit log: every order's outcome alongside the frozen signal snapshot/rule-table version
 * that triggered it. True page-number pagination, unlike OrderHistory's limit-only "last N" list —
 * reviewing history further back is the actual point of an audit trail, unlike order status/history,
 * which already has CSV export as its own "see everything" escape hatch.
 */
function AuditTrail() {
  const [data, setData] = useState<AuditEntryPage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  useEffect(() => {
    let cancelled = false
    setError(null)
    fetchAuditEntries(page)
      .then((result) => {
        if (!cancelled) setData(result)
      })
      .catch((reason) => {
        if (!cancelled) setError(describeError(reason, 'Could not load the audit trail.'))
      })
    return () => {
      cancelled = true
    }
  }, [page])

  const entries = data?.content ?? null

  return (
    <section>
      <h2>Audit trail</h2>
      {error && <p role="alert">{error}</p>}
      {entries === null && !error && <p>Loading…</p>}
      {entries !== null && entries.length === 0 && <p>No audit entries yet.</p>}
      {entries !== null && entries.length > 0 && (
        <>
          <div className="audit-trail-table-wrap">
            <table className="audit-trail-table">
              <thead>
                <tr>
                  <th>Ticker</th>
                  <th>Side</th>
                  <th>Call</th>
                  <th>Rule</th>
                  <th>Rule version</th>
                  <th>Hold term</th>
                  <th>Outcome</th>
                  <th>Entry price</th>
                  <th>Rejection reason</th>
                  <th>Logged at</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.id}>
                    <td>{entry.tickerSymbol}</td>
                    <td>{entry.side}</td>
                    <td>{entry.call}</td>
                    <td title={entry.matchedRuleRationale}>{entry.matchedRule}</td>
                    <td>{entry.ruleTableVersion}</td>
                    <td>{holdTermLabel(entry.holdTermMinDays, entry.holdTermMaxDays)}</td>
                    <td>
                      <span className={`order-status order-status--${statusTone(entry.resultStatus)}`}>
                        {entry.resultStatus}
                      </span>
                    </td>
                    <td>{entry.entryPrice ?? '—'}</td>
                    <td>{entry.rejectionReason ?? '—'}</td>
                    <td>{new Date(entry.loggedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data && (
            <div className="audit-trail-table__pagination">
              <button type="button" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>
                Previous
              </button>
              <span>
                Page {data.page + 1} of {Math.max(data.totalPages, 1)}
              </span>
              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={data.page + 1 >= data.totalPages}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </section>
  )
}

export default AuditTrail
