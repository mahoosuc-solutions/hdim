import { spawn } from 'node:child_process';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(dirname, '..', '..');

process.chdir(repoRoot);

const defaultArgs = ['--transport', 'stdio', '--minimal=false', '--disableTelemetry'];
const args = process.argv.slice(2);
const finalArgs = args.length > 0 ? args : defaultArgs;

const localBin = path.join(repoRoot, 'node_modules', '.bin', process.platform === 'win32' ? 'nx-mcp.cmd' : 'nx-mcp');

const spawnCommand = process.platform === 'win32' ? 'cmd.exe' : localBin;
const spawnArgs = process.platform === 'win32' ? ['/d', '/s', '/c', localBin, ...finalArgs] : finalArgs;

const child = spawn(spawnCommand, spawnArgs, {
  stdio: 'inherit',
  env: process.env,
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 1);
});
