import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { ApprovalDashboardPage } from './components/ApprovalDashboardPage.tsx'
import { ErrorBoundary } from './components/ErrorBoundary'
import { AppShell } from './components/AppShell.tsx'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppShell />}>
            <Route index element={<Navigate to="/evaluations" replace />} />
            <Route path="evaluations" element={<App />} />
            <Route path="approvals" element={<ApprovalDashboardPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  </StrictMode>,
)
