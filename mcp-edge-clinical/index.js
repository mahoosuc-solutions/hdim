// mcp-edge-clinical/index.js
const { createApp } = require('./server');
const { createAuditLogger } = require('hdim-mcp-edge-common');

const logger = createAuditLogger({ serviceName: 'hdim-clinical-edge' });
const port = Number(process.env.PORT || 3300);
const app = createApp();

const server = app.listen(port, () => {
  logger.info({ port, strategy: process.env.CLINICAL_TOOL_STRATEGY || 'composite' }, 'server started');
});

function shutdown(signal) {
  logger.info({ signal }, 'shutting down');
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('uncaughtException', (err) => {
  logger.fatal({ err }, 'uncaught exception');
  process.exit(1);
});

process.on('unhandledRejection', (reason) => {
  logger.fatal({ err: reason }, 'unhandled rejection');
  process.exit(1);
});

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
