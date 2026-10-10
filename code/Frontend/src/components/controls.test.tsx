import assert from 'node:assert/strict'
import test from 'node:test'
import { act, renderComponent } from '../test-support/react'
import PasswordInput from './PasswordInput'
import PaginationControls from './PaginationControls'

test('password visibility toggles without submitting a form and respects disabled state', async () => {
  for (const disabled of [false, true]) {
    const view = await renderComponent(
      <PasswordInput defaultValue="password" disabled={disabled} />,
    )
    try {
      const button = view.container.querySelector('button')
      const input = view.container.querySelector('input')
      assert.ok(button && input)
      assert.equal(button.type, 'button')
      assert.equal(input.type, 'password')
      await act(async () => {
        button.click()
      })
      assert.equal(input.type, disabled ? 'password' : 'text')
    } finally {
      await view.unmount()
    }
  }
})

test('pagination clamps the current page and blocks moving beyond its boundaries', async () => {
  const changes: number[] = []
  const view = await renderComponent(
    <PaginationControls page={0} totalPages={3} onPageChange={(page) => changes.push(page)} />,
  )
  try {
    const links = [...view.container.querySelectorAll('a')]
    const previous = links.find((link) => link.textContent?.includes('ก่อนหน้า'))
    const next = links.find((link) => link.textContent?.includes('ถัดไป'))
    assert.ok(previous && next)
    await act(async () => {
      previous.click()
      next.click()
    })
    assert.deepEqual(changes, [2])
  } finally {
    await view.unmount()
  }
})
