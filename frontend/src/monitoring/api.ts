import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type Checkpoint = 'MIN' | 'MID' | 'MAX'

/** Mirrors backend.backtest.CheckpointStats field-for-field. No winRate/expectancy fields --
 * those are Java-record derived methods, not serialized JSON, so they aren't recomputed here. */
export interface CheckpointStats {
  win: number
  loss: number
  wash: number
  notScored: number
  avgWinReturnPct: number
  avgLossReturnPct: number
  tpHit: number
  slHit: number
  horizonExpired: number
  avgHoldingDays: number
}

/** Mirrors backend.backtest.DirectionalOutcomeStats. */
export interface DirectionalOutcomeStats {
  totalCalls: number
  min: CheckpointStats
  mid: CheckpointStats
  max: CheckpointStats
}

/** Mirrors backend.monitoring.CheckpointDrift. */
export interface CheckpointDrift {
  checkpoint: Checkpoint
  scored: number
  liveExpectancyPctAfterCosts: number
  baselineExpectancyPctAfterCosts: number
  driftPct: number
  possibleDecay: boolean
  liveExpectancyPctAfterCostsAndFunding: number
  baselineExpectancyPctAfterCostsAndFunding: number
  driftPctAfterFunding: number
}

/** Mirrors backend.monitoring.DirectionalDrift. Empty `checkpoints` means either no calls at all,
 * or this rule-table version has no baseline (see RuleTableVersionDrift.hasBaseline). */
export interface DirectionalDrift {
  totalCalls: number
  checkpoints: CheckpointDrift[]
}

/** Mirrors backend.monitoring.RuleTableVersionDrift. */
export interface RuleTableVersionDrift {
  ruleTableVersion: string
  hasBaseline: boolean
  buy: DirectionalDrift
  sell: DirectionalDrift
}

/** Mirrors backend.monitoring.SignalDriftReport -- the GET /api/monitoring/signal-drift body. */
export interface SignalDriftReport {
  lookbackDays: number
  totalAuditEntriesConsidered: number
  scoredAuditEntries: number
  skippedAuditEntries: number
  versions: RuleTableVersionDrift[]
}

/** Mirrors backend.monitoring.WeightedVoteBucketOutcome. */
export interface WeightedVoteBucketOutcome {
  count: number
  scoring: CheckpointStats
}

/** Mirrors backend.monitoring.WeightedVoteDowngradeOutcome. */
export interface WeightedVoteDowngradeOutcome {
  count: number
  scoring: DirectionalOutcomeStats
}

/** Mirrors backend.monitoring.WeightedVoteShadowReport -- the GET
 * /api/monitoring/weighted-vote-shadow body. `knownLimitations` is the backend's own documented
 * caveat about what this replay can't measure (e.g. regime-gate SELL effects) -- render it
 * verbatim, don't drop it. */
export interface WeightedVoteShadowReport {
  lookbackDays: number
  totalEntriesConsidered: number
  skippedEntries: number
  agreeCount: number
  weightedOnlyBuy: WeightedVoteBucketOutcome
  weightedOnlySell: WeightedVoteBucketOutcome
  downgradedByWeighted: WeightedVoteDowngradeOutcome
  knownLimitations: string[]
}

/**
 * Thrown instead of MarketDataError when a monitoring endpoint 404s -- both
 * SignalDriftController/WeightedVoteShadowController are @ConditionalOnProperty(matchIfMissing =
 * true), so a 404 here means the feature is disabled server-side, not a generic request failure.
 * Spring's default 404 body isn't the structured MarketDataExceptionHandler shape, so
 * parseMarketDataError would otherwise mis-parse it as a generic UNKNOWN-code error.
 */
export class FeatureDisabledError extends Error {
  constructor() {
    super('This feature is disabled on this server.')
    this.name = 'FeatureDisabledError'
  }
}

async function getMonitoringReport<T>(path: string, lookbackDays?: number): Promise<T> {
  const query = lookbackDays !== undefined ? `?lookbackDays=${encodeURIComponent(lookbackDays)}` : ''
  const response = await apiFetch(`/api/monitoring/${path}${query}`)

  if (response.status === 404) {
    throw new FeatureDisabledError()
  }
  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as T
}

/**
 * Recomputes from scratch on every call, including a real Alpaca/Binance market-data fetch per
 * distinct ticker symbol -- never called automatically (e.g. on mount or on an interval), only in
 * direct response to an explicit user action. See DriftReportPanel.
 */
export async function fetchSignalDrift(lookbackDays?: number): Promise<SignalDriftReport> {
  return getMonitoringReport<SignalDriftReport>('signal-drift', lookbackDays)
}

/**
 * Recomputes from scratch on every call (a DB scan + walk-forward scoring over persisted
 * SignalCallEntry rows) -- same manual-trigger-only rule as fetchSignalDrift. See
 * WeightedVoteShadowPanel.
 */
export async function fetchWeightedVoteShadow(lookbackDays?: number): Promise<WeightedVoteShadowReport> {
  return getMonitoringReport<WeightedVoteShadowReport>('weighted-vote-shadow', lookbackDays)
}
