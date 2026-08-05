import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import KillSwitchControl from '../killswitch/KillSwitchControl'
import Tabs from '../layout/Tabs'
import NotificationPanel from '../notification/NotificationPanel'
import OrderHistory from '../order/OrderHistory'
import TickerMetrics from '../signal/TickerMetrics'
import TradingModeBanner from '../tradingmode/TradingModeBanner'
import Watchlist from '../watchlist/Watchlist'

const TABS = [
  { id: 'trade', label: 'Trade' },
  { id: 'orders', label: 'Orders' },
  { id: 'notifications', label: 'Notifications' },
] as const

type TabId = (typeof TABS)[number]['id']

function DashboardPage() {
  const { username, logout } = useAuth()
  const [activeTab, setActiveTab] = useState<TabId>('trade')
  const [watchlistRefreshKey, setWatchlistRefreshKey] = useState(0)
  const [orderHistoryRefreshKey, setOrderHistoryRefreshKey] = useState(0)
  const [lookupRequest, setLookupRequest] = useState<{ symbol: string; nonce: number } | null>(null)

  // Selecting a watchlist symbol from any tab jumps straight to Trade so the lookup it
  // triggers is immediately visible, matching a real trading terminal's "click a symbol,
  // see it" behavior.
  function handleWatchlistSelect(symbol: string) {
    setLookupRequest({ symbol, nonce: Date.now() })
    setActiveTab('trade')
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__brand">
          <span className="app-header__logo" aria-hidden="true">
            ◆
          </span>
          <h1>Auto-Trade Dashboard</h1>
        </div>
        <div className="app-header__user">
          <span>Signed in as {username}</span>
          <button type="button" onClick={() => logout()}>
            Log out
          </button>
        </div>
      </header>

      <div className="app-status-strip">
        <TradingModeBanner />
        <KillSwitchControl />
      </div>

      <div className="app-body">
        <aside className="app-sidebar">
          <Watchlist refreshKey={watchlistRefreshKey} onSelect={handleWatchlistSelect} />
        </aside>

        <main className="app-content">
          <Tabs tabs={[...TABS]} activeId={activeTab} onChange={(id) => setActiveTab(id as TabId)} />

          {/* hidden, not conditionally rendered, so switching tabs never loses in-progress
              state (a typed symbol, a pending order refresh) in the panel left behind. */}
          <div className="app-tab-panel" hidden={activeTab !== 'trade'}>
            <TickerMetrics
              lookupRequest={lookupRequest}
              onWatchlistChanged={() => setWatchlistRefreshKey((key) => key + 1)}
              onOrderPlaced={() => setOrderHistoryRefreshKey((key) => key + 1)}
            />
          </div>
          <div className="app-tab-panel" hidden={activeTab !== 'orders'}>
            <OrderHistory refreshKey={orderHistoryRefreshKey} />
          </div>
          <div className="app-tab-panel" hidden={activeTab !== 'notifications'}>
            <NotificationPanel />
          </div>
        </main>
      </div>
    </div>
  )
}

export default DashboardPage
