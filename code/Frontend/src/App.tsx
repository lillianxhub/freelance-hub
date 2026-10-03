import { Suspense } from 'react'
import { BrowserRouter } from 'react-router-dom'
import AppRoutes from './routes/AppRoutes'
import { Spinner } from './components/ui/spinner'
import { Toaster } from './components/ui/sonner'
import './styles/App.css'

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div className="flex min-h-screen flex-col items-center justify-center gap-2.5 text-center text-sm text-text-secondary"><Spinner className="size-6 text-primary" /><p>กำลังเปิดหน้า...</p></div>}>
        <AppRoutes />
      </Suspense>
      <Toaster richColors closeButton />
    </BrowserRouter>
  )
}

export default App
