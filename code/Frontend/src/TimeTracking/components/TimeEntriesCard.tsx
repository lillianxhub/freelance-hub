import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card'
import { Label } from '../../components/ui/label'
import { NativeSelect } from '../../components/ui/native-select'
import { DatePicker } from '../../components/ui/date-picker'
import { ToggleGroup, ToggleGroupItem } from '../../components/ui/toggle-group'
import { EmptyState, ErrorState, LoadingState } from '../../components/ViewState'
import type { Client } from '../../types/client'
import type { Project } from '../../types/project'
import type { Task } from '../../types/task'
import type { TimeEntry } from '../../types/timeTracking'
import type { RangePreset, TimeEntryTableProps, TimeFilters } from '../../types/timeTrackerPage'
import type { TablePaginationProps } from '../../components/ui/table'
import TimeEntryTable from './TimeEntryTable'
import { formatDurationSeconds } from '../../lib/formatters'
import { FiClock } from 'react-icons/fi'

interface TimeEntrySummary {
  entryCount: number
  totalSeconds: number
}

interface TimeEntriesCardProps {
  filters: TimeFilters
  rangePreset: RangePreset | ''
  clients: readonly Client[]
  projects: readonly Project[]
  filterTasks: readonly Task[]
  entries: readonly TimeEntry[]
  summary: TimeEntrySummary
  loading: boolean
  error: string
  pagination: TablePaginationProps
  onRangeChange: (preset: RangePreset) => void
  onClientChange: (value: string) => void
  onProjectChange: (value: string) => void
  onTaskChange: (value: string) => void
  onDateChange: (field: 'from' | 'to', value: string) => void
  onRetry: () => void
  onEdit: TimeEntryTableProps['onEdit']
  onDelete: TimeEntryTableProps['onDelete']
  onDuplicate: TimeEntryTableProps['onDuplicate']
}

export default function TimeEntriesCard({
  filters,
  rangePreset,
  clients,
  projects,
  filterTasks,
  entries,
  summary,
  loading,
  error,
  pagination,
  onRangeChange,
  onClientChange,
  onProjectChange,
  onTaskChange,
  onDateChange,
  onRetry,
  onEdit,
  onDelete,
  onDuplicate,
}: TimeEntriesCardProps) {
  return (
    <Card asChild>
      <section className="!p-0">
        <CardHeader className="p-6 pb-2">
          <div>
            <CardTitle>รายการเวลา</CardTitle>
            <CardDescription className="mt-1 text-sm text-text-secondary">
              รวม <strong className="text-text-primary">{formatDurationSeconds(summary.totalSeconds).replace(/\sนาที$/, '')}</strong> จาก {summary.entryCount} รายการ
            </CardDescription>
          </div>
          <CardAction>
            <ToggleGroup
              type="single"
              value={rangePreset}
              onValueChange={(value) => {
                if (value) onRangeChange(value as RangePreset)
              }}
              aria-label="ช่วงเวลารายการเวลา"
            >
              <ToggleGroupItem value="ALL">ทั้งหมด</ToggleGroupItem>
              <ToggleGroupItem value="DAY">วันนี้</ToggleGroupItem>
              <ToggleGroupItem value="WEEK">สัปดาห์นี้</ToggleGroupItem>
            </ToggleGroup>
          </CardAction>
        </CardHeader>

        <CardContent className="p-6 pt-0">
          <div className="mb-4 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="time-filter-client">ลูกค้า</Label>
              <NativeSelect
                id="time-filter-client"
                value={filters.client}
                onChange={(event) => onClientChange(event.target.value)}
              >
                <option value="ALL">ทุกลูกค้า</option>
                {clients.map((client) => (
                  <option key={client.id} value={client.id}>
                    {client.company_name || client.name}
                  </option>
                ))}
              </NativeSelect>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="time-filter-project">โปรเจกต์</Label>
              <NativeSelect
                id="time-filter-project"
                value={filters.project}
                onChange={(event) => onProjectChange(event.target.value)}
              >
                <option value="ALL">ทุกโปรเจกต์</option>
                {projects
                  .filter((project) => filters.client === 'ALL' || project.client_id === filters.client)
                  .map((project) => (
                    <option key={project.id} value={project.id}>
                      {project.name}
                    </option>
                  ))}
              </NativeSelect>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="time-filter-task">งาน</Label>
              <NativeSelect
                id="time-filter-task"
                value={filters.task}
                disabled={filters.project === 'ALL'}
                onChange={(event) => onTaskChange(event.target.value)}
              >
                <option value="ALL">ทุกงาน</option>
                {filterTasks.map((task) => (
                  <option key={task.id} value={task.id}>
                    {task.name}
                  </option>
                ))}
              </NativeSelect>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="time-filter-from">ตั้งแต่วันที่</Label>
              <DatePicker
                id="time-filter-from"
                value={filters.from}
                onChange={(value) => onDateChange('from', value)}
                aria-label="ตั้งแต่วันที่"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="time-filter-to">ถึงวันที่</Label>
              <DatePicker
                id="time-filter-to"
                value={filters.to}
                onChange={(value) => onDateChange('to', value)}
                aria-label="ถึงวันที่"
              />
            </div>
          </div>

          {loading && entries.length === 0 ? (
            <LoadingState label="กำลังโหลดรายการเวลา..." />
          ) : error && entries.length === 0 ? (
            <ErrorState message={error} onRetry={onRetry} />
          ) : entries.length === 0 ? (
            <EmptyState
              icon={<FiClock aria-hidden="true" />}
              title="ไม่มี Time entries"
              description="ลองเปลี่ยนตัวกรองหรือเพิ่ม Time entries ใหม่"
            />
          ) : (
            <TimeEntryTable
              entries={entries}
              projects={projects}
              pagination={pagination}
              onEdit={onEdit}
              onDelete={onDelete}
              onDuplicate={onDuplicate}
            />
          )}
        </CardContent>
      </section>
    </Card>
  )
}
