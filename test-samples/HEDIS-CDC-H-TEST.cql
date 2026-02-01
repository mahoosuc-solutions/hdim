library HEDIS_CDC_H_TEST version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

// ====================
// Terminology
// ====================

codesystem "LOINC": 'http://loinc.org'

valueset "HbA1c Laboratory Test": 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013'
valueset "Diabetes": 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001'

parameter "Measurement Period" Interval<DateTime>

context Patient

// ====================
// Population Criteria
// ====================

// Initial Population: Adults with diabetes
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    and exists "Diabetes Diagnosis"

define "Diabetes Diagnosis":
  [Condition: "Diabetes"] C
    where C.clinicalStatus ~ "active"
      and C.verificationStatus ~ "confirmed"
      and C.onsetDateTime before end of "Measurement Period"

// Denominator: All patients in initial population
define "Denominator":
  "Initial Population"

// VIOLATION 1: Missing "Numerator" definition (REQUIRED for proportion measures!)

// VIOLATION 2: Using invalid FHIR resource type "Lab" instead of "Observation"
define "HbA1c Tests":
  [Lab: "HbA1c Laboratory Test"] L
    where L.resultDate during "Measurement Period"

// VIOLATION 3: Missing status check on Observation (should check O.status)
define "Recent Lab Results":
  [Observation: "HbA1c Laboratory Test"] O
    where O.effective during "Measurement Period"

// VIOLATION 4: Missing null check before type casting
define "HbA1c Value":
  (First([Observation: "HbA1c Laboratory Test"]).value as Quantity).value

// Correct example for comparison
define "HbA1c Test Controlled Correct":
  [Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended', 'corrected'}
      and O.effective during "Measurement Period"
      and O.value is not null
      and (O.value as Quantity).value < 8.0
