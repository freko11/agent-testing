import type { OrderStatus } from '../trade/api'

export type StatusTone = 'success' | 'warning' | 'error' | 'neutral'

/** Shared between OrderHistory (E5-F3-S1) and AuditTrail (E6-F3-S3) — both render the same OrderStatus values. */
export function statusTone(status: OrderStatus): StatusTone {
  switch (status) {
    case 'FILLED':
    case 'PARTIALLY_FILLED':
      return 'success'
    case 'PARTIALLY_PROTECTED':
    case 'SUBMISSION_UNKNOWN':
      return 'warning'
    case 'REJECTED':
    case 'FAILED':
      return 'error'
    default:
      return 'neutral'
  }
}
