// Mock child_process BEFORE any module loads it
jest.mock('node:child_process', () => {
  const actual = jest.requireActual('node:child_process');
  const { promisify } = require('node:util');
  const mockExecFile = jest.fn((cmd, args, opts, cb) => {
    if (typeof opts === 'function') { cb = opts; opts = {}; }
    process.nextTick(() => cb(null, '24.0.7\n', ''));
  });
  // Attach custom promisify so util.promisify returns { stdout, stderr }
  mockExecFile[promisify.custom] = (cmd, args, opts) => {
    return Promise.resolve({ stdout: '24.0.7\n', stderr: '' });
  };
  return {
    ...actual,
    execFile: mockExecFile
  };
});

const supertest = require('supertest');
const express = require('express');
const { createMcpRouter } = require('hdim-mcp-edge-common');

describe('devops edge_health — docker reachable (success path)', () => {
  it('reports healthy when docker is reachable', async () => {
    // Load edge-health AFTER the mock is in place
    const { definition } = require('../lib/tools/edge-health');

    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools: [definition],
      serverName: 'test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));

    const res = await supertest(app).post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    });

    const payload = JSON.parse(res.body.result.content[0].text);
    expect(payload.status).toBe('healthy');
    expect(payload.downstream.docker.reachable).toBe(true);
    expect(payload.downstream.docker.version).toBe('24.0.7');
  });
});
