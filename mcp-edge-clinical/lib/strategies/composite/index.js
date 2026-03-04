function loadTools(client) {
  return [
    require('./tools/fhir-read').createDefinition(client),
    require('./tools/fhir-search').createDefinition(client),
    require('./tools/fhir-create').createDefinition(client),
    require('./tools/fhir-bundle').createDefinition(client),
    require('./tools/patient-summary').createDefinition(client),
    require('./tools/patient-timeline').createDefinition(client),
    require('./tools/patient-risk').createDefinition(client),
    require('./tools/patient-list').createDefinition(client),
    require('./tools/pre-visit-plan').createDefinition(client),
    require('./tools/care-gap-list').createDefinition(client),
    require('./tools/care-gap-identify').createDefinition(client),
    require('./tools/care-gap-close').createDefinition(client),
    require('./tools/care-gap-stats').createDefinition(client),
    require('./tools/care-gap-population').createDefinition(client),
    require('./tools/care-gap-provider').createDefinition(client),
    require('./tools/measure-evaluate').createDefinition(client),
    require('./tools/measure-results').createDefinition(client),
    require('./tools/measure-score').createDefinition(client),
    require('./tools/measure-population').createDefinition(client),
    require('./tools/cds-patient-view').createDefinition(client),
    require('./tools/health-score').createDefinition(client),
    require('./tools/cql-evaluate').createDefinition(client),
    require('./tools/cql-batch').createDefinition(client),
    require('./tools/cql-libraries').createDefinition(client),
    require('./tools/cql-result').createDefinition(client),
  ];
}

module.exports = { loadTools };
