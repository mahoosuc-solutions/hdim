/**
 * HDIM Scalable Test Data Generator
 *
 * Generates realistic patient populations and clinical data
 * based on customer profiles and population models.
 */

import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';

// ============================================================================
// TYPES
// ============================================================================

interface CustomerProfile {
  id: string;
  type: string;
  name: string;
  size: 'small' | 'medium' | 'large' | 'enterprise';
  patientVolume: {
    attributedLives?: number;
    annualAdmissions?: number;
    annualVisits?: number;
  };
  payerMix: {
    medicare: number;
    medicaid: number;
    commercial: number;
    selfPay: number;
  };
  qualityMeasures: string[];
  riskProfile: {
    lowRisk: number;
    risingRisk: number;
    moderateRisk: number;
    highRisk: number;
  };
}

interface PopulationModel {
  demographics: DemographicDistribution;
  conditions: ConditionPrevalence;
  utilization: UtilizationPatterns;
  careGaps: CareGapProbabilities;
}

interface DemographicDistribution {
  ageGroups: Array<{ min: number; max: number; weight: number }>;
  genderRatio: { male: number; female: number };
  ethnicityDistribution: Record<string, number>;
}

interface ConditionPrevalence {
  [conditionCode: string]: {
    name: string;
    basePrevalence: number;
    ageMultipliers: Record<string, number>;
    comorbidities: string[];
  };
}

interface UtilizationPatterns {
  pcpVisitsPerYear: Record<string, number>;
  edVisitsPerThousand: number;
  admissionsPerThousand: number;
}

interface CareGapProbabilities {
  [measureId: string]: {
    baselineCompliance: number;
    riskFactors: Record<string, number>;
  };
}

interface GeneratedPatient {
  resourceType: 'Patient';
  id: string;
  identifier: Array<{ system: string; value: string }>;
  name: Array<{ family: string; given: string[] }>;
  gender: 'male' | 'female';
  birthDate: string;
  address: Array<{
    city: string;
    state: string;
    postalCode: string;
  }>;
  extension?: Array<{
    url: string;
    valueCode?: string;
    valueCoding?: { system: string; code: string; display: string };
  }>;
}

interface GeneratedCondition {
  resourceType: 'Condition';
  id: string;
  clinicalStatus: { coding: Array<{ system: string; code: string }> };
  code: { coding: Array<{ system: string; code: string; display: string }> };
  subject: { reference: string };
  onsetDateTime: string;
}

interface GeneratedObservation {
  resourceType: 'Observation';
  id: string;
  status: 'final';
  category: Array<{ coding: Array<{ system: string; code: string }> }>;
  code: { coding: Array<{ system: string; code: string; display: string }> };
  subject: { reference: string };
  effectiveDateTime: string;
  valueQuantity?: { value: number; unit: string; system: string; code: string };
}

interface DataGenerationConfig {
  customerId: string;
  customerType: string;
  targetPopulationSize: number;
  measurementPeriodStart: string;
  measurementPeriodEnd: string;
  includeHistoricalData: boolean;
  historicalYears: number;
  outputFormat: 'fhir-bundle' | 'ndjson' | 'csv';
}

// ============================================================================
// CONSTANTS
// ============================================================================

const FIRST_NAMES_MALE = [
  'James', 'John', 'Robert', 'Michael', 'William', 'David', 'Richard', 'Joseph',
  'Thomas', 'Charles', 'Christopher', 'Daniel', 'Matthew', 'Anthony', 'Mark',
  'Donald', 'Steven', 'Paul', 'Andrew', 'Joshua', 'Kenneth', 'Kevin', 'Brian'
];

const FIRST_NAMES_FEMALE = [
  'Mary', 'Patricia', 'Jennifer', 'Linda', 'Barbara', 'Elizabeth', 'Susan',
  'Jessica', 'Sarah', 'Karen', 'Nancy', 'Lisa', 'Betty', 'Margaret', 'Sandra',
  'Ashley', 'Kimberly', 'Emily', 'Donna', 'Michelle', 'Dorothy', 'Carol', 'Amanda'
];

const LAST_NAMES = [
  'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis',
  'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson',
  'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson',
  'White', 'Harris', 'Sanchez', 'Clark', 'Ramirez', 'Lewis', 'Robinson', 'Walker'
];

