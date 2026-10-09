import { useProjects } from './useProjects'

export function useTasks(projectId?: string) {
  const workspace = useProjects()
  return {
    ...workspace,
    tasks: projectId
      ? workspace.data.tasks.filter((task) => task.project_id === projectId)
      : workspace.data.tasks,
  }
}
