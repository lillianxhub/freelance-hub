import { Button } from '../../../components/ui/button'
import { useCallback, useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { FiPlus } from 'react-icons/fi'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../../components/ui/dialog'
import PageHeader from '../../../components/PageHeader'
import { ErrorState, LoadingState } from '../../../components/ViewState'
import { useTimeEntries } from '../../useTimeEntries'
import type { TimeEntry } from '../../../types/timeTracking'
import type { RangePreset, TimeFilters } from '../../../types/timeTrackerPage'
import TimerPanel from '../../components/TimerPanel'
import TimeEntriesCard from '../../components/TimeEntriesCard'
import TimeEntryForm from '../../components/TimeEntryForm'
import { createEmptyManualForm, localDateValue } from '../../timeTracking.utils'
import { listTimerTasks } from '../../../services/timerOptions'
import {
  createManualTimeEntry,
  listTimeEntriesPage,
  summarizeTimeEntries,
  updateTimeEntry,
} from '../../../services/timeTracking'
import { getErrorMessage } from '../../../api/apiError'
import type { Task } from '../../../types/task'

const TIME_ENTRY_PAGE_LIMIT = 5

function toApiDateBoundary(value: string, endExclusive = false): string | undefined {
  if (!value) return undefined
  const date = new Date(`${value}T00:00:00+07:00`)
  if (endExclusive) date.setUTCDate(date.getUTCDate() + 1)
  return date.toISOString()
}

function TimeTrackerPage() {
  const workspace = useTimeEntries()
  const { data, loading, error, refresh, saveTimeEntry, deleteTimeEntry } = workspace
  const [manualOpen, setManualOpen] = useState(false)
  const [manualForm, setManualForm] = useState(createEmptyManualForm())
  const [manualTaskResult, setManualTaskResult] = useState<{
    projectId: string
    tasks: Task[]
  } | null>(null)
  const [filterTaskResult, setFilterTaskResult] = useState<{
    projectId: string
    tasks: Task[]
  } | null>(null)
  const [formError, setFormError] = useState('')
  const [filters, setFilters] = useState<TimeFilters>({
    client: 'ALL',
    project: 'ALL',
    task: 'ALL',
    billable: 'ALL',
    from: '',
    to: '',
  })
  const [timeEntryPage, setTimeEntryPage] = useState(1)
  const [rangePreset, setRangePreset] = useState<RangePreset | ''>('ALL')
  const [timeEntryResult, setTimeEntryResult] = useState<{
    key: string
    entries: TimeEntry[]
    meta: { page: number; limit: number; total: number; totalPages: number }
    summary: { entryCount: number; totalSeconds: number }
    error: string
  } | null>(null)
  const [timeEntriesRequestKey, setTimeEntriesRequestKey] = useState(0)

  const reloadTimeEntryData = useCallback(() => {
    setTimeEntriesRequestKey((current) => current + 1)
  }, [])

  const activeProjects = useMemo(
    () =>
      (data?.projects || []).filter(
        (project) => project.status !== 'COMPLETED' && project.status !== 'ARCHIVED',
      ),
    [data.projects],
  )
  const manualTasksLoaded = Boolean(
    manualOpen && manualForm.project_id && manualTaskResult?.projectId === manualForm.project_id,
  )
  const manualTasks = manualTasksLoaded ? (manualTaskResult?.tasks ?? []) : []
  const filterTasks =
    filters.project === 'ALL' || filterTaskResult?.projectId !== filters.project
      ? []
      : filterTaskResult.tasks
  const timeEntryQueryKey = JSON.stringify([filters, timeEntryPage, timeEntriesRequestKey])
  const currentTimeEntryResult = timeEntryResult?.key === timeEntryQueryKey ? timeEntryResult : null
  const serverEntries = currentTimeEntryResult?.entries ?? []
  const timeEntryMeta = currentTimeEntryResult?.meta ?? {
    page: 1,
    limit: TIME_ENTRY_PAGE_LIMIT,
    total: 0,
    totalPages: 0,
  }
  const timeEntrySummary = currentTimeEntryResult?.summary ?? { entryCount: 0, totalSeconds: 0 }
  const timeEntriesLoading = !loading && !currentTimeEntryResult
  const timeEntriesError = currentTimeEntryResult?.error ?? ''

  useEffect(() => {
    if (!manualOpen || !manualForm.project_id) return undefined

    let active = true
    const projectId = manualForm.project_id
    listTimerTasks(projectId)
      .then((tasks) => {
        if (!active) return
        setManualTaskResult({ projectId, tasks })
      })
      .catch(() => {
        if (!active) return
        setManualTaskResult({ projectId, tasks: [] })
      })

    return () => {
      active = false
    }
  }, [manualForm.project_id, manualOpen])

  useEffect(() => {
    if (filters.project === 'ALL') return undefined

    let active = true
    const projectId = filters.project
    listTimerTasks(projectId)
      .then((tasks) => {
        if (active) setFilterTaskResult({ projectId, tasks })
      })
      .catch(() => {
        if (active) setFilterTaskResult({ projectId, tasks: [] })
      })

    return () => {
      active = false
    }
  }, [filters.project])

  useEffect(() => {
    if (loading) return undefined
    let active = true

    const query = {
      clientId: filters.client === 'ALL' ? undefined : filters.client,
      projectId: filters.project === 'ALL' ? undefined : filters.project,
      taskId: filters.task === 'ALL' ? undefined : filters.task,
      from: toApiDateBoundary(filters.from),
      to: toApiDateBoundary(filters.to, true),
      page: timeEntryPage,
      limit: TIME_ENTRY_PAGE_LIMIT,
    }

    Promise.all([listTimeEntriesPage(query), summarizeTimeEntries(query)])
      .then(([pageResult, summary]) => {
        if (!active) return
        setTimeEntryResult({
          key: timeEntryQueryKey,
          entries: pageResult.entries,
          meta: pageResult.meta,
          summary,
          error: '',
        })
      })
      .catch((reason: unknown) => {
        if (!active) return
        setTimeEntryResult({
          key: timeEntryQueryKey,
          entries: [],
          meta: { page: timeEntryPage, limit: TIME_ENTRY_PAGE_LIMIT, total: 0, totalPages: 0 },
          summary: { entryCount: 0, totalSeconds: 0 },
          error: getErrorMessage(reason, 'ไม่สามารถโหลดรายการเวลาได้'),
        })
      })

    return () => {
      active = false
    }
  }, [filters, loading, timeEntryPage, timeEntryQueryKey])

  if (loading) return <LoadingState label="กำลังโหลดรายการเวลา..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const entries = serverEntries.filter((entry) => entry.ended_at)
  const safeTimeEntryPage = Math.min(timeEntryPage, Math.max(1, timeEntryMeta.totalPages))
  const visibleTimeEntries = entries
  const updateFilters = (update: (current: TimeFilters) => TimeFilters) => {
    setFilters(update)
    setTimeEntryPage(1)
  }

  const openManual = (entry: TimeEntry | null = null) => {
    if (entry) {
      const start = new Date(entry.started_at)
      const end = new Date(entry.ended_at ?? entry.started_at)
      setManualForm({
        ...entry,
        task_id: entry.task_id || '',
        duration_minutes: entry.duration_minutes ?? 0,
        entry_date: localDateValue(start),
        start_time: start.toTimeString().slice(0, 5),
        end_time: end.toTimeString().slice(0, 5),
        manual_mode: 'RANGE',
      })
    } else {
      const projectId = activeProjects[0]?.id || ''
      setManualForm(createEmptyManualForm(projectId))
    }
    setFormError('')
    setManualOpen(true)
  }

  const handleManualChange = (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = event.target
    const fieldValue =
      event.target instanceof HTMLInputElement && event.target.type === 'checkbox'
        ? event.target.checked
        : value
    setManualForm((current) => ({
      ...current,
      [name]: fieldValue,
      ...(name === 'project_id' ? { task_id: '' } : {}),
    }))
  }

  const saveManualEntry = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const project = data.projects.find((item) => item.id === manualForm.project_id)
    if (!project) {
      setFormError('กรุณาเลือกโปรเจกต์')
      return
    }

    const startedAt = new Date(`${manualForm.entry_date}T${manualForm.start_time}:00`)
    let endedAt: Date
    let duration: number
    if (manualForm.manual_mode === 'RANGE') {
      endedAt = new Date(`${manualForm.entry_date}T${manualForm.end_time}:00`)
      duration = Math.round((endedAt.getTime() - startedAt.getTime()) / 60000)
    } else {
      duration = Number(manualForm.duration_minutes)
      endedAt = new Date(startedAt.getTime() + duration * 60000)
    }
    if (!Number.isFinite(duration) || duration <= 0) {
      setFormError('เวลาเริ่มต้องน้อยกว่าเวลาสิ้นสุดและระยะเวลาต้องมากกว่า 0')
      return
    }

    try {
      if (manualForm.id) {
        await updateTimeEntry(manualForm.id, {
          projectId: manualForm.project_id,
          ...(manualForm.task_id ? { taskId: manualForm.task_id } : { clearTask: true }),
          description: manualForm.description,
          startedAt: startedAt.toISOString(),
          endedAt: endedAt.toISOString(),
        })
      } else {
        await createManualTimeEntry({
          projectId: manualForm.project_id,
          taskId: manualForm.task_id || null,
          description: manualForm.description,
          startedAt: startedAt.toISOString(),
          ...(manualForm.manual_mode === 'RANGE'
            ? { endedAt: endedAt.toISOString() }
            : { durationSeconds: duration * 60 }),
        })
      }
      reloadTimeEntryData()
      setManualOpen(false)
    } catch (saveError: unknown) {
      setFormError(getErrorMessage(saveError, 'บันทึกรายการเวลาไม่สำเร็จ'))
    }
  }

  const duplicateEntry = async (entry: TimeEntry) => {
    const duplicate = {
      ...entry,
      id: undefined,
      started_at: new Date().toISOString(),
      ended_at: new Date(Date.now() + (entry.duration_minutes ?? 0) * 60000).toISOString(),
    }
    await saveTimeEntry(duplicate)
    reloadTimeEntryData()
  }

  const applyRange = (preset: RangePreset) => {
    if (preset === 'ALL') {
      updateFilters((current) => ({ ...current, from: '', to: '' }))
      return
    }
    const now = new Date()
    const local = (date: Date) =>
      new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
    if (preset === 'DAY') {
      const today = local(now)
      updateFilters((current) => ({ ...current, from: today, to: today }))
      return
    }
    const weekday = now.getDay() || 7
    const start = new Date(now)
    start.setDate(start.getDate() - weekday + 1)
    updateFilters((current) => ({
      ...current,
      from: local(start),
      to: local(now),
    }))
  }

  return (
    <div className="mx-auto w-full max-w-auto">
      <PageHeader
        title="บันทึกเวลา"
        actions={
          <Button variant="default" className="h-10" type="button" onClick={() => openManual()}>
            <FiPlus aria-hidden="true" /> เพิ่มเวลาด้วยตนเอง
          </Button>
        }
      />

      <div className="mb-4 grid grid-cols-[minmax(0,1fr)] items-stretch gap-4">
        <TimerPanel
          workspace={workspace}
          onProjectOptionsOpen={() => {
            void refresh()
          }}
          onTimerChanged={reloadTimeEntryData}
        />
      </div>

      <TimeEntriesCard
        filters={filters}
        rangePreset={rangePreset}
        clients={data.clients}
        projects={data.projects}
        filterTasks={filterTasks}
        entries={visibleTimeEntries}
        summary={timeEntrySummary}
        loading={timeEntriesLoading}
        error={timeEntriesError}
        pagination={{
          page: safeTimeEntryPage,
          totalPages: Math.max(1, timeEntryMeta.totalPages),
          total: timeEntryMeta.total,
          onPageChange: setTimeEntryPage,
        }}
        onRangeChange={(preset) => {
          setRangePreset(preset)
          applyRange(preset)
        }}
        onClientChange={(value) =>
          updateFilters((current) => ({
            ...current,
            client: value,
            project: 'ALL',
            task: 'ALL',
          }))
        }
        onProjectChange={(value) =>
          updateFilters((current) => ({ ...current, project: value, task: 'ALL' }))
        }
        onTaskChange={(value) => updateFilters((current) => ({ ...current, task: value }))}
        onDateChange={(field, value) => {
          setRangePreset('')
          updateFilters((current) => ({ ...current, [field]: value }))
        }}
        onRetry={reloadTimeEntryData}
        onEdit={openManual}
        onDelete={async (entry) => {
          await deleteTimeEntry(entry.id)
          reloadTimeEntryData()
        }}
        onDuplicate={duplicateEntry}
      />

      <Dialog
        open={manualOpen}
        onOpenChange={(open) => {
          if (!open) setManualOpen(false)
        }}
      >
        <DialogContent className="max-h-[calc(100dvh-2rem)] !max-w-4xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{manualForm.id ? 'แก้ไขเวลา' : 'เพิ่มเวลา'}</DialogTitle>
          </DialogHeader>
          <TimeEntryForm
            value={manualForm}
            projects={data.projects}
            tasks={manualTasksLoaded ? manualTasks : []}
            error={formError}
            onChange={handleManualChange}
            onSubmit={saveManualEntry}
            onCancel={() => setManualOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default TimeTrackerPage
