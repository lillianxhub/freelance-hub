import { Suspense } from 'react'
import { BrowserRouter } from 'react-router-dom'
import AppRoutes from './routes/AppRoutes'
import './styles/App.css'

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div className="view-state route-loading"><div className="loading-spinner" /><p>กำลังเปิดหน้า...</p></div>}>
        <AppRoutes />
      </Suspense>
    </BrowserRouter>
  )
}

export default App