const CITIES_BY_STATE: Record<string, string[]> = {
  'CA': ['Los Angeles', 'San Francisco', 'San Diego', 'Sacramento', 'Fresno'],
  'TX': ['Houston', 'Dallas', 'Austin', 'San Antonio', 'Fort Worth'],
  'FL': ['Miami', 'Orlando', 'Tampa', 'Jacksonville', 'Fort Lauderdale'],
  'NY': ['New York', 'Buffalo', 'Rochester', 'Albany', 'Syracuse'],
  'IL': ['Chicago', 'Springfield', 'Peoria', 'Rockford', 'Naperville'],
  'PA': ['Philadelphia', 'Pittsburgh', 'Harrisburg', 'Allentown', 'Erie'],
  'OH': ['Columbus', 'Cleveland', 'Cincinnati', 'Toledo', 'Akron'],
  'GA': ['Atlanta', 'Augusta', 'Savannah', 'Columbus', 'Macon'],
  'NC': ['Charlotte', 'Raleigh', 'Greensboro', 'Durham', 'Winston-Salem'],
  'MI': ['Detroit', 'Grand Rapids', 'Ann Arbor', 'Lansing', 'Flint']
};

const ICD10_CODES: Record<string, { code: string; display: string }> = {
  'diabetes': { code: 'E11.9', display: 'Type 2 diabetes mellitus without complications' },
  'hypertension': { code: 'I10', display: 'Essential (primary) hypertension' },
  'chf': { code: 'I50.9', display: 'Heart failure, unspecified' },
  'copd': { code: 'J44.9', display: 'Chronic obstructive pulmonary disease, unspecified' },
  'ckd': { code: 'N18.3', display: 'Chronic kidney disease, stage 3' },
  'depression': { code: 'F32.9', display: 'Major depressive disorder, single episode' },
  'anxiety': { code: 'F41.1', display: 'Generalized anxiety disorder' },
  'obesity': { code: 'E66.9', display: 'Obesity, unspecified' },
  'hyperlipidemia': { code: 'E78.5', display: 'Hyperlipidemia, unspecified' },
  'atrial_fib': { code: 'I48.91', display: 'Unspecified atrial fibrillation' }
};

const LOINC_CODES: Record<string, { code: string; display: string; unit: string }> = {
  'hba1c': { code: '4548-4', display: 'Hemoglobin A1c', unit: '%' },
  'systolic_bp': { code: '8480-6', display: 'Systolic blood pressure', unit: 'mmHg' },
  'diastolic_bp': { code: '8462-4', display: 'Diastolic blood pressure', unit: 'mmHg' },
  'ldl': { code: '2089-1', display: 'LDL Cholesterol', unit: 'mg/dL' },
  'egfr': { code: '48642-3', display: 'eGFR', unit: 'mL/min/1.73m2' },
  'bmi': { code: '39156-5', display: 'Body mass index', unit: 'kg/m2' },
  'phq9': { code: '44261-6', display: 'PHQ-9 total score', unit: '{score}' }
};

// ============================================================================
// RANDOM UTILITIES
// ============================================================================

class SeededRandom {
  private seed: number;

  constructor(seed: string | number) {
    this.seed = typeof seed === 'string' ? this.hashString(seed) : seed;
  }

  private hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash);
  }

  next(): number {
    this.seed = (this.seed * 1103515245 + 12345) & 0x7fffffff;
    return this.seed / 0x7fffffff;
  }

  nextInt(min: number, max: number): number {
    return Math.floor(this.next() * (max - min + 1)) + min;
  }

  pick<T>(array: T[]): T {
    return array[this.nextInt(0, array.length - 1)];
  }

  weightedPick<T>(items: T[], weights: number[]): T {
    const totalWeight = weights.reduce((a, b) => a + b, 0);
    let random = this.next() * totalWeight;
    for (let i = 0; i < items.length; i++) {
      random -= weights[i];
      if (random <= 0) return items[i];
    }
    return items[items.length - 1];
  }

  probability(p: number): boolean {
    return this.next() < p;
  }

  gaussian(mean: number, stdDev: number): number {
    const u1 = this.next();
    const u2 = this.next();
    const z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    return mean + stdDev * z;
  }
}

// ============================================================================
// DATA GENERATOR CLASS
// ============================================================================

export class PatientDataGenerator {
  private random: SeededRandom;
  private config: DataGenerationConfig;
  private customerProfile: CustomerProfile;
  private populationModel: PopulationModel;

  constructor(
    config: DataGenerationConfig,
    customerProfile: CustomerProfile,
    populationModel: PopulationModel,
    seed?: string
  ) {
    this.config = config;
    this.customerProfile = customerProfile;
    this.populationModel = populationModel;
    this.random = new SeededRandom(seed || config.customerId);
  }

