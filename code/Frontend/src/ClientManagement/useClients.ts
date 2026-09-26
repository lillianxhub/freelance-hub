import { useWorkspace } from '../Workspace/useWorkspace'

export function useClients() {
  const workspace = useWorkspace()
  return { ...workspace, clients: workspace.data.clients }
}
