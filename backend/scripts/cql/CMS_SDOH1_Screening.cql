library CMS_SDOH1_Screening version '2024'

/*
 * CMS SDOH-1: Social Determinants of Health (SDOH) Screening Rate
 *
 * Description:
 * Percentage of patients 18 years and older who were screened for all five
 * Health-Related Social Needs (HRSN) domains at least once during the measurement period.
 *
 * Measure Steward: Centers for Medicare & Medicaid Services (CMS)
 * CMS Measure ID: SDOH-1 (CMS1238v1)
 *
 * Five HRSN Domains (all must be screened):
 * 1. Food Insecurity
 * 2. Housing Instability
 * 3. Transportation Needs
 * 4. Utility Difficulties
 * 5. Interpersonal Safety
 *
 * Population Criteria:
 * - Initial Population: Patients 18 years and older with at least one qualifying encounter
 * - Denominator: Initial population
 * - Numerator: Patients screened for all 5 HRSN domains
 * - Exclusions: Patients in hospice or who declined screening
 *
 * Regulatory Context:
 * - Mandatory for inpatient as of 2024
 * - Expanding to outpatient settings in 2025
 * - Required for MSSP ACOs via APP Plus starting 2028
 *
 * Data Source: EHR, Claims, QuestionnaireResponse
 */

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

// Code systems
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMED-CT": 'http://snomed.info/sct'
codesystem "ICD-10-CM": 'http://hl7.org/fhir/sid/icd-10-cm'
codesystem "CPT": 'http://www.ama-assn.org/go/cpt'
codesystem "HCPCS": 'http://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets'

// ============================================================================
// HRSN DOMAIN 1: FOOD INSECURITY
// ============================================================================
valueset "Food Insecurity Screening LOINC": {
  Code '88122-7' from "LOINC" display 'Within the past 12 months you worried food would run out',
  Code '88123-5' from "LOINC" display 'Within the past 12 months food you bought just did not last',
  Code '88124-3' from "LOINC" display 'Food insecurity risk [HVS]',
  Code '95610-9' from "LOINC" display 'At risk for food insecurity',
  Code '98978-6' from "LOINC" display 'Food insecurity screening panel'
}

valueset "Food Insecurity Positive LOINC": {
  Code 'LA28397-0' from "LOINC" display 'Often true',
  Code 'LA28398-8' from "LOINC" display 'Sometimes true'
}

// ============================================================================
// HRSN DOMAIN 2: HOUSING INSTABILITY
// ============================================================================
valueset "Housing Instability Screening LOINC": {
  Code '71802-3' from "LOINC" display 'Housing status',
  Code '93033-9' from "LOINC" display 'Are you worried about losing your housing',
  Code '98980-2' from "LOINC" display 'Housing instability screening panel',
  Code '56799-0' from "LOINC" display 'Address [Type] - temporary'
}

valueset "Housing Instability Z Codes": {
  Code 'Z59.00' from "ICD-10-CM" display 'Homelessness unspecified',
  Code 'Z59.01' from "ICD-10-CM" display 'Sheltered homelessness',
  Code 'Z59.02' from "ICD-10-CM" display 'Unsheltered homelessness',
  Code 'Z59.10' from "ICD-10-CM" display 'Inadequate housing unspecified',
  Code 'Z59.11' from "ICD-10-CM" display 'Inadequate housing environmental temperature',
  Code 'Z59.12' from "ICD-10-CM" display 'Inadequate housing utilities',
  Code 'Z59.19' from "ICD-10-CM" display 'Other inadequate housing'
}

// ============================================================================
// HRSN DOMAIN 3: TRANSPORTATION
// ============================================================================
valueset "Transportation Screening LOINC": {
  Code '93030-5' from "LOINC" display 'Transportation needs',
  Code '98979-4' from "LOINC" display 'Transportation access screening panel'
}

valueset "Transportation Z Codes": {
  Code 'Z59.82' from "ICD-10-CM" display 'Transportation insecurity'
}

// ============================================================================
// HRSN DOMAIN 4: UTILITY DIFFICULTIES
// ============================================================================
valueset "Utility Screening LOINC": {
  Code '93031-3' from "LOINC" display 'Utility needs - threatened or shut off',
  Code '98977-8' from "LOINC" display 'Utility needs screening panel'
}

valueset "Utility Z Codes": {
  Code 'Z59.12' from "ICD-10-CM" display 'Inadequate housing utilities'
}

