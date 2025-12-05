library HEDIS_CBP version '1.0.0'

/*
 * HEDIS Controlling High Blood Pressure (CBP)
 *
 * Measure Description:
 * The percentage of patients 18-85 years of age with a diagnosis of hypertension
 * who had adequate blood pressure control (<140/90 mmHg) during the measurement period.
 *
 * Denominator: Patients aged 18-85 with hypertension diagnosis
 * Numerator: Patients with most recent BP < 140/90 mmHg
 */

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Code Systems
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMED-CT": 'http://snomed.info/sct'

// Value Sets
valueset "Hypertension": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.104.12.1011'
valueset "Blood Pressure": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1012'

// Individual codes for simple matching
code "Essential Hypertension": '38341003' from "SNOMED-CT" display 'Essential hypertension'
code "Blood Pressure Panel": '85354-9' from "LOINC" display 'Blood pressure panel'
code "Systolic BP": '8480-6' from "LOINC" display 'Systolic blood pressure'
code "Diastolic BP": '8462-4' from "LOINC" display 'Diastolic blood pressure'

// Parameters
parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00.0, @2024-12-31T23:59:59.999]

// Contexts
context Patient

// Population Definitions

/*
 * Initial Population:
 * Patients aged 18-85 during the measurement period
 */
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 85

/*
 * Denominator:
 * Patients in initial population with hypertension diagnosis
 */
define "Denominator":
  "Initial Population"
    and exists "Hypertension Diagnosis"

/*
 * Hypertension Diagnosis:
 * Active hypertension conditions
 */
define "Hypertension Diagnosis":
  [Condition: code in "Hypertension"] HTN
    where HTN.clinicalStatus ~ ToConcept(FHIR.CodeableConcept { coding: { FHIR.Coding { code: FHIR.code { value: 'active' } } } })
  union
  [Condition] C
    where C.code.coding[0].code.value = '38341003'  // Essential Hypertension
      and C.clinicalStatus.coding[0].code.value = 'active'

/*
 * Numerator:
 * Patients in denominator with most recent BP < 140/90
 */
define "Numerator":
  "Denominator"
    and "Most Recent Systolic BP" < 140 'mm[Hg]'
    and "Most Recent Diastolic BP" < 90 'mm[Hg]'

/*
 * Blood Pressure Observations:
 * All BP readings during measurement period
 */
define "Blood Pressure Observations":
  [Observation: code in "Blood Pressure"] BP
    where BP.status.value in {'final', 'amended', 'corrected'}
      and BP.effective.value during "Measurement Period"
  union
  [Observation] O
    where O.code.coding[0].code.value = '85354-9'  // Blood pressure panel
      and O.status.value in {'final', 'amended', 'corrected'}

/*
 * Most Recent Blood Pressure:
 * The most recent BP reading
 */
define "Most Recent Blood Pressure":
  Last(
    "Blood Pressure Observations" BP
      sort by effective.value
  )

/*
 * Most Recent Systolic BP:
 * Systolic value from most recent reading
 */
define "Most Recent Systolic BP":
  First(
    "Most Recent Blood Pressure".component Component
      where Component.code.coding[0].code.value = '8480-6'
  ).value as FHIR.Quantity

/*
 * Most Recent Diastolic BP:
 * Diastolic value from most recent reading
 */
define "Most Recent Diastolic BP":
  First(
    "Most Recent Blood Pressure".component Component
      where Component.code.coding[0].code.value = '8462-4'
  ).value as FHIR.Quantity

/*
 * Care Gaps:
 * Identified gaps in hypertension care
 */
define "Care Gaps":
  if "Denominator" and not "Numerator" then
    'Patient has uncontrolled hypertension (BP >= 140/90). Recommend intensifying blood pressure management.'
  else if "Denominator" and not exists "Blood Pressure Observations" then
    'Patient with hypertension has no BP reading on record. Recommend measuring blood pressure.'
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
    mostRecentSystolic: "Most Recent Systolic BP".value,
    mostRecentDiastolic: "Most Recent Diastolic BP".value,
    careGap: "Care Gaps"
  }
