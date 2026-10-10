import assert from 'node:assert/strict'
import test from 'node:test'
import { validateProfileField, validateProfileFields } from './profile.validators'
import type { Profile, ProfileFieldName } from '../types/profile'

const profile: Pick<Profile, ProfileFieldName> = {
  display_name: 'Owner',
  first_name: 'First',
  last_name: 'Last',
  phone: '0812345678',
  address: 'Street',
  province: 'Province',
  district: 'District',
  sub_district: 'Sub',
  postal_code: '40000',
  tax_id: '',
}

test('profile requires its basic fields but permits an empty tax id', () => {
  assert.deepEqual(validateProfileFields(profile), {})
  for (const field of Object.keys(profile) as ProfileFieldName[]) {
    const error = validateProfileField(field, { ...profile, [field]: ' ' })
    assert.equal(Boolean(error), field !== 'tax_id')
  }
})

test('profile validates phone, postcode and optional tax id formats', () => {
  for (const field of ['phone', 'postal_code', 'tax_id'] as const)
    assert.ok(validateProfileField(field, { ...profile, [field]: 'invalid' }))
  assert.equal(validateProfileField('tax_id', { ...profile, tax_id: '1234567890123' }), undefined)
})
