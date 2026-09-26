import { useWorkspace } from '../Workspace/useWorkspace'

export function useTimeEntries(projectId?: string) {
  const workspace = useWorkspace()
  return {
    ...workspace,
    entries: projectId ? workspace.data.time_entries.filter((entry) => entry.project_id === projectId) : workspace.data.time_entries,
  }
}
