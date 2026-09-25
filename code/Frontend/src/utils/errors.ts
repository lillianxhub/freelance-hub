export function getErrorMessage(error: unknown, fallback = 'เกิดข้อผิดพลาด กรุณาลองใหม่'): string {
  return error instanceof Error && error.message ? error.message : fallback
}
