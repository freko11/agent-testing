import DriftReportPanel from './DriftReportPanel'
import WeightedVoteShadowPanel from './WeightedVoteShadowPanel'

/**
 * Signal Health tab (post-E8 follow-up) — the dashboard surface for E8-F5-S1/S3's diagnostic
 * monitoring endpoints, previously reachable only by curling the API. Both panels are
 * manual-load-only (see DriftReportPanel/WeightedVoteShadowPanel) since their backing endpoints
 * recompute expensively on every call.
 */
function SignalHealth() {
  return (
    <div className="signal-health">
      <DriftReportPanel />
      <WeightedVoteShadowPanel />
    </div>
  )
}

export default SignalHealth
