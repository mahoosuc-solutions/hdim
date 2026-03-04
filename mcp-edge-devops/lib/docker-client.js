const { spawn } = require('node:child_process');

const DEFAULT_COMPOSE_FILE = 'docker-compose.demo.yml';
const DEFAULT_DOCKER_BIN = 'docker';

function composeArgs(composeFile, subcommandArgs) {
  return ['compose', '-f', composeFile, ...subcommandArgs];
}

function parseComposePsJson(output) {
  const text = String(output).trim();
  if (!text) return [];

  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  const entries = [];
  for (const line of text.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed.startsWith('{')) continue;
    try {
      entries.push(JSON.parse(trimmed));
    } catch {
      continue;
    }
  }
  return entries;
}

async function runCommand(command, args, options = {}) {
  const timeoutMs = options.timeoutMs ?? 120_000;
  const cwd = options.cwd ?? process.cwd();

  return new Promise((resolve) => {
    let stdout = '';
    let stderr = '';
    let timedOut = false;

    const child = spawn(command, args, {
      cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    const timer = setTimeout(() => {
      timedOut = true;
      child.kill('SIGTERM');
    }, timeoutMs);

    child.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
    child.stderr.on('data', (chunk) => { stderr += chunk.toString(); });

    child.on('error', (error) => {
      clearTimeout(timer);
      resolve({ ok: false, exitCode: null, timedOut, stdout, stderr: `${stderr}${error.message}` });
    });

    child.on('close', (code) => {
      clearTimeout(timer);
      resolve({ ok: !timedOut && code === 0, exitCode: code, timedOut, stdout, stderr });
    });
  });
}

function createDockerClient({ composeFile, dockerBin, cwd } = {}) {
  const file = composeFile || process.env.HDIM_COMPOSE_FILE || DEFAULT_COMPOSE_FILE;
  const bin = dockerBin || process.env.DOCKER_BIN || DEFAULT_DOCKER_BIN;
  const workDir = cwd || process.cwd();

  async function ps() {
    const args = composeArgs(file, ['ps', '--format', 'json']);
    const result = await runCommand(bin, args, { cwd: workDir });
    return {
      ...result,
      services: result.ok ? parseComposePsJson(result.stdout) : [],
    };
  }

  async function logs(service, tail = 100) {
    const args = composeArgs(file, ['logs', service, '--tail', String(tail), '--no-color']);
    return runCommand(bin, args, { cwd: workDir });
  }

  async function restart(service) {
    const args = composeArgs(file, ['restart', service]);
    return runCommand(bin, args, { cwd: workDir, timeoutMs: 60_000 });
  }

  async function config() {
    const args = composeArgs(file, ['config']);
    return runCommand(bin, args, { cwd: workDir });
  }

  return { composeFile: file, ps, logs, restart, config };
}

module.exports = { createDockerClient, parseComposePsJson, composeArgs, runCommand };
