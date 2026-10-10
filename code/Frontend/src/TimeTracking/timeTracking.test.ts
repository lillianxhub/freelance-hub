import assert from 'node:assert/strict'
import test from 'node:test'
import { canTrackProject, createEmptyManualForm, localDateValue } from './timeTracking.utils'
import { validateManualTimeField, validateManualTimeForm } from './timeTracking.validators'
import type { ProjectStatus } from '../types/project'

test('only ACTIVE projects are selectable for time tracking', () => {
  for (const status of ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED'] as ProjectStatus[])
    assert.equal(canTrackProject({ status }), status === 'ACTIVE')
})

test('manual entry can be submitted without a task in both input modes', () => {
  const form = createEmptyManualForm('project-1')
  form.entry_date = '2026-10-10'
  assert.deepEqual(validateManualTimeForm(form), {})
  assert.deepEqual(validateManualTimeForm({ ...form, manual_mode: 'DURATION', end_time: '' }), {})
  assert.equal(validateManualTimeField('task_id', form), undefined)
})

test('manual validation rejects required fields and nonpositive durations', () => {
  const form = createEmptyManualForm('project-1')
  for (const field of ['project_id', 'entry_date', 'start_time'] as const)
    assert.ok(validateManualTimeField(field, { ...form, [field]: '' }))
  assert.ok(validateManualTimeField('end_time', { ...form, end_time: form.start_time }))
  assert.ok(validateManualTimeField('duration_minutes', { ...form, duration_minutes: 0 }))
  assert.ok(validateManualTimeField('duration_minutes', { ...form, duration_minutes: -1 }))
  assert.equal(
    validateManualTimeField('duration_minutes', { ...form, duration_minutes: 1 }),
    undefined,
  )
})

test('empty manual form has independent values and a local date', () => {
  const first = createEmptyManualForm('project-1')
  first.description = 'Edited'
  assert.equal(createEmptyManualForm().description, '')
  const date = new Date(2026, 9, 10, 0, 30)
  assert.equal(localDateValue(date), '2026-10-10')
  assert.equal(first.project_id, 'project-1')
})
