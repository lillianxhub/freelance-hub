import { useEffect, useState } from 'react'
import { FiSquare } from 'react-icons/fi'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card'
import { formatDate, formatDuration, formatTimer } from '../../lib/formatters'
import type { Project } from '../../types/project'
import type { Task } from '../../types/task'
import type { TimeEntry } from '../../types/timeTracking'
import { DASHBOARD_LIMITS } from '../dashboard.constants'

interface Props { entries: readonly TimeEntry[]; projects: readonly Project[]; tasks: readonly Task[]; onStop: () => Promise<void> }

export default function DashboardTimerCard({ entries, projects, tasks, onStop }: Props) {
  const initialRunningEntry = entries.find((entry) => !entry.ended_at) || null
  const [runningEntry, setRunningEntry] = useState(initialRunningEntry)
  const [tick, setTick] = useState(() => Date.now())
  const recentEntries = entries.filter((entry) => entry.ended_at).sort((a, b) => b.started_at.localeCompare(a.started_at)).slice(0, DASHBOARD_LIMITS.recentTimeEntries)
  const latestEntry = recentEntries[0]
  const runningProject = projects.find((project) => project.id === runningEntry?.project_id)
  const runningTask = tasks.find((task) => task.id === runningEntry?.task_id)
  const latestProject = projects.find((project) => project.id === latestEntry?.project_id)
  const latestTask = tasks.find((task) => task.id === latestEntry?.task_id)
  const elapsedSeconds = runningEntry ? Math.max(0, Math.floor((tick - Date.parse(runningEntry.started_at)) / 1000)) : 0

  useEffect(() => {
    if (!runningEntry) return undefined
    const interval = window.setInterval(() => setTick(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [runningEntry])

  useEffect(() => {
    setRunningEntry(initialRunningEntry)
  }, [initialRunningEntry])

  return (
    <Card>
      <CardHeader>
        <CardTitle>ตัวจับเวลา</CardTitle>
        <CardDescription>ดูสถานะและรายการเวลาล่าสุด</CardDescription>
        <CardAction>
          <Badge variant="secondary" className={runningEntry ? 'bg-green-soft text-green' : 'bg-surface-soft text-muted-foreground'}>
            <span className={runningEntry ? 'size-2 rounded-full bg-green' : 'size-2 rounded-full bg-subtle'} />
            {runningEntry ? 'กำลังทำงาน' : 'ไม่ได้จับเวลา'}
          </Badge>
        </CardAction>
      </CardHeader>
      <CardContent className="space-y-5">
        {runningEntry ? <div className="rounded-xl bg-primary-soft p-4">
          <p className="font-semibold text-text-primary">{runningProject?.name || 'ไม่ระบุโปรเจกต์'}</p>
          {runningTask?.name &&
            <p className="mt-1 text-sm text-primary">
              {runningTask.name}
            </p>}
          {runningEntry.description &&
            <p className="mt-1 truncate text-xs text-muted-foreground">
              {runningEntry.description}
            </p>
          }
          <p className="mt-5 text-center text-3xl font-bold tracking-wider tabular-nums text-text-primary">{formatTimer(elapsedSeconds)}</p>
          <p className="mt-1 text-center text-xs text-muted-foreground">เริ่มเมื่อ {formatDate(runningEntry.started_at, { hour: '2-digit', minute: '2-digit' })}</p>
          <Button className="mt-4 w-full" variant="destructive" onClick={() => void onStop()}>
            <FiSquare aria-hidden="true" /> หยุดจับเวลา
          </Button>
        </div> : latestEntry ?
          <div className="rounded-xl bg-primary-soft p-4">
            <p className="font-semibold text-text-primary">{latestEntry.project_name || latestProject?.name || 'ไม่ระบุโปรเจกต์'}</p>
            <p className="mt-1 text-sm text-primary">{latestEntry.task_name || latestTask?.name || 'ไม่ระบุงาน'}</p>
            {latestEntry.description && <p className="mt-1 truncate text-xs text-muted-foreground">{latestEntry.description}</p>}
            <p className="mt-5 text-center text-3xl font-bold tracking-wider tabular-nums text-text-primary">
              {formatTimer(Number(latestEntry.duration_seconds ?? Number(latestEntry.duration_minutes || 0) * 60))}
            </p>
            <p className="mt-1 text-center text-xs text-muted-foreground">รายการล่าสุด · {formatDate(latestEntry.started_at, { hour: '2-digit', minute: '2-digit' })}</p>
          </div> :
          <div className="rounded-xl bg-primary-soft p-6 text-center">
            <p className="font-medium text-text-primary">ยังไม่มีรายการเวลา</p>
            <p className="mt-1 text-xs text-muted-foreground">เมื่อบันทึกเวลาแล้ว รายการล่าสุดจะแสดงที่นี่</p>
          </div>
          }
        <div>
          <p className="mb-2 text-sm font-semibold text-text-primary">รายการเวลาล่าสุด</p>
          <div className="divide-y divide-border">
            {recentEntries.length ? recentEntries.map((entry) => {
              const project = projects.find((item) => item.id === entry.project_id)
              const task = tasks.find((item) => item.id === entry.task_id)
              return <div className="flex items-center justify-between gap-3 py-3" key={entry.id}>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-text-primary">
                    {entry.project_name || project?.name || 'ไม่ระบุโปรเจกต์'}
                  </p>
                  <p className="truncate text-xs text-muted-foreground">
                    {entry.task_name || task?.name || 'ไม่ระบุงาน'} · {formatDate(entry.started_at)}</p>
                </div>
                <strong className="shrink-0 text-sm tabular-nums text-text-primary">
                  {formatDuration(entry.duration_minutes)}
                </strong>
              </div>
            }) : <p className="py-4 text-center text-sm text-muted-foreground">ยังไม่มีรายการเวลา</p>}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
