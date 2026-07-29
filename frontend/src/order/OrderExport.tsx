import { useState } from 'react'
import { MarketDataError } from '../marketdata/api'
import { exportOrdersCsv } from './api'

type ExportState = 'idle' | 'exporting' | 'error'

function describeError(reason: unknown, fallback: string): string {
  return reason instanceof MarketDataError ? reason.message : fallback
}

function triggerDownload(filename: string, blob: Blob) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * Trade-history CSV export for a date range (E5-F3-S2). Independent of
 * whatever page of orders `OrderHistory` currently has loaded — always
 * queries the backend fresh for the requested range, so no refreshKey
 * coupling is needed with the table above it.
 */
function OrderExport() {
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [state, setState] = useState<ExportState>('idle')
  const [error, setError] = useState<string | null>(null)

  async function handleExport() {
    setError(null)
    if (!start || !end) {
      setError('Choose both a start and end date.')
      setState('error')
      return
    }
    if (start > end) {
      setError('Start date must not be after end date.')
      setState('error')
      return
    }

    setState('exporting')
    try {
      const { filename, blob } = await exportOrdersCsv(start, end)
      triggerDownload(filename, blob)
      setState('idle')
    } catch (reason) {
      setError(describeError(reason, 'Could not export trade history.'))
      setState('error')
    }
  }

  return (
    <div className="order-export">
      <label>
        Start date
        <input type="date" value={start} onChange={(e) => setStart(e.target.value)} />
      </label>
      <label>
        End date
        <input type="date" value={end} onChange={(e) => setEnd(e.target.value)} />
      </label>
      <button type="button" onClick={handleExport} disabled={state === 'exporting'}>
        {state === 'exporting' ? 'Exporting…' : 'Export to CSV'}
      </button>
      {state === 'error' && error && (
        <p className="order-export__error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export default OrderExport
