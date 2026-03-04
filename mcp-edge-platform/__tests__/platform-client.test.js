const { createPlatformClient } = require('../lib/platform-client');

const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('platform-client', () => {
  afterEach(() => mockFetch.mockReset());

  describe('configuration', () => {
    it('creates a client with default base URL', () => {
      const client = createPlatformClient();
      expect(client.baseUrl).toBe('http://localhost:18080');
    });
    it('creates a client with custom base URL', () => {
      const client = createPlatformClient({ baseUrl: 'http://gateway:8080' });
      expect(client.baseUrl).toBe('http://gateway:8080');
    });
    it('strips trailing slash from base URL', () => {
      const client = createPlatformClient({ baseUrl: 'http://localhost:18080/' });
      expect(client.baseUrl).toBe('http://localhost:18080');
    });
  });

  describe('get()', () => {
    it('sends GET with correct URL', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{"status":"UP"}' });
      const client = createPlatformClient({ baseUrl: 'http://gw:8080' });
      const result = await client.get('/actuator/health');
      expect(mockFetch).toHaveBeenCalledWith('http://gw:8080/actuator/health', expect.objectContaining({ method: 'GET' }));
      expect(result).toEqual({ status: 200, ok: true, body: '{"status":"UP"}', url: 'http://gw:8080/actuator/health' });
    });

    it('sends Bearer header when API key set', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ apiKey: 'test-key' });
      await client.get('/health');
      expect(mockFetch.mock.calls[0][1].headers.authorization).toBe('Bearer test-key');
    });

    it('omits Authorization header when no API key', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ apiKey: '' });
      await client.get('/health');
      expect(mockFetch.mock.calls[0][1].headers.authorization).toBeUndefined();
    });

    it('truncates responses over 20KB', async () => {
      const largeBody = 'x'.repeat(25000);
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => largeBody });
      const client = createPlatformClient();
      const result = await client.get('/large');
      expect(result.body.length).toBeLessThan(21000);
      expect(result.body).toContain('...[truncated]');
    });

    it('reports non-200 status without throwing', async () => {
      mockFetch.mockResolvedValue({ status: 500, ok: false, text: async () => 'Error' });
      const client = createPlatformClient();
      const result = await client.get('/fail');
      expect(result.ok).toBe(false);
      expect(result.status).toBe(500);
    });

    it('propagates fetch errors', async () => {
      mockFetch.mockRejectedValue(new Error('ECONNREFUSED'));
      const client = createPlatformClient();
      await expect(client.get('/fail')).rejects.toThrow('ECONNREFUSED');
    });

    it('uses AbortSignal.timeout', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ timeout: 5000 });
      await client.get('/test');
      expect(mockFetch.mock.calls[0][1].signal).toBeDefined();
    });
  });

  describe('post()', () => {
    it('sends POST with JSON body', async () => {
      mockFetch.mockResolvedValue({ status: 201, ok: true, text: async () => '{"created":true}' });
      const client = createPlatformClient();
      const result = await client.post('/api/demo', { force: true });
      expect(mockFetch.mock.calls[0][1].method).toBe('POST');
      expect(mockFetch.mock.calls[0][1].body).toBe('{"force":true}');
      expect(result.status).toBe(201);
    });

    it('allows per-request API key override', async () => {
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => '{}' });
      const client = createPlatformClient({ apiKey: 'default' });
      await client.post('/test', {}, { apiKey: 'override' });
      expect(mockFetch.mock.calls[0][1].headers.authorization).toBe('Bearer override');
    });

    it('truncates large POST responses', async () => {
      const largeBody = 'y'.repeat(25000);
      mockFetch.mockResolvedValue({ status: 200, ok: true, text: async () => largeBody });
      const client = createPlatformClient();
      const result = await client.post('/large', {});
      expect(result.body).toContain('...[truncated]');
    });
  });
});
