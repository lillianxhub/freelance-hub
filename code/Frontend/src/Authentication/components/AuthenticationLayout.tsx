import type { PropsWithChildren } from 'react'

export default function AuthenticationLayout({ children }: PropsWithChildren) {
  return <div className="auth-page">{children}</div>
}