  /**
   * Generate a complete patient population
   */
  generatePopulation(): {
    patients: GeneratedPatient[];
    conditions: GeneratedCondition[];
    observations: GeneratedObservation[];
    summary: PopulationSummary;
  } {
    const patients: GeneratedPatient[] = [];
    const conditions: GeneratedCondition[] = [];
    const observations: GeneratedObservation[] = [];

    console.log(`Generating ${this.config.targetPopulationSize} patients...`);

    for (let i = 0; i < this.config.targetPopulationSize; i++) {
      const patient = this.generatePatient(i);
      patients.push(patient);

      // Generate conditions based on demographics and risk
      const patientConditions = this.generateConditions(patient);
      conditions.push(...patientConditions);

      // Generate observations (vitals, labs)
      const patientObservations = this.generateObservations(patient, patientConditions);
      observations.push(...patientObservations);

      if ((i + 1) % 1000 === 0) {
        console.log(`  Generated ${i + 1} patients...`);
      }
    }

    const summary = this.calculateSummary(patients, conditions, observations);

    return { patients, conditions, observations, summary };
  }

  /**
   * Generate a single patient
   */
  private generatePatient(index: number): GeneratedPatient {
    const gender = this.random.probability(this.populationModel.demographics.genderRatio.female)
      ? 'female' : 'male';

    const age = this.generateAge();
    const birthDate = this.calculateBirthDate(age);

    const firstName = gender === 'male'
      ? this.random.pick(FIRST_NAMES_MALE)
      : this.random.pick(FIRST_NAMES_FEMALE);
    const lastName = this.random.pick(LAST_NAMES);

    const state = this.random.pick(Object.keys(CITIES_BY_STATE));
    const city = this.random.pick(CITIES_BY_STATE[state]);
    const postalCode = this.generatePostalCode(state);

    const patientId = `PT-${this.config.customerId}-${String(index).padStart(6, '0')}`;

    return {
      resourceType: 'Patient',
      id: patientId,
      identifier: [
        {
          system: 'http://healthdata-in-motion.com/patient-id',
          value: patientId
        },
        {
          system: 'http://hl7.org/fhir/sid/us-medicare',
          value: this.generateMBI()
        }
      ],
      name: [{ family: lastName, given: [firstName] }],
      gender,
      birthDate,
      address: [{ city, state, postalCode }],
      extension: this.generatePatientExtensions()
    };
  }

  /**
   * Generate age based on population model
   */
  private generateAge(): number {
    const ageGroups = this.populationModel.demographics.ageGroups;
    const weights = ageGroups.map(g => g.weight);
    const group = this.random.weightedPick(ageGroups, weights);
    return this.random.nextInt(group.min, group.max);
  }

  /**
   * Calculate birth date from age
   */
  private calculateBirthDate(age: number): string {
    const today = new Date();
    const birthYear = today.getFullYear() - age;
    const birthMonth = this.random.nextInt(1, 12);
    const birthDay = this.random.nextInt(1, 28);
    return `${birthYear}-${String(birthMonth).padStart(2, '0')}-${String(birthDay).padStart(2, '0')}`;
  }

  /**
   * Generate postal code for state
   */
  private generatePostalCode(state: string): string {
    const prefixes: Record<string, number[]> = {
      'CA': [90, 91, 92, 93, 94, 95, 96],
      'TX': [75, 76, 77, 78, 79],
      'FL': [32, 33, 34],
      'NY': [10, 11, 12, 13, 14],
      'IL': [60, 61, 62],
      'PA': [15, 16, 17, 18, 19],
      'OH': [43, 44, 45],
      'GA': [30, 31, 39],
      'NC': [27, 28],
      'MI': [48, 49]
    };
    const prefix = this.random.pick(prefixes[state] || [10]);
    const suffix = this.random.nextInt(100, 999);
    return `${prefix}${suffix}`;
  }

  /**
   * Generate Medicare Beneficiary Identifier
   */
  private generateMBI(): string {
    const chars = 'ACDEFGHJKMNPQRTUVWXY';
    const nums = '0123456789';
    let mbi = '';
    // MBI format: 1A12-A12-AA12
    mbi += this.random.pick(nums.split(''));
    mbi += this.random.pick(chars.split(''));
    mbi += this.random.pick((nums + chars).split(''));
    mbi += this.random.pick(nums.split(''));
    mbi += this.random.pick(chars.split(''));
    mbi += this.random.pick((nums + chars).split(''));
    mbi += this.random.pick(nums.split(''));
    mbi += this.random.pick(chars.split(''));
    mbi += this.random.pick(chars.split(''));
    mbi += this.random.pick((nums + chars).split(''));
    mbi += this.random.pick(nums.split(''));
    return mbi;
  }

