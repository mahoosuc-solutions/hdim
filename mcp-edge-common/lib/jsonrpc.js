function jsonRpcResult(id, result) {
  return { jsonrpc: '2.0', id, result };
}

function jsonRpcError(id, code, message, data) {
  const error = { code, message };
  if (data !== undefined) error.data = data;
  return { jsonrpc: '2.0', id, error };
}

function parseJsonRpcRequest(raw) {
  if (!raw || typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  if (!trimmed.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(trimmed);
    if (parsed.jsonrpc !== '2.0' || !parsed.method) return null;
    return parsed;
  } catch {
    return null;
  }
}

module.exports = { jsonRpcResult, jsonRpcError, parseJsonRpcRequest };
