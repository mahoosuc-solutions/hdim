const DEFAULT_ORIGINS = [
  'http://localhost:3000',
  'http://localhost:3100',
  'http://localhost:3200',
  'http://127.0.0.1:3000',
  'http://127.0.0.1:3100',
  'http://127.0.0.1:3200'
];

function createCorsOptions() {
  const envOrigins = process.env.MCP_EDGE_CORS_ORIGINS;

  let origin;
  if (!envOrigins) {
    origin = DEFAULT_ORIGINS;
  } else if (envOrigins.trim() === '*') {
    origin = '*';
  } else {
    origin = envOrigins.split(',').map(o => o.trim()).filter(Boolean);
  }

  return { origin, credentials: false };
}

module.exports = { createCorsOptions };
