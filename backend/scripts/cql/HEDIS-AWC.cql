library HEDIS_AWC version '2024'

/*
 * HEDIS AWC: Adolescent Well-Care Visits
 *
 * Description:
 * Percentage of adolescents 12-21 years of age who had at least one comprehensive
 * well-care visit with a PCP or OB/GYN practitioner during the measurement period.
 *
 * Measure Steward: National Committee for Quality Assurance (NCQA)
 * HEDIS Measure: AWC
 *
 * Population Criteria:
 * - Initial Population: Adolescents who turn 12-21 years of age during the measurement period
 * - Denominator: Same as initial population
 * - Numerator: Adolescents who had at least one comprehensive well-care visit during the measurement period
 * - Exclusions: Members in hospice care or with institutional stays
 *
 * Clinical Rationale:
 * Adolescence is a critical period for establishing healthy behaviors and
 * identifying risk factors. Annual well-care visits provide opportunities for:
 * - Preventive screenings (BMI, blood pressure, depression)
 * - Immunization updates
 * - Health education and counseling
 * - Early identification of behavioral health issues
 * - Guidance on nutrition, physical activity, and substance use prevention
 */

using FHIR version '4.0.1'

// Include common libraries
include FHIRHelpers version '4.0.1' called FHIRHelpers

// Code systems
codesystem "CPT": 'http://www.ama-assn.org/go/cpt'
codesystem "HCPCS": 'http://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets'
codesystem "SNOMED-CT": 'http://snomed.info/sct'
codesystem "LOINC": 'http://loinc.org'
codesystem "ICD-10-CM": 'http://hl7.org/fhir/sid/icd-10-cm'

// Value sets for well-care visits

// Comprehensive Well-Care Visit CPT codes
valueset "Well-Care Visit CPT": {
  Code '99384' from "CPT" display 'Initial comprehensive preventive medicine evaluation, 12-17 years',
  Code '99385' from "CPT" display 'Initial comprehensive preventive medicine evaluation, 18-39 years',
  Code '99394' from "CPT" display 'Periodic comprehensive preventive medicine, 12-17 years',
  Code '99395' from "CPT" display 'Periodic comprehensive preventive medicine, 18-39 years',
  Code '99461' from "CPT" display 'Initial care, per day, normal newborn',
  Code '99462' from "CPT" display 'Subsequent care, per day, normal newborn'
}

// Office Visit E&M codes that may qualify as well-care
valueset "Office Visit": {
  Code '99201' from "CPT" display 'Office/outpatient visit, new patient, level 1',
  Code '99202' from "CPT" display 'Office/outpatient visit, new patient, level 2',
  Code '99203' from "CPT" display 'Office/outpatient visit, new patient, level 3',
  Code '99204' from "CPT" display 'Office/outpatient visit, new patient, level 4',
  Code '99205' from "CPT" display 'Office/outpatient visit, new patient, level 5',
  Code '99211' from "CPT" display 'Office/outpatient visit, established patient, minimal',
  Code '99212' from "CPT" display 'Office/outpatient visit, established patient, level 2',
  Code '99213' from "CPT" display 'Office/outpatient visit, established patient, level 3',
  Code '99214' from "CPT" display 'Office/outpatient visit, established patient, level 4',
  Code '99215' from "CPT" display 'Office/outpatient visit, established patient, level 5'
}

// Preventive Care Services
valueset "Preventive Care Services": {
  Code 'Z00.00' from "ICD-10-CM" display 'Encounter for general adult medical examination without abnormal findings',
  Code 'Z00.01' from "ICD-10-CM" display 'Encounter for general adult medical examination with abnormal findings',
  Code 'Z00.121' from "ICD-10-CM" display 'Encounter for routine child health examination with abnormal findings',
  Code 'Z00.129' from "ICD-10-CM" display 'Encounter for routine child health examination without abnormal findings',
  Code 'Z76.1' from "ICD-10-CM" display 'Encounter for health supervision and care of foundling',
  Code 'Z76.2' from "ICD-10-CM" display 'Encounter for health supervision and care of other healthy infant and child'
}

// Annual Wellness Visit
valueset "Annual Wellness Visit": {
  Code 'G0438' from "HCPCS" display 'Annual wellness visit, first',
  Code 'G0439' from "HCPCS" display 'Annual wellness visit, subsequent'
}

