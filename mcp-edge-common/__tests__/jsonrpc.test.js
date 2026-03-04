const { jsonRpcResult, jsonRpcError, parseJsonRpcRequest } = require('../lib/jsonrpc');

describe('jsonrpc', () => {
  describe('jsonRpcResult', () => {
    it('wraps a result with jsonrpc 2.0 envelope', () => {
      const result = jsonRpcResult(42, { tools: [] });
      expect(result).toEqual({ jsonrpc: '2.0', id: 42, result: { tools: [] } });
    });

    it('handles null id', () => {
      const result = jsonRpcResult(null, 'ok');
      expect(result).toEqual({ jsonrpc: '2.0', id: null, result: 'ok' });
    });
  });

  describe('jsonRpcError', () => {
    it('wraps an error with code and message', () => {
      const err = jsonRpcError(1, -32601, 'Method not found');
      expect(err).toEqual({
        jsonrpc: '2.0',
        id: 1,
        error: { code: -32601, message: 'Method not found' }
      });
    });

    it('includes optional data field', () => {
      const err = jsonRpcError(1, -32603, 'Internal error', { detail: 'boom' });
      expect(err.error.data).toEqual({ detail: 'boom' });
    });
  });

  describe('parseJsonRpcRequest', () => {
    it('parses valid JSON-RPC request', () => {
      const raw = '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}';
      const parsed = parseJsonRpcRequest(raw);
      expect(parsed.method).toBe('tools/list');
      expect(parsed.id).toBe(1);
    });

    it('returns null for invalid JSON', () => {
      expect(parseJsonRpcRequest('not json')).toBeNull();
    });

    it('returns null for empty string', () => {
      expect(parseJsonRpcRequest('')).toBeNull();
    });

    it('returns null for invalid JSON that starts with {', () => {
      expect(parseJsonRpcRequest('{not valid json}')).toBeNull();
    });

    it('returns null for non-2.0 jsonrpc version', () => {
      expect(parseJsonRpcRequest('{"jsonrpc":"1.0","method":"test"}')).toBeNull();
    });

    it('returns null for missing method', () => {
      expect(parseJsonRpcRequest('{"jsonrpc":"2.0","id":1}')).toBeNull();
    });
  });
});
