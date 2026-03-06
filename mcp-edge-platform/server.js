// mcp-edge-platform/server.js
const express = require('express');
const path = require('node:path');
const cors = require('cors');
const helmet = require('helmet');
const { createHealthRouter, createMcpRouter, createRateLimiter, createCorsOptions, createAuditLogger, createMetrics, createMetricsRouter, wrapClientWithBreaker, traceContextMiddleware } = require('hdim-mcp-edge-common');
const { createPlatformClient } = require('./lib/platform-client');

function loadTools(metrics) {
  const rawClient = createPlatformClient();
  const client = wrapClientWithBreaker(rawClient, { name: 'platform', metrics });
  return [
    require('./lib/tools/edge-health').definition,
    require('./lib/tools/platform-health').createDefinition(client),
    require('./lib/tools/platform-info').definition,
    require('./lib/tools/fhir-metadata').createDefinition(client),
    require('./lib/tools/service-catalog').createDefinition(client),
    require('./lib/tools/dashboard-stats').createDefinition(client),
    require('./lib/tools/demo-status').createDefinition(client),
    require('./lib/tools/demo-seed').createDefinition(client)
  ];
}

function createApp() {
  const app = express();
  const metrics = createMetrics();

  app.use(helmet());
  app.use(cors(createCorsOptions()));
  app.use(express.json({ limit: '1mb' }));
  app.use(traceContextMiddleware());
  app.use(createRateLimiter({ metrics }));

  const tools = loadTools(metrics);

  app.use(createHealthRouter({
    serviceName: 'hdim-platform-edge',
    version: '0.1.0'
  }));
  app.use(createMetricsRouter(metrics.registry));

  const logger = createAuditLogger({ serviceName: 'hdim-platform-edge' });

  app.use(createMcpRouter({
    tools,
    serverName: 'hdim-platform-edge',
    serverVersion: '0.1.0',
    enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false',
    fixturesDir: path.join(__dirname, 'fixtures'),
    logger,
    metrics
  }));

  return app;
}

module.exports = { createApp };
