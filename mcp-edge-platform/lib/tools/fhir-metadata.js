function createDefinition(platformClient) {
  return {
    name: 'fhir_metadata',
    description: 'FHIR R4 capability statement from the HDIM FHIR service',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const res = await platformClient.get('/fhir/metadata');
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, metadata: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}
module.exports = { createDefinition };
