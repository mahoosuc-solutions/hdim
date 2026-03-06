const { createCircuitBreaker, wrapClientWithBreaker } = require('../lib/circuit-breaker');

describe('circuit-breaker', () => {
  describe('createCircuitBreaker', () => {
    it('passes through successful calls', async () => {
      const fn = jest.fn().mockResolvedValue({ ok: true });
      const breaker = createCircuitBreaker(fn, { name: 'test' });
      const result = await breaker.fire();
      expect(result).toEqual({ ok: true });
      expect(fn).toHaveBeenCalledTimes(1);
    });

    it('opens after repeated failures', async () => {
      let callCount = 0;
      const fn = jest.fn().mockImplementation(() => {
        callCount++;
        return Promise.reject(new Error('downstream failure'));
      });

      const breaker = createCircuitBreaker(fn, {
        name: 'test',
        errorThresholdPercentage: 50,
        resetTimeout: 60000,
        timeout: 5000
      });

      // Fire enough to trigger open (volumeThreshold = 5)
      for (let i = 0; i < 6; i++) {
        try { await breaker.fire(); } catch { /* expected */ }
      }

      // Breaker should be open now — next call should fail fast
      await expect(breaker.fire()).rejects.toThrow(/Breaker is open/);
    });

    it('emits metrics on state transitions', async () => {
      const mockMetrics = {
        circuitBreakerState: { set: jest.fn() },
        circuitBreakerFailures: { inc: jest.fn() }
      };

      const fn = jest.fn().mockResolvedValue({ ok: true });
      const breaker = createCircuitBreaker(fn, { name: 'svc', metrics: mockMetrics });

      await breaker.fire();
      expect(mockMetrics.circuitBreakerState.set).toHaveBeenCalledWith({ service: 'svc' }, 0);
    });

    it('emits failure metrics on errors', async () => {
      const mockMetrics = {
        circuitBreakerState: { set: jest.fn() },
        circuitBreakerFailures: { inc: jest.fn() }
      };

      const fn = jest.fn().mockRejectedValue(new Error('fail'));
      const breaker = createCircuitBreaker(fn, { name: 'svc', metrics: mockMetrics });

      try { await breaker.fire(); } catch { /* expected */ }
      expect(mockMetrics.circuitBreakerFailures.inc).toHaveBeenCalledWith({ service: 'svc' });
    });
  });

  describe('wrapClientWithBreaker', () => {
    it('wraps client get/post with circuit breakers', async () => {
      const mockClient = {
        baseUrl: 'http://localhost:8080',
        get: jest.fn().mockResolvedValue({ status: 200, ok: true, body: '{}' }),
        post: jest.fn().mockResolvedValue({ status: 201, ok: true, body: '{}' })
      };

      const wrapped = wrapClientWithBreaker(mockClient, { name: 'platform' });

      const getResult = await wrapped.get('/health');
      expect(getResult).toEqual({ status: 200, ok: true, body: '{}' });
      expect(mockClient.get).toHaveBeenCalledWith('/health', undefined);

      const postResult = await wrapped.post('/api', { data: 1 });
      expect(postResult).toEqual({ status: 201, ok: true, body: '{}' });
    });

    it('preserves baseUrl', () => {
      const mockClient = {
        baseUrl: 'http://test:9090',
        get: jest.fn(),
        post: jest.fn()
      };

      const wrapped = wrapClientWithBreaker(mockClient, { name: 'test' });
      expect(wrapped.baseUrl).toBe('http://test:9090');
    });

    it('exposes breaker instances for inspection', () => {
      const mockClient = { baseUrl: 'http://x', get: jest.fn(), post: jest.fn() };
      const wrapped = wrapClientWithBreaker(mockClient, { name: 'test' });
      expect(wrapped.getBreaker).toBeDefined();
      expect(wrapped.postBreaker).toBeDefined();
    });

    it('fast-fails when breaker is open', async () => {
      const mockClient = {
        baseUrl: 'http://x',
        get: jest.fn().mockRejectedValue(new Error('down')),
        post: jest.fn()
      };

      const wrapped = wrapClientWithBreaker(mockClient, {
        name: 'fail-test',
        errorThresholdPercentage: 50,
        resetTimeout: 60000
      });

      // Trigger enough failures to open breaker
      for (let i = 0; i < 6; i++) {
        try { await wrapped.get('/bad'); } catch { /* expected */ }
      }

      // Should now fast-fail without calling underlying client
      const callsBefore = mockClient.get.mock.calls.length;
      try { await wrapped.get('/bad'); } catch (err) {
        expect(err.message).toMatch(/Breaker is open/);
      }
      // Underlying client should NOT have been called again
      expect(mockClient.get.mock.calls.length).toBe(callsBefore);
    });
  });
});
