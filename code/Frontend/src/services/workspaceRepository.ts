import { createDemoWorkspace, demoUser } from '../data/demoData'
import { getSupabaseClient, isDemoMode } from '../lib/supabase'
import type { OwnedRecord, ResourceInput, ResourceName, ResourceRecord, WorkspaceData } from '../types/domain'

const workspaceKey = 'freelance-hub-demo-workspace-v5'
const resources = ['profiles', 'clients', 'projects', 'tasks', 'time_entries', 'finance_entries', 'invoices', 'invoice_items', 'payments'] as const satisfies readonly ResourceName[]
const resourcesWithUpdatedAt = new Set<ResourceName>(['profiles', 'clients', 'projects', 'tasks', 'time_entries', 'finance_entries', 'invoices'])

export function createEmptyWorkspace(): WorkspaceData {
  return { profiles: [], clients: [], projects: [], tasks: [], time_entries: [], finance_entries: [], invoices: [], invoice_items: [], payments: [] }
}

function loadWorkspace(): WorkspaceData {
  const stored = localStorage.getItem(workspaceKey)
  if (stored) {
    try {
      const saved = JSON.parse(stored) as Partial<WorkspaceData>
      if (resources.every((resource) => Array.isArray(saved[resource]))) return saved as WorkspaceData
    } catch {
      localStorage.removeItem(workspaceKey)
    }
  }
  const workspace = createDemoWorkspace()
  localStorage.setItem(workspaceKey, JSON.stringify(workspace))
  return workspace
}

function saveWorkspace(workspace: WorkspaceData) {
  localStorage.setItem(workspaceKey, JSON.stringify(workspace))
}

async function getOwnerId(): Promise<string> {
  if (isDemoMode) return demoUser.id
  const { data, error } = await getSupabaseClient().auth.getUser()
  if (error) throw error
  if (!data.user) throw new Error('กรุณาเข้าสู่ระบบอีกครั้ง')
  return data.user.id
}

export async function loadAllResources(): Promise<WorkspaceData> {
  if (isDemoMode) return structuredClone(loadWorkspace())

  const result = createEmptyWorkspace()
  await Promise.all(resources.map(async (resource) => {
    const { data, error } = await getSupabaseClient().from(resource).select('*')
    if (error) throw error
    const target = result as unknown as Record<ResourceName, unknown[]>
    target[resource] = data || []
  }))
  return result
}

export async function saveResource<K extends ResourceName>(resource: K, input: ResourceInput<K>): Promise<ResourceRecord<K>> {
  const ownerId = await getOwnerId()
  const record = {
    ...input,
    id: input.id || crypto.randomUUID(),
    owner_id: ownerId,
  } as ResourceRecord<K>
  if (resourcesWithUpdatedAt.has(resource)) (record as OwnedRecord).updated_at = new Date().toISOString()

  if (isDemoMode) {
    const workspace = loadWorkspace()
    const collection = workspace[resource] as ResourceRecord<K>[]
    const index = collection.findIndex((item) => item.id === record.id)
    if (index >= 0) collection[index] = { ...collection[index], ...record }
    else collection.unshift({ created_at: new Date().toISOString(), ...record })
    saveWorkspace(workspace)
    return structuredClone(record)
  }

  const { data, error } = await getSupabaseClient().from(resource).upsert(record as never).select().single()
  if (error) throw error
  return data as ResourceRecord<K>
}

export async function deleteResource(resource: ResourceName, id: string): Promise<void> {
  if (isDemoMode) {
    const workspace = loadWorkspace()
    const collection = workspace[resource] as OwnedRecord[]
    const remaining = collection.filter((item) => item.id !== id)
    workspace[resource] = remaining as never
    saveWorkspace(workspace)
    return
  }

  const { error } = await getSupabaseClient().from(resource).delete().eq('id', id)
  if (error) throw error
}

export function resetDemoWorkspace(): void {
  if (isDemoMode) saveWorkspace(createDemoWorkspace())
}
