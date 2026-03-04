const SERVICE_NAME_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/;
const MAX_TAIL = 10000;

function createDefinition(dockerClient) {
  return {
    name: 'docker_logs',
    description: 'Tail Docker compose service logs',
    inputSchema: {
      type: 'object',
      properties: {
        service: { type: 'string', description: 'Service name (e.g. postgres, fhir-service)' },
        tail: { type: 'number', description: 'Number of lines to tail (default 100)' }
      },
      required: ['service'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { service, tail } = args;
      const trimmedService = String(service || '').trim();
      if (!trimmedService) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: 'service is required' }) }] };
      }
      if (!SERVICE_NAME_PATTERN.test(trimmedService)) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: 'Invalid service name: only alphanumeric, hyphens, dots, underscores allowed' }) }] };
      }
      const rawTail = tail !== undefined ? Number(tail) : 100;
      if (rawTail < 1 || !Number.isFinite(rawTail)) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: 'tail must be a positive number' }) }] };
      }
      const safeTail = Math.min(rawTail, MAX_TAIL);
      try {
        const result = await dockerClient.logs(trimmedService, safeTail);
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              ok: result.ok,
              service,
              logs: result.stdout,
              stderr: result.stderr || null
            }, null, 2)
          }]
        };
      } catch (err) {
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2)
          }]
        };
      }
    }
  };
}

module.exports = { createDefinition };
