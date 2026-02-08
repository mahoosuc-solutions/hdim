/**
 * Measure Template Model - Pre-built templates for common quality measures
 * Used in the Measure Builder to jumpstart custom measure creation
 */

export interface MeasureTemplate {
  id: string;
  name: string;
  category: MeasureTemplateCategory;
  specialty: ProviderSpecialty;
  description: string;
  clinicalRationale: string;
  hedisAligned?: string; // Reference to HEDIS measure if applicable
  cmsAligned?: string; // Reference to CMS measure if applicable
  cqlTemplate: string;
  requiredValueSets: ValueSetReference[];
  populationCriteria: PopulationCriteria;
  tags: string[];
  complexity: 'basic' | 'intermediate' | 'advanced';
  estimatedPatients?: string; // e.g., "~15% of panel"
}

export type MeasureTemplateCategory =
  | 'PREVENTIVE_CARE'
  | 'CHRONIC_DISEASE'
  | 'BEHAVIORAL_HEALTH'
  | 'MEDICATION_MANAGEMENT'
  | 'WOMENS_HEALTH'
  | 'PEDIATRIC'
  | 'GERIATRIC'
  | 'CARE_COORDINATION'
  | 'PATIENT_SAFETY';

export type ProviderSpecialty =
  | 'PRIMARY_CARE'
  | 'INTERNAL_MEDICINE'
  | 'FAMILY_MEDICINE'
  | 'PEDIATRICS'
  | 'OBGYN'
  | 'CARDIOLOGY'
  | 'ENDOCRINOLOGY'
  | 'PSYCHIATRY'
  | 'ALL';

export interface ValueSetReference {
  oid: string;
  name: string;
  codeSystem: string;
  description?: string;
}

export interface PopulationCriteria {
  initialPopulation: string;
  denominator: string;
  numerator: string;
  denominatorExclusions?: string;
  denominatorExceptions?: string;
  numeratorExclusions?: string;
}

/**
 * Primary Care Measure Templates
 * Pre-built CQL templates for common primary care quality measures
 */
