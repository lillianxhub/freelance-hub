import assert from 'node:assert/strict'
import test from 'node:test'
import { act, renderComponent } from '../../test-support/react'
import { createEmptyManualForm } from '../timeTracking.utils'
import TimeEntryForm from './TimeEntryForm'

test('manual form submits without a task and blocks invalid time ranges', async () => {
  for (const valid of [true, false]) {
    let submissions = 0
    const form = {
      ...createEmptyManualForm('p1'),
      entry_date: '2026-10-10',
      end_time: valid ? '10:00' : '08:00',
    }
    const view = await renderComponent(
      <TimeEntryForm
        value={form}
        projects={[]}
        tasks={[]}
        error=""
        onChange={() => {}}
        onCancel={() => {}}
        onSubmit={(event) => {
          event.preventDefault()
          submissions++
        }}
      />,
    )
    try {
      const element = view.container.querySelector('form')
      assert.ok(element)
      await act(async () => {
        element.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))
      })
      assert.equal(submissions, valid ? 1 : 0)
      if (!valid) assert.match(view.container.textContent ?? '', /เวลาสิ้นสุดต้องหลังเวลาเริ่ม/)
      assert.equal(
        view.container.querySelector('select[name="task_id"] option')?.textContent,
        'ไม่ระบุงาน',
      )
    } finally {
      await view.unmount()
    }
  }
})
