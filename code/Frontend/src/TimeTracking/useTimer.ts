import { useEffect, useMemo, useState } from 'react'
import { cancelTimer as cancelTimerRequest, startTimer as startTimerRequest, stopTimer as stopTimerRequest } from '../services/timeTracking'
import { listTimerTasks } from '../services/timerOptions'
import type { Task } from '../types/task'
import type { TimerWorkspace } from '../types/timerWorkspace'
import { useCurrentTimer } from './useCurrentTimer'

interface UseTimerOptions {
  loadTaskOptions?: boolean
}

export function useTimer({ data, refresh }: TimerWorkspace, { loadTaskOptions = false }: UseTimerOptions = {}) {
  const [selectedProject, setSelectedProject] = useState('')
  const [selectedTask, setSelectedTask] = useState('')
  const [description, setDescription] = useState('')
  const [billable, setBillable] = useState(true)
  const [tick, setTick] = useState(() => Date.now())
  const [taskOptions, setTaskOptions] = useState<{ projectId: string; tasks: Task[]; error: string } | null>(null)
  const { currentTimer, refreshCurrentTimer } = useCurrentTimer()
  const activeProjects = useMemo(() => data.projects.filter((project) => project.status !== 'COMPLETED' && project.status !== 'ARCHIVED'), [data.projects])
  const fallbackRunningEntry = data.time_entries.find((entry) => !entry.ended_at) || null
  const currentEntry = currentTimer?.running ? currentTimer.timeEntry : null
  const runningEntry = useMemo(() => currentEntry
    ? {
      id: currentEntry.id,
      project_id: currentEntry.project.id,
      task_id: currentEntry.task?.id ?? null,
      description: currentEntry.description ?? '',
      started_at: currentEntry.startedAt,
    }
    : fallbackRunningEntry, [currentEntry, fallbackRunningEntry])
  const timerProjectId = selectedProject || activeProjects[0]?.id || ''
  const hasRunningEntry = Boolean(runningEntry)
  const selectedProjectData = data.projects.find((project) => project.id === timerProjectId)
  const selectedTasks = (taskOptions?.projectId === timerProjectId ? taskOptions.tasks : []).filter((task) => task.status !== 'DONE')
  const optionsError = taskOptions?.projectId === timerProjectId ? taskOptions.error : ''
  const runningProject = runningEntry && (data.projects.find((project) => project.id === runningEntry.project_id) || (currentEntry ? { id: currentEntry.project.id, name: currentEntry.project.name, color: '#4F6BFF' } : null))
  const runningTask = runningEntry && (data.tasks.find((task) => task.id === runningEntry.task_id) || (currentEntry?.task ? { id: currentEntry.task.id, name: currentEntry.task.title } : null))
  const elapsedSeconds = runningEntry ? Math.max(0, Math.floor((tick - Date.parse(runningEntry.started_at)) / 1000)) : 0

  useEffect(() => {
    if (!runningEntry) return undefined
    const interval = window.setInterval(() => setTick(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [runningEntry])

  useEffect(() => {
    if (!loadTaskOptions || !timerProjectId || hasRunningEntry) return undefined

    let active = true
    const projectId = timerProjectId
    listTimerTasks(projectId)
      .then((tasks) => {
        if (active) setTaskOptions({ projectId, tasks, error: '' })
      })
      .catch(() => {
        if (active) setTaskOptions({ projectId, tasks: [], error: 'ไม่สามารถโหลดงานของโปรเจกต์ได้' })
      })

    return () => {
      active = false
    }
  }, [loadTaskOptions, hasRunningEntry, timerProjectId])

  async function startTimer() {
    if (!selectedProjectData || runningEntry) return
    await startTimerRequest({
      projectId: timerProjectId,
      taskId: selectedTask || undefined,
      description: description.trim() || undefined,
    })
    await refresh()
    await refreshCurrentTimer()
    setDescription('')
  }

  async function stopTimer() {
    if (!runningEntry) return
    await stopTimerRequest()
    await refresh()
    await refreshCurrentTimer()
  }

  async function cancelTimer() {
    if (!runningEntry) return
    await cancelTimerRequest()
    await refresh()
    await refreshCurrentTimer()
  }

  return { activeProjects, runningEntry, timerProjectId, selectedTask, setSelectedProject, setSelectedTask, description, setDescription, billable, setBillable, selectedTasks, runningProject, runningTask, elapsedSeconds, startTimer, stopTimer, cancelTimer, optionsError }
}
