jest.mock('node:child_process', () => ({
  spawn: jest.fn()
}));

const { spawn } = require('node:child_process');
const { createDockerClient, runCommand, parseComposePsJson, composeArgs } = require('../lib/docker-client');
const EventEmitter = require('node:events');

function createMockProcess(exitCode = 0, stdout = '', stderr = '') {
  const proc = new EventEmitter();
  proc.stdout = new EventEmitter();
  proc.stderr = new EventEmitter();
  proc.kill = jest.fn();
  setImmediate(() => {
    if (stdout) proc.stdout.emit('data', Buffer.from(stdout));
    if (stderr) proc.stderr.emit('data', Buffer.from(stderr));
    proc.emit('close', exitCode);
  });
  return proc;
}

describe('docker-client', () => {
  describe('composeArgs', () => {
    it('builds compose args with file', () => {
      const args = composeArgs('docker-compose.demo.yml', ['ps', '--format', 'json']);
      expect(args).toEqual(['compose', '-f', 'docker-compose.demo.yml', 'ps', '--format', 'json']);
    });
  });

  describe('parseComposePsJson', () => {
    it('parses JSON array output', () => {
      const output = '[{"Service":"postgres","State":"running","Status":"Up 2 hours"}]';
      const result = parseComposePsJson(output);
      expect(result).toHaveLength(1);
      expect(result[0].Service).toBe('postgres');
    });

    it('parses NDJSON output', () => {
      const output = '{"Service":"postgres","State":"running"}\n{"Service":"redis","State":"running"}\n';
      const result = parseComposePsJson(output);
      expect(result).toHaveLength(2);
    });

    it('returns empty array for empty output', () => {
      expect(parseComposePsJson('')).toEqual([]);
    });

    it('skips malformed lines', () => {
      const output = 'not json\n{"Service":"postgres"}\n';
      const result = parseComposePsJson(output);
      expect(result).toHaveLength(1);
    });

    it('returns empty array for malformed JSON array', () => {
      expect(parseComposePsJson('[not valid json')).toEqual([]);
    });

    it('skips malformed NDJSON lines starting with {', () => {
      const result = parseComposePsJson('{not json}\n{"Name":"svc1"}');
      expect(result).toHaveLength(1);
      expect(result[0].Name).toBe('svc1');
    });
  });

  describe('createDockerClient', () => {
    it('creates a client with default compose file', () => {
      const client = createDockerClient();
      expect(client.composeFile).toBe('docker-compose.demo.yml');
    });

    it('creates a client with custom compose file', () => {
      const client = createDockerClient({ composeFile: 'docker-compose.yml' });
      expect(client.composeFile).toBe('docker-compose.yml');
    });

    it('exposes ps, logs, restart, config methods', () => {
      const client = createDockerClient();
      expect(typeof client.ps).toBe('function');
      expect(typeof client.logs).toBe('function');
      expect(typeof client.restart).toBe('function');
      expect(typeof client.config).toBe('function');
    });
  });
});

describe('runCommand', () => {
  beforeEach(() => spawn.mockReset());

  it('resolves with ok:true on exit code 0', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'output'));
    const result = await runCommand('docker', ['ps']);
    expect(result.ok).toBe(true);
    expect(result.stdout).toBe('output');
    expect(result.exitCode).toBe(0);
    expect(result.timedOut).toBe(false);
  });

  it('resolves with ok:false on non-zero exit', async () => {
    spawn.mockReturnValue(createMockProcess(1, '', 'error'));
    const result = await runCommand('docker', ['ps']);
    expect(result.ok).toBe(false);
    expect(result.exitCode).toBe(1);
    expect(result.stderr).toBe('error');
  });

  it('resolves with ok:false and timedOut:true on timeout', async () => {
    const proc = new EventEmitter();
    proc.stdout = new EventEmitter();
    proc.stderr = new EventEmitter();
    proc.kill = jest.fn(() => { setImmediate(() => proc.emit('close', null)); });
    spawn.mockReturnValue(proc);
    const result = await runCommand('sleep', ['100'], { timeoutMs: 50 });
    expect(result.ok).toBe(false);
    expect(result.timedOut).toBe(true);
    expect(proc.kill).toHaveBeenCalledWith('SIGTERM');
  });

  it('resolves with ok:false on spawn error', async () => {
    const proc = new EventEmitter();
    proc.stdout = new EventEmitter();
    proc.stderr = new EventEmitter();
    proc.kill = jest.fn();
    spawn.mockReturnValue(proc);
    setImmediate(() => proc.emit('error', new Error('ENOENT')));
    const result = await runCommand('nonexistent', []);
    expect(result.ok).toBe(false);
    expect(result.stderr).toContain('ENOENT');
  });
});

describe('dockerClient methods', () => {
  beforeEach(() => spawn.mockReset());

  it('ps() calls docker compose ps --format json', async () => {
    spawn.mockReturnValue(createMockProcess(0, '[{"Service":"pg","State":"running"}]'));
    const client = createDockerClient({ composeFile: 'dc.yml', dockerBin: 'docker' });
    const result = await client.ps();
    expect(result.ok).toBe(true);
    expect(result.services).toHaveLength(1);
    expect(result.services[0].Service).toBe('pg');
    expect(spawn).toHaveBeenCalledWith('docker',
      ['compose', '-f', 'dc.yml', 'ps', '--format', 'json'],
      expect.any(Object)
    );
  });

  it('ps() returns empty services on failure', async () => {
    spawn.mockReturnValue(createMockProcess(1, '', 'error'));
    const client = createDockerClient();
    const result = await client.ps();
    expect(result.ok).toBe(false);
    expect(result.services).toEqual([]);
  });

  it('logs() passes service and tail args', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'log output'));
    const client = createDockerClient({ composeFile: 'dc.yml', dockerBin: 'docker' });
    await client.logs('postgres', 50);
    const args = spawn.mock.calls[0][1];
    expect(args).toContain('logs');
    expect(args).toContain('postgres');
    expect(args).toContain('50');
  });

  it('restart() passes service name', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'restarted'));
    const client = createDockerClient({ composeFile: 'dc.yml', dockerBin: 'docker' });
    await client.restart('redis');
    const args = spawn.mock.calls[0][1];
    expect(args).toContain('restart');
    expect(args).toContain('redis');
  });

  it('config() calls docker compose config', async () => {
    spawn.mockReturnValue(createMockProcess(0, 'yaml config'));
    const client = createDockerClient({ composeFile: 'dc.yml', dockerBin: 'docker' });
    const result = await client.config();
    expect(result.ok).toBe(true);
    expect(result.stdout).toBe('yaml config');
    const args = spawn.mock.calls[0][1];
    expect(args).toContain('config');
  });
});