// ============================================================================
// HRSN DOMAIN 5: INTERPERSONAL SAFETY
// ============================================================================
valueset "Interpersonal Safety Screening LOINC": {
  Code '93038-8' from "LOINC" display 'Physically hurt by anyone',
  Code '93039-6' from "LOINC" display 'Insulted or talked down to by anyone',
  Code '93040-4' from "LOINC" display 'Threatened with harm by anyone',
  Code '93041-2' from "LOINC" display 'Screamed or cursed at by anyone',
  Code '98981-0' from "LOINC" display 'Interpersonal safety screening panel'
}

valueset "Interpersonal Safety Z Codes": {
  Code 'Z91.410' from "ICD-10-CM" display 'Personal history of adult physical and sexual abuse',
  Code 'Z91.411' from "ICD-10-CM" display 'Personal history of adult psychological abuse',
  Code 'Z91.412' from "ICD-10-CM" display 'Personal history of adult neglect',
  Code 'Z62.810' from "ICD-10-CM" display 'Personal history of physical and sexual abuse in childhood'
}

// ============================================================================
// SCREENING TOOLS
// ============================================================================
valueset "SDOH Risk Assessment HCPCS": {
  Code 'G0136' from "HCPCS" display 'Administration of a standardized, evidence-based SDOH risk assessment tool'
}

valueset "AHC-HRSN Screening": {
  Code '98976-0' from "LOINC" display 'AHC HRSN screening tool panel'
}

valueset "PRAPARE Screening": {
  Code '93025-5' from "LOINC" display 'PRAPARE protocol'
}

// ============================================================================
// QUALIFYING ENCOUNTERS
// ============================================================================
valueset "Office Visit": {
  Code '99201' from "CPT" display 'Office visit new patient level 1',
  Code '99202' from "CPT" display 'Office visit new patient level 2',
  Code '99203' from "CPT" display 'Office visit new patient level 3',
  Code '99204' from "CPT" display 'Office visit new patient level 4',
  Code '99205' from "CPT" display 'Office visit new patient level 5',
  Code '99211' from "CPT" display 'Office visit established patient minimal',
  Code '99212' from "CPT" display 'Office visit established patient level 2',
  Code '99213' from "CPT" display 'Office visit established patient level 3',
  Code '99214' from "CPT" display 'Office visit established patient level 4',
  Code '99215' from "CPT" display 'Office visit established patient level 5'
}

valueset "Annual Wellness Visit": {
  Code 'G0438' from "HCPCS" display 'Annual wellness visit first',
  Code 'G0439' from "HCPCS" display 'Annual wellness visit subsequent'
}

valueset "Hospice": {
  Code '385763009' from "SNOMED-CT" display 'Hospice care'
}

// ============================================================================
// PARAMETERS
// ============================================================================
parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00.0, @2024-12-31T23:59:59.9]

context Patient

// ============================================================================
// HELPER DEFINITIONS
// ============================================================================
define "Patient Age in Years":
  AgeInYearsAt(end of "Measurement Period")

define "Has Qualifying Encounter":
  exists (
    ([Encounter: "Office Visit"] union [Encounter: "Annual Wellness Visit"]) Encounter
      where Encounter.status = 'finished'
        and Encounter.period.start during "Measurement Period"
  )

define "Has Hospice":
  exists (
    [Condition: "Hospice"] HospiceCondition
      where HospiceCondition.clinicalStatus.coding[0].code = 'active'
  )

// ============================================================================
// DOMAIN SCREENING CHECKS
// ============================================================================

// Domain 1: Food Insecurity Screening
define "Food Insecurity Screened":
  exists (
    [Observation: "Food Insecurity Screening LOINC"] FoodObs
      where FoodObs.status in {'final', 'amended', 'corrected'}
        and FoodObs.effective as dateTime during "Measurement Period"
  )
  or exists (
    [QuestionnaireResponse] QR
      where QR.status = 'completed'
        and QR.authored during "Measurement Period"
        and exists (
          QR.item QRItem where QRItem.linkId in {'ahc-food-1', 'ahc-food-2', 'prapare-food-1'}
        )
  )

// Domain 2: Housing Instability Screening
define "Housing Instability Screened":
  exists (
    [Observation: "Housing Instability Screening LOINC"] HousingObs
      where HousingObs.status in {'final', 'amended', 'corrected'}
        and HousingObs.effective as dateTime during "Measurement Period"
  )
  or exists (
    [Condition: "Housing Instability Z Codes"] HousingCondition
      where HousingCondition.recordedDate during "Measurement Period"
  )

