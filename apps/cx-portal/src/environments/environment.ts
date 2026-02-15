export const environment = {
  production: false,
  // Base origin for CX API. Leave blank when serving API under the same origin at `/api/*`.
  // For cross-origin deployments (e.g. Vercel frontend + Cloud Run API), set window.__CX_API_URL at runtime.
  cxApiUrl: '',
  cxWsUrl: 'ws://' + (typeof window !== 'undefined' ? window.location.host : 'localhost:8200'),
};
