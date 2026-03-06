// mcp-edge-common/lib/circuit-breaker.js
const CircuitBreaker = require('opossum');

const STATE_MAP = { 0: 'closed', 1: 'open', 2: 'half-open' };
const STATE_GAUGE = { closed: 0, open: 1, halfOpen: 2 };

function createCircuitBreaker(fn, { name, metrics, timeout = 15000, errorThresholdPercentage = 50, resetTimeout = 30000 } = {}) {
  const breaker = new CircuitBreaker(fn, {
    timeout,
    errorThresholdPercentage,
    resetTimeout,
    volumeThreshold: 5
  });

  if (metrics && name) {
    breaker.on('success', () => {
      metrics.circuitBreakerState.set({ service: name }, STATE_GAUGE.closed);
    });
    breaker.on('open', () => {
      metrics.circuitBreakerState.set({ service: name }, STATE_GAUGE.open);
      metrics.circuitBreakerFailures.inc({ service: name });
    });
    breaker.on('halfOpen', () => {
      metrics.circuitBreakerState.set({ service: name }, STATE_GAUGE.halfOpen);
    });
    breaker.on('close', () => {
      metrics.circuitBreakerState.set({ service: name }, STATE_GAUGE.closed);
    });
    breaker.on('failure', () => {
      metrics.circuitBreakerFailures.inc({ service: name });
    });
  }

  return breaker;
}

function wrapClientWithBreaker(client, { name, metrics, ...opts } = {}) {
  const getBreaker = createCircuitBreaker(
    (path, options) => client.get(path, options),
    { name: `${name}_get`, metrics, ...opts }
  );
  const postBreaker = createCircuitBreaker(
    (path, body, options) => client.post(path, body, options),
    { name: `${name}_post`, metrics, ...opts }
  );

  return {
    baseUrl: client.baseUrl,
    get: (path, options) => getBreaker.fire(path, options),
    post: (path, body, options) => postBreaker.fire(path, body, options),
    getBreaker,
    postBreaker
  };
}

module.exports = { createCircuitBreaker, wrapClientWithBreaker, STATE_MAP, STATE_GAUGE };
