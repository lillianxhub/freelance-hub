import { useEffect, useMemo, useState } from 'react'
import { useWorkspace } from '../Workspace/useWorkspace'
import { listTimerProjects, listTimerTasks } from '../services/timerOptions'
import type { Project } from '../types/project'
import type { Task } from '../types/task'

export function useTimer() {
  const { data, save, remove } = useWorkspace()
  const [selectedProject, setSelectedProject] = useState('')
  const [selectedTask, setSelectedTask] = useState('')
  const [description, setDescription] = useState('')
  const [billable, setBillable] = useState(true)
  const [tick, setTick] = useState(() => Date.now())
  const [apiProjects, setApiProjects] = useState<Project[]>([])
  const [apiTasks, setApiTasks] = useState<Task[]>([])
  const [projectsLoaded, setProjectsLoaded] = useState(false)
  const [tasksLoaded, setTasksLoaded] = useState(false)
  const [optionsError, setOptionsError] = useState('')
  const activeProjects = useMemo(() => {
    const projects = projectsLoaded ? apiProjects : data.projects
    return projects.filter((project) => project.status !== 'COMPLETED' && project.status !== 'ARCHIVED')
  }, [apiProjects, data.projects, projectsLoaded])
  const runningEntry = data.time_entries.find((entry) => !entry.ended_at) || null
  const timerProjectId = selectedProject || activeProjects[0]?.id || ''
  const selectedProjectData = data.projects.find((project) => project.id === timerProjectId) || apiProjects.find((project) => project.id === timerProjectId)
  const selectedTasks = tasksLoaded ? apiTasks.filter((task) => task.status !== 'DONE') : data.tasks.filter((task) => task.project_id === timerProjectId && task.status !== 'DONE')
  const runningProject = runningEntry && data.projects.find((project) => project.id === runningEntry.project_id)
  const runningTask = runningEntry && (data.tasks.find((task) => task.id === runningEntry.task_id) || apiTasks.find((task) => task.id === runningEntry.task_id))
  const elapsedSeconds = runningEntry ? Math.max(0, Math.floor((tick - Date.parse(runningEntry.started_at)) / 1000)) : 0

  useEffect(() => {
    let active = true
    listTimerProjects()
      .then((projects) => {
        if (!active) return
        setApiProjects(projects)
        setProjectsLoaded(true)
        setOptionsError('')
      })
      .catch(() => {
        if (active) setOptionsError('ไม่สามารถโหลดโปรเจกต์จาก API ได้')
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!timerProjectId) {
      setApiTasks([])
      setTasksLoaded(true)
      return undefined
    }

    let active = true
    setTasksLoaded(false)
    listTimerTasks(timerProjectId)
      .then((tasks) => {
        if (!active) return
        setApiTasks(tasks)
        setTasksLoaded(true)
        setOptionsError('')
      })
      .catch(() => {
        if (!active) return
        setApiTasks(data.tasks.filter((task) => task.project_id === timerProjectId))
        setTasksLoaded(true)
        setOptionsError('ไม่สามารถโหลดงานจาก API ได้')
      })
    return () => {
      active = false
    }
  }, [data.tasks, timerProjectId])

  useEffect(() => {
    if (!runningEntry) return undefined
    const interval = window.setInterval(() => setTick(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [runningEntry])

  async function startTimer() {
    if (!selectedProjectData || runningEntry) return
    const profile = data.profiles[0]
    await save('time_entries', {
      project_id: timerProjectId,
      task_id: selectedTask || null,
      description: description.trim(),
      started_at: new Date().toISOString(),
      ended_at: null,
      duration_minutes: null,
      billable,
      rate_snapshot: selectedProjectData.billing_type === 'HOURLY' ? (selectedProjectData.hourly_rate || profile?.default_hourly_rate || 0) : 0,
      currency: selectedProjectData.currency || profile?.currency || 'THB',
      invoice_id: null,
    })
    setDescription('')
  }

  async function stopTimer() {
    if (!runningEntry) return
    const endedAt = new Date()
    const duration = Math.max(1, Math.round((endedAt.getTime() - Date.parse(runningEntry.started_at)) / 60000))
    await save('time_entries', { ...runningEntry, ended_at: endedAt.toISOString(), duration_minutes: duration })
  }

  async function cancelTimer() {
    if (runningEntry) await remove('time_entries', runningEntry.id)
  }

  return { activeProjects, runningEntry, timerProjectId, selectedTask, setSelectedProject, setSelectedTask, description, setDescription, billable, setBillable, selectedTasks, runningProject, runningTask, elapsedSeconds, startTimer, stopTimer, cancelTimer, optionsError }
}
