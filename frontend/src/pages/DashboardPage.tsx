import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import KillSwitchControl from '../killswitch/KillSwitchControl'
import NotificationPanel from '../notification/NotificationPanel'
import OrderHistory from '../order/OrderHistory'
import TickerMetrics from '../signal/TickerMetrics'
import TradingModeBanner from '../tradingmode/TradingModeBanner'
import Watchlist from '../watchlist/Watchlist'

function DashboardPage() {
  const { username, logout } = useAuth()
  const [watchlistRefreshKey, setWatchlistRefreshKey] = useState(0)
  const [orderHistoryRefreshKey, setOrderHistoryRefreshKey] = useState(0)
  const [lookupRequest, setLookupRequest] = useState<{ symbol: string; nonce: number } | null>(null)

  return (
    <>
      <header className="app-header">
        <h1 className="app-header__brand">Auto-Trade Dashboard</h1>
        <div className="app-header__user">
          <span>Signed in as {username}</span>
          <button type="button" onClick={() => logout()}>
            Log out
          </button>
        </div>
      </header>
      <main className="app-main">
        <div className="app-toolbar">
          <KillSwitchControl />
          <TradingModeBanner />
        </div>
        <NotificationPanel />
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
    </>
  )
}

export default DashboardPage
