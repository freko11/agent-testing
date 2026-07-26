function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

/**
 * Wraps fetch with the CSRF header state-changing requests need (the
 * backend's SecurityConfig echoes a raw XSRF-TOKEN cookie, not a masked
 * one, so no extra request is needed to "unlock" it — see SpaCsrfTokenRequestHandler)
 * and broadcasts a global event on 401 so the auth context can react to an
 * expired session without every caller checking the status itself.
 */
export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)

  if (method !== 'GET' && method !== 'HEAD') {
    const csrfToken = readCookie('XSRF-TOKEN')
    if (csrfToken) {
      headers.set('X-XSRF-TOKEN', csrfToken)
    }
  }

  const response = await fetch(input, { ...init, headers, credentials: 'same-origin' })

  if (response.status === 401) {
    window.dispatchEvent(new Event('auth:unauthorized'))
  }

  return response
}
