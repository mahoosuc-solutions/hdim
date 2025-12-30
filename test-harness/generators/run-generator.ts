import { PatientDataGenerator } from './data-generator';
import * as fs from 'fs';

const size = parseInt(process.argv[2] || '1000');
const profile = process.argv[3] || 'academic-medical-center';

const profiles: Record<string, any> = {
  'academic-medical-center': {
    name: 'Academic Medical Center',
    type: 'hospital',
    payerMix: { medicare: 0.45, medicaid: 0.15, commercial: 0.35, selfPay: 0.05 },
    riskProfile: { lowRisk: 0.50, risingRisk: 0.20, moderateRisk: 0.15, highRisk: 0.15 },
    conditions: {
      'E11': { name: 'Type 2 Diabetes', basePrevalence: 0.18, ageMultipliers: { '65+': 1.5 }, comorbidities: ['I10', 'N18'] },
      'I10': { name: 'Hypertension', basePrevalence: 0.38, ageMultipliers: { '65+': 1.8 }, comorbidities: ['E11'] },
      'I50': { name: 'Heart Failure', basePrevalence: 0.08, ageMultipliers: { '65+': 2.0 }, comorbidities: ['I10', 'E11'] },
      'J44': { name: 'COPD', basePrevalence: 0.12, ageMultipliers: { '65+': 1.6 }, comorbidities: [] },
      'N18': { name: 'CKD', basePrevalence: 0.15, ageMultipliers: { '65+': 1.7 }, comorbidities: ['E11', 'I10'] },
      'F32': { name: 'Depression', basePrevalence: 0.14, ageMultipliers: {}, comorbidities: [] }
    }
  },
  'large-multi-specialty': {
    name: 'Large Multi-Specialty Practice',
    type: 'provider',
    payerMix: { medicare: 0.35, medicaid: 0.12, commercial: 0.48, selfPay: 0.05 },
    riskProfile: { lowRisk: 0.55, risingRisk: 0.22, moderateRisk: 0.13, highRisk: 0.10 },
    conditions: {
      'E11': { name: 'Type 2 Diabetes', basePrevalence: 0.14, ageMultipliers: { '65+': 1.4 }, comorbidities: ['I10', 'E78'] },
      'I10': { name: 'Hypertension', basePrevalence: 0.32, ageMultipliers: { '65+': 1.6 }, comorbidities: ['E11'] },
      'E78': { name: 'Hyperlipidemia', basePrevalence: 0.28, ageMultipliers: { '40+': 1.3 }, comorbidities: ['I10'] },
      'M54': { name: 'Back Pain', basePrevalence: 0.18, ageMultipliers: { '40+': 1.2 }, comorbidities: [] },
      'J06': { name: 'Upper Respiratory', basePrevalence: 0.22, ageMultipliers: {}, comorbidities: [] },
      'F41': { name: 'Anxiety', basePrevalence: 0.16, ageMultipliers: {}, comorbidities: ['F32'] },
      'F32': { name: 'Depression', basePrevalence: 0.12, ageMultipliers: {}, comorbidities: ['F41'] },
      'K21': { name: 'GERD', basePrevalence: 0.15, ageMultipliers: { '40+': 1.3 }, comorbidities: [] }
    }
  },
  'regional-health-plan': {
    name: 'Regional Health Plan (500K Members)',
    type: 'health-plan',
    payerMix: { medicare: 0.28, medicaid: 0.22, commercial: 0.50, selfPay: 0.00 },
    riskProfile: { lowRisk: 0.52, risingRisk: 0.25, moderateRisk: 0.15, highRisk: 0.08 },
    conditions: {
      'E11': { name: 'Type 2 Diabetes', basePrevalence: 0.11, ageMultipliers: { '65+': 1.6 }, comorbidities: ['I10', 'E78', 'N18'] },
      'I10': { name: 'Hypertension', basePrevalence: 0.29, ageMultipliers: { '65+': 1.7 }, comorbidities: ['E11', 'E78'] },
      'E78': { name: 'Hyperlipidemia', basePrevalence: 0.25, ageMultipliers: { '40+': 1.4 }, comorbidities: ['I10'] },
      'I50': { name: 'Heart Failure', basePrevalence: 0.04, ageMultipliers: { '65+': 2.5 }, comorbidities: ['I10', 'E11'] },
      'J44': { name: 'COPD', basePrevalence: 0.06, ageMultipliers: { '65+': 1.8 }, comorbidities: [] },
      'N18': { name: 'CKD', basePrevalence: 0.08, ageMultipliers: { '65+': 2.0 }, comorbidities: ['E11', 'I10'] },
      'F32': { name: 'Depression', basePrevalence: 0.09, ageMultipliers: {}, comorbidities: ['F41'] },
      'F41': { name: 'Anxiety', basePrevalence: 0.11, ageMultipliers: {}, comorbidities: ['F32'] },
      'J45': { name: 'Asthma', basePrevalence: 0.08, ageMultipliers: { '0-17': 1.3 }, comorbidities: [] },
      'M54': { name: 'Back Pain', basePrevalence: 0.12, ageMultipliers: { '40+': 1.2 }, comorbidities: [] },
      'G43': { name: 'Migraine', basePrevalence: 0.10, ageMultipliers: {}, comorbidities: [] },
      'K21': { name: 'GERD', basePrevalence: 0.14, ageMultipliers: { '40+': 1.3 }, comorbidities: [] }
    }
  }
};

