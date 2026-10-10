import assert from 'node:assert/strict'
import test from 'node:test'
import {
  hasRequiredPassword,
  isValidEmail,
  isValidPhone,
  normalizePhone,
  passwordsMatch,
} from './authentication.validators'

test('password policy handles the UTF-8 byte boundary', () => {
  for (const password of ['password', 'a'.repeat(72), 'ก'.repeat(24), '😀'.repeat(18)])
    assert.equal(hasRequiredPassword(password), true)
  for (const password of ['', 'short', 'a'.repeat(73), 'ก'.repeat(25), '😀'.repeat(19)])
    assert.equal(hasRequiredPassword(password), false)
})

test('email, phone and confirmation validation reject invalid values', () => {
  assert.equal(isValidEmail(' owner@example.com '), true)
  for (const email of ['', 'owner', 'owner@', 'a b@example.com'])
    assert.equal(isValidEmail(email), false)
  assert.equal(normalizePhone('081-234-5678 extra 9'), '0812345678')
  assert.equal(isValidPhone('0812345678'), true)
  assert.equal(isValidPhone('081234567'), false)
  assert.equal(isValidPhone('abcdefghij'), false)
  assert.equal(passwordsMatch('password', 'password'), true)
  assert.equal(passwordsMatch('password', 'Password'), false)
})
