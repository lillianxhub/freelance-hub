import type { ManualTimeFieldErrors, ManualTimeFieldName, ManualTimeForm } from '../types/timeTrackerPage'

export function validateManualTimeField(
  field: ManualTimeFieldName,
  value: ManualTimeForm,
): string | undefined {
  if (field === 'project_id' && !value.project_id) return 'กรุณาเลือกโปรเจกต์'
  if (field === 'task_id' && !value.task_id) return 'กรุณาเลือกงาน'
  if (field === 'entry_date' && !/^\d{4}-\d{2}-\d{2}$/.test(value.entry_date)) return 'กรุณาเลือกวันที่'
  if (field === 'manual_mode' && !value.manual_mode) return 'กรุณาเลือกวิธีระบุเวลา'
  if (field === 'start_time' && !value.start_time) return 'กรุณาระบุเวลาเริ่ม'

  if (field === 'end_time') {
    if (!value.end_time) return 'กรุณาระบุเวลาสิ้นสุด'
    if (value.start_time && value.end_time <= value.start_time) return 'เวลาสิ้นสุดต้องหลังเวลาเริ่ม'
  }

  if (field === 'duration_minutes' && Number(value.duration_minutes) <= 0) {
    return 'ระยะเวลาต้องมากกว่า 0 นาที'
  }

  return undefined
}

export function validateManualTimeForm(value: ManualTimeForm): ManualTimeFieldErrors {
  const fields: ManualTimeFieldName[] = [
    'project_id',
    'task_id',
    'entry_date',
    'manual_mode',
    'start_time',
    value.manual_mode === 'RANGE' ? 'end_time' : 'duration_minutes',
  ]

  return fields.reduce<ManualTimeFieldErrors>((errors, field) => {
    const error = validateManualTimeField(field, value)
    if (error) errors[field] = error
    return errors
  }, {})
}
