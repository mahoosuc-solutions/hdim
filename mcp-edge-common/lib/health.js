const express = require('express');

function createHealthRouter({ serviceName, version, statusProvider }) {
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
      return { ...base, ...statusProvider() };
    }
    return base;
  }

  router.get('/health', (req, res) => {
    const body = buildStatus();
    const statusCode = body.status === 'unhealthy' ? 503 : 200;
    res.status(statusCode).json(body);
  });

  return router;
}

module.exports = { createHealthRouter };
