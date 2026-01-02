#!/usr/bin/env npx ts-node
/**
 * HDIM Interactive Implementation Planner
 *
 * Interactive CLI tool that guides users through creating a customer profile
 * and generates a detailed implementation timeline.
 */

import * as readline from 'readline';
import {
  calculateTimeline,
  formatTimelineText,
  formatTimelineMarkdown,
  formatTimelineJSON,
  EXAMPLE_PROFILES,
} from './timeline-calculator';

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

function question(prompt: string): Promise<string> {
  return new Promise((resolve) => {
    rl.question(prompt, (answer) => {
      resolve(answer.trim());
    });
  });
}

function select<T extends string>(prompt: string, options: { value: T; label: string }[]): Promise<T> {
  return new Promise((resolve) => {
    console.log(`\n${prompt}`);
    options.forEach((opt, i) => {
      console.log(`  ${i + 1}. ${opt.label}`);
    });

    const askChoice = () => {
      rl.question(`Enter choice (1-${options.length}): `, (answer) => {
        const idx = parseInt(answer, 10) - 1;
        if (idx >= 0 && idx < options.length) {
          resolve(options[idx].value);
        } else {
          console.log('Invalid choice. Please try again.');
          askChoice();
        }
      });
    };
    askChoice();
  });
}

async function main() {
  console.log('\n' + '═'.repeat(60));
  console.log('  HDIM IMPLEMENTATION TIMELINE PLANNER');
  console.log('═'.repeat(60));
  console.log('\nThis tool will guide you through creating a customer profile');
  console.log('and generate a detailed implementation timeline.\n');

  // Quick start option
  const mode = await select('How would you like to start?', [
    { value: 'quick', label: 'Quick start - Use an example profile' },
    { value: 'custom', label: 'Custom - Enter all details manually' },
  ]);

  let profile: any;

  if (mode === 'quick') {
    const profileType = await select('Select an example profile:', [
      { value: 'solo-practice', label: 'Solo Practice (Dr. Martinez - 1,200 patients, CSV upload)' },
      { value: 'small-practice', label: 'Small Practice (Riverside - 4,500 patients, FHIR API)' },
      { value: 'fqhc', label: 'FQHC (Community Health - 22,000 patients, n8n workflow)' },
      { value: 'midsize-aco', label: 'Mid-size ACO (Metro Health - 42,000 lives, SMART on FHIR)' },
      { value: 'large-health-system', label: 'Large Health System (Regional Med - 180,000 patients, Private Cloud)' },
    ]);

    profile = { ...EXAMPLE_PROFILES[profileType] };

    // Allow customization
    const customize = await select('Would you like to customize this profile?', [
      { value: 'no', label: 'No, use as-is' },
      { value: 'yes', label: 'Yes, let me adjust some values' },
    ]);

    if (customize === 'yes') {
      const orgName = await question(`Organization name [${profile.organizationName}]: `);
      if (orgName) profile.organizationName = orgName;

      const patients = await question(`Patient count [${profile.patientCount}]: `);
      if (patients) profile.patientCount = parseInt(patients, 10);

      const providers = await question(`Provider count [${profile.providerCount}]: `);
      if (providers) profile.providerCount = parseInt(providers, 10);

      const sites = await question(`Site count [${profile.siteCount}]: `);
      if (sites) profile.siteCount = parseInt(sites, 10);
    }
  } else {
    // Full custom entry
    const orgName = await question('Organization name: ');

    const orgType = await select('Organization type:', [
      { value: 'solo-practice', label: 'Solo Practice (1 provider)' },
      { value: 'small-practice', label: 'Small Practice (2-10 providers)' },
      { value: 'fqhc', label: 'FQHC (Federally Qualified Health Center)' },
      { value: 'rural-hospital', label: 'Rural Hospital / CAH' },
      { value: 'small-aco', label: 'Small ACO (<15,000 lives)' },
      { value: 'midsize-aco', label: 'Mid-size ACO (15,000-100,000 lives)' },
      { value: 'large-health-system', label: 'Large Health System' },
      { value: 'ipa', label: 'IPA (Independent Physician Association)' },
    ]);

    const ehrSystem = await select('EHR System:', [
      { value: 'none', label: 'None / Paper-based' },
      { value: 'athenahealth', label: 'athenahealth' },
      { value: 'epic', label: 'Epic' },
      { value: 'cerner', label: 'Cerner/Oracle Health' },
      { value: 'eclinicalworks', label: 'eClinicalWorks' },
      { value: 'nextgen', label: 'NextGen' },
      { value: 'allscripts', label: 'Allscripts' },
      { value: 'meditech', label: 'MEDITECH' },
      { value: 'other-modern', label: 'Other Modern EHR (FHIR-capable)' },
      { value: 'legacy', label: 'Legacy / Custom System' },
    ]);

    const integrationMethod = await select('Preferred integration method:', [
      { value: 'csv-upload', label: 'CSV Upload (manual, simplest)' },
      { value: 'fhir-api', label: 'FHIR API (automated, standard)' },
      { value: 'smart-on-fhir', label: 'SMART on FHIR (embedded in EHR)' },
      { value: 'n8n-workflow', label: 'n8n Workflow (flexible, custom)' },
      { value: 'private-cloud', label: 'Private Cloud (dedicated infrastructure)' },
      { value: 'on-premise', label: 'On-Premise (air-gapped, custom)' },
    ]);

    const itCapability = await select('IT capability level:', [
      { value: 'none', label: 'None - No dedicated IT staff' },
      { value: 'basic', label: 'Basic - Part-time IT or outsourced' },
      { value: 'moderate', label: 'Moderate - Full-time IT staff' },
      { value: 'advanced', label: 'Advanced - Dedicated IT team with integration experience' },
    ]);

    const hdimTier = await select('HDIM subscription tier:', [
      { value: 'community', label: 'Community ($49/mo)' },
      { value: 'professional', label: 'Professional ($299/mo)' },
      { value: 'enterprise', label: 'Enterprise ($999/mo)' },
      { value: 'enterprise-plus', label: 'Enterprise Plus ($2,499/mo)' },
      { value: 'health-system', label: 'Health System (Custom pricing)' },
    ]);

    const patients = await question('Total patient count: ');
    const providers = await question('Provider count: ');
    const sites = await question('Number of sites/locations: ');
    const dataSources = await question('Number of data sources to integrate: ');

    const qualityProgramsInput = await question('Quality programs (comma-separated, e.g., MIPS, ACO REACH, UDS): ');
    const qualityPrograms = qualityProgramsInput.split(',').map(p => p.trim()).filter(p => p);

    const hasFHIR = await select('Does the organization currently have FHIR capability?', [
      { value: 'yes', label: 'Yes' },
      { value: 'no', label: 'No' },
      { value: 'unknown', label: 'Unknown' },
    ]);

    const hasDedicatedIT = await select('Is there dedicated IT staff for this implementation?', [
      { value: 'yes', label: 'Yes' },
      { value: 'no', label: 'No' },
    ]);

    const requiresSOC2 = await select('Is SOC2 compliance required?', [
      { value: 'yes', label: 'Yes' },
      { value: 'no', label: 'No' },
    ]);

    const pilotFirst = await select('Should implementation include a pilot phase?', [
      { value: 'yes', label: 'Yes (recommended for larger organizations)' },
      { value: 'no', label: 'No (direct go-live)' },
    ]);

    profile = {
      organizationType: orgType,
      organizationName: orgName,
      ehrSystem: ehrSystem,
      integrationMethod: integrationMethod,
      itCapability: itCapability,
      hdimTier: hdimTier,
      patientCount: parseInt(patients, 10) || 1000,
      providerCount: parseInt(providers, 10) || 1,
      siteCount: parseInt(sites, 10) || 1,
      dataSourceCount: parseInt(dataSources, 10) || 1,
      qualityPrograms: qualityPrograms.length > 0 ? qualityPrograms : ['MIPS'],
      hasExistingFHIR: hasFHIR === 'yes',
      hasDedicatedIT: hasDedicatedIT === 'yes',
      requiresSOC2: requiresSOC2 === 'yes',
      requiresBAA: true,
      pilotFirst: pilotFirst === 'yes',
    };
  }

  // Get start date
  const startDateInput = await question('Implementation start date (YYYY-MM-DD, or press Enter for today): ');
  const startDate = startDateInput ? new Date(startDateInput) : new Date();

  // Calculate timeline
  console.log('\nCalculating timeline...\n');
  const result = calculateTimeline(profile, startDate);

  // Output format
  const outputFormat = await select('Output format:', [
    { value: 'text', label: 'Text (console output)' },
    { value: 'markdown', label: 'Markdown (copy-paste ready)' },
    { value: 'json', label: 'JSON (machine-readable)' },
  ]);

  console.log('\n');

  switch (outputFormat) {
    case 'markdown':
      console.log(formatTimelineMarkdown(result));
      break;
    case 'json':
      console.log(formatTimelineJSON(result));
      break;
    default:
      console.log(formatTimelineText(result));
  }

  // Save option
  const save = await select('Would you like to save this output?', [
    { value: 'no', label: 'No' },
    { value: 'yes', label: 'Yes' },
  ]);

  if (save === 'yes') {
    const fs = require('fs');
    const filename = `timeline-${profile.organizationName?.replace(/\s+/g, '-').toLowerCase() || 'customer'}-${new Date().toISOString().split('T')[0]}`;

    switch (outputFormat) {
      case 'markdown':
        fs.writeFileSync(`${filename}.md`, formatTimelineMarkdown(result));
        console.log(`\nSaved to ${filename}.md`);
        break;
      case 'json':
        fs.writeFileSync(`${filename}.json`, formatTimelineJSON(result));
        console.log(`\nSaved to ${filename}.json`);
        break;
      default:
        fs.writeFileSync(`${filename}.txt`, formatTimelineText(result));
        console.log(`\nSaved to ${filename}.txt`);
    }
  }

  rl.close();
}

main().catch((err) => {
  console.error('Error:', err);
  rl.close();
  process.exit(1);
});
