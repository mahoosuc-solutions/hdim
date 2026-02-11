export const environment = {
  production: true,
  cxApiUrl: '/api',
  cxWsUrl: 'wss://' + (typeof window !== 'undefined' ? window.location.host : 'localhost:8200'),
};
