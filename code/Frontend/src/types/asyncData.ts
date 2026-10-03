export interface AsyncDataState<T> {
  data: T
  loading: boolean
  error: string
  refresh: () => Promise<void>
}
