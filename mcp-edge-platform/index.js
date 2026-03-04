// mcp-edge-platform/index.js
const { createApp } = require('./server');

const port = Number(process.env.PORT || 3100);
const app = createApp();

const server = app.listen(port, () => {
  console.log(`[hdim-platform-edge] listening on :${port}`);
});

function shutdown(signal) {
  console.log(`[hdim-platform-edge] received ${signal}, shutting down`);
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