// SNOMED codes for well-child visits
valueset "Well-Child Visit SNOMED": {
  Code '410620009' from "SNOMED-CT" display 'Well child visit',
  Code '390906007' from "SNOMED-CT" display 'Follow-up encounter',
  Code '439740005' from "SNOMED-CT" display 'Postoperative follow-up visit',
  Code '185349003' from "SNOMED-CT" display 'Encounter for check up',
  Code '30346009' from "SNOMED-CT" display 'Evaluation and management of patient',
  Code '308335008' from "SNOMED-CT" display 'Patient encounter procedure'
}

// Hospice care
valueset "Hospice": {
  Code '385763009' from "SNOMED-CT" display 'Hospice care',
  Code '385765002' from "SNOMED-CT" display 'Hospice admission'
}

// Institutional stays
valueset "Institutional Stay": {
  Code '32485007' from "SNOMED-CT" display 'Hospital admission',
  Code '8715000' from "SNOMED-CT" display 'Hospital admission, elective',
  Code '183495009' from "SNOMED-CT" display 'Non-urgent hospital admission'
}

// Parameters
parameter "Measurement Period" Interval<DateTime> default Interval[@2023-01-01T00:00:00.0, @2023-12-31T23:59:59.9]

// Contexts
context Patient

// Helper functions
define "Patient Birth Date":
  Patient.birthDate

define "Patient Age in Years":
  AgeInYearsAt(end of "Measurement Period")

define "Age in Years at Start":
  AgeInYearsAt(start of "Measurement Period")

define "Age in Years at End":
  AgeInYearsAt(end of "Measurement Period")

// Initial Population
define "Initial Population":
  "Age in Years at Start" >= 12
    and "Age in Years at End" <= 21

// Denominator
define "Denominator":
  "Initial Population"
    and not "Has Hospice"
    and not "Has Institutional Stay"

// Exclusions
define "Has Hospice":
  exists (
    [Condition: "Hospice"] HospiceCondition
      where HospiceCondition.clinicalStatus.coding[0].code = 'active'
        and HospiceCondition.onset as dateTime during "Measurement Period"
  )

define "Has Institutional Stay":
  exists (
    [Encounter: "Institutional Stay"] InstitutionalEncounter
      where InstitutionalEncounter.status = 'finished'
        and InstitutionalEncounter.period.start during "Measurement Period"
        and duration in days of InstitutionalEncounter.period >= 90
  )

// Well-care visits
define "Well-Care Visits":
  (
    [Encounter: "Well-Care Visit CPT"]
      union [Encounter: "Annual Wellness Visit"]
      union [Encounter: "Well-Child Visit SNOMED"]
  ) WellCareEncounter
    where WellCareEncounter.status = 'finished'
      and WellCareEncounter.period.start during "Measurement Period"

define "Office Visits with Preventive Intent":
  [Encounter: "Office Visit"] OfficeVisit
    where OfficeVisit.status = 'finished'
      and OfficeVisit.period.start during "Measurement Period"
      and exists (
        OfficeVisit.reasonCode ReasonCode
          where ReasonCode in "Preventive Care Services"
      )

define "All Qualifying Visits":
  "Well-Care Visits"
    union "Office Visits with Preventive Intent"

// Numerator
define "Numerator":
  "Denominator"
    and "Has Well-Care Visit"

define "Has Well-Care Visit":
  exists "All Qualifying Visits"

// Care Gap Identification
define "Care Gap":
  "Denominator" and not "Numerator"

define "Care Gap Reason":
  if "Care Gap" then
    'No comprehensive well-care visit documented in the measurement period'
  else
    null

define "Recommendation":
  if "Care Gap" then
    'Schedule annual well-care visit with PCP or OB/GYN'
  else
    null

define "Days Since Last Visit":
  if exists "All Qualifying Visits" then
    days between
      Max("All Qualifying Visits" Visit return start of Visit.period)
      and end of "Measurement Period"
  else
    null

define "Visit Count":
  Count("All Qualifying Visits")

define "Last Visit Date":
  if exists "All Qualifying Visits" then
    Max("All Qualifying Visits" Visit return start of Visit.period)
  else
    null

define "Next Due Date":
  if "Has Well-Care Visit" then
    "Last Visit Date" + 1 year
  else
    start of "Measurement Period"

// Measure result summary
define "Measure Summary":
  {
    measureId: 'HEDIS_AWC',
    measureName: 'Adolescent Well-Care Visits',
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
    visitCount: "Visit Count",
    lastVisitDate: "Last Visit Date",
    daysSinceLastVisit: "Days Since Last Visit",
    nextDueDate: "Next Due Date",
    hasExclusion: ("Has Hospice" or "Has Institutional Stay")
  }
