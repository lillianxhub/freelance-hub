export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

export function getErrorMessage(error: unknown, fallback = 'เกิดข้อผิดพลาด กรุณาลองใหม่'): string {
  return error instanceof Error && error.message ? error.message : fallback
}
