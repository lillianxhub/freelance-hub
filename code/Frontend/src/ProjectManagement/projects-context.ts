import { createContext } from 'react'
import type { ProjectsContextValue } from './ProjectsContext'

export const ProjectsContext = createContext<ProjectsContextValue | null>(null)
