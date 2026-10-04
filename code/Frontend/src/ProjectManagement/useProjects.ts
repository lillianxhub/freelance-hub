import { useContext } from 'react'
import { ProjectsContext } from './projects-context'
import type { ProjectsContextValue } from './ProjectsContext'

export function useProjects(): ProjectsContextValue {
  const context = useContext(ProjectsContext)
  if (!context) throw new Error('useProjects must be used inside ProjectsProvider')
  return context
}
