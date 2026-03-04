const { createCorsOptions } = require('../lib/cors-config');

describe('createCorsOptions', () => {
  afterEach(() => { delete process.env.MCP_EDGE_CORS_ORIGINS; });

  it('defaults to localhost origins when no env set', () => {
    const opts = createCorsOptions();
    expect(opts.origin).toContain('http://localhost:3000');
    expect(opts.origin).toContain('http://localhost:3100');
    expect(opts.origin).toContain('http://localhost:3200');
    expect(opts.credentials).toBe(false);
  });

  it('parses comma-separated env origins', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = 'https://app.hdim.io,https://admin.hdim.io';
    const opts = createCorsOptions();
    expect(opts.origin).toEqual(['https://app.hdim.io', 'https://admin.hdim.io']);
  });

  it('allows wildcard via env for dev mode', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = '*';
    const opts = createCorsOptions();
    expect(opts.origin).toBe('*');
  });

  it('trims whitespace from origins', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = ' https://a.com , https://b.com ';
    const opts = createCorsOptions();
    expect(opts.origin).toEqual(['https://a.com', 'https://b.com']);
  });

  it('filters empty strings from origins', () => {
    process.env.MCP_EDGE_CORS_ORIGINS = 'https://a.com,,https://b.com,';
    const opts = createCorsOptions();
    expect(opts.origin).toEqual(['https://a.com', 'https://b.com']);
  });
});
