import assert from 'node:assert/strict'
import test from 'node:test'
import { createElement, type PropsWithChildren } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { act, renderHook } from '../test-support/react'
import { success } from '../test-support/api'
import { ClientsProvider } from '../ClientManagement/ClientsContext'
import { useClients } from '../ClientManagement/useClients'
import { ProjectsProvider } from '../ProjectManagement/ProjectsContext'
import { useProjects } from '../ProjectManagement/useProjects'
import { useTasks } from '../ProjectManagement/useTasks'
import { ProfileProvider } from '../Profile/ProfileContext'
import { useProfile } from '../Profile/useProfile'
import { TimeEntriesProvider } from '../TimeTracking/TimeEntriesContext'
import { useTimeEntries } from '../TimeTracking/useTimeEntries'
import { useAuth } from '../Authentication/useAuthentication'
import { useCurrentTimer } from '../TimeTracking/useCurrentTimer'
import { toProject } from '../services/project'
import { toTask } from '../services/task'
import type { TimeEntryInput } from '../types/timeTracking'

const project = { id: 'p1', clientId: 'c1', name: 'Project', status: 'ACTIVE' as const }
const task = { id: 't1', projectId: 'p1', name: 'Task', status: 'OPEN' as const, sortOrder: 0 }
const client = { id: 'c1', name: 'Client', status: 'ACTIVE' as const }

test('context hooks reject use outside their providers', () => {
  for (const hook of [
    useClients,
    useProjects,
    useProfile,
    useTimeEntries,
    useAuth,
    useCurrentTimer,
  ]) {
    function Consumer() {
      hook()
      return null
    }
    assert.throws(() => renderToStaticMarkup(createElement(Consumer)), /Provider/)
  }
})

test('client provider refreshes its projects after save and archive', async () => {
  const originalFetch = globalThis.fetch
  let reads = 0
  globalThis.fetch = async (url, options) => {
    if (String(url).includes('/projects?')) {
      reads++
      return success([project])
    }
    return success({ ...client, isActive: !String(options?.body).includes('false') })
  }
  const hook = await renderHook(useClients, ClientsProvider)
  try {
    assert.equal(hook.current.data.projects[0]?.id, 'p1')
    await act(async () => {
      await hook.current.saveClient({
        name: 'Client',
        company_name: '',
        email: '',
        phone: '',
        address: '',
        province: '',
        district: '',
        sub_district: '',
        postal_code: '',
        tax_id: '',
        notes: '',
        color: '#000',
        status: 'ACTIVE',
      })
    })
    await act(async () => {
      await hook.current.archiveClient('c1')
    })
    assert.equal(reads, 3)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
  }
})

test('project provider loads its related client and tasks and refreshes after mutations', async () => {
  const originalFetch = globalThis.fetch
  let reads = 0
  globalThis.fetch = async (url, options) => {
    if (options?.method === 'DELETE') return new Response(null, { status: 204 })
    const path = new URL(String(url), 'http://localhost').pathname
    if (path.includes('/clients/')) return success(client)
    if (path.includes('/tasks'))
      return success(path.endsWith('/tasks') && options?.method === 'GET' ? [task] : task)
    reads++
    return success(project)
  }
  function Wrapper({ children }: PropsWithChildren) {
    return createElement(ProjectsProvider, { projectId: 'p1' }, children)
  }
  const useProjectTasks = () => useTasks('p1')
  const hook = await renderHook(useProjectTasks, Wrapper)
  try {
    assert.equal(hook.current.data.clients[0]?.id, 'c1')
    assert.equal(hook.current.tasks[0]?.id, 't1')
    await act(async () => {
      await hook.current.saveProject({ ...toProject(project), id: undefined })
    })
    await act(async () => {
      await hook.current.saveTask({ ...toTask(task), id: undefined })
    })
    await act(async () => {
      await hook.current.deleteTask('p1', 't1')
    })
    await act(async () => {
      await hook.current.deleteProject('p1')
    })
    assert.equal(reads, 6)
  } finally {
    await hook.unmount()
    globalThis.fetch = originalFetch
  }
})

test('profile provider refreshes after update and time-entry provider delegates mutations', async () => {
  const originalFetch = globalThis.fetch
  let reads = 0
  globalThis.fetch = async (url, options) => {
    if (options?.method === 'DELETE') return new Response(null, { status: 204 })
    if (String(url).includes('/users/me')) {
      reads++
      return success({ id: 'u1', email: 'owner@example.com', displayName: 'Owner' })
    }
    if (String(url).includes('/clients?')) return success([client])
    if (String(url).includes('/projects?')) return success([project])
    return success({
      id: 'e1',
      projectId: 'p1',
      startedAt: '2026-10-10T00:00:00Z',
      durationSeconds: 60,
    })
  }
  const profileHook = await renderHook(useProfile, ProfileProvider)
  try {
    assert.ok(profileHook.current.data)
    const input = profileHook.current.data
    await act(async () => {
      await profileHook.current.updateProfile(input)
    })
    assert.equal(reads, 3)
  } finally {
    await profileHook.unmount()
  }
  const timeHook = await renderHook(useTimeEntries, TimeEntriesProvider)
  try {
    assert.equal(timeHook.current.data.clients[0]?.id, 'c1')
    assert.equal(timeHook.current.data.projects[0]?.id, 'p1')
    const input: TimeEntryInput = {
      project_id: 'p1',
      task_id: null,
      description: '',
      started_at: '2026-10-10T00:00:00Z',
      ended_at: null,
      duration_minutes: 1,
      billable: true,
      rate_snapshot: 0,
      currency: 'THB',
    }
    await act(async () => {
      assert.equal((await timeHook.current.saveTimeEntry(input)).id, 'e1')
    })
    await act(async () => {
      await timeHook.current.deleteTimeEntry('e1')
    })
  } finally {
    await timeHook.unmount()
    globalThis.fetch = originalFetch
  }
})
