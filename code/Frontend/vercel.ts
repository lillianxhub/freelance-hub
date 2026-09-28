import { routes, type VercelConfig } from '@vercel/config/v1'

// Production and Preview currently point to the same staging backend.
// Set BACKEND_ORIGIN for both Vercel environments; do not expose it via VITE_*.
const backendOrigin = process.env.BACKEND_ORIGIN
if (!backendOrigin || !/^https:\/\/[^/?#]+$/.test(backendOrigin)) {
  throw new Error('BACKEND_ORIGIN must be an HTTPS origin without a trailing slash')
}

export const config: VercelConfig = {
  rewrites: [
    routes.rewrite('/api/(.*)', `${backendOrigin}/api/$1`),
    routes.rewrite('/(.*)', '/index.html'),
  ],
}
