import assert from 'node:assert/strict'
import test from 'node:test'
import { loadThaiAddressData } from './thaiAddress'

test('address loading retries failures, validates nested records and shares cached requests', async () => {
  const originalFetch = globalThis.fetch
  try {
    for (const response of [
      new Response(null, { status: 500 }),
      Response.json({}),
      Response.json([]),
    ]) {
      globalThis.fetch = async () => response
      await assert.rejects(loadThaiAddressData())
    }
    let calls = 0
    globalThis.fetch = async () => {
      calls++
      return Response.json([
        null,
        { id: 'invalid', name_th: 'Invalid' },
        {
          id: 1,
          name: { th: 'จังหวัด' },
          districts: [
            {},
            {
              id: 2,
              name_th: 'อำเภอ',
              sub_districts: [
                null,
                { id: 3, name: { th: 'ตำบล' }, zip_code: 40000 },
                { id: 4, name_th: 'Invalid', zip_code: '40000' },
              ],
            },
          ],
        },
      ])
    }
    const first = loadThaiAddressData()
    assert.equal(first, loadThaiAddressData())
    assert.deepEqual(await first, [
      {
        id: 1,
        name_th: 'จังหวัด',
        districts: [
          { id: 2, name_th: 'อำเภอ', sub_districts: [{ id: 3, name_th: 'ตำบล', zip_code: 40000 }] },
        ],
      },
    ])
    await loadThaiAddressData()
    assert.equal(calls, 1)
  } finally {
    globalThis.fetch = originalFetch
  }
})
