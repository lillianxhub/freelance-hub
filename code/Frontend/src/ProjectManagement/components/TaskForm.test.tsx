import assert from 'node:assert/strict'
import test from 'node:test'
import { act, renderComponent } from '../../test-support/react'
import TaskForm from './TaskForm'

test('task form submits valid data and blocks buttons while saving', async () => {
  for (const saving of [false, true]) {
    let submissions = 0
    let cancellations = 0
    const view = await renderComponent(
      <TaskForm
        value={{ name: 'Task', description: '', status: 'OPEN', due_date: '' }}
        error=""
        saving={saving}
        onChange={() => {}}
        onSubmit={(event) => {
          event.preventDefault()
          submissions++
        }}
        onCancel={() => {
          cancellations++
        }}
      />,
    )
    try {
      const submit = view.container.querySelector<HTMLButtonElement>('button[type="submit"]')
      const cancel = view.container.querySelector<HTMLButtonElement>('button[type="button"]')
      assert.ok(submit && cancel)
      await act(async () => {
        submit.click()
        cancel.click()
      })
      assert.equal(submissions, saving ? 0 : 1)
      assert.equal(cancellations, saving ? 0 : 1)
      assert.equal(view.container.querySelector('input')?.disabled, saving)
    } finally {
      await view.unmount()
    }
  }
})
