import { createContext } from 'react'
import type { ClientsContextValue } from './ClientsContext'

export const ClientsContext = createContext<ClientsContextValue | null>(null)
