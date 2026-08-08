import { useEffect, useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchOrders, refreshOrder, type OrderSummary } from './api'
import OrderExport from './OrderExport'
import { statusTone } from './statusTone'

interface OrderHistoryProps {
  refreshKey: number
}

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

/**
 * Order status/history (E5-F3-S1). Reads straight from the DB — every order
 * already reflects its final synchronous outcome from OrderService.submitOrder,
 * so there's no background "still pending" state to poll for. The one
 * exception is SUBMISSION_UNKNOWN/PARTIALLY_PROTECTED (and, in principle, a
 * PENDING row orphaned by an app crash mid-submission), which only resolve via
 * this row's own manual "Refresh" button — no setInterval/automatic polling
 * anywhere, matching this codebase's bias against background action on
 * money-moving state.
 */
function OrderHistory({ refreshKey }: OrderHistoryProps) {
  const [orders, setOrders] = useState<OrderSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshingId, setRefreshingId] = useState<number | null>(null)
  const [rowErrors, setRowErrors] = useState<Record<number, string>>({})

  useEffect(() => {
    let cancelled = false
    setError(null)
    fetchOrders()
      .then((result) => {
        if (!cancelled) setOrders(result)
      })
      .catch((reason) => {
        if (!cancelled) setError(describeError(reason, 'Could not load order history.'))
      })
    return () => {
      cancelled = true
    }
  }, [refreshKey])

  async function handleRefresh(id: number) {
    setRefreshingId(id)
    setRowErrors((current) => {
      const rest = { ...current }
      delete rest[id]
      return rest
    })
    try {
      const updated = await refreshOrder(id)
      setOrders((current) => (current ? current.map((order) => (order.id === id ? updated : order)) : current))
    } catch (reason) {
      setRowErrors((current) => ({ ...current, [id]: describeError(reason, 'Could not refresh this order.') }))
    } finally {
      setRefreshingId(null)
    }
  }

  return (
    <section>
      <h2>Order history</h2>
      <OrderExport />
      {error && <p role="alert">{error}</p>}
      {orders === null && !error && <p>Loading…</p>}
      {orders !== null && orders.length === 0 && <p>No orders placed yet.</p>}
      {orders !== null && orders.length > 0 && (
        <div className="order-history-table-wrap">
          <table className="order-history-table">
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Side</th>
                <th>Quantity</th>
                <th>Entry price</th>
                <th>Take-profit</th>
                <th>Stop-loss</th>
                <th>Status</th>
                <th>Detail</th>
                <th>Submitted</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td>{order.tickerSymbol}</td>
                  <td>{order.side}</td>
                  <td>{order.quantity}</td>
                  <td>{order.entryPrice ?? '—'}</td>
                  <td>{order.takeProfitPrice}</td>
                  <td>{order.stopLossPrice}</td>
                  <td>
                    <span className={`order-status order-status--${statusTone(order.status)}`}>{order.status}</span>
                  </td>
                  <td>{order.rejectionReason ?? '—'}</td>
                  <td>{order.submittedAt ? new Date(order.submittedAt).toLocaleString() : '—'}</td>
                  <td>
                    <button type="button" onClick={() => handleRefresh(order.id)} disabled={refreshingId === order.id}>
                      {refreshingId === order.id ? 'Refreshing…' : 'Refresh'}
                    </button>
                    {rowErrors[order.id] && (
                      <p className="order-history-table__row-error" role="alert">
                        {rowErrors[order.id]}
                      </p>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

export default OrderHistory
