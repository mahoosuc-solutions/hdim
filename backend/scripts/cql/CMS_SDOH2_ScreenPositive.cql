library CMS_SDOH2_ScreenPositive version '2024'

/*
 * CMS SDOH-2: Social Determinants of Health (SDOH) Screen Positive Rate
 *
 * Description:
 * Percentage of patients 18 years and older who were screened for HRSN domains
 * and who screened positive for at least one health-related social need.
 *
 * Measure Steward: Centers for Medicare & Medicaid Services (CMS)
 * CMS Measure ID: SDOH-2 (CMS1239v1)
 *
 * Positive Screen Criteria by Domain:
 * 1. Food Insecurity - "Often true" or "Sometimes true" to either screening question
 * 2. Housing Instability - "I do not have housing" or "I am worried about losing my housing"
 * 3. Transportation - "Yes" to transportation barrier question
 * 4. Utility Difficulties - "Yes" or "Already shut off" to utility question
 * 5. Interpersonal Safety - Any answer except "Never" to safety questions
 *
 * Population Criteria:
 * - Initial Population: Patients screened for at least one HRSN domain
 * - Denominator: Initial population with completed screening
 * - Numerator: Patients with at least one positive screen result
 *
 * Purpose:
 * This measure tracks the identification rate of HRSN needs in the population,
 * helping organizations understand the prevalence of social needs and
 * allocate intervention resources appropriately.
 *
 * Regulatory Context:
 * - Paired with SDOH-1 for complete SDOH quality reporting
 * - Required for MSSP ACOs via APP Plus starting 2028
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
// SCREENING CODES (Same as SDOH-1)
// ============================================================================
valueset "Food Insecurity Screening LOINC": {
  Code '88122-7' from "LOINC" display 'Food worry question',
  Code '88123-5' from "LOINC" display 'Food did not last question',
  Code '88124-3' from "LOINC" display 'Food insecurity risk'
}

valueset "Housing Instability Screening LOINC": {
  Code '71802-3' from "LOINC" display 'Housing status',
  Code '93033-9' from "LOINC" display 'Housing worry'
}

valueset "Transportation Screening LOINC": {
  Code '93030-5' from "LOINC" display 'Transportation needs'
}

valueset "Utility Screening LOINC": {
  Code '93031-3' from "LOINC" display 'Utility needs'
}

valueset "Interpersonal Safety Screening LOINC": {
  Code '93038-8' from "LOINC" display 'Physically hurt by anyone',
  Code '93039-6' from "LOINC" display 'Insulted or talked down to',
  Code '93040-4' from "LOINC" display 'Threatened with harm',
  Code '93041-2' from "LOINC" display 'Screamed or cursed at'
}

// ============================================================================
// Z CODES FOR DOCUMENTED NEEDS (Indicates positive screen)
// ============================================================================
valueset "Food Insecurity Z Codes": {
  Code 'Z59.41' from "ICD-10-CM" display 'Food insecurity',
  Code 'Z59.48' from "ICD-10-CM" display 'Other specified lack of adequate food'
}

valueset "Housing Instability Z Codes": {
  Code 'Z59.00' from "ICD-10-CM" display 'Homelessness unspecified',
  Code 'Z59.01' from "ICD-10-CM" display 'Sheltered homelessness',
  Code 'Z59.02' from "ICD-10-CM" display 'Unsheltered homelessness',
  Code 'Z59.10' from "ICD-10-CM" display 'Inadequate housing unspecified',
  Code 'Z59.811' from "ICD-10-CM" display 'Housing instability, housed'
}

valueset "Transportation Z Codes": {
  Code 'Z59.82' from "ICD-10-CM" display 'Transportation insecurity'
}

valueset "Utility Z Codes": {
  Code 'Z59.12' from "ICD-10-CM" display 'Inadequate housing utilities'
}

valueset "Interpersonal Safety Z Codes": {
  Code 'Z91.410' from "ICD-10-CM" display 'Personal history of abuse',
  Code 'Z91.411' from "ICD-10-CM" display 'Personal history of psychological abuse',
  Code 'Z62.810' from "ICD-10-CM" display 'Personal history of abuse in childhood'
}

valueset "Office Visit": {
  Code '99201' from "CPT" display 'Office visit new patient level 1',
  Code '99202' from "CPT" display 'Office visit new patient level 2',
  Code '99203' from "CPT" display 'Office visit new patient level 3',
  Code '99204' from "CPT" display 'Office visit new patient level 4',
  Code '99205' from "CPT" display 'Office visit new patient level 5',
  Code '99211' from "CPT" display 'Office visit established minimal',
  Code '99212' from "CPT" display 'Office visit established level 2',
  Code '99213' from "CPT" display 'Office visit established level 3',
  Code '99214' from "CPT" display 'Office visit established level 4',
  Code '99215' from "CPT" display 'Office visit established level 5'
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
    [Encounter: "Office Visit"] Encounter
      where Encounter.status = 'finished'
        and Encounter.period.start during "Measurement Period"
  )

define "Has Hospice":
  exists (
    [Condition: "Hospice"] HospiceCondition
      where HospiceCondition.clinicalStatus.coding[0].code = 'active'
  )

// ============================================================================
// SCREENING COMPLETION CHECKS
// ============================================================================
define "Food Insecurity Screened":
  exists (
    [Observation: "Food Insecurity Screening LOINC"] Obs
      where Obs.status in {'final', 'amended', 'corrected'}
        and Obs.effective as dateTime during "Measurement Period"
  )

define "Housing Instability Screened":
  exists (
    [Observation: "Housing Instability Screening LOINC"] Obs
      where Obs.status in {'final', 'amended', 'corrected'}
        and Obs.effective as dateTime during "Measurement Period"
  )

define "Transportation Screened":
  exists (
    [Observation: "Transportation Screening LOINC"] Obs
      where Obs.status in {'final', 'amended', 'corrected'}
        and Obs.effective as dateTime during "Measurement Period"
  )

define "Utility Screened":
  exists (
    [Observation: "Utility Screening LOINC"] Obs
      where Obs.status in {'final', 'amended', 'corrected'}
        and Obs.effective as dateTime during "Measurement Period"
  )

define "Interpersonal Safety Screened":
  exists (
    [Observation: "Interpersonal Safety Screening LOINC"] Obs
      where Obs.status in {'final', 'amended', 'corrected'}
        and Obs.effective as dateTime during "Measurement Period"
  )

define "Any Domain Screened":
  "Food Insecurity Screened"
    or "Housing Instability Screened"
    or "Transportation Screened"
    or "Utility Screened"
    or "Interpersonal Safety Screened"

// ============================================================================
// POSITIVE SCREEN DETECTION
// ============================================================================

// Domain 1: Food Insecurity Positive
define "Food Insecurity Positive":
  exists (
    [Observation: "Food Insecurity Screening LOINC"] FoodObs
      where FoodObs.status in {'final', 'amended', 'corrected'}
        and FoodObs.effective as dateTime during "Measurement Period"
        and (
          FoodObs.value as CodeableConcept in {
            Code 'LA28397-0' from "LOINC",  // Often true
            Code 'LA28398-8' from "LOINC"   // Sometimes true
          }
          or FoodObs.interpretation.coding[0].code = 'POS'
        )
  )
  or exists (
    [Condition: "Food Insecurity Z Codes"] FoodCondition
      where FoodCondition.recordedDate during "Measurement Period"
  )

// Domain 2: Housing Instability Positive
define "Housing Instability Positive":
  exists (
    [Observation: "Housing Instability Screening LOINC"] HousingObs
      where HousingObs.status in {'final', 'amended', 'corrected'}
        and HousingObs.effective as dateTime during "Measurement Period"
        and HousingObs.interpretation.coding[0].code = 'POS'
  )
  or exists (
    [Condition: "Housing Instability Z Codes"] HousingCondition
      where HousingCondition.recordedDate during "Measurement Period"
        and HousingCondition.clinicalStatus.coding[0].code = 'active'
  )

// Domain 3: Transportation Positive
define "Transportation Positive":
  exists (
    [Observation: "Transportation Screening LOINC"] TransportObs
      where TransportObs.status in {'final', 'amended', 'corrected'}
        and TransportObs.effective as dateTime during "Measurement Period"
        and (
          TransportObs.value as boolean = true
          or TransportObs.interpretation.coding[0].code = 'POS'
        )
  )
  or exists (
    [Condition: "Transportation Z Codes"] TransportCondition
      where TransportCondition.recordedDate during "Measurement Period"
        and TransportCondition.clinicalStatus.coding[0].code = 'active'
  )

// Domain 4: Utility Positive
define "Utility Positive":
  exists (
    [Observation: "Utility Screening LOINC"] UtilityObs
      where UtilityObs.status in {'final', 'amended', 'corrected'}
        and UtilityObs.effective as dateTime during "Measurement Period"
        and (
          UtilityObs.value as boolean = true
          or UtilityObs.interpretation.coding[0].code = 'POS'
        )
  )
  or exists (
    [Condition: "Utility Z Codes"] UtilityCondition
      where UtilityCondition.recordedDate during "Measurement Period"
        and UtilityCondition.clinicalStatus.coding[0].code = 'active'
  )

// Domain 5: Interpersonal Safety Positive
define "Interpersonal Safety Positive":
  exists (
    [Observation: "Interpersonal Safety Screening LOINC"] SafetyObs
      where SafetyObs.status in {'final', 'amended', 'corrected'}
        and SafetyObs.effective as dateTime during "Measurement Period"
        and SafetyObs.interpretation.coding[0].code = 'POS'
  )
  or exists (
    [Condition: "Interpersonal Safety Z Codes"] SafetyCondition
      where SafetyCondition.recordedDate during "Measurement Period"
        and SafetyCondition.clinicalStatus.coding[0].code = 'active'
  )

// Any positive screen result
define "Any Domain Positive":
  "Food Insecurity Positive"
    or "Housing Instability Positive"
    or "Transportation Positive"
    or "Utility Positive"
    or "Interpersonal Safety Positive"

// ============================================================================
// POPULATION DEFINITIONS
// ============================================================================

// Initial Population: Adults 18+ with qualifying encounter AND screened
define "Initial Population":
  "Patient Age in Years" >= 18
    and "Has Qualifying Encounter"
    and "Any Domain Screened"

// Denominator: Initial population excluding hospice
define "Denominator":
  "Initial Population"
    and not "Has Hospice"

// Denominator Exclusion
define "Denominator Exclusion":
  "Has Hospice"

// Numerator: Screened positive for at least one HRSN domain
define "Numerator":
  "Denominator"
    and "Any Domain Positive"

// ============================================================================
// CARE GAP IDENTIFICATION
// ============================================================================

// Note: For SDOH-2, a "care gap" is when someone screens positive but
// there's no documented intervention or referral
define "Positive Without Intervention":
  "Any Domain Positive"
    and not "Has Intervention Documented"

define "Has Intervention Documented":
  exists (
    [Procedure] Proc
      where Proc.status = 'completed'
        and Proc.performed as dateTime during "Measurement Period"
        and Proc.category.coding[0].code = 'social-service'
  )
  or exists (
    [ServiceRequest] SR
      where SR.status in {'active', 'completed'}
        and SR.authoredOn during "Measurement Period"
        and SR.category.coding[0].code = 'social-service'
  )

define "Care Gap":
  "Positive Without Intervention"

define "Care Gap Reason":
  if "Care Gap" then
    'Patient screened positive for HRSN need without documented intervention or referral'
  else
    null

define "Recommendation":
  if "Care Gap" then
    'Provide referral to community resources or social services for identified HRSN needs'
  else
    null

// ============================================================================
// POSITIVE DOMAIN SUMMARY
// ============================================================================
define "Positive Domains Count":
  (if "Food Insecurity Positive" then 1 else 0) +
  (if "Housing Instability Positive" then 1 else 0) +
  (if "Transportation Positive" then 1 else 0) +
  (if "Utility Positive" then 1 else 0) +
  (if "Interpersonal Safety Positive" then 1 else 0)

define "Positive Domains":
  {
    foodInsecurity: "Food Insecurity Positive",
    housingInstability: "Housing Instability Positive",
    transportation: "Transportation Positive",
    utilities: "Utility Positive",
    interpersonalSafety: "Interpersonal Safety Positive"
  }

// ============================================================================
// MEASURE SUMMARY
// ============================================================================
define "Measure Summary":
  {
    measureId: 'CMS_SDOH2',
    measureName: 'SDOH Screen Positive Rate',
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
    anyDomainScreened: "Any Domain Screened",
    anyDomainPositive: "Any Domain Positive",
    positiveDomainCount: "Positive Domains Count",
    foodInsecurityPositive: "Food Insecurity Positive",
    housingInstabilityPositive: "Housing Instability Positive",
    transportationPositive: "Transportation Positive",
    utilityPositive: "Utility Positive",
    interpersonalSafetyPositive: "Interpersonal Safety Positive",
    hasInterventionDocumented: "Has Intervention Documented",
    hasExclusion: "Denominator Exclusion"
  }
