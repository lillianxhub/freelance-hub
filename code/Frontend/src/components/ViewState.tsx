import { Button } from './ui/button'
import type { EmptyStateProps, ErrorStateProps, LoadingStateProps } from '../types/ui'
import { FiAlertCircle, FiInbox } from 'react-icons/fi'
import { Spinner } from './ui/spinner'

export function LoadingState({ label = 'กำลังโหลดข้อมูล...' }: LoadingStateProps) {
  return (
    <div
      className="flex min-h-[300px] flex-col items-center justify-center gap-2.5 text-center text-sm text-text-secondary"
      aria-busy="true"
    >
      <Spinner className="size-6 text-primary" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div
      className="flex min-h-[300px] flex-col items-center justify-center gap-2.5 text-center"
      role="alert"
    >
      <span className="grid size-[46px] place-items-center rounded-[14px] bg-red-soft text-2xl font-bold text-destructive">
        <FiAlertCircle aria-hidden="true" />
      </span>
      <h2 className="m-0 text-xl font-semibold text-text-primary">โหลดข้อมูลไม่สำเร็จ</h2>
      <p className="m-0 max-w-[460px] text-sm leading-relaxed text-text-secondary">{message}</p>
      {onRetry && (
        <Button variant="default" type="button" onClick={onRetry}>
          ลองใหม่
        </Button>
      )}
    </div>
  )
}

export function EmptyState({
  icon = <FiInbox aria-hidden="true" />,
  title,
  description,
  action,
}: EmptyStateProps) {
  return (
    <div className="flex min-h-[300px] flex-col items-center justify-center gap-2.5 text-center">
      <span className="grid size-[46px] place-items-center rounded-[14px] bg-primary-soft text-2xl font-bold text-primary">
        {icon}
      </span>
      <h2 className="m-0 text-xl font-semibold text-text-primary">{title}</h2>
      <p className="m-0 max-w-[460px] text-sm leading-relaxed text-text-secondary">{description}</p>
      {action}
    </div>
  )
}
