import type { ReactNode } from 'react'

export interface AppRouteDefinition {
  path: string
  label: string
  element: ReactNode
}
