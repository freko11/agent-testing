import { useAuth } from '../auth/AuthContext'
import TickerMetrics from '../signal/TickerMetrics'

function DashboardPage() {
  const { username, logout } = useAuth()

  return (
    <main>
      <header>
        <span>Signed in as {username}</span>
        <button type="button" onClick={() => logout()}>
          Log out
        </button>
      </header>
      <h1>Auto-Trade Dashboard</h1>
      <TickerMetrics />
    </main>
  )
}

export default DashboardPage
