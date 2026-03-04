const { createAuditLogger } = require('hdim-mcp-edge-common');

function createPhiAuditLogger({ serviceName, stream } = {}) {
  const name = serviceName || 'mcp-edge-clinical';
  const pino = createAuditLogger({ serviceName: name, stream });

  function logToolAccess({ tool, role, tenantId, patientId, success, durationMs, phi, write }) {
    const action = !phi ? 'TOOL_CALL' : write ? 'PHI_WRITE' : 'PHI_ACCESS';
    const record = { action, source: name, tool, role, tenantId, success, durationMs };
    if (phi && patientId) record.patientId = patientId;
    pino.info(record);
  }

  function logAuthDenied({ tool, role }) {
    pino.warn({ action: 'AUTH_DENIED', source: name, tool, role });
  }

  return { logToolAccess, logAuthDenied };
}

module.exports = { createPhiAuditLogger };
