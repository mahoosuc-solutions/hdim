#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const MANIFEST_PATH = path.join(__dirname, 'manifest.json');
const CONTEXT_PATH = '/tmp/live-test-context.json';

async function loadPatients(gatewayUrl, tenantId) {
  const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf8'));
  const context = { patients: {}, resources: {} };

  console.log(`Loading ${manifest.phenotypes.length} phenotypes into ${gatewayUrl}...`);

  for (const phenotype of manifest.phenotypes) {
    const bundlePath = path.join(__dirname, phenotype.bundle);
    const bundle = JSON.parse(fs.readFileSync(bundlePath, 'utf8'));

    console.log(`  Loading: ${phenotype.id} (${bundle.entry.length} resources)`);

    const res = await fetch(`${gatewayUrl}/fhir`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/fhir+json',
        'Accept': 'application/fhir+json',
        'x-tenant-id': tenantId
      },
      body: JSON.stringify(bundle)
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Failed to load ${phenotype.id}: ${res.status} ${text}`);
    }

    const responseBundle = await res.json();

    const patientEntry = responseBundle.entry?.find(e =>
      e.response?.location?.startsWith('Patient/')
    );
    if (patientEntry) {
      const patientId = patientEntry.response.location.split('/')[1];
      context.patients[phenotype.id] = patientId;
    }

    context.resources[phenotype.id] = {};
    for (const entry of (responseBundle.entry || [])) {
      if (entry.response?.location) {
        const [resourceType, id] = entry.response.location.split('/');
        if (!context.resources[phenotype.id][resourceType]) {
          context.resources[phenotype.id][resourceType] = [];
        }
        context.resources[phenotype.id][resourceType].push(id);
      }
    }

    console.log(`    Patient ID: ${context.patients[phenotype.id] || 'unknown'}`);
  }

  fs.writeFileSync(CONTEXT_PATH, JSON.stringify(context, null, 2));
  console.log(`\nContext written to ${CONTEXT_PATH}`);
  console.log(`Loaded ${Object.keys(context.patients).length} patients.`);

  return context;
}

if (require.main === module) {
  const args = process.argv.slice(2);
  const gatewayUrl = args.includes('--gateway-url')
    ? args[args.indexOf('--gateway-url') + 1]
    : process.env.HDIM_BASE_URL || 'http://localhost:18080';
  const tenantId = args.includes('--tenant-id')
    ? args[args.indexOf('--tenant-id') + 1]
    : 'demo';

  loadPatients(gatewayUrl, tenantId)
    .then(() => process.exit(0))
    .catch(err => { console.error(err); process.exit(1); });
}

module.exports = { loadPatients };
