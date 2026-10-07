import type { OwnedRecord } from './common'

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED'

export interface Task extends OwnedRecord {
  project_id: string
  name: string
  description: string
  status: TaskStatus
  sort_order: number
  due_date: string
}

export type TaskInput = Omit<Task, keyof OwnedRecord> & Partial<OwnedRecord>
