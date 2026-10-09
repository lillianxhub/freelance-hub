import type { ProjectStatus } from '../../types/project'

export const projectStatusLabels: Record<ProjectStatus, string> = {
  PLANNED: 'วางแผน',
  ACTIVE: 'กำลังดำเนินการ',
  ON_HOLD: 'พักงาน',
  COMPLETED: 'เสร็จสิ้น',
  ARCHIVED: 'เก็บถาวร',
}

export const allowedStatusTransitions: Record<ProjectStatus, readonly ProjectStatus[]> = {
  PLANNED: ['PLANNED', 'ACTIVE', 'ARCHIVED'],
  ACTIVE: ['ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED'],
  ON_HOLD: ['ON_HOLD', 'ACTIVE', 'ARCHIVED'],
  COMPLETED: ['COMPLETED', 'ARCHIVED'],
  ARCHIVED: ['ARCHIVED', 'PLANNED', 'ACTIVE'],
}

export const statusSelectClasses: Record<ProjectStatus, string> = {
  PLANNED: '!bg-violet-soft !text-violet',
  ACTIVE: '!bg-green-soft !text-green',
  ON_HOLD: '!bg-orange-soft !text-orange',
  COMPLETED: '!bg-green-soft !text-green',
  ARCHIVED: '!bg-red-soft !text-destructive',
}
