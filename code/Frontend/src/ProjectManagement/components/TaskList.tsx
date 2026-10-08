import { Button } from '../../components/ui/button'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '../../components/ui/alert-dialog'
import { useState } from 'react'
import type { TaskListProps } from '../../types/projectDetailPage'
import type { Task } from '../../types/task'
import StatusBadge from '../../components/StatusBadge'
import { Link } from 'react-router-dom'
import { FiArrowDown, FiArrowUp, FiCheck, FiEdit2, FiTrash2 } from 'react-icons/fi'

export default function TaskList({ tasks, startIndex = 0, totalTasks = tasks.length, changingTaskId, onToggle, onMove, onEdit, onDelete }: TaskListProps) {
  const [deleteTask, setDeleteTask] = useState<Task | null>(null)
  return (
    <div className="flex flex-col">
      {tasks.map((task, index) => (
        <article
          className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-start gap-3 border-b border-border py-3 last:border-b-0 lg:grid-cols-[auto_minmax(0,1fr)_auto_auto] lg:items-center"
          key={task.id}
        >
          <Button
            variant="ghost"
            size="icon-sm"
            className={`rounded-md border ${task.status === 'COMPLETED' ? 'border-green bg-green text-white hover:bg-green hover:text-white' : 'border-border bg-background text-text-secondary'}`}
            type="button"
            aria-label={task.status === 'COMPLETED' ? 'กลับไปทำงานต่อ' : 'ทำเครื่องหมายว่าเสร็จแล้ว'}
            aria-pressed={task.status === 'COMPLETED'}
            disabled={task.status === 'OPEN' || changingTaskId !== null}
            onClick={() => onToggle(task)}
          >
            {task.status === 'COMPLETED' && <FiCheck aria-hidden="true" />}
          </Button>
          <div className="min-w-0">
            {task.status === 'COMPLETED' ? (
              <strong className="block truncate text-sm font-semibold text-text-primary" title={task.name}>{task.name}</strong>
            ) : (
              <Link
                className="block truncate text-sm font-semibold text-text-primary hover:text-primary hover:underline"
                title={`จับเวลา: ${task.name}`}
                to={`/time-tracker?projectId=${encodeURIComponent(task.project_id)}&taskId=${encodeURIComponent(task.id)}`}
              >
                {task.name}
              </Link>
            )}
            <small className="mt-1 block truncate text-xs text-text-secondary">{task.description || 'ไม่มีรายละเอียด'}</small>
          </div>
          <StatusBadge className="shrink-0" status={task.status} />
          <div className="col-start-2 col-end-[-1] flex items-center gap-1 lg:col-auto">
            <Button
              variant="ghost"
              size="icon-sm"
              type="button"
              aria-label="เลื่อนงานขึ้น"
              disabled={startIndex + index === 0}
              onClick={() => onMove(task, -1)}
            >
              <FiArrowUp aria-hidden="true" />
            </Button>
            <Button
              variant="ghost"
              size="icon-sm"
              type="button"
              aria-label="เลื่อนงานลง"
              disabled={startIndex + index === totalTasks - 1}
              onClick={() => onMove(task, 1)}
            >
              <FiArrowDown aria-hidden="true" />
            </Button>
            <Button
              variant="ghost"
              size="icon-sm"
              type="button"
              aria-label="แก้ไขงาน"
              onClick={() => onEdit(task)}
            >
              <FiEdit2 aria-hidden="true" />
            </Button>
            <Button
              variant="ghost"
              size="icon-sm"
              type="button"
              aria-label="ลบงาน"
              onClick={() => setDeleteTask(task)}
            >
              <FiTrash2 aria-hidden="true" />
            </Button>
          </div>
        </article>
      ))}
      {tasks.length === 0 && <p className="m-0 py-7 text-center text-sm text-text-secondary">ยังไม่มีงานในโปรเจกต์นี้</p>}
      <AlertDialog open={deleteTask !== null} onOpenChange={(open) => { if (!open) setDeleteTask(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>ลบงานนี้หรือไม่</AlertDialogTitle>
            <AlertDialogDescription>งาน “{deleteTask?.name}” จะถูกลบออกจากโปรเจกต์</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>ยกเลิก</AlertDialogCancel>
            <AlertDialogAction onClick={() => { if (deleteTask) onDelete(deleteTask); setDeleteTask(null) }}>ลบงาน</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
