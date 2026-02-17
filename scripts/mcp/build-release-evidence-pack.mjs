import process from 'node:process';

import { createServer } from './hdim-docker-mcp.mjs';

function parseArgs(argv) {
  const args = [...argv];
  const parsed = {
    reportDir: 'logs/mcp-reports',
    outputDir: 'logs/mcp-reports',
    includeDiff: true,
  };

  while (args.length > 0) {
    const token = args.shift();
    switch (token) {
      case '--report-dir':
        parsed.reportDir = args.shift() ?? parsed.reportDir;
        break;
      case '--output-dir':
        parsed.outputDir = args.shift() ?? parsed.outputDir;
        break;
      case '--no-diff':
        parsed.includeDiff = false;
        break;
      default:
        break;
    }
  }

  return parsed;
}

async function run() {
  const options = parseArgs(process.argv.slice(2));
  const server = createServer();
  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_release_evidence_pack'];
  if (!tool) throw new Error('hdim_release_evidence_pack tool is not available');

  const response = await tool.handler({
    reportDir: options.reportDir,
    outputDir: options.outputDir,
    includeDiff: options.includeDiff,
  });
  const payload = JSON.parse(response.content[0].text);
  process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);

  if (payload.error) process.exit(1);
}

if (import.meta.url === `file://${process.argv[1]}`) {
  run().catch((error) => {
    // eslint-disable-next-line no-console
    console.error(error);
    process.exit(1);
  });
}

export { parseArgs };
