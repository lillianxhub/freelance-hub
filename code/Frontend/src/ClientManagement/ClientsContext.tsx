import { createContext, useCallback, type PropsWithChildren } from "react";
import { useScopedWorkspace } from "../shared/useScopedWorkspace";
import {
    createEmptyWorkspace,
    listClients,
    listProjects,
    listTimeEntries,
} from "../services/workspace";
import type { WorkspaceContextValue } from "../types/workspaceContext";

export const ClientsContext = createContext<WorkspaceContextValue | null>(null);

export function ClientsProvider({ children }: PropsWithChildren) {
    const load = useCallback(async () => {
        const [clients, projects, timeEntries] = await Promise.all([
            listClients(),
            listProjects(),
            listTimeEntries(),
        ]);
        return {
            ...createEmptyWorkspace(),
            clients,
            projects,
            time_entries: timeEntries,
        };
    }, []);
    const value = useScopedWorkspace(load);
    return (
        <ClientsContext.Provider value={value}>
            {children}
        </ClientsContext.Provider>
    );
}
