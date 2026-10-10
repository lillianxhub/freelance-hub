import assert from 'node:assert/strict'
import test from 'node:test'
import { getPaginationItems } from './pagination'

test('pagination handles empty, small, first, middle and last pages', () => {
  assert.deepEqual(getPaginationItems(1, 0), [])
  assert.deepEqual(getPaginationItems(3, 5), [1, 2, 3, 4, 5])
  assert.deepEqual(getPaginationItems(1, 10), [1, 2, 'ellipsis', 10])
  assert.deepEqual(getPaginationItems(5, 10), [1, 'ellipsis', 4, 5, 6, 'ellipsis', 10])
  assert.deepEqual(getPaginationItems(10, 10), [1, 'ellipsis', 9, 10])
})
