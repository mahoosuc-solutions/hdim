// mcp-edge-clinical/index.js
const { createApp } = require('./server');

const port = Number(process.env.PORT || 3300);
const app = createApp();

const server = app.listen(port, () => {
  console.log(`[hdim-clinical-edge] listening on :${port} (strategy: ${process.env.CLINICAL_TOOL_STRATEGY || 'composite'})`);
});

function shutdown(signal) {
  console.log(`[hdim-clinical-edge] received ${signal}, shutting down`);
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));

module.exports = { app };
