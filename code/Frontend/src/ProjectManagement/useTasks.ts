import { useWorkspace } from '../Workspace/useWorkspace'

export function useTasks(projectId?: string) {
  const workspace = useWorkspace()
  return {
    ...workspace,
    tasks: projectId ? workspace.data.tasks.filter((task) => task.project_id === projectId) : workspace.data.tasks,
  }
}
