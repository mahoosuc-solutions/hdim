const { createMetrics, createMetricsRouter } = require('../lib/metrics');
const express = require('express');
const request = require('supertest');

describe('metrics', () => {
  describe('createMetrics', () => {
    it('returns a registry and metric objects', () => {
      const m = createMetrics();
      expect(m.registry).toBeTruthy();
      expect(m.toolCallCounter).toBeDefined();
      expect(m.toolDurationHistogram).toBeDefined();
      expect(m.rateLimitCounter).toBeDefined();
      expect(m.activeConnections).toBeDefined();
      expect(m.circuitBreakerState).toBeDefined();
      expect(m.circuitBreakerFailures).toBeDefined();
    });

    it('counter can be incremented without error', () => {
      const m = createMetrics();
      expect(() => m.toolCallCounter.inc({ tool: 'test', role: 'dev', status: 'success' })).not.toThrow();
      expect(() => m.rateLimitCounter.inc()).not.toThrow();
      expect(() => m.circuitBreakerFailures.inc({ service: 'test' })).not.toThrow();
    });

    it('histogram can observe values', () => {
      const m = createMetrics();
      expect(() => m.toolDurationHistogram.observe({ tool: 'test' }, 0.123)).not.toThrow();
    });

    it('gauge can be set', () => {
      const m = createMetrics();
      expect(() => m.activeConnections.inc()).not.toThrow();
      expect(() => m.activeConnections.dec()).not.toThrow();
      expect(() => m.circuitBreakerState.set({ service: 'test' }, 1)).not.toThrow();
    });
  });

  describe('createMetricsRouter', () => {
    let app;
    let metrics;

    beforeEach(() => {
      metrics = createMetrics();
      app = express();
      app.use(createMetricsRouter(metrics.registry));
    });

    afterEach(async () => {
      metrics.registry.clear();
    });

    it('GET /metrics returns 200 with prometheus text format', async () => {
      const res = await request(app).get('/metrics');
      expect(res.status).toBe(200);
      expect(res.headers['content-type']).toMatch(/text\/plain|application\/openmetrics/);
      expect(res.text).toContain('mcp_tool_calls_total');
      expect(res.text).toContain('mcp_tool_duration_seconds');
      expect(res.text).toContain('mcp_rate_limit_rejections_total');
      expect(res.text).toContain('mcp_active_connections');
    });

    it('includes default process metrics', async () => {
      const res = await request(app).get('/metrics');
      expect(res.text).toContain('process_cpu');
    });

    it('reflects incremented counters', async () => {
      metrics.toolCallCounter.inc({ tool: 'edge_health', role: 'developer', status: 'success' });
      metrics.toolCallCounter.inc({ tool: 'edge_health', role: 'developer', status: 'success' });
      metrics.toolCallCounter.inc({ tool: 'edge_health', role: 'developer', status: 'error' });

      const res = await request(app).get('/metrics');
      expect(res.text).toMatch(/mcp_tool_calls_total\{.*tool="edge_health".*status="success".*\}\s+2/);
      expect(res.text).toMatch(/mcp_tool_calls_total\{.*tool="edge_health".*status="error".*\}\s+1/);
    });

    it('returns 501 when registry is null', async () => {
      const nullApp = express();
      nullApp.use(createMetricsRouter(null));
      const res = await request(nullApp).get('/metrics');
      expect(res.status).toBe(501);
    });
  });
});
