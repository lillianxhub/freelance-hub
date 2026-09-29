import { useContext } from 'react'
import { TimeEntriesContext } from './TimeEntriesContext'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export function useTimeEntries(): WorkspaceContextValue {
  const context = useContext(TimeEntriesContext)
  if (!context) throw new Error('useTimeEntries must be used inside TimeEntriesProvider')
  return context
}
