import assert from 'node:assert/strict'
import test from 'node:test'
import { formatMoney } from './number'
import { initials } from './string'
import { ApiError, getErrorMessage } from '../api/apiError'

test('currency formatting rounds and treats empty values as zero', () => {
  assert.match(formatMoney('1234.567'), /1,234\.57/)
  assert.equal(formatMoney(null), formatMoney(0))
  assert.equal(formatMoney('invalid'), formatMoney(0))
  assert.match(formatMoney(-10), /10/)
})

test('initials handle whitespace and the fallback', () => {
  assert.equal(initials('  first   last other '), 'FL')
  assert.equal(initials('owner'), 'O')
  assert.equal(initials(' '), 'FH')
  assert.equal(initials(), 'FH')
})

test('error messages preserve API metadata and safely handle unknown errors', () => {
  const error = new ApiError('Invalid', 400, {
    code: 'VALIDATION_ERROR',
    fieldErrors: { name: 'Required' },
    traceId: 'trace',
  })
  assert.equal(getErrorMessage(error), 'Invalid')
  assert.equal(error.fieldErrors?.name, 'Required')
  assert.equal(error.traceId, 'trace')
  assert.equal(getErrorMessage(null, 'Fallback'), 'Fallback')
  assert.equal(getErrorMessage(new Error(''), 'Fallback'), 'Fallback')
})
