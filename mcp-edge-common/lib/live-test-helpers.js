const fs = require('fs');
const { execFileSync } = require('child_process');

const TEST_CONTEXT_PATH = '/tmp/live-test-context.json';

const LIVE_TEST_DEFAULTS = {
  tenantId: 'demo',
  gatewayUrl: process.env.HDIM_BASE_URL || 'http://localhost:18080',
};

async function isGatewayReachable(url) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 2000);
  try {
    const res = await fetch(`${url}/actuator/health`, { signal: controller.signal });
    return res.ok;
  } catch {
    return false;
  } finally {
    clearTimeout(timeout);
  }
}

async function isDockerReachable() {
  try {
    execFileSync('docker', ['info'], { stdio: 'ignore', timeout: 5000 });
    return true;
  } catch {
    return false;
  }
}

function readTestContext() {
  try {
    return JSON.parse(fs.readFileSync(TEST_CONTEXT_PATH, 'utf8'));
  } catch {
    return null;
  }
}

function writeTestContext(ctx) {
  fs.writeFileSync(TEST_CONTEXT_PATH, JSON.stringify(ctx, null, 2));
}

function callTool(request, toolName, args = {}, role = 'platform_admin', id = 1) {
  return request.post('/mcp')
    .set('x-operator-role', role)
    .send({
      jsonrpc: '2.0',
      id,
      method: 'tools/call',
      params: { name: toolName, arguments: args }
    });
}

function skipUnlessLive(checkFn) {
  return async () => {
    const reachable = await checkFn();
    if (!reachable) {
      console.warn('Live dependency unreachable — skipping live tests');
    }
    return reachable;
  };
}

module.exports = {
  LIVE_TEST_DEFAULTS,
  isGatewayReachable,
  isDockerReachable,
  readTestContext,
  writeTestContext,
  callTool,
  skipUnlessLive,
  TEST_CONTEXT_PATH
};
