const { createClinicalClient } = require('../lib/clinical-client');

describe('createClinicalClient', () => {
  let originalFetch;

  beforeEach(() => { originalFetch = global.fetch; });
  afterEach(() => {
    global.fetch = originalFetch;
    delete process.env.HDIM_BASE_URL;
    delete process.env.MCP_EDGE_API_KEY;
  });

  it('uses HDIM_BASE_URL env var', () => {
    process.env.HDIM_BASE_URL = 'http://gateway:18080';
    const client = createClinicalClient();
    expect(client.baseUrl).toBe('http://gateway:18080');
  });

  it('defaults to http://localhost:18080', () => {
    const client = createClinicalClient();
    expect(client.baseUrl).toBe('http://localhost:18080');
  });

  it('strips trailing slash from baseUrl', () => {
    const client = createClinicalClient({ baseUrl: 'http://gw:18080/' });
    expect(client.baseUrl).toBe('http://gw:18080');
  });

  describe('get()', () => {
    it('sends GET with Authorization and X-Tenant-ID headers', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 200, ok: true,
        text: () => Promise.resolve('{"id":"123"}')
      });
      const client = createClinicalClient({ baseUrl: 'http://gw:18080', apiKey: 'key1' });
      const res = await client.get('/patient/health-record?patient=abc', { tenantId: 'acme' });
      expect(global.fetch).toHaveBeenCalledWith(
        'http://gw:18080/patient/health-record?patient=abc',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            authorization: 'Bearer key1',
            'x-tenant-id': 'acme'
          })
        })
      );
      expect(res).toEqual({ status: 200, ok: true, body: '{"id":"123"}', url: 'http://gw:18080/patient/health-record?patient=abc' });
    });

    it('truncates response body at 20KB', async () => {
      const bigBody = 'x'.repeat(25_000);
      global.fetch = jest.fn().mockResolvedValue({
        status: 200, ok: true, text: () => Promise.resolve(bigBody)
      });
      const client = createClinicalClient({ baseUrl: 'http://gw:18080' });
      const res = await client.get('/path', { tenantId: 't1' });
      expect(res.body.length).toBeLessThan(21_000);
      expect(res.body).toContain('...[truncated]');
    });

    it('omits X-Tenant-ID when tenantId not provided', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 200, ok: true, text: () => Promise.resolve('{}')
      });
      const client = createClinicalClient({ baseUrl: 'http://gw:18080' });
      await client.get('/path');
      const headers = global.fetch.mock.calls[0][1].headers;
      expect(headers['x-tenant-id']).toBeUndefined();
    });
  });

  describe('post()', () => {
    it('truncates response body at 20KB', async () => {
      const bigBody = 'y'.repeat(25_000);
      global.fetch = jest.fn().mockResolvedValue({
        status: 201, ok: true, text: () => Promise.resolve(bigBody)
      });
      const client = createClinicalClient({ baseUrl: 'http://gw:18080' });
      const res = await client.post('/some/path', { data: 1 }, { tenantId: 't1' });
      expect(res.body.length).toBeLessThan(21_000);
      expect(res.body).toContain('...[truncated]');
    });

    it('sends POST with body, Authorization, and X-Tenant-ID', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        status: 201, ok: true, text: () => Promise.resolve('{"created":true}')
      });
      const client = createClinicalClient({ baseUrl: 'http://gw:18080', apiKey: 'key2' });
      const res = await client.post('/care-gap/close', { gapId: 'g1' }, { tenantId: 'acme' });
      expect(global.fetch).toHaveBeenCalledWith(
        'http://gw:18080/care-gap/close',
        expect.objectContaining({
          method: 'POST',
          body: '{"gapId":"g1"}',
          headers: expect.objectContaining({
            authorization: 'Bearer key2',
            'x-tenant-id': 'acme',
            'content-type': 'application/json'
          })
        })
      );
      expect(res).toEqual({ status: 201, ok: true, body: '{"created":true}', url: 'http://gw:18080/care-gap/close' });
    });
  });
});