const selected = profiles[profile] || profiles['academic-medical-center'];
console.log(`\n🏥 Generating ${size} patients for ${selected.name}...\n`);

const config: any = {
  customerId: profile,
  customerType: selected.type,
  targetPopulationSize: size,
  measurementPeriodStart: '2024-01-01',
  measurementPeriodEnd: '2024-12-31',
  includeHistoricalData: true,
  historicalYears: 2,
  outputFormat: 'fhir-bundle'
};

const customerProfile: any = {
  id: profile,
  name: selected.name,
  type: selected.type,
  size: size >= 50000 ? 'enterprise' : size >= 10000 ? 'large' : 'medium',
  patientVolume: {
    attributedLives: size,
    annualAdmissions: selected.type === 'hospital' ? Math.round(size * 0.35) : 0,
    annualVisits: Math.round(size * 8)
  },
  qualityMeasures: selected.type === 'hospital' ? ['HEDIS', 'CMS-Stars', 'Leapfrog'] : ['HEDIS', 'MIPS', 'PCMH'],
  payerMix: selected.payerMix,
  riskProfile: selected.riskProfile
};

const populationModel: any = {
  demographics: {
    ageGroups: [
      { min: 0, max: 17, weight: 0.12 },
      { min: 18, max: 39, weight: 0.22 },
      { min: 40, max: 64, weight: 0.32 },
      { min: 65, max: 74, weight: 0.18 },
      { min: 75, max: 100, weight: 0.16 }
    ],
    genderRatio: { male: 0.49, female: 0.51 },
    ethnicityDistribution: { white: 0.60, black: 0.13, hispanic: 0.18, asian: 0.06, other: 0.03 }
  },
  conditions: selected.conditions,
  utilization: {
    averageAnnualVisits: 8,
    edVisitRate: 0.15,
    admissionRate: 0.08
  },
  careGaps: {
    CDC_HBA1C: 0.48,
    CBP: 0.42,
    BCS: 0.35,
    COL: 0.38,
    TRC: 0.55
  }
};

const generator = new PatientDataGenerator(config, customerProfile, populationModel, 'amc-seed');
const data = generator.generatePopulation();
const bundle = generator.exportAsFhirBundle(data);

const outFile = `./datasets/${profile}-${size}-fhir.json`;
fs.writeFileSync(outFile, JSON.stringify(bundle, null, 2));

console.log(`✅ Generated ${data.patients.length} patients`);
console.log(`   ${data.conditions.length} conditions`);
console.log(`   ${data.observations.length} observations`);
console.log(`📁 Output: ${outFile} (${(fs.statSync(outFile).size / 1024 / 1024).toFixed(1)} MB)\n`);
