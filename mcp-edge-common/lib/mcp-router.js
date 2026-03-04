const express = require('express');
const { jsonRpcResult, jsonRpcError } = require('./jsonrpc');
const { extractOperatorRole, authorizeToolCall } = require('./auth');
const { isDemoMode, loadFixture } = require('./demo-mode');

const MCP_PROTOCOL_VERSION = '2025-11-25';

function createMcpRouter({ tools, serverName, serverVersion, enforceRoleAuth = true, fixturesDir }) {
  const router = express.Router();
  const toolMap = new Map(tools.map((t) => [t.name, t]));

  function handleInitialize(id) {
    return jsonRpcResult(id, {
      protocolVersion: MCP_PROTOCOL_VERSION,
      serverInfo: { name: serverName, version: serverVersion },
      capabilities: { tools: { listChanged: false } }
    });
  }

  function handleToolsList(id) {
    const toolDefs = tools.map(({ name, description, inputSchema }) => ({
      name, description, inputSchema
    }));
    return jsonRpcResult(id, { tools: toolDefs });
  }

  async function handleToolsCall(id, params, req) {
    const { name, arguments: args } = params || {};
    const tool = toolMap.get(name);
    if (!tool) {
      return jsonRpcError(id, -32602, `Tool not found: ${name}`, {
        tool: name, reason: 'unknown_tool'
      });
    }

    const role = extractOperatorRole(req);
    const authResult = authorizeToolCall({
      toolName: name, role, enforce: enforceRoleAuth
    });
    if (!authResult.allowed) {
      return jsonRpcError(id, -32603, `Forbidden: ${authResult.reason}`, {
        tool: name, role, reason: authResult.reason
      });
    }

    // Demo mode — return fixture if available
    if (fixturesDir && isDemoMode()) {
      const fixture = loadFixture(fixturesDir, name);
      if (fixture) {
        return jsonRpcResult(id, {
          content: [{ type: 'text', text: JSON.stringify(fixture, null, 2) }]
        });
      }
    }

    try {
      const result = await tool.handler(args || {}, { req });
      return jsonRpcResult(id, result);
    } catch (err) {
      return jsonRpcError(id, -32603, 'Tool execution error', {
        tool: name, detail: err?.message || String(err)
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
