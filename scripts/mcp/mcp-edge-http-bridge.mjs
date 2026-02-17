import process from 'node:process';

const endpoint = process.env.MCP_EDGE_URL || 'http://localhost:3100/mcp';
const apiKey = process.env.MCP_EDGE_API_KEY || '';
const protocolVersion = process.env.MCP_PROTOCOL_VERSION || '2025-11-25';

const headerTerminatorCrlf = Buffer.from('\r\n\r\n');
const headerTerminatorLf = Buffer.from('\n\n');
let buffer = Buffer.alloc(0);

function writeFrame(payload) {
  const body = JSON.stringify(payload);
  const head = `Content-Length: ${Buffer.byteLength(body, 'utf8')}\r\n\r\n`;
  process.stdout.write(head + body);
}

function parseHeaders(headerBytes) {
  const text = headerBytes.toString('utf8');
  const lines = text.split(/\r?\n/);
  const headers = {};
  for (const line of lines) {
    const idx = line.indexOf(':');
    if (idx === -1) continue;
    const key = line.slice(0, idx).trim().toLowerCase();
    const value = line.slice(idx + 1).trim();
    headers[key] = value;
  }
  return headers;
}

function findHeaderTerminatorIndex(bytes) {
  const crlfIndex = bytes.indexOf(headerTerminatorCrlf);
  const lfIndex = bytes.indexOf(headerTerminatorLf);

  if (crlfIndex === -1 && lfIndex === -1) return { index: -1, length: 0 };
  if (crlfIndex !== -1 && (lfIndex === -1 || crlfIndex <= lfIndex)) {
    return { index: crlfIndex, length: headerTerminatorCrlf.length };
  }
  return { index: lfIndex, length: headerTerminatorLf.length };
}

async function forwardMessage(message) {
  const headers = {
    'content-type': 'application/json',
    accept: 'application/json',
    'mcp-protocol-version': protocolVersion,
  };
  if (apiKey) {
    headers.authorization = `Bearer ${apiKey}`;
    headers['x-api-key'] = apiKey;
  }

  const response = await fetch(endpoint, {
    method: 'POST',
    headers,
    body: JSON.stringify(message),
  });

  if (response.status === 204) return;
  const text = await response.text();
  if (!text) return;
  const payload = JSON.parse(text);

  if (Array.isArray(payload)) {
    for (const item of payload) writeFrame(item);
    return;
  }
  writeFrame(payload);
}

function writeRpcError(id, message, data) {
  writeFrame({
    jsonrpc: '2.0',
    id: id ?? null,
    error: {
      code: -32603,
      message,
      data,
    },
  });
}

async function processChunk(chunk) {
  buffer = Buffer.concat([buffer, chunk]);
  while (true) {
    const { index: headerEnd, length: terminatorLength } = findHeaderTerminatorIndex(buffer);
    if (headerEnd === -1) return;

    const headerBytes = buffer.subarray(0, headerEnd);
    const headers = parseHeaders(headerBytes);
    const contentLength = Number.parseInt(headers['content-length'] || '', 10);
    if (!Number.isFinite(contentLength) || contentLength < 0) {
      buffer = buffer.subarray(headerEnd + headerTerminator.length);
      continue;
    }

    const frameEnd = headerEnd + terminatorLength + contentLength;
    if (buffer.length < frameEnd) return;

    const bodyBytes = buffer.subarray(headerEnd + terminatorLength, frameEnd);
    buffer = buffer.subarray(frameEnd);

    let message;
    try {
      message = JSON.parse(bodyBytes.toString('utf8'));
    } catch (error) {
      writeRpcError(null, 'Invalid JSON payload', { detail: error?.message || String(error) });
      continue;
    }

    try {
      await forwardMessage(message);
    } catch (error) {
      const id = message && typeof message === 'object' && !Array.isArray(message) ? message.id ?? null : null;
      writeRpcError(id, 'Failed to reach MCP edge HTTP endpoint', {
        endpoint,
        detail: error?.message || String(error),
      });
    }
  }
}

process.stdin.on('data', (chunk) => {
  processChunk(chunk).catch((error) => {
    writeRpcError(null, 'Bridge processing error', { detail: error?.message || String(error) });
  });
});

process.stdin.on('error', (error) => {
  process.stderr.write(`[mcp-edge-http-bridge] stdin error: ${error?.message || String(error)}\n`);
});
