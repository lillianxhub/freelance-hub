import { useContext } from 'react'
import { TimeEntriesContext } from './TimeEntriesContext'
import type { TimeEntriesContextValue } from './TimeEntriesContext'

export function useTimeEntries(): TimeEntriesContextValue {
  const context = useContext(TimeEntriesContext)
  if (!context) throw new Error('useTimeEntries must be used inside TimeEntriesProvider')
  return context
}
