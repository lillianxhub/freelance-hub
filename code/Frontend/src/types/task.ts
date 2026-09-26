import type { OwnedRecord } from './common'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'

export interface Task extends OwnedRecord {
  project_id: string
  name: string
  description: string
  status: TaskStatus
  sort_order: number
  due_date: string
}
