import type { ApiResponseError } from '../types/api'

export class ApiError extends Error {
  public readonly code?: string
  public readonly details: Record<string, unknown> | null
  public readonly fieldErrors: Record<string, string> | null
  public readonly timestamp?: string
  public readonly traceId?: string
  constructor(
    message: string,
    public readonly status: number,
    metadata: ApiResponseError = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.code = metadata.code
    this.details = metadata.details ?? null
    this.fieldErrors = metadata.fieldErrors ?? null
    this.timestamp = metadata.timestamp
    this.traceId = metadata.traceId ?? undefined
  }
}

export function getErrorMessage(error: unknown, fallback = 'เกิดข้อผิดพลาด กรุณาลองใหม่'): string {
  return error instanceof Error && error.message ? error.message : fallback
}
