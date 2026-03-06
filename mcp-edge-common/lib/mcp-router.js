const express = require('express');
const { jsonRpcResult, jsonRpcError } = require('./jsonrpc');
const { extractOperatorRole, authorizeToolCall } = require('./auth');
const { isDemoMode, loadFixture } = require('./demo-mode');
const { validateToolParams } = require('./param-validator');
const { scrubSensitive } = require('./audit-log');

const MCP_PROTOCOL_VERSION = '2025-11-25';

function extractPhiContext(tool, args) {
  const a = tool.audit;
  if (!a) return { phi: false, write: false, patientId: undefined };
  const patientId = a.patientIdArg ? (args?.[a.patientIdArg] ?? undefined) : undefined;
  return { phi: !!a.phi, write: !!a.write, patientId };
}

function createMcpRouter({ tools, serverName, serverVersion, enforceRoleAuth = true, fixturesDir, logger, rolePolicies, phiAuditLogger, strategyManager, metrics }) {
  const router = express.Router();
  // Static fallbacks when no strategyManager is present
  const staticToolMap = new Map(tools.map((t) => [t.name, t]));

  function getTools() {
    return strategyManager ? strategyManager.tools : tools;
  }
  function getToolMap() {
    return strategyManager ? strategyManager.toolMap : staticToolMap;
  }
  function getRolePolicies() {
    return strategyManager ? strategyManager.rolePolicies : rolePolicies;
  }
  function getFixturesDir() {
    return strategyManager ? strategyManager.fixturesDir : fixturesDir;
  }

  function handleInitialize(id) {
    return jsonRpcResult(id, {
      protocolVersion: MCP_PROTOCOL_VERSION,
      serverInfo: { name: serverName, version: serverVersion },
      capabilities: { tools: { listChanged: !!strategyManager } }
    });
  }

  function handleToolsList(id) {
    const currentTools = getTools();
    const toolDefs = currentTools.map(({ name, description, inputSchema }) => ({
      name, description, inputSchema
    }));
    const result = { tools: toolDefs };
    if (strategyManager && strategyManager.consumeListChangedFlag()) {
      result._meta = { listChanged: true };
    }
    return jsonRpcResult(id, result);
  }

  async function handleToolsCall(id, params, req) {
    const start = Date.now();
    const { name, arguments: args } = params || {};
    const currentToolMap = getToolMap();
    const tool = currentToolMap.get(name);
    if (!tool) {
      return jsonRpcError(id, -32602, `Tool not found: ${name}`, {
        tool: name, reason: 'unknown_tool'
      });
    }

    const role = extractOperatorRole(req);
    const currentPolicies = getRolePolicies();
    const authResult = authorizeToolCall({
      toolName: name, role, enforce: enforceRoleAuth, customPolicies: currentPolicies
    });
    if (!authResult.allowed) {
      if (logger) logger.warn({ tool: name, role, reason: authResult.reason, duration_ms: Date.now() - start }, 'tool_forbidden');
      if (phiAuditLogger) phiAuditLogger.logAuthDenied({ tool: name, role });
      if (metrics) metrics.toolCallCounter.inc({ tool: name, role: role || 'none', status: 'forbidden' });
      return jsonRpcError(id, -32603, `Forbidden: ${authResult.reason}`, {
        tool: name, role, reason: authResult.reason
      });
    }

    // Param validation
    const validationError = validateToolParams(tool.inputSchema, args);
    if (validationError) {
      return jsonRpcError(id, -32602, 'Invalid params', {
        tool: name, reason: 'invalid_params', detail: validationError
      });
    }

    // Demo mode — return fixture if available
    const currentFixturesDir = getFixturesDir();
    if (currentFixturesDir && isDemoMode()) {
      const fixture = loadFixture(currentFixturesDir, name);
      if (fixture) {
        const duration_ms = Date.now() - start;
        if (logger) logger.info({ tool: name, role, success: true, duration_ms, demo: true }, 'tool_call');
        if (phiAuditLogger) {
          const ctx = extractPhiContext(tool, args);
          phiAuditLogger.logToolAccess({
            tool: name, role, tenantId: args?.tenantId ?? null,
            patientId: ctx.patientId, success: true, durationMs: duration_ms, phi: ctx.phi, write: ctx.write
          });
        }
        if (metrics) {
          metrics.toolCallCounter.inc({ tool: name, role: role || 'none', status: 'success' });
          metrics.toolDurationHistogram.observe({ tool: name }, duration_ms / 1000);
        }
        return jsonRpcResult(id, {
          content: [{ type: 'text', text: JSON.stringify(fixture, null, 2) }]
        });
      }
    }

    try {
      const handlerContext = { req };
      if (strategyManager) {
        handlerContext.strategyManager = strategyManager;
      }
      if (phiAuditLogger) {
        handlerContext.phiAuditLogger = phiAuditLogger;
      }
      const result = await tool.handler(args || {}, handlerContext);
      const duration_ms = Date.now() - start;
      if (logger) logger.info({ tool: name, role, success: true, duration_ms, demo: isDemoMode() }, 'tool_call');
      if (metrics) {
        metrics.toolCallCounter.inc({ tool: name, role: role || 'none', status: 'success' });
        metrics.toolDurationHistogram.observe({ tool: name }, duration_ms / 1000);
      }
      if (phiAuditLogger) {
        const ctx = extractPhiContext(tool, args);
        phiAuditLogger.logToolAccess({
          tool: name, role, tenantId: (args || {}).tenantId ?? null,
          patientId: ctx.patientId, success: true, durationMs: duration_ms, phi: ctx.phi, write: ctx.write
        });
      }
      return jsonRpcResult(id, result);
    } catch (err) {
      const duration_ms = Date.now() - start;
      const safeMessage = scrubSensitive(err?.message || String(err));
      if (logger) logger.error({ tool: name, role, error_code: -32603, duration_ms, demo: isDemoMode() }, 'tool_error');
      if (metrics) {
        metrics.toolCallCounter.inc({ tool: name, role: role || 'none', status: 'error' });
        metrics.toolDurationHistogram.observe({ tool: name }, duration_ms / 1000);
      }
      if (phiAuditLogger) {
        const ctx = extractPhiContext(tool, args);
        phiAuditLogger.logToolAccess({
          tool: name, role, tenantId: (args || {}).tenantId ?? null,
          patientId: ctx.patientId, success: false, durationMs: duration_ms, phi: ctx.phi, write: ctx.write
        });
      }
      return jsonRpcError(id, -32603, 'Tool execution error', {
        tool: name, detail: safeMessage
      });
    }
  }

  router.post('/mcp', async (req, res) => {
    const { jsonrpc, id, method, params } = req.body || {};

    if (jsonrpc !== '2.0') {
      return res.json(jsonRpcError(id ?? null, -32600, 'Invalid JSON-RPC version'));
    }

    // Notifications (no id) get 204
    if (id === undefined || id === null) {
      if (method === 'notifications/initialized' || method === 'notifications/cancelled') {
        return res.status(204).end();
      }
    }

    let response;
    switch (method) {
      case 'initialize':
        response = handleInitialize(id);
        break;
      case 'tools/list':
        response = handleToolsList(id);
        break;
      case 'tools/call':
        response = await handleToolsCall(id, params, req);
        break;
      default:
        response = jsonRpcError(id, -32601, `Method not found: ${method}`);
    }

    res.json(response);
  });

  return router;
}

module.exports = { createMcpRouter };
