import { useEffect, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchNotifications, markAllNotificationsRead, markNotificationRead, type NotificationSummary } from './api'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

/**
 * In-app notification list (E5-F4-S1) — order-outcome and watchlist
 * signal-change events. No {@code setInterval}/automatic polling: same
 * "manual refresh only" bias as {@code OrderHistory} (E5-F3-S1). The
 * watchlist-signal half is produced by a backend scheduled job running on
 * its own interval, not by any frontend action, so a manual refresh button
 * is the primary way new notifications surface here.
 */
function NotificationPanel() {
  const [notifications, setNotifications] = useState<NotificationSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [markingId, setMarkingId] = useState<number | null>(null)

  function load() {
    setLoading(true)
    setError(null)
    fetchNotifications()
      .then((result) => setNotifications(result))
      .catch((reason) => setError(describeError(reason, 'Could not load notifications.')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  async function handleMarkRead(id: number) {
    setMarkingId(id)
    try {
      await markNotificationRead(id)
      setNotifications((current) =>
        current
          ? current.map((n) => (n.id === id ? { ...n, readAt: n.readAt ?? new Date().toISOString() } : n))
          : current,
      )
    } catch (reason) {
      setError(describeError(reason, 'Could not mark this notification as read.'))
    } finally {
      setMarkingId(null)
    }
  }

  async function handleMarkAllRead() {
    try {
      await markAllNotificationsRead()
      const now = new Date().toISOString()
      setNotifications((current) => (current ? current.map((n) => ({ ...n, readAt: n.readAt ?? now })) : current))
    } catch (reason) {
      setError(describeError(reason, 'Could not mark all notifications as read.'))
    }
  }

  const unreadCount = notifications?.filter((n) => n.readAt === null).length ?? 0

  return (
    <section>
      <h2>
        Notifications{unreadCount > 0 && <span className="notification-panel__unread-badge">{unreadCount}</span>}
      </h2>
      <div className="notification-panel__actions">
        <button type="button" onClick={load} disabled={loading}>
          {loading ? 'Refreshing…' : 'Refresh'}
        </button>
        <button type="button" onClick={handleMarkAllRead} disabled={unreadCount === 0}>
          Mark all read
        </button>
      </div>
      {error && <p role="alert">{error}</p>}
      {notifications === null && !error && <p>Loading…</p>}
      {notifications !== null && notifications.length === 0 && <p>No notifications yet.</p>}
      {notifications !== null && notifications.length > 0 && (
        <ul className="notification-panel__list">
          {notifications.map((notification) => (
            <li
              key={notification.id}
              className={`notification-panel__item${notification.readAt === null ? ' notification-panel__item--unread' : ''}`}
            >
              <div className="notification-panel__item-body">
                <span className="notification-panel__event-type">{notification.eventType}</span>
                <span className="notification-panel__message">{notification.message}</span>
                <span className="notification-panel__timestamp">{new Date(notification.createdAt).toLocaleString()}</span>
              </div>
              {notification.readAt === null && (
                <button type="button" onClick={() => handleMarkRead(notification.id)} disabled={markingId === notification.id}>
                  {markingId === notification.id ? 'Marking…' : 'Mark read'}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export default NotificationPanel
