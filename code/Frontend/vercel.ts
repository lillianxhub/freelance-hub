import { routes, type VercelConfig } from '@vercel/config/v1'

// Production and Preview currently point to the same staging backend.
// Set BACKEND_ORIGIN for both Vercel environments; do not expose it via VITE_*.
const backendOrigin = process.env.BACKEND_ORIGIN
const apiBasePath = process.env.VITE_API_BASE_URL || '/api'
if (!backendOrigin || !/^https:\/\/[^/?#]+$/.test(backendOrigin)) {
  throw new Error('BACKEND_ORIGIN must be an HTTPS origin without a trailing slash')
}
if (!/^\/(?!\/)[^?#]*[^/]$/.test(apiBasePath)) {
  throw new Error('VITE_API_BASE_URL must be a same-origin path without a trailing slash')
}

export const config: VercelConfig = {
  rewrites: [
    routes.rewrite(`${apiBasePath}/(.*)`, `${backendOrigin}${apiBasePath}/$1`),
    routes.rewrite('/(.*)', '/index.html'),
  ],
}
