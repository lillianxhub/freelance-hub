import type { FormEvent } from 'react'
import type { Task } from './task'
import type { ResourceInput } from './workspace'

export type TaskDraft = Omit<ResourceInput<'tasks'>, 'project_id' | 'sort_order'> & { project_id?: string; sort_order?: number }

export interface TaskFormProps {
  value: TaskDraft
  error: string
  onChange: (value: TaskDraft) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}

export interface TaskListProps {
  tasks: readonly Task[]
  onToggle: (task: Task) => void
  onMove: (task: Task, direction: -1 | 1) => void
  onEdit: (task: Task) => void
  onDelete: (task: Task) => void
}
