import { api } from '../api/apiClient'
import type { ApiMeta, ApiProject } from '../types/api'
import type { Project, ProjectInput, ProjectStatus } from '../types/project'

function emptyString(value: string | null | undefined): string {
  return value ?? ''
}

export function toProject(source: ApiProject): Project {
  return {
    id: source.id,
    owner_id: '',
    client_id: source.clientId ?? source.client?.id ?? '',
    client_name: source.client?.name,
    name: source.name,
    description: emptyString(source.description),
    color: source.color || '#4F6BFF',
    status: source.status,
    billing_type: 'HOURLY',
    hourly_rate: null,
    fixed_price: null,
    budget_hours: source.targetHours ?? (source.targetMinutes ? source.targetMinutes / 60 : null),
    budget_amount: null,
    currency: 'THB',
    start_date: emptyString(source.startDate),
    end_date: emptyString(source.endDate),
    created_at: source.createdAt,
    updated_at: source.updatedAt,
    task_progress: source.taskProgress
      ? {
          total_tasks: source.taskProgress.totalTasks,
          completed_tasks: source.taskProgress.completedTasks,
          percent: source.taskProgress.percent,
        }
      : undefined,
    time_tracking: source.timeTracking
      ? {
          tracked_seconds: source.timeTracking.trackedSeconds,
          tracked_hours: source.timeTracking.trackedHours,
          usage_percent: source.timeTracking.usagePercent,
        }
      : (source.timeTracking ?? undefined),
  }
}

function projectPayload(input: ProjectInput) {
  return {
    clientId: input.client_id,
    name: input.name,
    description: input.description || undefined,
    startDate: input.start_date || undefined,
    endDate: input.end_date || undefined,
    color: input.color || undefined,
    targetMinutes: input.budget_hours ? Math.round(Number(input.budget_hours) * 60) : undefined,
  }
}

export interface ProjectsPageResult {
  projects: Project[]
  meta: ApiMeta
}
export interface ProjectListFilters {
  clientId?: string
  search?: string
  status?: Project['status'] | 'ALL'
  sortBy?: 'update_at' | 'end_date' | 'project_name'
  direction?: 'ASC' | 'DESC'
}

export async function listProjects(filters: ProjectListFilters = {}): Promise<Project[]> {
  return (await listProjectsPage(1, 10, filters)).projects
}

export async function listAllProjects(filters: ProjectListFilters = {}): Promise<Project[]> {
  const firstPage = await listProjectsPage(1, 100, filters)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.meta.totalPages - 1) }, (_, index) =>
      listProjectsPage(index + 2, 100, filters),
    ),
  )
  return [...firstPage.projects, ...remainingPages.flatMap((page) => page.projects)]
}

export async function getProjectById(id: string): Promise<Project> {
  return toProject((await api.get<ApiProject>(`/projects/${encodeURIComponent(id)}`)).data)
}

export async function listProjectsPage(
  page = 1,
  limit = 10,
  filters: ProjectListFilters = {},
): Promise<ProjectsPageResult> {
  const query = new URLSearchParams({
    page: String(page),
    limit: String(limit),
    sortBy: filters.sortBy ?? 'project_name',
    direction: filters.direction ?? 'ASC',
  })
  if (filters.clientId) query.set('clientId', filters.clientId)
  if (filters.search?.trim()) query.set('search', filters.search.trim())
  if (filters.status) query.set('status', filters.status)
  const response = await api.get<ApiProject[]>(`/projects?${query.toString()}`)
  return {
    projects: response.data.map(toProject),
    meta: response.meta ?? { page, limit, total: response.data.length, totalPages: 1 },
  }
}

export async function saveProject(input: ProjectInput): Promise<Project> {
  if (!input.id)
    return toProject((await api.post<ApiProject>('/projects', projectPayload(input))).data)
  if (input.status === 'ARCHIVED') {
    await api.delete(`/projects/${input.id}`)
    return { ...input, status: 'ARCHIVED' } as Project
  }
  return toProject((await api.put<ApiProject>(`/projects/${input.id}`, projectPayload(input))).data)
}

export async function deleteProject(id: string): Promise<void> {
  await api.delete(`/projects/${encodeURIComponent(id)}`)
}

export async function changeProjectStatus(id: string, status: ProjectStatus): Promise<Project> {
  return toProject(
    (await api.patch<ApiProject>(`/projects/${encodeURIComponent(id)}/status`, { status })).data,
  )
}