  /**
   * Generate patient extensions (race, ethnicity, etc.)
   */
  private generatePatientExtensions(): GeneratedPatient['extension'] {
    const ethnicities = Object.entries(this.populationModel.demographics.ethnicityDistribution);
    const ethnicity = this.random.weightedPick(
      ethnicities.map(e => e[0]),
      ethnicities.map(e => e[1])
    );

    return [
      {
        url: 'http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity',
        valueCoding: {
          system: 'urn:oid:2.16.840.1.113883.6.238',
          code: ethnicity,
          display: ethnicity
        }
      }
    ];
  }

  /**
   * Generate conditions for a patient
   */
  private generateConditions(patient: GeneratedPatient): GeneratedCondition[] {
    const conditions: GeneratedCondition[] = [];
    const age = this.calculateAge(patient.birthDate);
    const ageGroup = this.getAgeGroup(age);

    for (const [conditionKey, conditionData] of Object.entries(this.populationModel.conditions)) {
      let probability = conditionData.basePrevalence;

      // Apply age multiplier
      if (conditionData.ageMultipliers[ageGroup]) {
        probability *= conditionData.ageMultipliers[ageGroup];
      }

      if (this.random.probability(probability)) {
        const icdInfo = ICD10_CODES[conditionKey];
        if (icdInfo) {
          conditions.push({
            resourceType: 'Condition',
            id: `${patient.id}-${conditionKey}`,
            clinicalStatus: {
              coding: [{ system: 'http://terminology.hl7.org/CodeSystem/condition-clinical', code: 'active' }]
            },
            code: {
              coding: [{
                system: 'http://hl7.org/fhir/sid/icd-10-cm',
                code: icdInfo.code,
                display: icdInfo.display
              }]
            },
            subject: { reference: `Patient/${patient.id}` },
            onsetDateTime: this.generateOnsetDate(age)
          });
        }
      }
    }

    return conditions;
  }

  /**
   * Generate observations based on conditions
   */
  private generateObservations(
    patient: GeneratedPatient,
    conditions: GeneratedCondition[]
  ): GeneratedObservation[] {
    const observations: GeneratedObservation[] = [];
    const conditionCodes = conditions.map(c => c.code.coding[0].code);

    // Generate vitals for everyone
    observations.push(this.generateVitalSign(patient, 'systolic_bp', 110, 180));
    observations.push(this.generateVitalSign(patient, 'diastolic_bp', 60, 100));
    observations.push(this.generateVitalSign(patient, 'bmi', 18, 45));

    // Condition-specific observations
    if (conditionCodes.includes('E11.9')) { // Diabetes
      observations.push(this.generateLabResult(patient, 'hba1c', 5.0, 12.0));
    }

    if (conditionCodes.includes('N18.3')) { // CKD
      observations.push(this.generateLabResult(patient, 'egfr', 15, 90));
    }

    if (conditionCodes.includes('E78.5')) { // Hyperlipidemia
      observations.push(this.generateLabResult(patient, 'ldl', 70, 200));
    }

    if (conditionCodes.includes('F32.9')) { // Depression
      observations.push(this.generateLabResult(patient, 'phq9', 0, 27));
    }

    return observations;
  }

  /**
   * Generate a vital sign observation
   */
  private generateVitalSign(
    patient: GeneratedPatient,
    type: string,
    min: number,
    max: number
  ): GeneratedObservation {
    const loincInfo = LOINC_CODES[type];
    const value = Math.round(this.random.gaussian((min + max) / 2, (max - min) / 4) * 10) / 10;
    const clampedValue = Math.max(min, Math.min(max, value));

    return {
      resourceType: 'Observation',
      id: `${patient.id}-${type}-${Date.now()}`,
      status: 'final',
      category: [{
        coding: [{ system: 'http://terminology.hl7.org/CodeSystem/observation-category', code: 'vital-signs' }]
      }],
      code: {
        coding: [{
          system: 'http://loinc.org',
          code: loincInfo.code,
          display: loincInfo.display
        }]
      },
      subject: { reference: `Patient/${patient.id}` },
      effectiveDateTime: this.generateRecentDate(),
      valueQuantity: {
        value: clampedValue,
        unit: loincInfo.unit,
        system: 'http://unitsofmeasure.org',
        code: loincInfo.unit
      }
    };
  }

