import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import OrderHistory from '../order/OrderHistory'
import TickerMetrics from '../signal/TickerMetrics'
import Watchlist from '../watchlist/Watchlist'

function DashboardPage() {
  const { username, logout } = useAuth()
  const [watchlistRefreshKey, setWatchlistRefreshKey] = useState(0)
  const [orderHistoryRefreshKey, setOrderHistoryRefreshKey] = useState(0)
  const [lookupRequest, setLookupRequest] = useState<{ symbol: string; nonce: number } | null>(null)

  return (
    <main>
      <header>
        <span>Signed in as {username}</span>
        <button type="button" onClick={() => logout()}>
          Log out
        </button>
      </header>
      <h1>Auto-Trade Dashboard</h1>
      <Watchlist
        refreshKey={watchlistRefreshKey}
        onSelect={(symbol) => setLookupRequest({ symbol, nonce: Date.now() })}
      />
      <TickerMetrics
        lookupRequest={lookupRequest}
        onWatchlistChanged={() => setWatchlistRefreshKey((key) => key + 1)}
        onOrderPlaced={() => setOrderHistoryRefreshKey((key) => key + 1)}
      />
      <OrderHistory refreshKey={orderHistoryRefreshKey} />
    </main>
  )
}

export default DashboardPage
