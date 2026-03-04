function createDefinition(dockerClient) {
  return {
    name: 'compose_config',
    description: 'Validate Docker compose configuration and environment variables',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const result = await dockerClient.config();
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              ok: result.ok,
              composeFile: dockerClient.composeFile,
              config: result.ok ? result.stdout : null,
              stderr: result.stderr || null
            }, null, 2)
          }]
        };
      } catch (err) {
        return {
          content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }]
        };
      }
    }
  };
}

module.exports = { createDefinition };
