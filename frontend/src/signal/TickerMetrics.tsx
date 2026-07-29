import { useEffect, useState, type FormEvent } from 'react'
import { fetchChartData, type ChartDataResponse } from '../chart/api'
import PriceChart from '../chart/PriceChart'
import { MarketDataError } from '../marketdata/api'
import TradeForm from '../trade/TradeForm'
import { addToWatchlist } from '../watchlist/api'
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

function AddToWatchlistButton({ symbol, onAdded }: { symbol: string; onAdded?: () => void }) {
  const [status, setStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle')
  const [error, setError] = useState<string | null>(null)

  async function handleClick() {
    setStatus('saving')
    setError(null)
    try {
      await addToWatchlist(symbol)
      setStatus('saved')
      onAdded?.()
    } catch (reason) {
      setStatus('error')
      setError(reason instanceof MarketDataError ? reason.message : 'Could not add to watchlist.')
    }
  }

  return (
    <div>
      <button type="button" onClick={handleClick} disabled={status === 'saving' || status === 'saved'}>
        {status === 'saved' ? 'Added to watchlist' : status === 'saving' ? 'Adding…' : 'Add to watchlist'}
      </button>
      {error && <p role="alert">{error}</p>}
    </div>
  )
}

interface TickerMetricsProps {
  /** Set by the watchlist's "revisit" click — a fresh object (new nonce) re-triggers the lookup even for the same symbol. */
  lookupRequest?: { symbol: string; nonce: number } | null
  onWatchlistChanged?: () => void
}

function TickerMetrics({ lookupRequest, onWatchlistChanged }: TickerMetricsProps) {
  const [symbol, setSymbol] = useState('')
  const [result, setResult] = useState<SignalResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [chartData, setChartData] = useState<ChartDataResponse | null>(null)
  const [chartError, setChartError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function runLookup(rawSymbol: string) {
    const trimmed = rawSymbol.trim()
    if (!trimmed) return

    setLoading(true)
    setError(null)
    setResult(null)
    setChartError(null)
    setChartData(null)

    // allSettled, not all: the chart and the signal/stat-tiles are two independent fetches
    // (chart-data never 422s INSUFFICIENT_PRICE_HISTORY, unlike /signal), so one failing
    // must not blank out the other.
    const [signalOutcome, chartOutcome] = await Promise.allSettled([fetchSignal(trimmed), fetchChartData(trimmed)])

    if (signalOutcome.status === 'fulfilled') {
      setResult(signalOutcome.value)
    } else {
      const reason = signalOutcome.reason
      setError(reason instanceof MarketDataError ? describeError(trimmed, reason) : 'Something went wrong. Please try again.')
    }

    if (chartOutcome.status === 'fulfilled') {
      setChartData(chartOutcome.value)
    } else {
      const reason = chartOutcome.reason
      setChartError(
        reason instanceof MarketDataError ? describeError(trimmed, reason) : 'Something went wrong loading the chart.',
      )
    }

    setLoading(false)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    await runLookup(symbol)
  }

  useEffect(() => {
    if (!lookupRequest) return
    setSymbol(lookupRequest.symbol)
    void runLookup(lookupRequest.symbol)
    // lookupRequest is a fresh object per watchlist click, so this intentionally re-runs on every
    // click (including re-selecting the same symbol) rather than only when the symbol text changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lookupRequest])

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
        <>
          <TickerMetricsResult signal={result} />
          <AddToWatchlistButton key={result.ticker.symbol} symbol={result.ticker.symbol} onAdded={onWatchlistChanged} />
          <TradeForm key={result.ticker.symbol} signal={result} />
        </>
      )}
      {chartError && <p role="alert">{chartError}</p>}
      {chartData && chartData.candles.length > 0 && (
        <div className="price-chart-container">
          <PriceChart candles={chartData.candles} indicators={chartData.indicators} />
          {chartData.indicators.length === 0 && (
            <p className="chart-note">Showing price only — not enough history yet for indicator overlays.</p>
          )}
        </div>
      )}
    </section>
  )
}

export default TickerMetrics
