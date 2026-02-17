# CQL Engine Visualization Dashboard - Frontend

Real-time visualization dashboard for monitoring CQL measure evaluation batch processing.

## Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

**Access:** http://localhost:5173/

## Environment Variables

Copy `.env.example` to `.env` and adjust values as needed.

```bash
cp .env.example .env
```

Key variables for integrations health/session testing:
- `VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK` (`true|false`)
- `VITE_INTEGRATIONS_API_URL` (optional base URL for integrations endpoint)
- `VITE_INTEGRATIONS_HEALTH_POLL_MS` (polling interval, default `5000`)
- `VITE_SESSION_EXPIRED_REDIRECT_URL` (default `/login`)
- `VITE_AUTH_LOGIN_URL` (optional external IdP login URL for production auth redirect)
- `VITE_AUTH_TOKEN_STORAGE_KEY` (optional token storage key, default `authToken`)
- `VITE_AUTH_TOKEN_QUERY_PARAM` (optional callback token param, default `access_token`)

## Live Session-Expiry Test

Use helper script:

```bash
./scripts/verify-session-expiry-flow.sh
# or
npm run verify:session-flow
```

Automated targeted suite:

```bash
npm run test:session-flow
```

Run the full local handoff sequence (detect + targeted tests):

```bash
npm run run:session-flow-handoff
```

Predict which CI session-flow checks will run for your current branch:

```bash
npm run detect:session-flow-checks
```

Browser e2e (Chromium):

```bash
npm run e2e:session-flow
```

This uses `e2e/playwright.session.config.ts` (isolated from sales API seed/teardown).

External auth redirect e2e (Chromium):

```bash
npm run e2e:session-flow:external-auth
```

This uses `e2e/playwright.session.external-auth.config.ts` and validates `VITE_AUTH_LOGIN_URL` redirect behavior.

Auth callback e2e (Chromium):

```bash
npm run e2e:auth-callback
```

This uses `e2e/playwright.auth-callback.config.ts` and validates `/auth/callback` token handling.

CI workflow: `.github/workflows/frontend-session-flow-e2e.yml` runs both session-flow gates on frontend code changes.
- `session-flow` runs on frontend changes.
- `session-flow-external-auth` runs when auth/login-related files change (or on manual dispatch).
- `auth-callback` runs when auth/login-related files change (or on manual dispatch).

1. Set `VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK=true` in `.env`.
2. Run `npm run dev`.
3. Open `http://localhost:5173/evaluations`.
4. Force integrations endpoint to return `401` (expired token/session).
   - Dev shortcut: click `Force 401: On` in the connection status area.
5. Verify behavior:
   - polling stops (no repeated `401` loop),
   - token keys are cleared from `localStorage`,
   - UI transitions to `/login`,
   - refreshing `/evaluations` while expired still redirects to `/login`,
   - signing in on `/login` returns to `/evaluations`.

## Status

✅ **Phase 2.1 Complete** - Frontend Project Setup & WebSocket Integration

All 9 tasks completed:
1. React + TypeScript project initialized with Vite
2. Core dependencies installed (MUI, Zustand, Recharts)
3. Project structure created (components, hooks, services, store)
4. WebSocket service with auto-reconnection implemented
5. TypeScript event types defined (mirroring backend)
6. Custom WebSocket hook created
7. Connection status indicator component built
8. Basic dashboard layout created
9. WebSocket connection tested and verified

## Architecture

- **Backend**: CQL Engine Service (port 8082) - Spring Boot + Kafka + WebSocket
- **Frontend**: React SPA (port 5173) - TypeScript + MUI + Zustand
- **Communication**: WebSocket (`ws://localhost:8082/ws/evaluation-progress`)

## Features

### Real-Time Dashboard
- Live connection status indicator
- Summary statistics (completed, failed, success rate, compliance)
- Active batch progress with metrics
- Recent events feed (last 10 events)

### WebSocket Service
- Auto-reconnection with exponential backoff
- Tenant-based filtering
- Event routing to Zustand store
- Connection state management

For detailed documentation, see the complete README.
