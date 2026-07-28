import { useState, type FormEvent } from 'react'
import { MarketDataError } from '../marketdata/api'
import { fetchSignal, type MovingAverageResult, type SignalResponse } from './api'

const ERROR_MESSAGES: Record<string, (symbol: string, backendMessage: string) => string> = {
  TICKER_NOT_REGISTERED: (symbol) =>
    `"${symbol}" isn't a registered ticker yet. Register it before looking up its signal.`,
  NO_PRICE_DATA: (symbol) => `No price data is available for "${symbol}" right now.`,
  MARKET_CLOSED: (symbol) =>
    `The stock market is closed right now, so "${symbol}"'s metrics aren't shown as current data. Try again during regular hours (9:30am-4:00pm ET, Mon-Fri).`,
  MARKET_DATA_RATE_LIMITED: () => 'The market data provider is rate-limiting requests. Try again shortly.',
  MARKET_DATA_UNAVAILABLE: (symbol) => `Market data for "${symbol}" is temporarily unavailable. Try again later.`,
  INSUFFICIENT_PRICE_HISTORY: (symbol) =>
    `Not enough price history exists yet for "${symbol}" to compute indicators.`,
}

function describeError(symbol: string, error: MarketDataError): string {
  const describe = ERROR_MESSAGES[error.code]
  return describe ? describe(symbol, error.message) : error.message
}

function formatOrDash(value: number | null, digits = 2, suffix = ''): string {
  return value === null ? '—' : `${value.toFixed(digits)}${suffix}`
}

function relationLabel(ma: MovingAverageResult): string {
  switch (ma.relation) {
    case 'SHORT_ABOVE_LONG':
      return `${ma.shortPeriod}-day above ${ma.longPeriod}-day`
    case 'SHORT_BELOW_LONG':
      return `${ma.shortPeriod}-day below ${ma.longPeriod}-day`
    case 'EQUAL':
      return `${ma.shortPeriod}-day equal to ${ma.longPeriod}-day`
  }
}

interface StatTileProps {
  label: string
  value: string
  hint?: string
}

function StatTile({ label, value, hint }: StatTileProps) {
  return (
    <div className="stat-tile">
      <span className="stat-tile__label">{label}</span>
      <span className="stat-tile__value">{value}</span>
      {hint && <span className="stat-tile__hint">{hint}</span>}
    </div>
  )
}

function SignalBadge({ signal }: { signal: SignalResponse }) {
  const { call, matchedRule, holdTerm } = signal
  return (
    <div className={`signal-badge signal-badge--${call.toLowerCase()}`} role="status">
      <span className="signal-badge__call">{call}</span>
      <span className="signal-badge__rule">{matchedRule}</span>
      {holdTerm && <span className="signal-badge__hold-term">Suggested hold-term: {holdTerm.label}</span>}
    </div>
  )
}

function TickerMetricsResult({ signal }: { signal: SignalResponse }) {
  const { ticker, indicators } = signal
  const asOf = new Date(indicators.asOf).toLocaleString()

  return (
    <div>
      <p>
        {ticker.symbol} ({ticker.assetType}) · {indicators.source} · as of {asOf}
      </p>
      <SignalBadge signal={signal} />
      <div className="stat-tile-grid">
        <StatTile label="Price" value={formatOrDash(indicators.price, 4)} />
        <StatTile label="RSI (14)" value={indicators.rsi.toFixed(2)} hint="Oversold <30 · Overbought >70" />
        <StatTile
          label="MACD (12,26,9)"
          value={indicators.macd.line.toFixed(4)}
          hint={`Signal ${indicators.macd.signal.toFixed(4)} · Histogram ${indicators.macd.histogram.toFixed(4)}`}
        />
        <StatTile
          label={`MA crossover (${indicators.movingAverage.shortPeriod}/${indicators.movingAverage.longPeriod})`}
          value={relationLabel(indicators.movingAverage)}
          hint={`MA${indicators.movingAverage.shortPeriod} ${indicators.movingAverage.shortMa.toFixed(4)} · MA${indicators.movingAverage.longPeriod} ${indicators.movingAverage.longMa.toFixed(4)}`}
        />
        <StatTile label="Volatility (ATR%)" value={formatOrDash(indicators.volatility, 4, '%')} />
        <StatTile label="Volume" value={formatOrDash(indicators.volume, 2)} />
        <StatTile
          label="Volume trend (10/30)"
          value={formatOrDash(indicators.volumeTrend, 4)}
          hint="Ratio of 10-day to 30-day average volume"
        />
      </div>
    </div>
  )
}

function TickerMetrics() {
  const [symbol, setSymbol] = useState('')
  const [result, setResult] = useState<SignalResponse | null>(null)
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
      setResult(await fetchSignal(trimmed))
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
      {result && <TickerMetricsResult signal={result} />}
    </section>
  )
}

export default TickerMetrics
