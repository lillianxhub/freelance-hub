import { createDemoWorkspace, demoUser } from '../data/demoData'
import { isDemoMode, supabase } from '../lib/supabase'

const workspaceKey = 'freelance-hub-demo-workspace-v4'
const resources = ['profiles', 'clients', 'projects', 'tasks', 'time_entries', 'finance_entries', 'invoices', 'invoice_items', 'payments']
const resourcesWithUpdatedAt = new Set(['profiles', 'clients', 'projects', 'tasks', 'time_entries', 'finance_entries', 'invoices'])

function loadWorkspace() {
  try {
    const saved = JSON.parse(localStorage.getItem(workspaceKey))
    if (saved && resources.every((resource) => Array.isArray(saved[resource]))) return saved
  } catch {
    localStorage.removeItem(workspaceKey)
  }
  const workspace = createDemoWorkspace()
  localStorage.setItem(workspaceKey, JSON.stringify(workspace))
  return workspace
}

function saveWorkspace(workspace) {
  localStorage.setItem(workspaceKey, JSON.stringify(workspace))
}

async function getOwnerId() {
  if (isDemoMode) return demoUser.id
  const { data, error } = await supabase.auth.getUser()
  if (error) throw error
  if (!data.user) throw new Error('กรุณาเข้าสู่ระบบอีกครั้ง')
  return data.user.id
}

export async function loadAllResources() {
  if (isDemoMode) return structuredClone(loadWorkspace())

  const result = {}
  await Promise.all(resources.map(async (resource) => {
    const { data, error } = await supabase.from(resource).select('*')
    if (error) throw error
    result[resource] = data || []
  }))
  return result
}

export async function saveResource(resource, input) {
  if (!resources.includes(resource)) throw new Error(`Unknown resource: ${resource}`)
  const ownerId = await getOwnerId()
  const record = {
    ...input,
    id: input.id || crypto.randomUUID(),
    owner_id: ownerId,
  }
  if (resourcesWithUpdatedAt.has(resource)) record.updated_at = new Date().toISOString()

  if (isDemoMode) {
    const workspace = loadWorkspace()
    const index = workspace[resource].findIndex((item) => item.id === record.id)
    if (index >= 0) workspace[resource][index] = { ...workspace[resource][index], ...record }
    else workspace[resource].unshift({ created_at: new Date().toISOString(), ...record })
    saveWorkspace(workspace)
    return structuredClone(record)
  }

  const { data, error } = await supabase.from(resource).upsert(record).select().single()
  if (error) throw error
  return data
}

export async function deleteResource(resource, id) {
  if (!resources.includes(resource)) throw new Error(`Unknown resource: ${resource}`)

  if (isDemoMode) {
    const workspace = loadWorkspace()
    workspace[resource] = workspace[resource].filter((item) => item.id !== id)
    saveWorkspace(workspace)
    return
  }

  const { error } = await supabase.from(resource).delete().eq('id', id)
  if (error) throw error
}

export function resetDemoWorkspace() {
  if (!isDemoMode) return
  saveWorkspace(createDemoWorkspace())
}
