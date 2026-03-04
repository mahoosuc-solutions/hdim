// mcp-edge-clinical/lib/strategies/full-surface/index.js
// Full-surface strategy — 68 tools: 52 FHIR (factory-generated) + 16 domain (reused from composite).

const { RESOURCE_REGISTRY } = require('./resource-registry');
const { generateFhirTools } = require('./tool-factory');

function loadTools(client) {
  const fhirTools = generateFhirTools(RESOURCE_REGISTRY, client);

  // Reuse domain tools from composite strategy
  const domainTools = [
    require('../composite/tools/care-gap-list').createDefinition(client),
    require('../composite/tools/care-gap-identify').createDefinition(client),
    require('../composite/tools/care-gap-close').createDefinition(client),
    require('../composite/tools/care-gap-stats').createDefinition(client),
    require('../composite/tools/care-gap-population').createDefinition(client),
    require('../composite/tools/care-gap-provider').createDefinition(client),
    require('../composite/tools/measure-evaluate').createDefinition(client),
    require('../composite/tools/measure-results').createDefinition(client),
    require('../composite/tools/measure-score').createDefinition(client),
    require('../composite/tools/measure-population').createDefinition(client),
    require('../composite/tools/cds-patient-view').createDefinition(client),
    require('../composite/tools/health-score').createDefinition(client),
    require('../composite/tools/cql-evaluate').createDefinition(client),
    require('../composite/tools/cql-batch').createDefinition(client),
    require('../composite/tools/cql-libraries').createDefinition(client),
    require('../composite/tools/cql-result').createDefinition(client),
  ];

  return [...fhirTools, ...domainTools];
}

module.exports = { loadTools };
