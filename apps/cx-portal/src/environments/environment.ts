export const environment = {
  production: false,
  cxApiUrl: '/api',
  cxWsUrl: 'ws://' + (typeof window !== 'undefined' ? window.location.host : 'localhost:8200'),
};
