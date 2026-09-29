import { useEffect, useMemo, useState } from 'react'
import { cancelTimer as cancelTimerRequest, startTimer as startTimerRequest, stopTimer as stopTimerRequest } from '../services/timeTracking'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export function useTimer({ data, refresh }: WorkspaceContextValue) {
  const [selectedProject, setSelectedProject] = useState('')
  const [selectedTask, setSelectedTask] = useState('')
  const [description, setDescription] = useState('')
  const [billable, setBillable] = useState(true)
  const [tick, setTick] = useState(() => Date.now())
  const activeProjects = useMemo(() => data.projects.filter((project) => project.status !== 'COMPLETED' && project.status !== 'ARCHIVED'), [data.projects])
  const runningEntry = data.time_entries.find((entry) => !entry.ended_at) || null
  const timerProjectId = selectedProject || activeProjects[0]?.id || ''
  const selectedProjectData = data.projects.find((project) => project.id === timerProjectId)
  const selectedTasks = data.tasks.filter((task) => task.project_id === timerProjectId && task.status !== 'DONE')
  const runningProject = runningEntry && data.projects.find((project) => project.id === runningEntry.project_id)
  const runningTask = runningEntry && data.tasks.find((task) => task.id === runningEntry.task_id)
  const elapsedSeconds = runningEntry ? Math.max(0, Math.floor((tick - Date.parse(runningEntry.started_at)) / 1000)) : 0

  useEffect(() => {
    if (!runningEntry) return undefined
    const interval = window.setInterval(() => setTick(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [runningEntry])

  async function startTimer() {
    if (!selectedProjectData || runningEntry) return
    await startTimerRequest({
      projectId: timerProjectId,
      taskId: selectedTask || undefined,
      description: description.trim() || undefined,
    })
    await refresh()
    setDescription('')
  }

  async function stopTimer() {
    if (!runningEntry) return
    await stopTimerRequest()
    await refresh()
  }

  async function cancelTimer() {
    if (!runningEntry) return
    await cancelTimerRequest()
    await refresh()
  }

  return { activeProjects, runningEntry, timerProjectId, selectedTask, setSelectedProject, setSelectedTask, description, setDescription, billable, setBillable, selectedTasks, runningProject, runningTask, elapsedSeconds, startTimer, stopTimer, cancelTimer, optionsError: '' }
}
