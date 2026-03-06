// mcp-edge-clinical/server.js
const express = require('express');
const path = require('node:path');
const cors = require('cors');
const helmet = require('helmet');
const { createHealthRouter, createMcpRouter, createRateLimiter, createCorsOptions, createAuditLogger, createMetrics, createMetricsRouter, wrapClientWithBreaker, traceContextMiddleware } = require('hdim-mcp-edge-common');
const { createClinicalClient } = require('./lib/clinical-client');
const { createPhiAuditLogger } = require('./lib/phi-audit');
const { StrategyManager, VALID_STRATEGIES } = require('./lib/strategy-manager');

function loadStrategy(strategyName, client) {
  if (!VALID_STRATEGIES.includes(strategyName)) {
    throw new Error(`Unknown clinical tool strategy: "${strategyName}". Valid: ${VALID_STRATEGIES.join(', ')}`);
  }
  const strategy = require(`./lib/strategies/${strategyName}`);
  return strategy.loadTools(client);
}

function loadRolePolicies(strategyName) {
  try {
    const { clinicalRolePolicies } = require(`./lib/strategies/${strategyName}/role-policies`);
    return clinicalRolePolicies();
  } catch {
    return undefined; // fall back to common default policies
  }
}

function parseAllowedStrategies() {
  const env = process.env.CLINICAL_ALLOWED_STRATEGIES;
  if (!env) return VALID_STRATEGIES;
  return env.split(',').map((s) => s.trim()).filter(Boolean);
}

function createApp() {
  const app = express();
  const metrics = createMetrics();

  app.use(helmet());
  app.use(cors(createCorsOptions()));
  app.use(express.json({ limit: '1mb' }));
  app.use(traceContextMiddleware());
  app.use(createRateLimiter({ metrics }));

  const strategyName = process.env.CLINICAL_TOOL_STRATEGY || 'composite';
  const rawClient = createClinicalClient();
  const client = wrapClientWithBreaker(rawClient, { name: 'clinical', metrics });

  app.use(createHealthRouter({
    serviceName: 'hdim-clinical-edge',
    version: '0.1.0'
  }));
  app.use(createMetricsRouter(metrics.registry));

  const logger = createAuditLogger({ serviceName: 'hdim-clinical-edge' });
  const phiAuditLogger = createPhiAuditLogger({ serviceName: 'hdim-clinical-edge' });

  const strategyManager = new StrategyManager({
    baselineStrategy: strategyName,
    allowedStrategies: parseAllowedStrategies(),
    client,
    logger,
    phiAuditLogger
  });

  // Admin + infrastructure tools — persist across strategy swaps
  strategyManager.registerAdminTools([
    require('./lib/tools/edge-health').definition,
    require('./lib/tools/admin-preview-strategy').definition,
    require('./lib/tools/admin-set-strategy').definition,
    require('./lib/tools/admin-rollback-strategy').definition,
  ]);

  app.use(createMcpRouter({
    tools: strategyManager.tools,
    serverName: 'hdim-clinical-edge',
    serverVersion: '0.1.0',
    enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false',
    fixturesDir: strategyManager.fixturesDir,
    logger,
    rolePolicies: strategyManager.rolePolicies,
    phiAuditLogger,
    strategyManager,
    metrics
  }));

  return app;
}

module.exports = { createApp, loadStrategy, loadRolePolicies };
