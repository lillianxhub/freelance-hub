import assert from 'node:assert/strict'
import test from 'node:test'

test('Vercel rewrites API requests to the configured staging backend', async () => {
  process.env.BACKEND_ORIGIN = 'https://staging.example.com'
  const { config } = await import('../../vercel')

  assert.deepEqual(config.rewrites, [
    { source: '/api/(.*)', destination: 'https://staging.example.com/api/$1' },
    { source: '/(.*)', destination: '/index.html' },
  ])
})