// Domain 3: Transportation Screening
define "Transportation Screened":
  exists (
    [Observation: "Transportation Screening LOINC"] TransportObs
      where TransportObs.status in {'final', 'amended', 'corrected'}
        and TransportObs.effective as dateTime during "Measurement Period"
  )
  or exists (
    [Condition: "Transportation Z Codes"] TransportCondition
      where TransportCondition.recordedDate during "Measurement Period"
  )

// Domain 4: Utility Screening
define "Utility Screened":
  exists (
    [Observation: "Utility Screening LOINC"] UtilityObs
      where UtilityObs.status in {'final', 'amended', 'corrected'}
        and UtilityObs.effective as dateTime during "Measurement Period"
  )

// Domain 5: Interpersonal Safety Screening
define "Interpersonal Safety Screened":
  exists (
    [Observation: "Interpersonal Safety Screening LOINC"] SafetyObs
      where SafetyObs.status in {'final', 'amended', 'corrected'}
        and SafetyObs.effective as dateTime during "Measurement Period"
  )

// All 5 domains screened
define "All Domains Screened":
  "Food Insecurity Screened"
    and "Housing Instability Screened"
    and "Transportation Screened"
    and "Utility Screened"
    and "Interpersonal Safety Screened"

// Screening tool administered (alternative path)
define "SDOH Screening Tool Administered":
  exists (
    [Procedure: "SDOH Risk Assessment HCPCS"] SdohProc
      where SdohProc.status = 'completed'
        and SdohProc.performed as dateTime during "Measurement Period"
  )
  or exists (
    [QuestionnaireResponse] QR
      where QR.questionnaire in {'AHC-HRSN', 'PRAPARE', 'sdoh-screening'}
        and QR.status = 'completed'
        and QR.authored during "Measurement Period"
  )

// ============================================================================
// POPULATION DEFINITIONS
// ============================================================================

// Initial Population: Adults 18+ with qualifying encounter
define "Initial Population":
  "Patient Age in Years" >= 18
    and "Has Qualifying Encounter"

// Denominator: Same as initial population, excluding hospice
define "Denominator":
  "Initial Population"
    and not "Has Hospice"

// Denominator Exclusions
define "Denominator Exclusion":
  "Has Hospice"

// Numerator: All 5 HRSN domains screened OR validated screening tool administered
define "Numerator":
  "Denominator"
    and ("All Domains Screened" or "SDOH Screening Tool Administered")

// ============================================================================
// CARE GAP IDENTIFICATION
// ============================================================================
define "Care Gap":
  "Denominator" and not "Numerator"

define "Missing Domains":
  {
    foodInsecurity: not "Food Insecurity Screened",
    housingInstability: not "Housing Instability Screened",
    transportation: not "Transportation Screened",
    utilities: not "Utility Screened",
    interpersonalSafety: not "Interpersonal Safety Screened"
  }

define "Care Gap Reason":
  if "Care Gap" then
    'SDOH screening incomplete - not all 5 HRSN domains assessed during measurement period'
  else
    null

define "Recommendation":
  if "Care Gap" then
    'Administer AHC-HRSN or PRAPARE screening tool to assess all 5 HRSN domains'
  else
    null

// Domain completion count
define "Domains Screened Count":
  (if "Food Insecurity Screened" then 1 else 0) +
  (if "Housing Instability Screened" then 1 else 0) +
  (if "Transportation Screened" then 1 else 0) +
  (if "Utility Screened" then 1 else 0) +
  (if "Interpersonal Safety Screened" then 1 else 0)

define "Completion Percentage":
  ("Domains Screened Count" / 5.0) * 100.0

// ============================================================================
// MEASURE SUMMARY
// ============================================================================
define "Measure Summary":
  {
    measureId: 'CMS_SDOH1',
    measureName: 'SDOH Screening Rate',
    measureVersion: '2024',
    patientId: Patient.id,
    measurementPeriod: "Measurement Period",
    inInitialPopulation: "Initial Population",
    inDenominator: "Denominator",
    inNumerator: "Numerator",
    careGap: "Care Gap",
    careGapReason: "Care Gap Reason",
    recommendation: "Recommendation",
    patientAge: "Patient Age in Years",
    domainsScreenedCount: "Domains Screened Count",
    completionPercentage: "Completion Percentage",
    foodInsecurityScreened: "Food Insecurity Screened",
    housingInstabilityScreened: "Housing Instability Screened",
    transportationScreened: "Transportation Screened",
    utilityScreened: "Utility Screened",
    interpersonalSafetyScreened: "Interpersonal Safety Screened",
    allDomainsScreened: "All Domains Screened",
    hasExclusion: "Denominator Exclusion"
  }
