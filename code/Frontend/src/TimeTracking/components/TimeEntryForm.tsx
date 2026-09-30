import { NativeSelect } from '../../components/ui/native-select'
import { Input } from '../../components/ui/input'
import { Button } from '../../components/ui/button'
import { useState, type ChangeEvent, type FocusEvent, type FormEvent } from 'react'
import FormLabel from '../../components/FormLabel'
import type { FieldErrorProps } from '../../types/ui'
import type { ManualTimeFieldErrors, ManualTimeFieldName, TimeEntryFormProps } from '../../types/timeTrackerPage'
import { validateManualTimeField, validateManualTimeForm } from '../timeTracking.validators'

function FieldError({ id, message }: FieldErrorProps) {
  return message ? <p id={id} className="field-error">{message}</p> : null
}

function TimeEntryForm({ value, projects, tasks, error, onChange, onSubmit, onCancel }: TimeEntryFormProps) {
  const [fieldErrors, setFieldErrors] = useState<ManualTimeFieldErrors>({})

  const updateFieldError = (field: ManualTimeFieldName, nextValue: typeof value) => {
    if (!fieldErrors[field]) return
    setFieldErrors((current) => ({ ...current, [field]: validateManualTimeField(field, nextValue) }))
  }

  const handleChange = (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const field = event.target.name as ManualTimeFieldName
    const nextValue = {
      ...value,
      [field]: event.target.value,
      ...(field === 'project_id' ? { task_id: '' } : {}),
    }
    onChange(event)
    updateFieldError(field, nextValue)
  }

  const handleBlur = (event: FocusEvent<HTMLInputElement | HTMLSelectElement>) => {
    const field = event.target.name as ManualTimeFieldName
    setFieldErrors((current) => ({ ...current, [field]: validateManualTimeField(field, value) }))
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    const validationErrors = validateManualTimeForm(value)
    setFieldErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      event.preventDefault()
      return
    }
    onSubmit(event)
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {error && <p className="form-error">{error}</p>}
      <div className="form-grid">
        <div className="form-field">
          <FormLabel htmlFor="manual-project" required>โปรเจกต์</FormLabel>
          <NativeSelect id="manual-project" name="project_id" value={value.project_id} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.project_id)} aria-describedby={fieldErrors.project_id ? 'manual-project-error' : undefined}>
            <option value="">เลือกโปรเจกต์</option>
            {projects.filter((project) => project.status !== 'ARCHIVED').map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
          </NativeSelect>
          <FieldError id="manual-project-error" message={fieldErrors.project_id} />
        </div>
        <div className="form-field">
          <FormLabel htmlFor="manual-task" required>งาน</FormLabel>
          <NativeSelect id="manual-task" name="task_id" value={value.task_id || ''} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.task_id)} aria-describedby={fieldErrors.task_id ? 'manual-task-error' : undefined}>
            <option value="">เลือกงาน</option>
            {tasks.filter((task) => task.project_id === value.project_id && task.status !== 'DONE').map((task) => <option key={task.id} value={task.id}>{task.name}</option>)}
          </NativeSelect>
          <FieldError id="manual-task-error" message={fieldErrors.task_id} />
        </div>
        <div className="form-field full">
          <FormLabel htmlFor="manual-description">คำอธิบาย</FormLabel>
          <Input id="manual-description" name="description" value={value.description} onChange={handleChange} />
        </div>
        <div className="form-field">
          <FormLabel htmlFor="manual-date" required>วันที่</FormLabel>
          <Input id="manual-date" name="entry_date" type="date" value={value.entry_date} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.entry_date)} aria-describedby={fieldErrors.entry_date ? 'manual-date-error' : undefined} />
          <FieldError id="manual-date-error" message={fieldErrors.entry_date} />
        </div>
        <div className="form-field">
          <FormLabel htmlFor="manual-mode" required>วิธีระบุเวลา</FormLabel>
          <NativeSelect id="manual-mode" name="manual_mode" value={value.manual_mode} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.manual_mode)} aria-describedby={fieldErrors.manual_mode ? 'manual-mode-error' : undefined}>
            <option value="RANGE">เวลาเริ่ม–สิ้นสุด</option>
            <option value="DURATION">ระยะเวลา</option>
          </NativeSelect>
          <FieldError id="manual-mode-error" message={fieldErrors.manual_mode} />
        </div>
        <div className="form-field">
          <FormLabel htmlFor="manual-start" required>เวลาเริ่ม</FormLabel>
          <Input id="manual-start" name="start_time" type="time" value={value.start_time} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.start_time)} aria-describedby={fieldErrors.start_time ? 'manual-start-error' : undefined} />
          <FieldError id="manual-start-error" message={fieldErrors.start_time} />
        </div>
        {value.manual_mode === 'RANGE' ? (
          <div className="form-field">
            <FormLabel htmlFor="manual-end" required>เวลาสิ้นสุด</FormLabel>
            <Input id="manual-end" name="end_time" type="time" value={value.end_time} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.end_time)} aria-describedby={fieldErrors.end_time ? 'manual-end-error' : undefined} />
            <FieldError id="manual-end-error" message={fieldErrors.end_time} />
          </div>
        ) : (
          <div className="form-field">
            <FormLabel htmlFor="manual-duration" required>ระยะเวลา (นาที)</FormLabel>
            <Input id="manual-duration" name="duration_minutes" type="number" min="1" value={value.duration_minutes} onChange={handleChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.duration_minutes)} aria-describedby={fieldErrors.duration_minutes ? 'manual-duration-error' : undefined} />
            <FieldError id="manual-duration-error" message={fieldErrors.duration_minutes} />
          </div>
        )}
      </div>
      <div className="form-actions">
        <Button variant="outline" className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</Button>
        <Button variant="default" className="button button-primary" type="submit">บันทึกรายการ</Button>
      </div>
    </form>
  )
}

export default TimeEntryForm
