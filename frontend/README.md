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
