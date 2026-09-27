import { useContext } from 'react'
import { ProjectsContext } from './ProjectsContext'
import type { WorkspaceContextValue } from '../types/workspaceContext'

export function useProjects(): WorkspaceContextValue {
  const context = useContext(ProjectsContext)
  if (!context) throw new Error('useProjects must be used inside ProjectsProvider')
  return context
}
