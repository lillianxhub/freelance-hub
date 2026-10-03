import type { Client } from '../types/client'
import type { Project } from '../types/project'
import type { Task } from '../types/task'
import type { TimeEntry } from '../types/timeTracking'

export interface DashboardMockData {
  clients: Client[]
  projects: Project[]
  tasks: Task[]
  time_entries: TimeEntry[]
}

function dateKey(date: Date): string {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
}

function offsetDate(now: Date, days: number): string {
  const date = new Date(now)
  date.setDate(date.getDate() + days)
  return dateKey(date)
}

export function createDashboardMock(now = new Date()): DashboardMockData {
  const owner_id = 'dashboard-mock'
  const clients: Client[] = [
    { id: 'client-northstar', owner_id, name: 'Northstar Studio', company_name: 'Northstar Studio', email: '', phone: '', address: '', province: '', district: '', sub_district: '', postal_code: '', tax_id: '', notes: '', status: 'ACTIVE', color: '#4F6BFF' },
    { id: 'client-orbit', owner_id, name: 'Orbit Labs', company_name: 'Orbit Labs', email: '', phone: '', address: '', province: '', district: '', sub_district: '', postal_code: '', tax_id: '', notes: '', status: 'ACTIVE', color: '#8B5CF6' },
  ]

  const project = (id: string, client_id: string, name: string, color: string, status: Project['status'], endOffset: number, target: number): Project => ({
    id, owner_id, client_id, name, color, status, description: '', billing_type: 'HOURLY', hourly_rate: null,
    fixed_price: null, budget_hours: target, budget_amount: null, currency: 'THB', start_date: offsetDate(now, -30), end_date: offsetDate(now, endOffset),
  })
  const projects = [
    project('project-web', 'client-northstar', 'Website Redesign', '#4F6BFF', 'ACTIVE', 12, 40),
    project('project-app', 'client-orbit', 'Mobile App', '#8B5CF6', 'ACTIVE', 20, 40),
    project('project-brand', 'client-northstar', 'Brand Identity', '#EF4444', 'PLANNED', 35, 20),
    project('project-marketing', 'client-orbit', 'Marketing Campaign', '#10B981', 'ACTIVE', 8, 20),
  ]

  const task = (id: string, project_id: string, name: string, status: Task['status'], dueOffset: number, sort_order: number): Task => ({
    id, owner_id, project_id, name, status, due_date: offsetDate(now, dueOffset), sort_order, description: '',
  })
  const tasks = [
    task('task-1', 'project-web', 'ออกแบบหน้า Dashboard', 'IN_PROGRESS', 2, 1),
    task('task-2', 'project-web', 'วางระบบ Design tokens', 'DONE', -2, 2),
    task('task-3', 'project-web', 'Responsive QA', 'TODO', 5, 3),
    task('task-4', 'project-app', 'เชื่อมต่อ API', 'IN_REVIEW', 3, 1),
    task('task-5', 'project-app', 'ออกแบบ User flow', 'DONE', -4, 2),
    task('task-6', 'project-brand', 'สรุป Brand guideline', 'TODO', 10, 1),
    task('task-7', 'project-marketing', 'เตรียมสไลด์นำเสนอ', 'TODO', 1, 1),
    task('task-8', 'project-marketing', 'กำหนดกลุ่มเป้าหมาย', 'DONE', -1, 2),
  ]

  const entry = (id: string, project_id: string, task_id: string, dayOffset: number, minutes: number, hour: number): TimeEntry => {
    const started = new Date(`${offsetDate(now, dayOffset)}T${String(hour).padStart(2, '0')}:00:00`)
    const ended = new Date(started.getTime() + minutes * 60_000)
    return {
      id, owner_id, project_id, task_id, description: '', started_at: started.toISOString(), ended_at: ended.toISOString(),
      duration_minutes: minutes, duration_seconds: minutes * 60, billable: true, rate_snapshot: 0, currency: 'THB',
    }
  }
  const runningStartedAt = new Date(now.getTime() - 84 * 60_000).toISOString()
  const time_entries: TimeEntry[] = [
    { id: 'entry-running', owner_id, project_id: 'project-web', task_id: 'task-1', description: 'ปรับหน้า Dashboard', started_at: runningStartedAt, ended_at: null, duration_minutes: null, duration_seconds: null, billable: true, rate_snapshot: 0, currency: 'THB' },
    entry('entry-1', 'project-web', 'task-1', 0, 150, 9),
    entry('entry-2', 'project-marketing', 'task-7', -1, 225, 10),
    // entry('entry-3', 'project-app', 'task-4', -2, 300, 9),
    // entry('entry-4', 'project-web', 'task-2', -3, 180, 13),
    // entry('entry-5', 'project-app', 'task-5', -4, 270, 9),
    // entry('entry-6', 'project-brand', 'task-6', -5, 120, 14),
    // entry('entry-7', 'project-web', 'task-3', -6, 240, 9),
    // entry('entry-8', 'project-marketing', 'task-8', -8, 210, 10),
    // entry('entry-9', 'project-app', 'task-5', -10, 240, 9),
  ]

  return { clients, projects, tasks, time_entries }
}
