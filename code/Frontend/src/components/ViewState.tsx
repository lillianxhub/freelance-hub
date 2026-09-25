import type { ReactNode } from 'react'

export function LoadingState({ label = 'กำลังโหลดข้อมูล...' }: { label?: string }) {
  return (
    <div className="view-state" aria-busy="true">
      <span className="loading-spinner" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="view-state error-view" role="alert">
      <span className="state-symbol">!</span>
      <h2>โหลดข้อมูลไม่สำเร็จ</h2>
      <p>{message}</p>
      {onRetry && <button className="button button-primary" type="button" onClick={onRetry}>ลองใหม่</button>}
    </div>
  )
}

interface EmptyStateProps {
  icon?: string
  title: string
  description: string
  action?: ReactNode
}

export function EmptyState({ icon = '◇', title, description, action }: EmptyStateProps) {
  return (
    <div className="view-state empty-view">
      <span className="state-symbol">{icon}</span>
      <h2>{title}</h2>
      <p>{description}</p>
      {action}
    </div>
  )
}
