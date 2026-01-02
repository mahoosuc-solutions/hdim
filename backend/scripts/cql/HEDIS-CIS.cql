library HEDIS_CIS version '2024'

/*
 * HEDIS CIS: Childhood Immunization Status
 *
 * Description:
 * Percentage of children 2 years of age who had four diphtheria, tetanus and
 * acellular pertussis (DTaP); three polio (IPV); one measles, mumps and rubella (MMR);
 * three haemophilus influenzae type B (HiB); three hepatitis B (HepB), one chicken pox
 * (VZV); four pneumococcal conjugate (PCV); one hepatitis A (HepA); two or three rotavirus
 * (RV); and two influenza (flu) vaccines by their second birthday.
 *
 * Measure Steward: National Committee for Quality Assurance (NCQA)
 * HEDIS Measure: CIS
 *
 * Population Criteria:
 * - Initial Population: Children who turn 2 years of age during the measurement period
 * - Denominator: Same as initial population
 * - Numerator: Children who receive all required immunizations by age 2
 * - Exclusions: Children in hospice care or with contraindications
 *
 * Clinical Rationale:
 * Childhood immunizations protect against serious diseases and prevent outbreaks.
 * The CDC's Advisory Committee on Immunization Practices (ACIP) recommends a series
 * of vaccinations for children by age 2 to protect them from 14 serious diseases.
 */

using FHIR version '4.0.1'

// Include common libraries
include FHIRHelpers version '4.0.1' called FHIRHelpers

// Code systems
codesystem "CVX": 'http://hl7.org/fhir/sid/cvx' // Vaccine Administered Code Set
codesystem "SNOMED-CT": 'http://snomed.info/sct'
codesystem "LOINC": 'http://loinc.org'

// Value sets for vaccine codes (simplified for PoC - using CVX codes)

// DTaP - Diphtheria, Tetanus, Pertussis (4 doses required)
valueset "DTaP Vaccine": {
  Code '20' from "CVX" display 'DTaP',
  Code '106' from "CVX" display 'DTaP, 5 pertussis antigens',
  Code '107' from "CVX" display 'DTaP, unspecified formulation',
  Code '110' from "CVX" display 'DTaP-hepatitis B and poliovirus vaccine',
  Code '120' from "CVX" display 'DTaP-Hib-IPV',
  Code '130' from "CVX" display 'DTaP-IPV'
}

// IPV - Inactivated Poliovirus (3 doses required)
valueset "IPV Vaccine": {
  Code '10' from "CVX" display 'IPV',
  Code '89' from "CVX" display 'Poliovirus vaccine, unspecified',
  Code '110' from "CVX" display 'DTaP-hepatitis B and poliovirus vaccine',
  Code '120' from "CVX" display 'DTaP-Hib-IPV',
  Code '130' from "CVX" display 'DTaP-IPV'
}

// MMR - Measles, Mumps, Rubella (1 dose required)
valueset "MMR Vaccine": {
  Code '03' from "CVX" display 'MMR',
  Code '94' from "CVX" display 'MMRV'
}

// HiB - Haemophilus influenzae type B (3 doses required)
valueset "HiB Vaccine": {
  Code '17' from "CVX" display 'Hib, unspecified formulation',
  Code '46' from "CVX" display 'Hib (PRP-D)',
  Code '47' from "CVX" display 'Hib (HbOC)',
  Code '48' from "CVX" display 'Hib (PRP-T)',
  Code '49' from "CVX" display 'Hib (PRP-OMP)',
  Code '120' from "CVX" display 'DTaP-Hib-IPV'
}

// HepB - Hepatitis B (3 doses required)
valueset "HepB Vaccine": {
  Code '08' from "CVX" display 'Hep B, adolescent or pediatric',
  Code '44' from "CVX" display 'Hep B, unspecified formulation',
  Code '110' from "CVX" display 'DTaP-hepatitis B and poliovirus vaccine'
}

// VZV - Varicella (Chicken Pox) (1 dose required)
valueset "VZV Vaccine": {
  Code '21' from "CVX" display 'Varicella',
  Code '94' from "CVX" display 'MMRV'
}