  /**
   * Generate a lab result observation
   */
  private generateLabResult(
    patient: GeneratedPatient,
    type: string,
    min: number,
    max: number
  ): GeneratedObservation {
    const loincInfo = LOINC_CODES[type];
    const value = Math.round(this.random.gaussian((min + max) / 2, (max - min) / 4) * 10) / 10;
    const clampedValue = Math.max(min, Math.min(max, value));

    return {
      resourceType: 'Observation',
      id: `${patient.id}-${type}-${Date.now()}`,
      status: 'final',
      category: [{
        coding: [{ system: 'http://terminology.hl7.org/CodeSystem/observation-category', code: 'laboratory' }]
      }],
      code: {
        coding: [{
          system: 'http://loinc.org',
          code: loincInfo.code,
          display: loincInfo.display
        }]
      },
      subject: { reference: `Patient/${patient.id}` },
      effectiveDateTime: this.generateRecentDate(),
      valueQuantity: {
        value: clampedValue,
        unit: loincInfo.unit,
        system: 'http://unitsofmeasure.org',
        code: loincInfo.unit
      }
    };
  }

  /**
   * Calculate age from birth date
   */
  private calculateAge(birthDate: string): number {
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  /**
   * Get age group label
   */
  private getAgeGroup(age: number): string {
    if (age < 18) return 'pediatric';
    if (age < 40) return 'young_adult';
    if (age < 65) return 'middle_aged';
    if (age < 75) return 'senior';
    return 'elderly';
  }

  /**
   * Generate onset date for a condition
   */
  private generateOnsetDate(currentAge: number): string {
    const yearsAgo = this.random.nextInt(1, Math.min(10, currentAge - 18));
    const date = new Date();
    date.setFullYear(date.getFullYear() - yearsAgo);
    return date.toISOString().split('T')[0];
  }

  /**
   * Generate a recent date within measurement period
   */
  private generateRecentDate(): string {
    const start = new Date(this.config.measurementPeriodStart);
    const end = new Date(this.config.measurementPeriodEnd);
    const range = end.getTime() - start.getTime();
    const randomTime = start.getTime() + this.random.next() * range;
    return new Date(randomTime).toISOString();
  }

  /**
   * Calculate population summary statistics
   */
  private calculateSummary(
    patients: GeneratedPatient[],
    conditions: GeneratedCondition[],
    observations: GeneratedObservation[]
  ): PopulationSummary {
    const ageDistribution: Record<string, number> = {};
    const genderDistribution: Record<string, number> = { male: 0, female: 0 };
    const conditionPrevalence: Record<string, number> = {};

    for (const patient of patients) {
      const age = this.calculateAge(patient.birthDate);
      const ageGroup = this.getAgeGroup(age);
      ageDistribution[ageGroup] = (ageDistribution[ageGroup] || 0) + 1;
      genderDistribution[patient.gender]++;
    }

    for (const condition of conditions) {
      const code = condition.code.coding[0].display;
      conditionPrevalence[code] = (conditionPrevalence[code] || 0) + 1;
    }

    // Convert to percentages
    for (const key of Object.keys(ageDistribution)) {
      ageDistribution[key] = Math.round((ageDistribution[key] / patients.length) * 100);
    }
    for (const key of Object.keys(conditionPrevalence)) {
      conditionPrevalence[key] = Math.round((conditionPrevalence[key] / patients.length) * 100);
    }

    return {
      totalPatients: patients.length,
      totalConditions: conditions.length,
      totalObservations: observations.length,
      ageDistribution,
      genderDistribution: {
        male: Math.round((genderDistribution.male / patients.length) * 100),
        female: Math.round((genderDistribution.female / patients.length) * 100)
      },
      conditionPrevalence,
      generatedAt: new Date().toISOString()
    };
  }

  /**
   * Export population as FHIR Bundle
   */
  exportAsFhirBundle(data: {
    patients: GeneratedPatient[];
    conditions: GeneratedCondition[];
    observations: GeneratedObservation[];
  }): object {
    const entries = [
      ...data.patients.map(p => ({ resource: p, request: { method: 'PUT', url: `Patient/${p.id}` } })),
      ...data.conditions.map(c => ({ resource: c, request: { method: 'PUT', url: `Condition/${c.id}` } })),
      ...data.observations.map(o => ({ resource: o, request: { method: 'PUT', url: `Observation/${o.id}` } }))
    ];

    return {
      resourceType: 'Bundle',
      type: 'transaction',
      meta: {
        generatedBy: 'HDIM Test Data Generator',
        customerId: this.config.customerId,
        customerType: this.config.customerType,
        generatedAt: new Date().toISOString()
      },
      entry: entries
    };
  }
}

interface PopulationSummary {
  totalPatients: number;
  totalConditions: number;
  totalObservations: number;
  ageDistribution: Record<string, number>;
  genderDistribution: Record<string, number>;
  conditionPrevalence: Record<string, number>;
  generatedAt: string;
}

export default PatientDataGenerator;
