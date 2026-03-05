// mcp-edge-clinical/server.js
const express = require('express');
const path = require('node:path');
const cors = require('cors');
const helmet = require('helmet');
const { createHealthRouter, createMcpRouter, createRateLimiter, createCorsOptions, createAuditLogger } = require('hdim-mcp-edge-common');
const { createClinicalClient } = require('./lib/clinical-client');
const { createPhiAuditLogger } = require('./lib/phi-audit');

const VALID_STRATEGIES = ['composite', 'high-value', 'full-surface'];

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

function createApp() {
  const app = express();
  app.use(helmet());
  app.use(cors(createCorsOptions()));
  app.use(express.json({ limit: '1mb' }));
  app.use(createRateLimiter());

  const strategyName = process.env.CLINICAL_TOOL_STRATEGY || 'composite';
  const client = createClinicalClient();
  const tools = loadStrategy(strategyName, client);

  // Infrastructure tool — loaded regardless of strategy
  tools.push(require('./lib/tools/edge-health').definition);

  app.use(createHealthRouter({
    serviceName: 'hdim-clinical-edge',
    version: '0.1.0'
  }));

  const logger = createAuditLogger({ serviceName: 'hdim-clinical-edge' });
  const phiAuditLogger = createPhiAuditLogger({ serviceName: 'hdim-clinical-edge' });

  app.use(createMcpRouter({
    tools,
    serverName: 'hdim-clinical-edge',
    serverVersion: '0.1.0',
    enforceRoleAuth: process.env.MCP_EDGE_ENFORCE_ROLE_AUTH !== 'false',
    fixturesDir: path.join(__dirname, 'lib', 'strategies', strategyName, 'fixtures'),
    logger,
    rolePolicies: loadRolePolicies(strategyName),
    phiAuditLogger
  }));

  return app;
}

module.exports = { createApp };
