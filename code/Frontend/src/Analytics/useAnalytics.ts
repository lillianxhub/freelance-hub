import { useMemo } from 'react'
import { useWorkspace } from '../Workspace/useWorkspace'
import { summarizeTime } from '../utils/analytics'

export function useAnalytics() {
  const workspace = useWorkspace()
  const summary = useMemo(() => summarizeTime(workspace.data.time_entries), [workspace.data.time_entries])
  return { ...workspace, summary }
}
