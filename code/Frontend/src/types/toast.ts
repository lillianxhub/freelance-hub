export interface ToastMessage {
  success: boolean
  message: string
}

export interface ToastProps extends ToastMessage {
  duration?: number
  onClose: () => void
}
