library HEDIS_BCS version '2024'

/*
 * HEDIS BCS: Breast Cancer Screening
 *
 * Description:
 * Percentage of women 50-74 years of age who had a mammogram to screen
 * for breast cancer in the 27 months prior to the end of the measurement period.
 *
 * Measure Steward: National Committee for Quality Assurance (NCQA)
 * HEDIS Measure: BCS
 *
 * Population Criteria:
 * - Initial Population: Women 50-74 years of age
 * - Denominator: Same as initial population
 * - Numerator: Women who had a mammogram in the 27 months prior to the end of the measurement period
 * - Exclusions: Women who had bilateral mastectomy or two unilateral mastectomies
 *
 * Clinical Rationale:
 * Breast cancer is the most common cancer among women in the United States.
 * Regular mammography screening can detect breast cancer at an early stage,
 * when treatment is more effective and survival rates are higher.
 */

using FHIR version '4.0.1'

// Include common libraries
include FHIRHelpers version '4.0.1' called FHIRHelpers

// Code systems
codesystem "SNOMED-CT": 'http://snomed.info/sct'
codesystem "LOINC": 'http://loinc.org'
codesystem "CPT": 'http://www.ama-assn.org/go/cpt'
codesystem "ICD-10-CM": 'http://hl7.org/fhir/sid/icd-10-cm'

// Value sets (simplified for PoC)
valueset "Mammography": {
  // CPT codes for mammography
  Code '77067' from "CPT" display 'Screening mammography, bilateral',
  Code '77063' from "CPT" display 'Screening digital breast tomosynthesis',
  // SNOMED codes
  Code '24623002' from "SNOMED-CT" display 'Screening mammography',
  Code '241055006' from "SNOMED-CT" display 'Mammography screening',
  // LOINC codes
  Code '24604-1' from "LOINC" display 'Mammogram'
}

valueset "Bilateral Mastectomy": {
  Code '428571003' from "SNOMED-CT" display 'Bilateral mastectomy',
  Code '27865001' from "SNOMED-CT" display 'Bilateral simple mastectomy',
  Code '456903003' from "SNOMED-CT" display 'Bilateral radical mastectomy'
}

valueset "Unilateral Mastectomy": {
  Code '428571003' from "SNOMED-CT" display 'Mastectomy of breast',
  Code '172043006' from "SNOMED-CT" display 'Mastectomy',
  Code '384723003' from "SNOMED-CT" display 'Simple mastectomy',
  Code '234254000' from "SNOMED-CT" display 'Radical mastectomy'
}

// Administrative sex codes
code "Female": 'female' from "http://hl7.org/fhir/administrative-gender"

// Parameters
parameter "Measurement Period" Interval<DateTime> default Interval[@2023-01-01T00:00:00.0, @2023-12-31T23:59:59.9]

// Contexts
context Patient

// Helper functions
define "Patient Birth Date":
  Patient.birthDate

define "Patient Age":
  AgeInYearsAt(end of "Measurement Period")

define "Patient Gender":
  Patient.gender

define "Is Female":
  "Patient Gender" = 'female'

// Initial Population
define "Initial Population":
  "Is Female"
    and "Patient Age" >= 50
    and "Patient Age" <= 74

// Denominator
define "Denominator":
  "Initial Population"
    and not "Has Bilateral Mastectomy"

// Numerator
define "Numerator":
  "Denominator"
    and "Has Mammogram in Last 27 Months"

// Exclusions
define "Denominator Exclusions":
  "Has Bilateral Mastectomy"

// Supporting definitions

define "Has Bilateral Mastectomy":
  exists (
    [Procedure: "Bilateral Mastectomy"] BilateralMastectomy
      where BilateralMastectomy.status = 'completed'
        and BilateralMastectomy.performed as dateTime before end of "Measurement Period"
  )
  or "Has Two Unilateral Mastectomies"

define "Has Two Unilateral Mastectomies":
  (
    Count(
      [Procedure: "Unilateral Mastectomy"] UnilateralMastectomy
        where UnilateralMastectomy.status = 'completed'
          and UnilateralMastectomy.performed as dateTime before end of "Measurement Period"
    )
  ) >= 2

define "Mammography Procedures":
  [Procedure: "Mammography"] Mammogram
    where Mammogram.status = 'completed'

define "Has Mammogram in Last 27 Months":
  exists (
    "Mammography Procedures" Mammogram
      where Mammogram.performed as dateTime during Interval[
        end of "Measurement Period" - 27 months,
        end of "Measurement Period"
      ]
  )

define "Most Recent Mammogram":
  Last(
    "Mammography Procedures" Mammogram
      where Mammogram.performed as dateTime before end of "Measurement Period"
      sort by (performed as dateTime)
  )

define "Most Recent Mammogram Date":
  "Most Recent Mammogram".performed as dateTime

define "Days Since Last Mammogram":
  if "Most Recent Mammogram" is not null
    then days between "Most Recent Mammogram Date" and end of "Measurement Period"
    else null

// Care Gap Identification
define "Care Gap":
  not "Numerator"
    and "Denominator"
    and not "Denominator Exclusions"

define "Care Gap Reason":
  if "Care Gap" then
    if not "Has Mammogram in Last 27 Months" then
      'No mammogram screening in the last 27 months'
    else
      'Unknown reason for not meeting numerator criteria'
  else
    null

define "Recommendation":
  if "Care Gap" then
    'Schedule mammogram screening'
  else
    null

// Measure result summary
define "Measure Summary":
  {
    measureId: 'HEDIS_BCS',
    measureName: 'Breast Cancer Screening',
    measureVersion: '2024',
    patientId: Patient.id,
    measurementPeriod: "Measurement Period",
    inInitialPopulation: "Initial Population",
    inDenominator: "Denominator",
    inNumerator: "Numerator",
    hasExclusion: "Denominator Exclusions",
    careGap: "Care Gap",
    careGapReason: "Care Gap Reason",
    recommendation: "Recommendation",
    patientAge: "Patient Age",
    isEligible: "Denominator",
    metCriteria: "Numerator",
    daysSinceLastMammogram: "Days Since Last Mammogram"
  }
