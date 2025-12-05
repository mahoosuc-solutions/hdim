library HEDIS_CDC_H version '1.0.0'

/*
 * HEDIS Comprehensive Diabetes Care - HbA1c Control (<8%)
 *
 * Measure Description:
 * The percentage of patients 18-75 years of age with diabetes (type 1 or type 2)
 * who had hemoglobin A1c (HbA1c) control (<8.0%) during the measurement period.
 *
 * Denominator: Patients with diabetes
 * Numerator: Patients with most recent HbA1c < 8.0%
 */

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Code Systems
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMED-CT": 'http://snomed.info/sct'

// Value Sets - Using standard codes
valueset "Diabetes": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "HbA1c Laboratory Test": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1013'

// Individual codes for simple matching
code "Hemoglobin A1c": '4548-4' from "LOINC" display 'Hemoglobin A1c'
code "Type 1 Diabetes": '46635009' from "SNOMED-CT" display 'Type 1 diabetes mellitus'
code "Type 2 Diabetes": '44054006' from "SNOMED-CT" display 'Type 2 diabetes mellitus'

// Parameters
parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00.0, @2024-12-31T23:59:59.999]

// Contexts
context Patient

// Helper Functions
define function ToInterval(period FHIR.Period):
  if period is null then
    null
  else
    Interval[period."start".value, period."end".value]

// Population Definitions

/*
 * Initial Population:
 * Patients aged 18-75 during the measurement period
 */
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75

/*
 * Denominator:
 * Patients in initial population with diabetes diagnosis
 */
define "Denominator":
  "Initial Population"
    and exists "Diabetes Diagnosis"

/*
 * Diabetes Diagnosis:
 * Active diabetes conditions (Type 1 or Type 2)
 */
define "Diabetes Diagnosis":
  [Condition: code in "Diabetes"] Diabetes
    where Diabetes.clinicalStatus ~ ToConcept(FHIR.CodeableConcept { coding: { FHIR.Coding { code: FHIR.code { value: 'active' } } } })
  union
  [Condition] C
    where (C.code.coding[0].code.value = '44054006'   // Type 2 Diabetes
           or C.code.coding[0].code.value = '46635009') // Type 1 Diabetes
      and C.clinicalStatus.coding[0].code.value = 'active'

/*
 * Numerator:
 * Patients in denominator with most recent HbA1c < 8.0%
 */
define "Numerator":
  "Denominator"
    and "Most Recent HbA1c" < 8.0 '%'

/*
 * HbA1c Tests:
 * All HbA1c observations during measurement period
 */
define "HbA1c Tests":
  [Observation: code in "HbA1c Laboratory Test"] HbA1c
    where HbA1c.status.value in {'final', 'amended', 'corrected'}
      and HbA1c.value is not null
      and HbA1c.effective.value during "Measurement Period"
  union
  [Observation] O
    where O.code.coding[0].code.value = '4548-4'  // LOINC HbA1c code
      and O.status.value in {'final', 'amended', 'corrected'}
      and O.value is not null

/*
 * Most Recent HbA1c:
 * The most recent HbA1c result
 */
define "Most Recent HbA1c":
  Last(
    "HbA1c Tests" HbA1c
      sort by effective.value
  ).value as FHIR.Quantity

/*
 * Care Gaps:
 * Identified gaps in diabetes care
 */
define "Care Gaps":
  if "Denominator" and not "Numerator" then
    'Patient has uncontrolled diabetes (HbA1c >= 8.0%). Recommend intensifying diabetes management.'
  else if "Denominator" and not exists "HbA1c Tests" then
    'Patient with diabetes has no HbA1c test on record. Recommend ordering HbA1c test.'
  else
    null

/*
 * Final Result:
 * Summary of measure evaluation
 */
define "Measure Result":
  {
    inDenominator: "Denominator",
    inNumerator: "Numerator",
    mostRecentHbA1c: "Most Recent HbA1c".value,
    careGap: "Care Gaps"
  }
