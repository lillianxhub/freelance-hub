import type { Project } from './project'
import type { Task } from './task'
import type { TimeEntry } from './timeTracking'

export interface TimerWorkspace {
  data: {
    projects: Pick<Project, 'id' | 'name' | 'color' | 'status'>[]
    tasks: Pick<Task, 'id' | 'name'>[]
    time_entries: Pick<
      TimeEntry,
      'id' | 'project_id' | 'task_id' | 'description' | 'started_at' | 'ended_at'
    >[]
  }
  refresh: () => Promise<void>
}
