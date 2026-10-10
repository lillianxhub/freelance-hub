import assert from 'node:assert/strict'
import test from 'node:test'
import {
  normalizeDigits,
  validateClient,
  validateClientField,
  validateClientFields,
} from './client.validators'
import type { ClientInput } from '../types/client'

const client: ClientInput = {
  name: 'Owner',
  company_name: 'Company',
  email: 'owner@example.com',
  phone: '0812345678',
  address: 'Street',
  province: 'Province',
  district: 'District',
  sub_district: 'Sub',
  postal_code: '40000',
  tax_id: '1234567890123',
  status: 'ACTIVE',
  notes: '',
  color: '#4F6BFF',
}

test('client form accepts valid data and identifies each invalid field', () => {
  assert.deepEqual(validateClientFields(client), {})
  for (const field of Object.keys(client) as (keyof ClientInput)[]) {
    if (field === 'notes' || field === 'color') continue
    const invalid = { ...client, [field]: '' }
    assert.ok(validateClientFields(invalid)[field as keyof ReturnType<typeof validateClientFields>])
  }
  for (const field of ['email', 'phone', 'postal_code', 'tax_id'] as const)
    assert.ok(validateClientField(field, { ...client, [field]: 'invalid' }))
})

test('minimal client validation requires either contact or company name', () => {
  assert.equal(validateClient({ ...client, name: '', company_name: 'Company' }), null)
  assert.equal(validateClient({ ...client, company_name: '' }), null)
  assert.ok(validateClient({ ...client, name: ' ', company_name: ' ' }))
  assert.ok(validateClient({ ...client, email: 'invalid' }))
  assert.equal(normalizeDigits('12a-34567', 5), '12345')
})
