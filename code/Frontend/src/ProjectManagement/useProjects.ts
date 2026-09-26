import { useWorkspace } from '../Workspace/useWorkspace'

export function useProjects() {
  const workspace = useWorkspace()
  return { ...workspace, projects: workspace.data.projects }
}
