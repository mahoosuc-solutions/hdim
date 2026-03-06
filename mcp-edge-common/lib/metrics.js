// mcp-edge-common/lib/metrics.js
const express = require('express');

let client;
try {
  client = require('prom-client');
} catch {
  client = null;
}

function createMetrics() {
  if (!client) {
    return {
      toolCallCounter: { inc() {} },
      toolDurationHistogram: { observe() {} },
      rateLimitCounter: { inc() {} },
      activeConnections: { inc() {}, dec() {} },
      circuitBreakerState: { set() {} },
      circuitBreakerFailures: { inc() {} },
      registry: null
    };
  }

  const registry = new client.Registry();
  client.collectDefaultMetrics({ register: registry });

  const toolCallCounter = new client.Counter({
    name: 'mcp_tool_calls_total',
    help: 'Total MCP tool calls',
    labelNames: ['tool', 'role', 'status'],
    registers: [registry]
  });

  const toolDurationHistogram = new client.Histogram({
    name: 'mcp_tool_duration_seconds',
    help: 'MCP tool call duration in seconds',
    labelNames: ['tool'],
    buckets: [0.01, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10],
    registers: [registry]
  });

  const rateLimitCounter = new client.Counter({
    name: 'mcp_rate_limit_rejections_total',
    help: 'Total rate limit rejections',
    registers: [registry]
  });

  const activeConnections = new client.Gauge({
    name: 'mcp_active_connections',
    help: 'Number of active MCP connections',
    registers: [registry]
  });

  const circuitBreakerState = new client.Gauge({
    name: 'mcp_circuit_breaker_state',
    help: 'Circuit breaker state (0=closed, 1=open, 2=half-open)',
    labelNames: ['service'],
    registers: [registry]
  });

  const circuitBreakerFailures = new client.Counter({
    name: 'mcp_circuit_breaker_failures_total',
    help: 'Total circuit breaker failures',
    labelNames: ['service'],
    registers: [registry]
  });

  return {
    toolCallCounter,
    toolDurationHistogram,
    rateLimitCounter,
    activeConnections,
    circuitBreakerState,
    circuitBreakerFailures,
    registry
  };
}

function createMetricsRouter(registry) {
  const router = express.Router();

  router.get('/metrics', async (req, res) => {
    if (!registry) {
      return res.status(501).json({ error: 'Metrics not available (prom-client not installed)' });
    }
    try {
      const metrics = await registry.metrics();
      res.set('Content-Type', registry.contentType);
      res.end(metrics);
    } catch (err) {
      res.status(500).end(String(err));
    }
  });

  return router;
}

module.exports = { createMetrics, createMetricsRouter };
