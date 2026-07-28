/**
 * lightweight-charts needs concrete JS color values — it can't consume the app's index.css
 * light-dark() rules directly, and reading a light-dark()-valued custom property back out via
 * getComputedStyle is unreliable cross-browser. These hex values intentionally mirror the
 * existing .signal-badge palette (candleUp/candleDown match --buy/--sell) plus two more
 * colorblind-safe hues (blue/violet) for the non-directional MA overlay lines, per the
 * dataviz skill's "one accessible palette reused everywhere" constraint.
 */
export interface ChartPalette {
  candleUp: string
  candleDown: string
  maShort: string
  maLong: string
  rsi: string
  rsiReference: string
  background: string
  text: string
  grid: string
}

const LIGHT_PALETTE: ChartPalette = {
  candleUp: '#0f766e',
  candleDown: '#c2410c',
  maShort: '#1d4ed8',
  maLong: '#7c3aed',
  rsi: '#4338ca',
  rsiReference: '#9ca3af',
  background: 'transparent',
  text: '#1f2933',
  grid: '#e5e7eb',
}

const DARK_PALETTE: ChartPalette = {
  candleUp: '#2dd4bf',
  candleDown: '#fb923c',
  maShort: '#60a5fa',
  maLong: '#c4b5fd',
  rsi: '#a5b4fc',
  rsiReference: '#6b7280',
  background: 'transparent',
  text: '#e5e7eb',
  grid: '#3a3a3a',
}

export function isDarkMode(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

export function currentPalette(): ChartPalette {
  return isDarkMode() ? DARK_PALETTE : LIGHT_PALETTE
}
