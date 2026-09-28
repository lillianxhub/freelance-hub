// Set BACKEND_ORIGIN to the HTTPS Render origin in Vercel project settings.
// Requests remain same-origin in the browser, so the HttpOnly refresh cookie works.
const backendOrigin = process.env.BACKEND_ORIGIN
if (!backendOrigin || !/^https:\/\/[^/]+$/.test(backendOrigin)) {
  throw new Error('BACKEND_ORIGIN must be an HTTPS origin without a trailing slash')
}

export const config = {
  rewrites: [
    { source: '/api/:path*', destination: `${backendOrigin}/api/:path*` },
    { source: '/:path*', destination: '/index.html' },
  ],
}
