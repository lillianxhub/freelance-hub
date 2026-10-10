import assert from 'node:assert/strict'
import test from 'node:test'
import {
  deleteClient,
  getClientById,
  listClientOptions,
  listClients,
  listClientsPage,
  saveClient,
  updateClientStatus,
} from './client'
import { success } from '../test-support/api'
import type { ClientInput } from '../types/client'

const client: ClientInput = {
  name: 'Owner',
  company_name: '',
  email: '',
  phone: '',
  address: '',
  province: '',
  district: '',
  sub_district: '',
  postal_code: '',
  tax_id: '',
  notes: '',
  status: 'ACTIVE',
  color: '#4F6BFF',
}

test('client list maps optional fields, archive flags and pagination filters', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (url) => {
    const query = new URL(String(url), 'http://localhost').searchParams
    assert.equal(query.get('search'), 'Owner')
    assert.equal(query.get('status'), 'ARCHIVED')
    return success([{ id: 'c1', name: 'Owner', isActive: false, status: 'ACTIVE' }])
  }
  try {
    const result = await listClientsPage(2, 5, { search: ' Owner ', status: 'ARCHIVED' })
    assert.equal(result.clients[0].status, 'ARCHIVED')
    assert.equal(result.clients[0].postal_code, '')
    assert.deepEqual(result.meta, { page: 2, limit: 5, total: 1, totalPages: 1 })
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('client options load every page and only request active clients', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (url) => {
    const query = new URL(String(url), 'http://localhost').searchParams
    assert.equal(query.get('status'), 'ACTIVE')
    const page = Number(query.get('page'))
    return success([{ id: `c${page}`, name: `Client ${page}`, status: 'ACTIVE' }], {
      page,
      limit: 100,
      total: 201,
      totalPages: 3,
    })
  }
  try {
    assert.deepEqual(
      (await listClientOptions()).map((item) => item.id),
      ['c1', 'c2', 'c3'],
    )
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('client mutations create, edit, restore and archive through the correct routes', async () => {
  const originalFetch = globalThis.fetch
  const requests: { url: string; method?: string; body: Record<string, unknown> }[] = []
  globalThis.fetch = async (url, options) => {
    const body = options?.body ? JSON.parse(String(options.body)) : {}
    requests.push({ url: String(url), method: options?.method, body })
    return success({
      id: 'c1',
      name: 'Owner',
      isActive: body.isActive ?? false,
      status: 'ARCHIVED',
    })
  }
  try {
    await saveClient(client)
    assert.equal(requests[0].method, 'POST')
    assert.equal(requests[0].url, '/api/clients')
    assert.equal('companyName' in requests[0].body, false)
    await saveClient({ ...client, id: 'c1' })
    assert.equal(requests[1].url, '/api/clients/c1')
    assert.deepEqual(requests[2].body, { isActive: true })
    await saveClient({ ...client, id: 'c1', status: 'ARCHIVED' })
    assert.deepEqual(requests[3].body, { isActive: false })
    await deleteClient('c / 1')
    assert.equal(requests[4].url, '/api/clients/c%20%2F%201/status')
    await updateClientStatus('c1', true)
    await getClientById('c / 1')
    assert.equal(requests[6].url, '/api/clients/c%20%2F%201')
    globalThis.fetch = async () => success([])
    assert.deepEqual(await listClients(), [])
  } finally {
    globalThis.fetch = originalFetch
  }
})
