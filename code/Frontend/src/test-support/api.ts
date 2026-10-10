import type { ApiMeta } from '../types/api'

export function success(data: unknown, meta: ApiMeta | null = null): Response {
  return Response.json({ success: true, message: '', data, meta, error: null })
}
