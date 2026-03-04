#!/usr/bin/env node
import readline from 'node:readline';
import { createAliasRegistry, mapRequestForEdge, mapResponseForVscode } from './bridge-helpers.mjs';

const endpoint = process.env.MCP_EDGE_URL || 'http://localhost:3300/mcp';
const apiKey = process.env.MCP_EDGE_API_KEY || '';
const operatorRole = process.env.MCP_EDGE_OPERATOR_ROLE || '';
const protocolVersion = process.env.MCP_PROTOCOL_VERSION || '2025-11-25';
const { registerAlias, resolveAlias } = createAliasRegistry();

function writeJson(obj) {
  process.stdout.write(`${JSON.stringify(obj)}\n`);
}

async function forwardRpc(message) {
  const headers = {
    'content-type': 'application/json',
    'MCP-Protocol-Version': protocolVersion
  };

  if (apiKey) {
    headers.authorization = `Bearer ${apiKey}`;
  }

  if (operatorRole) {
    headers['x-operator-role'] = operatorRole;
  }

  const response = await fetch(endpoint, {
    method: 'POST',
    headers,
    body: JSON.stringify(message)
  });

  if (response.status === 204) return null;

  const body = await response.json().catch(() => ({
    jsonrpc: '2.0',
    id: message?.id ?? null,
    error: {
      code: -32603,
      message: 'Invalid JSON response from MCP edge'
    }
  }));

  return body;
}

const rl = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
let processingQueue = Promise.resolve();

async function handleLine(line) {
  const raw = line.trim();
  if (!raw) return;

  // Strip null bytes (WSL/Windows may inject UTF-16 noise on stdout)
  const cleaned = raw.replace(/\0/g, '').trim();
  if (!cleaned || !cleaned.startsWith('{')) return;

  let message;
  try {
    message = JSON.parse(cleaned);
  } catch {
    // Silently drop non-JSON lines (WSL startup messages, etc.)
    return;
  }

  try {
    // VS Code may probe logging level support. MCP edge does not implement this.
    if (message?.method === 'logging/setLevel') {
      if (message?.id !== undefined) {
        writeJson({ jsonrpc: '2.0', id: message.id, result: {} });
      }
      return;
    }

    const mappedRequest = mapRequestForEdge(message, resolveAlias);
    const rpcResponse = await forwardRpc(mappedRequest);
    if (rpcResponse) writeJson(mapResponseForVscode(rpcResponse, registerAlias));
  } catch (error) {
    writeJson({
      jsonrpc: '2.0',
      id: message?.id ?? null,
      error: {
        code: -32603,
        message: 'Bridge transport error',
        data: {
          detail: error?.message || String(error),
          endpoint
        }
      }
    });
  }
}

rl.on('line', (line) => {
  processingQueue = processingQueue.then(() => handleLine(line)).catch((error) => {
    writeJson({
      jsonrpc: '2.0',
      id: null,
      error: {
        code: -32603,
        message: 'Bridge processing error',
        data: { detail: error?.message || String(error) }
      }
    });
  });
});
