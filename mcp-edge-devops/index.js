// mcp-edge-devops/index.js
const { createApp } = require('./server');
const { createAuditLogger } = require('hdim-mcp-edge-common');

const logger = createAuditLogger({ serviceName: 'hdim-devops-edge' });
const port = Number(process.env.PORT || 3200);
const app = createApp();

const server = app.listen(port, () => {
  logger.info({ port }, 'server started');
});

function shutdown(signal) {
  logger.info({ signal }, 'shutting down');
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
