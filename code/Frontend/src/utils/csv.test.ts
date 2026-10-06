import assert from 'node:assert/strict'
import test from 'node:test'
import { toCsv } from './csv'

test('toCsv escapes quotes and commas safely', () => {
  assert.equal(toCsv([['Name', 'Notes'], ['Maya', 'Logo, "final"']]), '"Name","Notes"\n"Maya","Logo, ""final"""')
})
