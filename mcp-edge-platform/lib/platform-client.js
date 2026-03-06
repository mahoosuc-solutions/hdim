const { propagationHeaders } = require('hdim-mcp-edge-common');

const DEFAULT_BASE_URL = 'http://localhost:18080';
const DEFAULT_TIMEOUT = 15_000;

function createPlatformClient({ baseUrl, apiKey, timeout } = {}) {
  const normalizedBase = (baseUrl || process.env.HDIM_BASE_URL || DEFAULT_BASE_URL)
    .trim().replace(/\/$/, '');
  const defaultApiKey = apiKey || process.env.MCP_EDGE_API_KEY || '';
  const requestTimeout = timeout || DEFAULT_TIMEOUT;

  function buildHeaders(overrideApiKey, traceContext) {
    const headers = {
      accept: 'application/json',
      'content-type': 'application/json',
      connection: 'keep-alive',
      ...propagationHeaders(traceContext)
    };
    const key = overrideApiKey || defaultApiKey;
    if (key) headers.authorization = `Bearer ${key}`;
    return headers;
  }

  async function get(path, { apiKey: overrideKey, traceContext } = {}) {
    const url = `${normalizedBase}${path}`;
    const response = await fetch(url, {
      method: 'GET',
      headers: buildHeaders(overrideKey, traceContext),
      signal: AbortSignal.timeout(requestTimeout)
    });
    const text = await response.text();
    const truncated = text.length > 20_000 ? `${text.slice(0, 20_000)}\n...[truncated]` : text;
    return { status: response.status, ok: response.ok, body: truncated, url };
  }

  async function post(path, body, { apiKey: overrideKey, traceContext } = {}) {
    const url = `${normalizedBase}${path}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: buildHeaders(overrideKey, traceContext),
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(requestTimeout)
    });
    const text = await response.text();
    const truncated = text.length > 20_000 ? `${text.slice(0, 20_000)}\n...[truncated]` : text;
    return { status: response.status, ok: response.ok, body: truncated, url };
  }

  return { baseUrl: normalizedBase, get, post };
}

module.exports = { createPlatformClient };
