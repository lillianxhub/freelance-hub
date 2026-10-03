import type { ApiCurrentTimer } from './api'

export interface CurrentTimerContextValue {
  currentTimer: ApiCurrentTimer | null
  refreshCurrentTimer: () => Promise<void>
}
