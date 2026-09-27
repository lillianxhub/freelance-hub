import type {
  ButtonHTMLAttributes,
  PropsWithChildren,
  ReactNode,
} from 'react'
import type { ClientStatus } from './client'
import type { ProjectStatus } from './project'
import type { TaskStatus } from './task'
import type { InvoiceStatus } from './billing'

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'text'
}

export interface ModalProps extends PropsWithChildren {
  open: boolean
  title: string
  eyebrow?: string
  onClose: () => void
  size?: 'small' | 'medium' | 'large'
}

export interface PageHeaderProps {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}

export interface SidebarLinkProps {
  to: string
  label: string
  icon: string
  badge?: number
}

export interface SidebarProps {
  open: boolean
  onClose: () => void
}

export type SupportedStatus = ClientStatus | ProjectStatus | TaskStatus | InvoiceStatus

export interface StatusBadgeProps {
  status: SupportedStatus
}

export interface TopbarProps {
  onMenu: () => void
}

export interface LoadingStateProps {
  label?: string
}

export interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

export interface EmptyStateProps {
  icon?: string
  title: string
  description: string
  action?: ReactNode
}
