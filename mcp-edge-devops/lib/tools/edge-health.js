const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const execFileAsync = promisify(execFile);

const startTime = Date.now();

const definition = {
  name: 'edge_health',
  description: 'Local MCP edge health/status for devops sidecar with Docker probe',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    let dockerReachable = false;
    let dockerVersion = null;

    try {
      const { stdout } = await execFileAsync('docker', ['info', '--format', '{{.ServerVersion}}'], { timeout: 3000 });
      dockerReachable = true;
      dockerVersion = stdout.trim();
    } catch {
      // docker unreachable
    }

    const status = dockerReachable ? 'healthy' : 'degraded';
    const payload = {
      status,
      service: 'hdim-devops-edge',
      version: '0.1.0',
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString(),
      downstream: {
        docker: { reachable: dockerReachable, version: dockerVersion }
      }
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
