import { useState, type FormEvent } from 'react'
import { fetchPriceHistory, MarketDataError, type PriceHistoryResponse } from './api'

const ERROR_MESSAGES: Record<string, (symbol: string, backendMessage: string) => string> = {
  TICKER_NOT_REGISTERED: (symbol) =>
    `"${symbol}" isn't a registered ticker yet. Register it before looking up price history.`,
  NO_PRICE_DATA: (symbol) => `No price data is available for "${symbol}" right now.`,
  MARKET_DATA_RATE_LIMITED: () => 'The market data provider is rate-limiting requests. Try again shortly.',
  MARKET_DATA_UNAVAILABLE: (symbol) => `Market data for "${symbol}" is temporarily unavailable. Try again later.`,
}

function describeError(symbol: string, error: MarketDataError): string {
  const describe = ERROR_MESSAGES[error.code]
  return describe ? describe(symbol, error.message) : error.message
}

function TickerLookup() {
  const [symbol, setSymbol] = useState('')
  const [result, setResult] = useState<PriceHistoryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = symbol.trim()
    if (!trimmed) return

    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const response = await fetchPriceHistory(trimmed)
      setResult(response)
    } catch (err) {
      setError(err instanceof MarketDataError ? describeError(trimmed, err) : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <h2>Ticker lookup</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Ticker symbol
          <input
            value={symbol}
            onChange={(event) => setSymbol(event.target.value)}
            placeholder="e.g. AAPL or BTCUSDT"
            required
          />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? 'Looking up…' : 'Look up'}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      {result && (
        <p>
          {result.ticker.symbol}: {result.candles.length} candle{result.candles.length === 1 ? '' : 's'} from{' '}
          {result.source}.
        </p>
      )}
    </section>
  )
}

export default TickerLookup
