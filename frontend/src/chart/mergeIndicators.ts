import type { Candle } from '../marketdata/api'
import type { ChartIndicatorPoint } from './api'

export interface CandlestickPoint {
  time: string
  open: number
  high: number
  low: number
  close: number
}

export interface LinePoint {
  time: string
  value: number
}

/** lightweight-charts accepts a business-day string ('yyyy-mm-dd') for daily bars; candle/indicator
 * timestamps are always midnight-UTC ISO instants, so slicing off the time-of-day component is exact. */
function toBusinessDay(timestamp: string): string {
  return timestamp.slice(0, 10)
}

export function toCandlestickSeries(candles: Candle[]): CandlestickPoint[] {
  return candles.map((candle) => ({
    time: toBusinessDay(candle.timestamp),
    open: Number(candle.open),
    high: Number(candle.high),
    low: Number(candle.low),
    close: Number(candle.close),
  }))
}

export function toMaShortSeries(indicators: ChartIndicatorPoint[]): LinePoint[] {
  return indicators.map((point) => ({ time: toBusinessDay(point.timestamp), value: point.maShort }))
}

export function toMaLongSeries(indicators: ChartIndicatorPoint[]): LinePoint[] {
  return indicators.map((point) => ({ time: toBusinessDay(point.timestamp), value: point.maLong }))
}

export function toRsiSeries(indicators: ChartIndicatorPoint[]): LinePoint[] {
  return indicators.map((point) => ({ time: toBusinessDay(point.timestamp), value: point.rsi }))
}
