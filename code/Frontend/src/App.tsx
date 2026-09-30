import { Suspense } from 'react'
import { BrowserRouter } from 'react-router-dom'
import AppRoutes from './routes/AppRoutes'
import { Spinner } from './components/ui/spinner'
import { Toaster } from './components/ui/sonner'
import './styles/App.css'

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div className="view-state route-loading"><Spinner className="size-6 text-primary" /><p>กำลังเปิดหน้า...</p></div>}>
        <AppRoutes />
      </Suspense>
      <Toaster richColors closeButton />
    </BrowserRouter>
  )
}

export default App
