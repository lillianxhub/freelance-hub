import type { ReactNode } from 'react'
import type { IconType } from 'react-icons'
import type { ClientStatus } from './client'
import type { ProjectStatus } from './project'
import type { TaskStatus } from './task'

export interface PageHeaderProps {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}

export interface SidebarLinkProps {
  to: string
  label: string
  icon: IconType
  badge?: number
}

export interface SidebarProps {
  open: boolean
  onClose: () => void
}

export type SupportedStatus = ClientStatus | ProjectStatus | TaskStatus

export interface StatusBadgeProps {
  status: SupportedStatus
  label?: string
  className?: string
}

export interface FormLabelProps {
  htmlFor: string
  children: ReactNode
  required?: boolean
}

export interface FieldErrorProps {
  id: string
  message?: string
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
  icon?: ReactNode
  title: string
  description: string
  action?: ReactNode
}
