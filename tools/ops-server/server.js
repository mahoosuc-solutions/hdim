const http = require('http');
const { exec } = require('child_process');
const { URL } = require('url');

const PORT = Number(process.env.PORT || 4710);
const WORKSPACE_DIR = process.env.WORKSPACE_DIR || '/workspace';
const COMPOSE_FILE =
  process.env.COMPOSE_FILE || `${WORKSPACE_DIR}/docker-compose.demo.yml`;
const DEMO_SEEDING_URL = process.env.DEMO_SEEDING_URL || 'http://demo-seeding-service:8098';

let lastCommand = null;

function json(res, status, payload) {
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  });
  res.end(JSON.stringify(payload));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.on('data', (chunk) => (data += chunk));
    req.on('end', () => {
      if (!data) return resolve({});
      try {
        resolve(JSON.parse(data));
      } catch (err) {
        reject(err);
      }
    });
  });
}

function run(cmd, options = {}) {
  return new Promise((resolve) => {
    const started = Date.now();
    exec(cmd, { ...options, maxBuffer: 10 * 1024 * 1024 }, (err, stdout, stderr) => {
      const durationMs = Date.now() - started;
      resolve({
        exitCode: err ? err.code || 1 : 0,
        durationMs,
        stdout: stdout || '',
        stderr: stderr || '',
      });
    });
  });
}

async function getComposeStatus() {
  const result = await run(`docker compose -f ${COMPOSE_FILE} ps --format json`);
  if (result.exitCode === 0) {
    try {
      const lines = result.stdout.split('\n').filter(Boolean);
      const services = lines.map((line) => JSON.parse(line)).map((svc) => ({
        name: svc.Name,
        state: svc.State,
        health: svc.Health || '',
        ports: svc.Ports || '',
      }));
      return { services };
    } catch (err) {
      return { services: [], error: 'Failed to parse compose status' };
    }
  }

  const fallback = await run(`docker compose -f ${COMPOSE_FILE} ps`);
  return {
    services: [],
    error: fallback.stderr || fallback.stdout || 'compose status unavailable',
  };
}

async function getSeedingTail() {
  const logs = await run('docker logs --tail 120 hdim-demo-seeding');
  if (logs.exitCode !== 0) {
    return [];
  }
  return logs.stdout.split('\n').filter(Boolean).slice(-60);
}

async function handleCommand(payload) {
  const action = payload.action;
  let cmd = '';

  if (action === 'start') {
    cmd = `docker compose -f ${COMPOSE_FILE} up -d`;
  } else if (action === 'stop') {
    cmd = `docker compose -f ${COMPOSE_FILE} down -v`;
  } else if (action === 'validate') {
    cmd = `bash ${WORKSPACE_DIR}/validate-system.sh`;
  } else if (action === 'capture-logs') {
    cmd = `bash ${WORKSPACE_DIR}/scripts/capture-compose-logs.sh`;
  } else if (action === 'seed') {
    const tenantId = payload.tenantId || 'acme-health';
    const count = Number(payload.patientCount || 200);
    cmd = [
      'curl -s -w "\\n%{http_code}"',
      '-X POST',
      `${DEMO_SEEDING_URL}/demo/api/v1/demo/patients/generate`,
      `-H "X-Tenant-ID: ${tenantId}"`,
      '-H "Content-Type: application/json"',
      `-d '{"count": ${count}}'`,
    ].join(' ');
  } else {
    return { error: `Unsupported action: ${action}` };
  }

  const result = await run(cmd, { cwd: WORKSPACE_DIR });
  lastCommand = {
    action,
    exitCode: result.exitCode,
    durationMs: result.durationMs,
    outputTail: (result.stdout + '\n' + result.stderr).split('\n').filter(Boolean).slice(-80),
  };
  return lastCommand;
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    return json(res, 204, {});
  }

  const url = new URL(req.url || '/', `http://${req.headers.host}`);

  if (req.method === 'GET' && url.pathname === '/ops/status') {
    const status = await getComposeStatus();
    const seedingTail = await getSeedingTail();
    return json(res, 200, {
      ...status,
      seedingTail,
      lastCommand,
      timestamp: new Date().toISOString(),
    });
  }

  if (req.method === 'POST' && url.pathname === '/ops/command') {
    try {
      const payload = await readBody(req);
      const commandResult = await handleCommand(payload);
      const status = await getComposeStatus();
      const seedingTail = await getSeedingTail();
      return json(res, 200, {
        ...status,
        seedingTail,
        lastCommand: commandResult,
        timestamp: new Date().toISOString(),
      });
    } catch (err) {
      return json(res, 500, { error: err.message || 'Command failed' });
    }
  }

  return json(res, 404, { error: 'Not found' });
});

server.listen(PORT, () => {
  console.log(`HDIM ops server running on port ${PORT}`);
});