// PCV - Pneumococcal Conjugate (4 doses required)
valueset "PCV Vaccine": {
  Code '133' from "CVX" display 'PCV13',
  Code '152' from "CVX" display 'Pneumococcal Conjugate, unspecified',
  Code '100' from "CVX" display 'Pneumococcal conjugate PCV 7'
}

// HepA - Hepatitis A (1 dose required)
valueset "HepA Vaccine": {
  Code '83' from "CVX" display 'Hep A, ped/adol, 2 dose',
  Code '85' from "CVX" display 'Hep A, unspecified formulation'
}

// RV - Rotavirus (2 or 3 doses required depending on vaccine type)
valueset "RV Vaccine": {
  Code '116' from "CVX" display 'Rotavirus, pentavalent (3 doses)',
  Code '119' from "CVX" display 'Rotavirus, monovalent (2 doses)',
  Code '122' from "CVX" display 'Rotavirus, unspecified formulation'
}

// Influenza (2 doses required for first-time recipients)
valueset "Influenza Vaccine": {
  Code '88' from "CVX" display 'Influenza, unspecified formulation',
  Code '135' from "CVX" display 'Influenza, high dose seasonal',
  Code '140' from "CVX" display 'Influenza, seasonal, injectable',
  Code '141' from "CVX" display 'Influenza, seasonal, intranasal',
  Code '150' from "CVX" display 'Influenza, injectable, quadrivalent',
  Code '153' from "CVX" display 'Influenza, injectable, MDCK',
  Code '155' from "CVX" display 'Influenza, recombinant, injectable',
  Code '161' from "CVX" display 'Influenza, injectable, quadrivalent, adjuvanted'
}

