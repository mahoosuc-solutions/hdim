function loadTools(client) {
  return [
    require('./tools/patient-read').createDefinition(client),
    require('./tools/patient-search').createDefinition(client),
    require('./tools/observation-read').createDefinition(client),
    require('./tools/observation-search').createDefinition(client),
    require('./tools/condition-read').createDefinition(client),
    require('./tools/condition-search').createDefinition(client),
    require('./tools/medication-read').createDefinition(client),
    require('./tools/medication-search').createDefinition(client),
    require('./tools/encounter-read').createDefinition(client),
    require('./tools/encounter-search').createDefinition(client),
    require('./tools/care-gap-list').createDefinition(client),
    require('./tools/care-gap-close').createDefinition(client),
    require('./tools/care-gap-stats').createDefinition(client),
    require('./tools/measure-evaluate').createDefinition(client),
    require('./tools/measure-results').createDefinition(client),
  ];
}

module.exports = { loadTools };
