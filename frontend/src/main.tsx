import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { ApprovalDashboardPage } from './components/ApprovalDashboardPage.tsx'
import { SalesPage } from './components/sales'
import { ErrorBoundary } from './components/ErrorBoundary'
import { AppShell } from './components/AppShell.tsx'
import { LoginPage } from './components/LoginPage.tsx'
import { ExternalAuthMockPage } from './components/ExternalAuthMockPage.tsx'
import { AuthCallbackPage } from './components/AuthCallbackPage.tsx'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
          <Route path="/external-login" element={<ExternalAuthMockPage />} />
          <Route path="/" element={<AppShell />}>
            <Route index element={<Navigate to="/evaluations" replace />} />
            <Route path="evaluations" element={<App />} />
            <Route path="approvals" element={<ApprovalDashboardPage />} />
            <Route path="sales" element={<SalesPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/evaluations" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  </StrictMode>,
)
