import { spawn } from 'node:child_process';
import process from 'node:process';

if (process.env.RUN_MCP_SMOKE !== '1') {
  process.exit(0);
}

const platform = process.platform;

let command;
let args;

if (platform === 'win32') {
  command = 'powershell.exe';
  args = ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', 'scripts/mcp/nx-mcp-smoke.ps1'];
} else {
  command = 'bash';
  args = ['scripts/mcp/nx-mcp-smoke.sh'];
}

const child = spawn(command, args, { stdio: 'inherit', env: process.env });
child.on('exit', (code) => process.exit(code ?? 1));

