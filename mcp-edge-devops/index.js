// mcp-edge-devops/index.js
const { createApp } = require('./server');

const port = Number(process.env.PORT || 3200);
const app = createApp();

const server = app.listen(port, () => {
  console.log(`[hdim-devops-edge] listening on :${port}`);
});

function shutdown(signal) {
  console.log(`[hdim-devops-edge] received ${signal}, shutting down`);
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
