import { apiFetch } from '../auth/api'
import { parseMarketDataError } from '../marketdata/api'

export type NotificationEventType =
  | 'ORDER_FILLED'
  | 'ORDER_PARTIALLY_FILLED'
  | 'ORDER_REJECTED'
  | 'ORDER_FAILED'
  | 'ORDER_CANCELLED'
  | 'ORDER_PARTIALLY_PROTECTED'
  | 'ORDER_SUBMISSION_UNKNOWN'
  | 'SIGNAL_CHANGED'

/** A single in-app notification (E5-F4-S1) — mirrors NotificationResponse field-for-field. */
export interface NotificationSummary {
  id: number
  tickerSymbol: string
  eventType: NotificationEventType
  message: string
  readAt: string | null
  createdAt: string
}

export async function fetchNotifications(limit = 50): Promise<NotificationSummary[]> {
  const response = await apiFetch(`/api/notifications?limit=${limit}`)

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  return (await response.json()) as NotificationSummary[]
}

export async function fetchUnreadCount(): Promise<number> {
  const response = await apiFetch('/api/notifications/unread-count')

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }

  const body = (await response.json()) as { unreadCount: number }
  return body.unreadCount
}

export async function markNotificationRead(id: number): Promise<void> {
  const response = await apiFetch(`/api/notifications/${id}/read`, { method: 'POST' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }
}

export async function markAllNotificationsRead(): Promise<void> {
  const response = await apiFetch('/api/notifications/read-all', { method: 'POST' })

  if (!response.ok) {
    throw await parseMarketDataError(response)
  }
}
