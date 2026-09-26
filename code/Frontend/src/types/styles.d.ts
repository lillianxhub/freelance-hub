import 'react'

declare module 'react' {
  interface CSSProperties {
    '--dot-color'?: string
    '--project-color'?: string
    '--metric-color'?: string
    '--metric-soft'?: string
  }
}