// Hospice care
valueset "Hospice": {
  Code '385763009' from "SNOMED-CT" display 'Hospice care'
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

define "Patient Age in Months":
  AgeInMonthsAt(end of "Measurement Period")

define "Turned 2 During Measurement Period":
  AgeInYearsAt(start of "Measurement Period") < 2
    and AgeInYearsAt(end of "Measurement Period") >= 2

// Initial Population
define "Initial Population":
  "Turned 2 During Measurement Period"

// Denominator
define "Denominator":
  "Initial Population"
    and not "Has Hospice"

// Exclusions
define "Has Hospice":
  exists (
    [Condition: "Hospice"] HospiceCondition
      where HospiceCondition.clinicalStatus.coding[0].code = 'active'
  )

// Immunization helper functions
define "All Immunizations":
  [Immunization] I
    where I.status = 'completed'
      and I.occurrence as dateTime before end of "Measurement Period"
      and AgeInMonthsAt(I.occurrence as dateTime) <= 24

define "DTaP Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "DTaP Vaccine"

define "IPV Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "IPV Vaccine"

define "MMR Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "MMR Vaccine"

define "HiB Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "HiB Vaccine"

define "HepB Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "HepB Vaccine"

define "VZV Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "VZV Vaccine"

define "PCV Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "PCV Vaccine"

define "HepA Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "HepA Vaccine"

define "RV Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "RV Vaccine"

define "Influenza Immunizations":
  "All Immunizations" Imm
    where Imm.vaccineCode in "Influenza Vaccine"

// Required doses met
define "Has 4 DTaP":
  Count("DTaP Immunizations") >= 4

define "Has 3 IPV":
  Count("IPV Immunizations") >= 3

define "Has 1 MMR":
  Count("MMR Immunizations") >= 1

define "Has 3 HiB":
  Count("HiB Immunizations") >= 3

define "Has 3 HepB":
  Count("HepB Immunizations") >= 3

define "Has 1 VZV":
  Count("VZV Immunizations") >= 1

define "Has 4 PCV":
  Count("PCV Immunizations") >= 4

define "Has 1 HepA":
  Count("HepA Immunizations") >= 1

define "Has 2 RV":
  Count("RV Immunizations") >= 2

define "Has 2 Influenza":
  Count("Influenza Immunizations") >= 2

// Numerator - All required immunizations
define "Numerator":
  "Denominator"
    and "Has 4 DTaP"
    and "Has 3 IPV"
    and "Has 1 MMR"
    and "Has 3 HiB"
    and "Has 3 HepB"
    and "Has 1 VZV"
    and "Has 4 PCV"
    and "Has 1 HepA"
    and "Has 2 RV"
    and "Has 2 Influenza"

// Care Gap Identification
define "Care Gap":
  "Denominator" and not "Numerator"

define "Missing Immunizations":
  if "Care Gap" then
    Flatten({
      if not "Has 4 DTaP" then { 'DTaP: ' + ToString(Count("DTaP Immunizations")) + '/4 doses' } else {},
      if not "Has 3 IPV" then { 'IPV: ' + ToString(Count("IPV Immunizations")) + '/3 doses' } else {},
      if not "Has 1 MMR" then { 'MMR: ' + ToString(Count("MMR Immunizations")) + '/1 dose' } else {},
      if not "Has 3 HiB" then { 'HiB: ' + ToString(Count("HiB Immunizations")) + '/3 doses' } else {},
      if not "Has 3 HepB" then { 'HepB: ' + ToString(Count("HepB Immunizations")) + '/3 doses' } else {},
      if not "Has 1 VZV" then { 'VZV: ' + ToString(Count("VZV Immunizations")) + '/1 dose' } else {},
      if not "Has 4 PCV" then { 'PCV: ' + ToString(Count("PCV Immunizations")) + '/4 doses' } else {},
      if not "Has 1 HepA" then { 'HepA: ' + ToString(Count("HepA Immunizations")) + '/1 dose' } else {},
      if not "Has 2 RV" then { 'RV: ' + ToString(Count("RV Immunizations")) + '/2 doses' } else {},
      if not "Has 2 Influenza" then { 'Flu: ' + ToString(Count("Influenza Immunizations")) + '/2 doses' } else {}
    })
  else
    null

define "Care Gap Reason":
  if "Care Gap" then
    'Missing required immunizations: ' + ToString(Count("Missing Immunizations")) + ' series incomplete'
  else
    null

define "Recommendation":
  if "Care Gap" then
    'Schedule immunization appointments to complete: ' + ToString("Missing Immunizations")
  else
    null

// Immunization compliance percentage
define "Compliance Percentage":
  (
    (if "Has 4 DTaP" then 1 else 0) +
    (if "Has 3 IPV" then 1 else 0) +
    (if "Has 1 MMR" then 1 else 0) +
    (if "Has 3 HiB" then 1 else 0) +
    (if "Has 3 HepB" then 1 else 0) +
    (if "Has 1 VZV" then 1 else 0) +
    (if "Has 4 PCV" then 1 else 0) +
    (if "Has 1 HepA" then 1 else 0) +
    (if "Has 2 RV" then 1 else 0) +
    (if "Has 2 Influenza" then 1 else 0)
  ) / 10.0 * 100

// Measure result summary
define "Measure Summary":
  {
    measureId: 'HEDIS_CIS',
    measureName: 'Childhood Immunization Status',
    measureVersion: '2024',
    patientId: Patient.id,
    measurementPeriod: "Measurement Period",
    inInitialPopulation: "Initial Population",
    inDenominator: "Denominator",
    inNumerator: "Numerator",
    careGap: "Care Gap",
    careGapReason: "Care Gap Reason",
    recommendation: "Recommendation",
    patientAgeMonths: "Patient Age in Months",
    compliancePercentage: "Compliance Percentage",
    immunizationStatus: {
      dtap: { required: 4, received: Count("DTaP Immunizations"), complete: "Has 4 DTaP" },
      ipv: { required: 3, received: Count("IPV Immunizations"), complete: "Has 3 IPV" },
      mmr: { required: 1, received: Count("MMR Immunizations"), complete: "Has 1 MMR" },
      hib: { required: 3, received: Count("HiB Immunizations"), complete: "Has 3 HiB" },
      hepB: { required: 3, received: Count("HepB Immunizations"), complete: "Has 3 HepB" },
      vzv: { required: 1, received: Count("VZV Immunizations"), complete: "Has 1 VZV" },
      pcv: { required: 4, received: Count("PCV Immunizations"), complete: "Has 4 PCV" },
      hepA: { required: 1, received: Count("HepA Immunizations"), complete: "Has 1 HepA" },
      rv: { required: 2, received: Count("RV Immunizations"), complete: "Has 2 RV" },
      flu: { required: 2, received: Count("Influenza Immunizations"), complete: "Has 2 Influenza" }
    },
    missingImmunizations: "Missing Immunizations"
  }