export const PRIMARY_CARE_TEMPLATES: MeasureTemplate[] = [
  // ===== PREVENTIVE CARE =====
  {
    id: 'tpl-diabetes-hba1c-control',
    name: 'Diabetes: HbA1c Control',
    category: 'CHRONIC_DISEASE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients with diabetes who had HbA1c control (<8.0%) during the measurement period.',
    clinicalRationale: 'Good glycemic control reduces risk of diabetes complications including retinopathy, nephropathy, and cardiovascular disease.',
    hedisAligned: 'CDC - Comprehensive Diabetes Care',
    cqlTemplate: `library DiabetesHbA1cControl version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "HbA1c Laboratory Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013'

// Code Systems
codesystem "LOINC": 'http://loinc.org'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Age 18-75 at start of measurement period
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75

// Has diabetes diagnosis
define "Has Diabetes":
  exists([Condition: "Diabetes"] D
    where D.clinicalStatus ~ 'active'
      and D.verificationStatus ~ 'confirmed')

// Initial Population: Adults 18-75 with diabetes
define "Initial Population":
  "In Demographic"
    and "Has Diabetes"

// Denominator: Same as Initial Population
define "Denominator":
  "Initial Population"

// Most recent HbA1c test
define "Most Recent HbA1c":
  Last([Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"
    sort by effective)

// Numerator: HbA1c < 8.0%
define "Numerator":
  "Most Recent HbA1c".value < 8.0 '%'

// Denominator Exclusions: None for this measure
define "Denominator Exclusions":
  false
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.464.1003.103.12.1001', name: 'Diabetes', codeSystem: 'SNOMED-CT' },
      { oid: '2.16.840.1.113883.3.464.1003.198.12.1013', name: 'HbA1c Laboratory Test', codeSystem: 'LOINC' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 18-75 with diabetes diagnosis',
      denominator: 'Initial Population',
      numerator: 'Most recent HbA1c < 8.0%',
      denominatorExclusions: 'None',
    },
    tags: ['diabetes', 'chronic disease', 'lab test', 'hedis'],
    complexity: 'intermediate',
    estimatedPatients: '~10-15% of adult panel',
  },

  {
    id: 'tpl-bp-control',
    name: 'Hypertension: Blood Pressure Control',
    category: 'CHRONIC_DISEASE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients with hypertension who had adequately controlled blood pressure (<140/90 mmHg) during the measurement period.',
    clinicalRationale: 'Blood pressure control reduces risk of stroke, heart attack, kidney disease, and other cardiovascular complications.',
    hedisAligned: 'CBP - Controlling Blood Pressure',
    cqlTemplate: `library BloodPressureControl version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Essential Hypertension": 'urn:oid:2.16.840.1.113883.3.464.1003.104.12.1011'
valueset "Blood Pressure Measurement": 'urn:oid:2.16.840.1.113883.3.464.1003.118.12.1035'

// Code Systems
codesystem "LOINC": 'http://loinc.org'

// Codes
code "Systolic BP": '8480-6' from "LOINC"
code "Diastolic BP": '8462-4' from "LOINC"

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Age 18-85 at start of measurement period
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 85

// Has hypertension diagnosis
define "Has Hypertension":
  exists([Condition: "Essential Hypertension"] H
    where H.clinicalStatus ~ 'active'
      and H.verificationStatus ~ 'confirmed')

// Initial Population
define "Initial Population":
  "In Demographic"
    and "Has Hypertension"

// Denominator
define "Denominator":
  "Initial Population"

// Most recent BP reading
define "Most Recent BP":
  Last([Observation] O
    where O.code in "Blood Pressure Measurement"
      and O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"
    sort by effective)

// Extract systolic and diastolic from components
define "Systolic Value":
  singleton from (
    "Most Recent BP".component C
      where C.code ~ "Systolic BP"
    return C.value as Quantity
  )

define "Diastolic Value":
  singleton from (
    "Most Recent BP".component C
      where C.code ~ "Diastolic BP"
    return C.value as Quantity
  )

// Numerator: BP < 140/90
define "Numerator":
  "Systolic Value" < 140 'mm[Hg]'
    and "Diastolic Value" < 90 'mm[Hg]'

// Denominator Exclusions
define "Denominator Exclusions":
  false
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.464.1003.104.12.1011', name: 'Essential Hypertension', codeSystem: 'ICD-10' },
      { oid: '2.16.840.1.113883.3.464.1003.118.12.1035', name: 'Blood Pressure Measurement', codeSystem: 'LOINC' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 18-85 with hypertension diagnosis',
      denominator: 'Initial Population',
      numerator: 'Most recent BP < 140/90 mmHg',
    },
    tags: ['hypertension', 'chronic disease', 'vital signs', 'hedis'],
    complexity: 'intermediate',
    estimatedPatients: '~25-30% of adult panel',
  },

  {
    id: 'tpl-breast-cancer-screening',
    name: 'Breast Cancer Screening',
    category: 'PREVENTIVE_CARE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of women 50-74 who had a mammogram to screen for breast cancer in the past 2 years.',
    clinicalRationale: 'Regular mammography screening can detect breast cancer early when treatment is most effective.',
    hedisAligned: 'BCS - Breast Cancer Screening',
    cqlTemplate: `library BreastCancerScreening version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Mammography": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1018'
valueset "Bilateral Mastectomy": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1005'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Female patients 50-74
define "In Demographic":
  Patient.gender = 'female'
    and AgeInYearsAt(end of "Measurement Period") >= 50
    and AgeInYearsAt(end of "Measurement Period") <= 74

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// Had mammogram in past 27 months (allows for scheduling flexibility)
define "Has Mammogram":
  exists([Procedure: "Mammography"] M
    where M.status = 'completed'
      and M.performed during Interval[start of "Measurement Period" - 15 months, end of "Measurement Period"])
  or exists([DiagnosticReport: "Mammography"] DR
    where DR.status in {'final', 'amended'}
      and DR.effective during Interval[start of "Measurement Period" - 15 months, end of "Measurement Period"])

// Numerator
define "Numerator":
  "Has Mammogram"

// History of bilateral mastectomy
define "Bilateral Mastectomy History":
  exists([Procedure: "Bilateral Mastectomy"] P
    where P.status = 'completed'
      and P.performed before end of "Measurement Period")

// Denominator Exclusions
define "Denominator Exclusions":
  "Bilateral Mastectomy History"
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.464.1003.108.12.1018', name: 'Mammography', codeSystem: 'CPT/HCPCS' },
      { oid: '2.16.840.1.113883.3.464.1003.198.12.1005', name: 'Bilateral Mastectomy', codeSystem: 'ICD-10-PCS' },
    ],
    populationCriteria: {
      initialPopulation: 'Women 50-74 years of age',
      denominator: 'Initial Population',
      numerator: 'Mammogram within 27 months',
      denominatorExclusions: 'Bilateral mastectomy',
    },
    tags: ['screening', 'preventive', 'womens health', 'hedis'],
    complexity: 'basic',
    estimatedPatients: '~15-20% of female panel',
  },

  {
    id: 'tpl-colorectal-screening',
    name: 'Colorectal Cancer Screening',
    category: 'PREVENTIVE_CARE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of adults 45-75 who had appropriate screening for colorectal cancer.',
    clinicalRationale: 'Colorectal cancer screening can prevent cancer through polyp removal and detect cancer early.',
    hedisAligned: 'COL - Colorectal Cancer Screening',
    cqlTemplate: `library ColorectalCancerScreening version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Colonoscopy": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1020'
valueset "Fecal Occult Blood Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1011'
valueset "FIT-DNA Test": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1039'
valueset "Flexible Sigmoidoscopy": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1010'
valueset "CT Colonography": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1038'
valueset "Colorectal Cancer": 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1001'
valueset "Total Colectomy": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1019'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Adults 45-75
define "In Demographic":
  AgeInYearsAt(end of "Measurement Period") >= 45
    and AgeInYearsAt(end of "Measurement Period") <= 75

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// Colonoscopy within 10 years
define "Has Colonoscopy":
  exists([Procedure: "Colonoscopy"] C
    where C.status = 'completed'
      and C.performed during Interval[start of "Measurement Period" - 10 years, end of "Measurement Period"])

// FOBT within 1 year
define "Has FOBT":
  exists([Observation: "Fecal Occult Blood Test"] F
    where F.status in {'final', 'amended'}
      and F.effective during "Measurement Period")

// FIT-DNA within 3 years
define "Has FIT DNA":
  exists([Observation: "FIT-DNA Test"] F
    where F.status in {'final', 'amended'}
      and F.effective during Interval[start of "Measurement Period" - 3 years, end of "Measurement Period"])

// Flexible sigmoidoscopy within 5 years
define "Has Sigmoidoscopy":
  exists([Procedure: "Flexible Sigmoidoscopy"] S
    where S.status = 'completed'
      and S.performed during Interval[start of "Measurement Period" - 5 years, end of "Measurement Period"])

// CT Colonography within 5 years
define "Has CT Colonography":
  exists([Procedure: "CT Colonography"] CT
    where CT.status = 'completed'
      and CT.performed during Interval[start of "Measurement Period" - 5 years, end of "Measurement Period"])

// Numerator: Any appropriate screening
define "Numerator":
  "Has Colonoscopy"
    or "Has FOBT"
    or "Has FIT DNA"
    or "Has Sigmoidoscopy"
    or "Has CT Colonography"

// History of colorectal cancer or total colectomy
define "Denominator Exclusions":
  exists([Condition: "Colorectal Cancer"] CRC
    where CRC.clinicalStatus ~ 'active')
  or exists([Procedure: "Total Colectomy"] TC
    where TC.status = 'completed'
      and TC.performed before end of "Measurement Period")
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.464.1003.108.12.1020', name: 'Colonoscopy', codeSystem: 'CPT' },
      { oid: '2.16.840.1.113883.3.464.1003.198.12.1011', name: 'Fecal Occult Blood Test', codeSystem: 'LOINC' },
      { oid: '2.16.840.1.113883.3.464.1003.108.12.1039', name: 'FIT-DNA Test', codeSystem: 'LOINC' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 45-75 years of age',
      denominator: 'Initial Population',
      numerator: 'Colonoscopy (10 yrs), FOBT (1 yr), FIT-DNA (3 yrs), or Sigmoidoscopy (5 yrs)',
      denominatorExclusions: 'History of colorectal cancer or total colectomy',
    },
    tags: ['screening', 'preventive', 'cancer', 'hedis'],
    complexity: 'intermediate',
    estimatedPatients: '~40-50% of adult panel',
  },

  {
    id: 'tpl-depression-screening',
    name: 'Depression Screening (PHQ-9)',
    category: 'BEHAVIORAL_HEALTH',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients 12 and older screened for depression using PHQ-9 with follow-up plan documented.',
    clinicalRationale: 'Depression is highly prevalent and often undiagnosed. Screening enables early intervention and treatment.',
    cmsAligned: 'CMS2v11 - Preventive Care and Screening: Screening for Depression',
    cqlTemplate: `library DepressionScreening version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "PHQ-9 Tool": 'urn:oid:2.16.840.1.113883.3.600.145'
valueset "Depression Diagnosis": 'urn:oid:2.16.840.1.113883.3.600.146'

// Codes
codesystem "LOINC": 'http://loinc.org'
code "PHQ-9 Score": '44249-1' from "LOINC"

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Age 12 and older
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 12

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// PHQ-9 screening performed
define "PHQ9 Screening":
  [Observation: "PHQ-9 Score"] O
    where O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"

define "Has PHQ9 Screening":
  exists("PHQ9 Screening")

// Most recent PHQ-9 score
define "Most Recent PHQ9":
  Last("PHQ9 Screening" S
    sort by effective)

// Positive screen (score >= 10)
define "Positive Screen":
  "Most Recent PHQ9".value >= 10

// Follow-up documented for positive screen
define "Follow Up Documented":
  exists([CarePlan] CP
    where CP.status = 'active'
      and CP.created during "Measurement Period"
      and exists(CP.category C where C ~ 'mental-health'))
  or exists([ServiceRequest] SR
    where SR.status in {'active', 'completed'}
      and SR.authoredOn during "Measurement Period"
      and SR.category ~ 'mental-health')

// Numerator: Screened AND (negative OR positive with follow-up)
define "Numerator":
  "Has PHQ9 Screening"
    and (not "Positive Screen" or "Follow Up Documented")

// Already diagnosed with depression
define "Denominator Exclusions":
  exists([Condition: "Depression Diagnosis"] D
    where D.clinicalStatus ~ 'active'
      and D.onset before start of "Measurement Period")
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.600.145', name: 'PHQ-9 Tool', codeSystem: 'LOINC' },
      { oid: '2.16.840.1.113883.3.600.146', name: 'Depression Diagnosis', codeSystem: 'ICD-10' },
    ],
    populationCriteria: {
      initialPopulation: 'Patients 12 years and older',
      denominator: 'Initial Population',
      numerator: 'PHQ-9 screening performed with follow-up if positive',
      denominatorExclusions: 'Pre-existing depression diagnosis',
    },
    tags: ['screening', 'behavioral health', 'depression', 'phq-9'],
    complexity: 'intermediate',
    estimatedPatients: '~90% of panel (all ages 12+)',
  },

  {
    id: 'tpl-statin-therapy',
    name: 'Statin Therapy for CVD Prevention',
    category: 'MEDICATION_MANAGEMENT',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients with cardiovascular disease or diabetes who are on statin therapy.',
    clinicalRationale: 'Statins reduce cardiovascular events and mortality in high-risk patients.',
    hedisAligned: 'SPC - Statin Therapy for Patients with Cardiovascular Disease',
    cqlTemplate: `library StatinTherapyCVD version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Statin Medications": 'urn:oid:2.16.840.1.113883.3.526.3.1003'
valueset "Atherosclerotic Cardiovascular Disease": 'urn:oid:2.16.840.1.113883.3.464.1003.104.12.1003'
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "Statin Allergy": 'urn:oid:2.16.840.1.113883.3.526.3.1002'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Adults 21-75
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 21
    and AgeInYearsAt(start of "Measurement Period") <= 75

// Has ASCVD
define "Has ASCVD":
  exists([Condition: "Atherosclerotic Cardiovascular Disease"] CVD
    where CVD.clinicalStatus ~ 'active'
      and CVD.verificationStatus ~ 'confirmed')

// Has diabetes
define "Has Diabetes":
  exists([Condition: "Diabetes"] D
    where D.clinicalStatus ~ 'active'
      and D.verificationStatus ~ 'confirmed')

// Initial Population: High-risk patients
define "Initial Population":
  "In Demographic"
    and ("Has ASCVD" or "Has Diabetes")

// Denominator
define "Denominator":
  "Initial Population"

// Active statin prescription
define "On Statin":
  exists([MedicationRequest: "Statin Medications"] MR
    where MR.status = 'active'
      and MR.authoredOn during "Measurement Period")
  or exists([MedicationStatement: "Statin Medications"] MS
    where MS.status = 'active'
      and MS.effective overlaps "Measurement Period")

// Numerator
define "Numerator":
  "On Statin"

// Statin allergy or intolerance
define "Denominator Exclusions":
  exists([AllergyIntolerance: "Statin Allergy"] A
    where A.clinicalStatus ~ 'active')
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.526.3.1003', name: 'Statin Medications', codeSystem: 'RxNorm' },
      { oid: '2.16.840.1.113883.3.464.1003.104.12.1003', name: 'ASCVD', codeSystem: 'ICD-10' },
      { oid: '2.16.840.1.113883.3.464.1003.103.12.1001', name: 'Diabetes', codeSystem: 'SNOMED-CT' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 21-75 with ASCVD or diabetes',
      denominator: 'Initial Population',
      numerator: 'Active statin prescription',
      denominatorExclusions: 'Statin allergy or documented intolerance',
    },
    tags: ['medication', 'cardiovascular', 'statin', 'hedis'],
    complexity: 'intermediate',
    estimatedPatients: '~15-25% of adult panel',
  },

  {
    id: 'tpl-immunization-flu',
    name: 'Annual Influenza Immunization',
    category: 'PREVENTIVE_CARE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients 6 months and older who received an influenza immunization during the flu season.',
    clinicalRationale: 'Annual flu vaccination reduces illness, hospitalization, and death from influenza.',
    cqlTemplate: `library InfluenzaImmunization version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Influenza Vaccine": 'urn:oid:2.16.840.1.113883.3.526.3.1254'
valueset "Influenza Vaccine Administered": 'urn:oid:2.16.840.1.113883.3.526.3.402'
valueset "Egg Allergy": 'urn:oid:2.16.840.1.113883.3.526.3.1253'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Age 6 months and older
define "In Demographic":
  AgeInMonthsAt(start of "Measurement Period") >= 6

// Flu season (October 1 - March 31)
define "Flu Season":
  Interval[
    DateTime(year from start of "Measurement Period", 10, 1),
    DateTime(year from start of "Measurement Period" + 1, 3, 31)
  ]

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// Received flu vaccine
define "Has Flu Vaccine":
  exists([Immunization: "Influenza Vaccine"] I
    where I.status = 'completed'
      and I.occurrence during "Flu Season")
  or exists([Procedure: "Influenza Vaccine Administered"] P
    where P.status = 'completed'
      and P.performed during "Flu Season")

// Numerator
define "Numerator":
  "Has Flu Vaccine"

// Severe egg allergy (contraindication for most flu vaccines)
define "Denominator Exclusions":
  exists([AllergyIntolerance: "Egg Allergy"] A
    where A.clinicalStatus ~ 'active'
      and A.criticality = 'high')
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.526.3.1254', name: 'Influenza Vaccine', codeSystem: 'CVX' },
      { oid: '2.16.840.1.113883.3.526.3.402', name: 'Influenza Vaccine Administered', codeSystem: 'CPT' },
    ],
    populationCriteria: {
      initialPopulation: 'Patients 6 months and older',
      denominator: 'Initial Population',
      numerator: 'Flu vaccine administered during flu season',
      denominatorExclusions: 'Severe egg allergy',
    },
    tags: ['immunization', 'preventive', 'flu', 'vaccine'],
    complexity: 'basic',
    estimatedPatients: '~100% of panel eligible',
  },

  {
    id: 'tpl-tobacco-cessation',
    name: 'Tobacco Use Screening & Cessation',
    category: 'PREVENTIVE_CARE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients 18+ screened for tobacco use and, if identified as a tobacco user, received cessation intervention.',
    clinicalRationale: 'Tobacco cessation counseling and pharmacotherapy significantly increase quit rates.',
    cqlTemplate: `library TobaccoCessation version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Tobacco Use Screening": 'urn:oid:2.16.840.1.113883.3.526.3.1278'
valueset "Tobacco User": 'urn:oid:2.16.840.1.113883.3.526.3.1170'
valueset "Tobacco Non-User": 'urn:oid:2.16.840.1.113883.3.526.3.1189'
valueset "Tobacco Cessation Counseling": 'urn:oid:2.16.840.1.113883.3.526.3.509'
valueset "Tobacco Cessation Medications": 'urn:oid:2.16.840.1.113883.3.526.3.1190'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Adults 18+
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 18

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// Tobacco screening performed
define "Tobacco Screening":
  [Observation: "Tobacco Use Screening"] O
    where O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"

define "Has Screening":
  exists("Tobacco Screening")

// Identified as tobacco user
define "Is Tobacco User":
  exists("Tobacco Screening" S
    where S.value in "Tobacco User")

// Cessation intervention provided
define "Cessation Intervention":
  exists([Procedure: "Tobacco Cessation Counseling"] C
    where C.status = 'completed'
      and C.performed during "Measurement Period")
  or exists([MedicationRequest: "Tobacco Cessation Medications"] M
    where M.status = 'active'
      and M.authoredOn during "Measurement Period")

// Numerator: Screened AND (non-user OR user with intervention)
define "Numerator":
  "Has Screening"
    and (not "Is Tobacco User" or "Cessation Intervention")

// No exclusions for this measure
define "Denominator Exclusions":
  false
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.526.3.1278', name: 'Tobacco Use Screening', codeSystem: 'LOINC' },
      { oid: '2.16.840.1.113883.3.526.3.509', name: 'Tobacco Cessation Counseling', codeSystem: 'CPT' },
      { oid: '2.16.840.1.113883.3.526.3.1190', name: 'Tobacco Cessation Medications', codeSystem: 'RxNorm' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 18 years and older',
      denominator: 'Initial Population',
      numerator: 'Screened for tobacco use; if user, received cessation intervention',
    },
    tags: ['screening', 'preventive', 'tobacco', 'counseling'],
    complexity: 'intermediate',
    estimatedPatients: '~100% of adult panel',
  },

  {
    id: 'tpl-bmi-screening',
    name: 'BMI Screening and Follow-Up',
    category: 'PREVENTIVE_CARE',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients 18+ with BMI documented and, if outside normal parameters, a follow-up plan documented.',
    clinicalRationale: 'Obesity is a major risk factor for chronic diseases. Identification enables targeted interventions.',
    cqlTemplate: `library BMIScreening version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Codes
codesystem "LOINC": 'http://loinc.org'
code "BMI": '39156-5' from "LOINC"

// Value Sets
valueset "Follow Up for BMI": 'urn:oid:2.16.840.1.113883.3.600.1.1525'
valueset "Referrals for BMI": 'urn:oid:2.16.840.1.113883.3.600.1.1527'

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Adults 18+
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 18

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// BMI documented
define "BMI Observation":
  [Observation: "BMI"] O
    where O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"

define "Has BMI":
  exists("BMI Observation")

// Most recent BMI
define "Most Recent BMI":
  Last("BMI Observation" B
    sort by effective)

// BMI outside normal range (18.5-25)
define "Abnormal BMI":
  "Most Recent BMI".value < 18.5 'kg/m2'
    or "Most Recent BMI".value >= 25 'kg/m2'

// Follow-up plan documented
define "Follow Up Plan":
  exists([Procedure: "Follow Up for BMI"] P
    where P.status = 'completed'
      and P.performed during "Measurement Period")
  or exists([ServiceRequest: "Referrals for BMI"] R
    where R.status in {'active', 'completed'}
      and R.authoredOn during "Measurement Period")
  or exists([CarePlan] CP
    where CP.status = 'active'
      and CP.created during "Measurement Period"
      and exists(CP.category C where C ~ 'nutrition'))

// Numerator: BMI documented AND (normal OR abnormal with follow-up)
define "Numerator":
  "Has BMI"
    and (not "Abnormal BMI" or "Follow Up Plan")

// Medical reasons for no BMI (e.g., palliative care)
define "Denominator Exclusions":
  false
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.600.1.1525', name: 'Follow Up for BMI', codeSystem: 'CPT' },
      { oid: '2.16.840.1.113883.3.600.1.1527', name: 'Referrals for BMI', codeSystem: 'SNOMED-CT' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 18 years and older',
      denominator: 'Initial Population',
      numerator: 'BMI documented; if abnormal, follow-up plan documented',
    },
    tags: ['screening', 'preventive', 'bmi', 'obesity'],
    complexity: 'intermediate',
    estimatedPatients: '~100% of adult panel',
  },

  {
    id: 'tpl-falls-risk',
    name: 'Falls Risk Assessment (65+)',
    category: 'GERIATRIC',
    specialty: 'PRIMARY_CARE',
    description: 'Measures the percentage of patients 65+ who were screened for future fall risk and received appropriate interventions if at risk.',
    clinicalRationale: 'Falls are a leading cause of injury in older adults. Risk assessment enables prevention.',
    cqlTemplate: `library FallsRiskAssessment version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Value Sets
valueset "Falls Screening": 'urn:oid:2.16.840.1.113883.3.464.1003.118.12.1028'
valueset "Fall Risk Assessment": 'urn:oid:2.16.840.1.113883.3.464.1003.118.12.1034'
valueset "Falls Care Plan": 'urn:oid:2.16.840.1.113883.3.464.1003.118.12.1032'

// Codes
codesystem "LOINC": 'http://loinc.org'
code "Falls Risk Score": '73830-2' from "LOINC"

// Parameters
parameter "Measurement Period" Interval<DateTime>

context Patient

// Adults 65+
define "In Demographic":
  AgeInYearsAt(start of "Measurement Period") >= 65

// Initial Population
define "Initial Population":
  "In Demographic"

// Denominator
define "Denominator":
  "Initial Population"

// Falls screening performed
define "Falls Screening":
  [Observation: "Falls Risk Score"] O
    where O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"

define "Has Screening":
  exists("Falls Screening")

// At risk for falls (score indicates elevated risk)
define "At Risk":
  exists("Falls Screening" S
    where S.interpretation ~ 'high')

// Falls prevention intervention
define "Prevention Intervention":
  exists([CarePlan: "Falls Care Plan"] CP
    where CP.status = 'active'
      and CP.created during "Measurement Period")
  or exists([Procedure: "Fall Risk Assessment"] P
    where P.status = 'completed'
      and P.performed during "Measurement Period")

// Numerator: Screened AND (not at risk OR at risk with intervention)
define "Numerator":
  "Has Screening"
    and (not "At Risk" or "Prevention Intervention")

// No exclusions
define "Denominator Exclusions":
  false
`,
    requiredValueSets: [
      { oid: '2.16.840.1.113883.3.464.1003.118.12.1028', name: 'Falls Screening', codeSystem: 'LOINC' },
      { oid: '2.16.840.1.113883.3.464.1003.118.12.1032', name: 'Falls Care Plan', codeSystem: 'SNOMED-CT' },
    ],
    populationCriteria: {
      initialPopulation: 'Adults 65 years and older',
      denominator: 'Initial Population',
      numerator: 'Falls risk screening; if at risk, intervention documented',
    },
    tags: ['screening', 'geriatric', 'falls', 'safety'],
    complexity: 'intermediate',
    estimatedPatients: '~20-30% of panel (65+)',
  },
];

/**
 * Get templates by category
 */
export function getTemplatesByCategory(category: MeasureTemplateCategory): MeasureTemplate[] {
  return PRIMARY_CARE_TEMPLATES.filter(t => t.category === category);
}

/**
 * Get templates by specialty
 */
export function getTemplatesBySpecialty(specialty: ProviderSpecialty): MeasureTemplate[] {
  return PRIMARY_CARE_TEMPLATES.filter(t => t.specialty === specialty || t.specialty === 'ALL');
}

/**
 * Get templates by complexity
 */
export function getTemplatesByComplexity(complexity: 'basic' | 'intermediate' | 'advanced'): MeasureTemplate[] {
  return PRIMARY_CARE_TEMPLATES.filter(t => t.complexity === complexity);
}

/**
 * Search templates by text
 */
export function searchTemplates(query: string): MeasureTemplate[] {
  const lowerQuery = query.toLowerCase();
  return PRIMARY_CARE_TEMPLATES.filter(t =>
    t.name.toLowerCase().includes(lowerQuery) ||
    t.description.toLowerCase().includes(lowerQuery) ||
    t.tags.some(tag => tag.toLowerCase().includes(lowerQuery))
  );
}
