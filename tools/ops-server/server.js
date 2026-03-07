const http = require('http');
const { exec } = require('child_process');
const { URL } = require('url');

const PORT = Number(process.env.PORT || 4710);
const WORKSPACE_DIR = process.env.WORKSPACE_DIR || '/workspace';
const COMPOSE_FILE =
  process.env.COMPOSE_FILE || `${WORKSPACE_DIR}/docker-compose.demo.yml`;
const DEMO_SEEDING_URL = process.env.DEMO_SEEDING_URL || 'http://demo-seeding-service:8098';
const STATUS_CACHE_TTL_MS = Number.isFinite(Number(process.env.STATUS_CACHE_TTL_MS))
  ? Math.max(500, Number(process.env.STATUS_CACHE_TTL_MS))
  : 5000;

let lastCommand = null;
let cachedStatusPayload = null;
let cachedStatusExpiresAt = 0;
let pendingStatusPromise = null;

function createDefaultSeedingProgress() {
  return {
    phase: 'idle',
    percent: 0,
    counts: {},
    updatedAt: new Date().toISOString(),
  };
}

function parseSeedingProgress(seedingTail) {
  const progress = createDefaultSeedingProgress();

  for (const rawLine of seedingTail) {
    const line = String(rawLine || '').trim();
    if (!line) {
      continue;
    }

    const patientsCreatedMatch = line.match(/Created\s+(\d+)\s+patients/i);
    if (patientsCreatedMatch) {
      progress.counts.patientsCreated = Number(patientsCreatedMatch[1]);
    }

    const patientsLoadedMatch = line.match(/Loaded\s+(\d+)\s+patients/i);
    if (patientsLoadedMatch) {
      progress.counts.patientsLoaded = Number(patientsLoadedMatch[1]);
    }

    if (/Checking demo-seeding-service availability/i.test(line)) {
      progress.phase = 'waiting-service';
      progress.percent = Math.max(progress.percent, 10);
    }

    if (/Loading SMOKE seed|Loading HEDIS|Seeding:/i.test(line)) {
      progress.phase = 'seeding';
      progress.percent = Math.max(progress.percent, 50);
    }

    if (/Syncing CQL libraries/i.test(line)) {
      progress.phase = 'syncing-cql';
      progress.percent = Math.max(progress.percent, 80);
    }

    if (/Data seeding completed|Non-interactive mode/i.test(line)) {
      progress.phase = 'completed';
      progress.percent = 100;
      delete progress.lastError;
    }

    if (/✗|Error:|is not available/i.test(line)) {
      progress.phase = 'failed';
      progress.percent = Math.min(progress.percent, 95);
      progress.lastError = line;
    }
  }

  progress.updatedAt = new Date().toISOString();
  return progress;
}

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

async function buildStatusPayload(commandOverride = null) {
  const status = await getComposeStatus();
  const seedingTail = await getSeedingTail();
  const seedingProgress = parseSeedingProgress(seedingTail);
  return {
    ...status,
    seedingTail,
    seedingProgress,
    lastCommand: commandOverride || lastCommand,
    timestamp: new Date().toISOString(),
  };
}

async function getStatusPayload({ forceRefresh = false, commandOverride = null } = {}) {
  const now = Date.now();
  if (!forceRefresh && cachedStatusPayload && now < cachedStatusExpiresAt) {
    return cachedStatusPayload;
  }

  if (!forceRefresh && pendingStatusPromise) {
    return pendingStatusPromise;
  }

  pendingStatusPromise = buildStatusPayload(commandOverride)
    .then((payload) => {
      cachedStatusPayload = payload;
      cachedStatusExpiresAt = Date.now() + STATUS_CACHE_TTL_MS;
      return payload;
    })
    .finally(() => {
      pendingStatusPromise = null;
    });

  return pendingStatusPromise;
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
    const profile = payload.profile === 'full' ? 'full' : 'smoke';
    const scheduleMode = normalizeScheduleMode(payload.scheduleMode || 'none');
    const commands = [];

    if (profile === 'full') {
      commands.push(`SEED_PROFILE=full bash ${WORKSPACE_DIR}/scripts/seed-all-demo-data.sh`);
    } else {
      commands.push(`bash ${WORKSPACE_DIR}/scripts/seed-all-demo-data.sh`);
    }

    if (scheduleMode !== 'none') {
      commands.push(`SEED_SCHEDULE_MODE=${scheduleMode} bash ${WORKSPACE_DIR}/scripts/seed-fhir-schedule.sh`);
    }

    if (!payload.profile && payload.patientCount) {
      const tenantId = payload.tenantId || 'acme-health';
      const count = Number(payload.patientCount || 200);
      commands.push([
        'curl -s -w "\\n%{http_code}"',
        '-X POST',
        `${DEMO_SEEDING_URL}/demo/api/v1/demo/patients/generate`,
        `-H "X-Tenant-ID: ${tenantId}"`,
        '-H "Content-Type: application/json"',
        `-d \'{"count": ${count}}\'`,
      ].join(' '));
    }

    cmd = commands.join(' && ');
  } else if (action === 'seed-schedule') {
    const scheduleMode = normalizeScheduleMode(payload.scheduleMode || 'both');
    if (scheduleMode === 'none') {
      return { error: 'scheduleMode must be one of appointment-task, encounter, both' };
    }
    cmd = `SEED_SCHEDULE_MODE=${scheduleMode} bash ${WORKSPACE_DIR}/scripts/seed-fhir-schedule.sh`;
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

function normalizeScheduleMode(mode) {
  if (!mode) return 'none';
  if (mode === 'appointment-task' || mode === 'encounter' || mode === 'both' || mode === 'none') {
    return mode;
  }
  return 'none';
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    return json(res, 204, {});
  }

  const url = new URL(req.url || '/', `http://${req.headers.host}`);

  if (req.method === 'GET' && url.pathname === '/ops/status') {
    const payload = await getStatusPayload();
    return json(res, 200, payload);
  }

  if (req.method === 'POST' && url.pathname === '/ops/command') {
    try {
      const payload = await readBody(req);
      const commandResult = await handleCommand(payload);
      const statusPayload = await getStatusPayload({
        forceRefresh: true,
        commandOverride: commandResult,
      });
      return json(res, 200, statusPayload);
    } catch (err) {
      return json(res, 500, { error: err.message || 'Command failed' });
    }
  }

  return json(res, 404, { error: 'Not found' });
});

server.listen(PORT, () => {
  console.log(`HDIM ops server running on port ${PORT}`);
});
