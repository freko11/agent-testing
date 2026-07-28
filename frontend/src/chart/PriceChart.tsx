import { useEffect, useRef } from 'react'
import { CandlestickSeries, ColorType, LineSeries, LineStyle, createChart } from 'lightweight-charts'
import type { Candle } from '../marketdata/api'
import type { ChartIndicatorPoint } from './api'
import { currentPalette } from './palette'
import { toCandlestickSeries, toMaLongSeries, toMaShortSeries, toRsiSeries } from './mergeIndicators'

interface PriceChartProps {
  candles: Candle[]
  indicators: ChartIndicatorPoint[]
}

const PRICE_PANE_STRETCH = 3
const RSI_PANE_STRETCH = 1
const RSI_OVERSOLD = 30
const RSI_OVERBOUGHT = 70

/**
 * Thin useRef/useEffect wrapper around lightweight-charts' imperative API (no React peer
 * dependency exists for it — see the Explore/Plan-agent research behind E3-F2-S1). The chart is
 * fully torn down and rebuilt on every prop change, which is simplest and acceptable here since a
 * new ticker lookup has no zoom/pan state worth preserving across renders.
 */
function PriceChart({ candles, indicators }: PriceChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const palette = currentPalette()
    const chart = createChart(container, {
      autoSize: true,
      layout: {
        background: { type: ColorType.Solid, color: palette.background },
        textColor: palette.text,
      },
      grid: {
        vertLines: { color: palette.grid },
        horzLines: { color: palette.grid },
      },
      timeScale: { borderColor: palette.grid },
      rightPriceScale: { borderColor: palette.grid },
    })

    const candlestickSeries = chart.addSeries(CandlestickSeries, {
      upColor: palette.candleUp,
      borderUpColor: palette.candleUp,
      wickUpColor: palette.candleUp,
      downColor: palette.candleDown,
      borderDownColor: palette.candleDown,
      wickDownColor: palette.candleDown,
    })
    candlestickSeries.setData(toCandlestickSeries(candles))
    chart.panes()[0].setStretchFactor(PRICE_PANE_STRETCH)

    const maShortSeries = chart.addSeries(LineSeries, { color: palette.maShort, lineWidth: 2 })
    maShortSeries.setData(toMaShortSeries(indicators))

    const maLongSeries = chart.addSeries(LineSeries, { color: palette.maLong, lineWidth: 2 })
    maLongSeries.setData(toMaLongSeries(indicators))

    const rsiPane = chart.addPane()
    rsiPane.setStretchFactor(RSI_PANE_STRETCH)
    const rsiSeries = rsiPane.addSeries(LineSeries, { color: palette.rsi, lineWidth: 2 })
    rsiSeries.setData(toRsiSeries(indicators))
    rsiSeries.createPriceLine({
      price: RSI_OVERSOLD,
      color: palette.rsiReference,
      lineStyle: LineStyle.Dashed,
      lineWidth: 1,
      axisLabelVisible: true,
      title: 'Oversold',
    })
    rsiSeries.createPriceLine({
      price: RSI_OVERBOUGHT,
      color: palette.rsiReference,
      lineStyle: LineStyle.Dashed,
      lineWidth: 1,
      axisLabelVisible: true,
      title: 'Overbought',
    })

    chart.timeScale().fitContent()

    return () => {
      chart.remove()
    }
  }, [candles, indicators])

  return <div ref={containerRef} className="price-chart" role="img" aria-label="Price chart with moving-average overlays and an RSI subplot" />
}

export default PriceChart
