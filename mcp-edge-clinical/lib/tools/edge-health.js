const startTime = Date.now();

const definition = {
  name: 'edge_health',
  description: 'Local MCP edge health/status with downstream FHIR server probe',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
  handler: async () => {
    const baseUrl = process.env.HDIM_BASE_URL || 'http://localhost:18080';
    const fhirUrl = process.env.HDIM_FHIR_URL || `${baseUrl}/fhir/metadata`;
    let fhirReachable = false;

    try {
      const res = await fetch(fhirUrl, {
        signal: AbortSignal.timeout(3000)
      });
      fhirReachable = res.ok;
    } catch {
      // FHIR server unreachable
    }

    const status = fhirReachable ? 'healthy' : 'degraded';
    const payload = {
      status,
      service: 'hdim-clinical-edge',
      version: '0.1.0',
      uptime: (Date.now() - startTime) / 1000,
      timestamp: new Date().toISOString(),
      demoMode: process.env.HDIM_DEMO_MODE === 'true',
      downstream: {
        fhir: { url: fhirUrl, reachable: fhirReachable }
      }
    };
    return { content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }] };
  }
};

module.exports = { definition };
