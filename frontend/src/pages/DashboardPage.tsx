import { useAuth } from '../auth/AuthContext'

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
      <p>Placeholder route — dashboard content lands in a later story.</p>
    </main>
  )
}

export default DashboardPage
