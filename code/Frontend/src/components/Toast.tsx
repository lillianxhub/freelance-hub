import { useEffect } from 'react'
import { FiAlertCircle, FiCheckCircle, FiX } from 'react-icons/fi'
import type { ToastProps } from '../types/toast'

export default function Toast({ success, message, duration = 4000, onClose }: ToastProps) {
  useEffect(() => {
    const timeout = window.setTimeout(onClose, duration)
    return () => window.clearTimeout(timeout)
  }, [duration, message, onClose, success])

  return (
    <div className={`toast ${success ? 'toast-success' : 'toast-error'}`} role={success ? 'status' : 'alert'}>
      <span className="toast-status-icon" aria-hidden="true">
        {success ? <FiCheckCircle /> : <FiAlertCircle />}
      </span>
      <span>{message}</span>
      <button className="toast-close" type="button" onClick={onClose} aria-label="ปิดข้อความ">
        <FiX aria-hidden="true" />
      </button>
    </div>
  )
}
