import { Button } from '../../components/ui/button'
import { TablePagination } from '../../components/ui/table'
import type { TimeEntryTableProps } from '../../types/timeTrackerPage'
import { formatDate, formatDurationSeconds } from '../../lib/formatters'
import { FiEdit3, FiTrash2 } from 'react-icons/fi'

const datePartsFormatter = new Intl.DateTimeFormat('en-US', {
  timeZone: 'Asia/Bangkok',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

function dateKey(value: string | Date): string {
  const parts = datePartsFormatter.formatToParts(new Date(value))
  const year = parts.find((part) => part.type === 'year')?.value || ''
  const month = parts.find((part) => part.type === 'month')?.value || ''
  const day = parts.find((part) => part.type === 'day')?.value || ''
  return `${year}-${month}-${day}`
}

function durationSeconds(entry: TimeEntryTableProps['entries'][number]): number {
  return entry.duration_seconds ?? (entry.duration_minutes ?? 0) * 60
}

function durationLabel(seconds: number): string {
  return formatDurationSeconds(seconds).replace(/\sนาที$/, '')
}

interface EntryGroup {
  key: string
  entries: TimeEntryTableProps['entries'][number][]
}

export default function TimeEntryTable({ entries, projects, tasks = [], pagination, onEdit, onDelete }: TimeEntryTableProps) {
  const groups = Array.from(
    entries.reduce((result, entry) => {
      const key = dateKey(entry.started_at)
      const group = result.get(key) || { key, entries: [] }
      group.entries.push(entry)
      result.set(key, group)
      return result
    }, new Map<string, EntryGroup>()).values(),
  ).sort((a, b) => b.key.localeCompare(a.key))

  const todayKey = dateKey(new Date())

  return (
    <div className="w-full pt-4">
      {groups.map((group, groupIndex) => {
        const totalSeconds = group.entries.reduce((total, entry) => total + durationSeconds(entry), 0)
        const groupDate = `${group.key}T00:00:00+07:00`

        return (
          <section key={group.key} className={groupIndex > 0 ? 'pt-5' : ''}>
            <div className="flex items-center justify-between border-b border-border pb-2.5">
              <h3 className="text-base font-semibold text-text-primary">
                {group.key === todayKey ? 'วันนี้ ' : ''}{formatDate(groupDate)}
              </h3>
              <span className="text-base font-medium text-text-secondary">{durationLabel(totalSeconds)}</span>
            </div>

            <div>
              {group.entries.map((entry) => {
                const project = projects.find((item) => item.id === entry.project_id)
                const task = tasks.find((item) => item.id === entry.task_id)?.name || entry.task_name || 'ไม่ระบุงาน'
                const description = entry.description.trim()
                const secondaryText = description ? `${task} — ${description}` : task

                return (
                  <div key={entry.id} className="grid grid-cols-[auto_minmax(0,1fr)_auto_auto] items-center gap-3 border-b border-border py-3 last:border-b-0">
                    <span
                      className="size-2.5 shrink-0 rounded-full"
                      style={{ backgroundColor: project?.color || 'var(--primary)' }}
                      aria-hidden="true"
                    />
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-text-primary">{project?.name || 'ไม่ระบุโปรเจกต์'}</p>
                      <p className="truncate text-xs text-text-secondary">{secondaryText}</p>
                    </div>
                    <strong className="text-sm font-bold tabular-nums text-text-primary">{durationLabel(durationSeconds(entry))}</strong>
                    <div className="flex items-center gap-0.5">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-muted-foreground hover:bg-muted hover:text-text-primary"
                        type="button"
                        onClick={() => onEdit(entry)}
                        aria-label="แก้ไขรายการเวลา"
                        title="แก้ไข"
                      >
                        <FiEdit3 aria-hidden="true" />
                        <span className="hidden sm:inline">แก้ไข</span>
                      </Button>
                      {/* <Button
                        variant="ghost"
                        size="sm"
                        className="text-muted-foreground hover:bg-muted hover:text-text-primary"
                        type="button"
                        onClick={() => onDuplicate(entry)}
                        aria-label="คัดลอกรายการเวลา"
                        title="คัดลอก"
                      >
                        <FiCopy aria-hidden="true" />
                        <span className="hidden sm:inline">คัดลอก</span>
                      </Button> */}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-muted-foreground hover:bg-red-soft hover:text-destructive"
                        type="button"
                        onClick={() => onDelete(entry)}
                        aria-label="ลบรายการเวลา"
                        title="ลบ"
                      >
                        <FiTrash2 aria-hidden="true" />
                        <span className="hidden sm:inline">ลบ</span>
                      </Button>
                    </div>
                  </div>
                )
              })}
            </div>
          </section>
        )
      })}

      {pagination && <TablePagination {...pagination} />}
    </div>
  )
}
