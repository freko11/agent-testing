import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { apiFetch } from './api'

interface AuthContextValue {
  username: string | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function checkSession() {
      // GET first, unauthenticated — this is what primes the XSRF-TOKEN
      // cookie so the very first POST (login) already has one to echo back.
      await apiFetch('/api/auth/csrf')
      const response = await apiFetch('/api/auth/me')
      if (cancelled) return
      if (response.ok) {
        const data = (await response.json()) as { username: string }
        setUsername(data.username)
      } else {
        setUsername(null)
      }
      setLoading(false)
    }

    checkSession()

    const handleUnauthorized = () => setUsername(null)
    window.addEventListener('auth:unauthorized', handleUnauthorized)
    return () => {
      cancelled = true
      window.removeEventListener('auth:unauthorized', handleUnauthorized)
    }
  }, [])

  async function login(usernameInput: string, password: string) {
    const response = await apiFetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ username: usernameInput, password }),
    })
    if (!response.ok) {
      throw new Error('Invalid credentials')
    }
    setUsername(usernameInput)
  }

  async function logout() {
    await apiFetch('/api/auth/logout', { method: 'POST' })
    setUsername(null)
  }

  return (
    <AuthContext.Provider value={{ username, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
