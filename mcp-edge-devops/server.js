// mcp-edge-devops/server.js
const express = require('express');
const path = require('node:path');
const cors = require('cors');
const helmet = require('helmet');
const { createHealthRouter, createMcpRouter, createRateLimiter, createCorsOptions, createAuditLogger } = require('hdim-mcp-edge-common');

const { createDockerClient } = require('./lib/docker-client');

function loadTools() {
  const dockerClient = createDockerClient();
  return [
    require('./lib/tools/edge-health').definition,
    require('./lib/tools/docker-status').createDefinition(dockerClient),
    require('./lib/tools/docker-logs').createDefinition(dockerClient),
    require('./lib/tools/docker-restart').createDefinition(dockerClient),
    require('./lib/tools/service-dependencies').definition,
    require('./lib/tools/compose-config').createDefinition(dockerClient),
    require('./lib/tools/build-status').createDefinition()
  ];
}

function createApp() {
  const app = express();
  app.use(helmet());
  app.use(cors(createCorsOptions()));
  app.use(express.json({ limit: '1mb' }));
  app.use(createRateLimiter());

  const tools = loadTools();

  app.use(createHealthRouter({
    serviceName: 'hdim-devops-edge',
    version: '0.1.0'
  }));

  const logger = createAuditLogger({ serviceName: 'hdim-devops-edge' });

  app.use(createMcpRouter({
    tools,
    serverName: 'hdim-devops-edge',
    serverVersion: '0.1.0',
    enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false',
    fixturesDir: path.join(__dirname, 'fixtures'),
    logger
  }));

  return app;
}

module.exports = { createApp };
