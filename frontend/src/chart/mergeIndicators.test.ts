import { describe, expect, it } from 'vitest'
import type { Candle } from '../marketdata/api'
import type { ChartIndicatorPoint } from './api'
import { toCandlestickSeries, toMaLongSeries, toMaShortSeries, toRsiSeries } from './mergeIndicators'

function candle(timestamp: string, close: string): Candle {
  return { timestamp, open: close, high: close, low: close, close, volume: '1000000' }
}

function point(timestamp: string, rsi: number, maShort: number, maLong: number): ChartIndicatorPoint {
  return { timestamp, rsi, maShort, maLong }
}

describe('toCandlestickSeries', () => {
  it('converts string OHLC fields to numbers and timestamps to business-day strings', () => {
    const candles = [candle('2026-02-09T00:00:00Z', '113.10')]

    expect(toCandlestickSeries(candles)).toEqual([{ time: '2026-02-09', open: 113.1, high: 113.1, low: 113.1, close: 113.1 }])
  })

  it('returns an empty array for no candles', () => {
    expect(toCandlestickSeries([])).toEqual([])
  })
})

describe('toMaShortSeries / toMaLongSeries / toRsiSeries', () => {
  it('maps each indicator point to its own {time, value} pair', () => {
    const indicators = [point('2026-02-09T00:00:00Z', 77.8751, 111.92, 108.94)]

    expect(toMaShortSeries(indicators)).toEqual([{ time: '2026-02-09', value: 111.92 }])
    expect(toMaLongSeries(indicators)).toEqual([{ time: '2026-02-09', value: 108.94 }])
    expect(toRsiSeries(indicators)).toEqual([{ time: '2026-02-09', value: 77.8751 }])
  })

  it('returns empty arrays when the indicator series is empty (fewer than 34 candles)', () => {
    expect(toMaShortSeries([])).toEqual([])
    expect(toMaLongSeries([])).toEqual([])
    expect(toRsiSeries([])).toEqual([])
  })
})
