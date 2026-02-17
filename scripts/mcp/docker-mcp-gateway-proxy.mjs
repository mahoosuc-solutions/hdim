import { spawn, spawnSync } from 'node:child_process';
import process from 'node:process';

const args = process.argv.slice(2);
const gatewayArgs =
  args.length > 0 ? args : ['mcp', 'gateway', 'run', '--transport', 'stdio', '--servers', 'hdim-platform'];

const probeOutput = (result) => `${result.stdout ?? ''}${result.stderr ?? ''}`.toLowerCase();

const probeCommand = (command) => {
  const result = spawnSync(command, ['mcp', 'version'], { encoding: 'utf8' });
  return {
    ok: result.status === 0,
    output: probeOutput(result),
  };
};

const resolveDockerCommand = () => {
  if (process.env.DOCKER_MCP_COMMAND) {
    return process.env.DOCKER_MCP_COMMAND;
  }

  const dockerProbe = probeCommand('docker');
  if (dockerProbe.ok) {
    return 'docker';
  }

  const socketFailure =
    dockerProbe.output.includes('/var/run/docker.sock') ||
    dockerProbe.output.includes('dial unix') ||
    dockerProbe.output.includes('permission denied');

  if (socketFailure) {
    const dockerExeProbe = probeCommand('docker.exe');
    if (dockerExeProbe.ok) {
      return 'docker.exe';
    }
  }

  return 'docker';
};

const dockerCommand = resolveDockerCommand();

const child = spawn(dockerCommand, gatewayArgs, {
  stdio: ['pipe', 'pipe', 'pipe'],
  env: {
    ...process.env,
    TERM: 'dumb',
    NO_COLOR: '1',
  },
});

process.stdin.pipe(child.stdin);
child.stderr.pipe(process.stderr);
process.stderr.write(`[docker-mcp-gateway-proxy] using command: ${dockerCommand}\n`);

let buffer = Buffer.alloc(0);

const contentLengthHeader = Buffer.from('Content-Length:');
const headerTerminator = Buffer.from('\r\n\r\n');

child.stdout.on('data', (chunk) => {
  buffer = Buffer.concat([buffer, chunk]);

  while (buffer.length > 0) {
    const startIndex = buffer.indexOf(contentLengthHeader);
    if (startIndex === -1) {
      // Keep a small tail in case the next chunk completes a header token.
      if (buffer.length > 256) {
        buffer = buffer.subarray(buffer.length - 256);
      }
      return;
    }

    if (startIndex > 0) {
      buffer = buffer.subarray(startIndex);
    }

    const headerEndIndex = buffer.indexOf(headerTerminator);
    if (headerEndIndex === -1) {
      return;
    }

    const headerBytes = buffer.subarray(0, headerEndIndex);
    const headerText = headerBytes.toString('utf8');
    const match = /Content-Length:\s*(\d+)/i.exec(headerText);

    if (!match) {
      // Malformed header block; drop this line and keep scanning.
      const nextLineIndex = buffer.indexOf('\n');
      if (nextLineIndex === -1) {
        buffer = Buffer.alloc(0);
        return;
      }
      buffer = buffer.subarray(nextLineIndex + 1);
      continue;
    }

    const contentLength = Number.parseInt(match[1], 10);
    if (!Number.isFinite(contentLength) || contentLength < 0) {
      buffer = buffer.subarray(headerEndIndex + headerTerminator.length);
      continue;
    }

    const frameLength = headerEndIndex + headerTerminator.length + contentLength;
    if (buffer.length < frameLength) {
      return;
    }

    const frame = buffer.subarray(0, frameLength);
    process.stdout.write(frame);
    buffer = buffer.subarray(frameLength);
  }
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 1);
});

child.on('error', (error) => {
  process.stderr.write(`[docker-mcp-gateway-proxy] spawn failed: ${error.message}\n`);
  process.exit(1);
});
