import type { StatusTone } from '../order/statusTone'

/** possibleDecay -> the same order-status tone classes OrderHistory/AuditTrail already use. */
export function decayTone(possibleDecay: boolean): StatusTone {
  return possibleDecay ? 'error' : 'success'
}

/** How many disagreements a weighted-vote bucket found -> a tone, so a "0" reads calm and a
 * nonzero count reads as worth a look, without implying a nonzero count is itself a problem
 * (disagreement alone isn't decay -- it's just something to review). */
export function disagreementCountTone(count: number): StatusTone {
  return count > 0 ? 'warning' : 'neutral'
}
