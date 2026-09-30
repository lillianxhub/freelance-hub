import { Button } from './ui/button'
import type { EmptyStateProps, ErrorStateProps, LoadingStateProps } from '../types/ui'
import { FiAlertCircle, FiInbox } from 'react-icons/fi'
import { Spinner } from './ui/spinner'

export function LoadingState({ label = 'กำลังโหลดข้อมูล...' }: LoadingStateProps) {
  return (
    <div className="view-state" aria-busy="true">
      <Spinner className="size-6 text-primary" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="view-state error-view" role="alert">
      <span className="state-symbol"><FiAlertCircle aria-hidden="true" /></span>
      <h2>โหลดข้อมูลไม่สำเร็จ</h2>
      <p>{message}</p>
      {onRetry && <Button variant="default" className="button button-primary" type="button" onClick={onRetry}>ลองใหม่</Button>}
    </div>
  )
}

export function EmptyState({ icon = <FiInbox aria-hidden="true" />, title, description, action }: EmptyStateProps) {
  return (
    <div className="view-state empty-view">
      <span className="state-symbol">{icon}</span>
      <h2>{title}</h2>
      <p>{description}</p>
      {action}
    </div>
  )
}
