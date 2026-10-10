import assert from 'node:assert/strict'
import test from 'node:test'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { renderComponent } from '../../test-support/react'
import { AuthContext } from '../useAuthentication'
import ProtectedRoute from './ProtectedRoute'
import type { AuthContextValue, AuthUser } from '../../types/auth'

test('protected routes wait for auth, redirect guests and allow authenticated users', async () => {
  const user: AuthUser = {
    id: 'u1',
    email: 'owner@example.com',
    user_metadata: { full_name: 'Owner' },
  }
  for (const state of [
    { loading: true, user: null, text: 'กำลังตรวจสอบการเข้าสู่ระบบ' },
    { loading: false, user: null, text: 'Login page' },
    { loading: false, user, text: 'Private page' },
  ]) {
    const context: AuthContextValue = {
      loading: state.loading,
      user: state.user,
      session: state.user ? { user: state.user } : null,
      login: async () => {},
      register: async () => {},
      logout: async () => {},
    }
    const view = await renderComponent(
      <AuthContext.Provider value={context}>
        <MemoryRouter initialEntries={['/private']}>
          <Routes>
            <Route
              path="/private"
              element={
                <ProtectedRoute>
                  <span>Private page</span>
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<span>Login page</span>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )
    try {
      assert.ok(view.container.textContent?.includes(state.text))
    } finally {
      await view.unmount()
    }
  }
})
