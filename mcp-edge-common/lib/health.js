const express = require('express');

function createHealthRouter({ serviceName, version, statusProvider, checks }) {
  const router = express.Router();
  const startTime = Date.now();

  function buildStatus() {
    const base = {
      status: 'healthy',
      service: serviceName,
      version,
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString()
    };
    if (statusProvider) {
      Object.assign(base, statusProvider());
    }
    // Run registered health checks
    if (checks && checks.length > 0) {
      const checkResults = {};
      for (const check of checks) {
        try {
          checkResults[check.name] = check.fn();
        } catch {
          checkResults[check.name] = { status: 'error' };
          base.status = 'degraded';
        }
      }
      base.checks = checkResults;
    }
    return base;
  }

  router.get('/health', (req, res) => {
    const body = buildStatus();
    const statusCode = body.status === 'unhealthy' ? 503 : 200;
    res.status(statusCode).json(body);
  });

  // Deep health probe with full check details
  router.get('/health/ready', (req, res) => {
    const body = buildStatus();
    body.ready = body.status !== 'unhealthy';
    const statusCode = body.ready ? 200 : 503;
    res.status(statusCode).json(body);
  });

  return router;
}

module.exports = { createHealthRouter };
