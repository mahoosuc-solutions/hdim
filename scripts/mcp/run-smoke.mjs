import { spawn } from 'node:child_process';
import process from 'node:process';

if (process.env.RUN_MCP_SMOKE !== '1' && process.env.RUN_DOCKER_MCP_TOOLKIT_SMOKE !== '1') {
  process.exit(0);
}

const platform = process.platform;

const run = async (command, args) => {
  const child = spawn(command, args, { stdio: 'inherit', env: process.env });
  const exitCode = await new Promise((resolve) => child.on('exit', (code) => resolve(code ?? 1)));
  if (exitCode !== 0) process.exit(exitCode);
};

if (process.env.RUN_MCP_SMOKE === '1') {
  if (platform === 'win32') {
    await run('powershell.exe', [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      'scripts/mcp/nx-mcp-smoke.ps1',
    ]);
  } else {
    await run('bash', ['scripts/mcp/nx-mcp-smoke.sh']);
  }
}

if (process.env.RUN_DOCKER_MCP_TOOLKIT_SMOKE === '1') {
  if (platform === 'win32') {
    await run('powershell.exe', [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      'scripts/mcp/docker-toolkit-smoke.ps1',
    ]);
  } else {
    await run('bash', ['scripts/mcp/docker-toolkit-smoke.sh']);
  }
}
