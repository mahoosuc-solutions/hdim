library HEDIS_COL version '1.0.0'

/*
 * HEDIS Colorectal Cancer Screening (COL)
 *
 * Measure Description:
 * The percentage of patients 50-75 years of age who had appropriate screening
 * for colorectal cancer.
 *
 * Denominator: Patients aged 50-75
 * Numerator: Patients with one or more of:
 *   - Colonoscopy in last 10 years
 *   - FOBT in last year
 *   - FIT-DNA test in last 3 years
 * Exclusions: Total colectomy, advanced illness, hospice
 */

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

// Code Systems
codesystem "LOINC": 'http://loinc.org'
codesystem "SNOMED-CT": 'http://snomed.info/sct'
codesystem "CPT": 'http://www.ama-assn.org/go/cpt'

// Value Sets
valueset "Colonoscopy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.108.12.1020'
valueset "FOBT": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1011'
valueset "FIT-DNA": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.108.12.1039'
valueset "Total Colectomy": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.198.12.1019'

// Individual codes for simple matching
code "Colonoscopy SNOMED": '73761001' from "SNOMED-CT" display 'Colonoscopy'
code "Colonoscopy Flexible SNOMED": '446521004' from "SNOMED-CT" display 'Colonoscopy through colostomy'
code "FOBT LOINC": '2335-8' from "LOINC" display 'Fecal occult blood'
code "FOBT Qualitative": '27396-1' from "LOINC" display 'Fecal occult blood [Presence] in Stool'
code "FIT-DNA Cologuard": '77353-1' from "LOINC" display 'Cells.neoplastic/100 cells in Stool by Immunoassay'
code "FIT-DNA Result": '77354-9' from "LOINC" display 'Cells.neoplastic [Presence] in Stool by Immunoassay'
code "Total Colectomy SNOMED": '26390003' from "SNOMED-CT" display 'Total colectomy'

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
 * Patients aged 50-75 during the measurement period
 */
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 50
    and AgeInYearsAt(start of "Measurement Period") <= 75

/*
 * Denominator:
 * Initial population without exclusions
 */
define "Denominator":
  "Initial Population"
    and not exists "Total Colectomy Procedure"

/*
 * Exclusions:
 * Patients with total colectomy
 */
define "Total Colectomy Procedure":
  [Procedure: code in "Total Colectomy"] Colectomy
    where Colectomy.status.value = 'completed'
      and Colectomy.performed.value before end of "Measurement Period"
  union
  [Procedure] P
    where P.code.coding[0].code.value = '26390003'  // Total colectomy SNOMED
      and P.status.value = 'completed'

/*
 * Numerator:
 * Patients with appropriate screening
 */
define "Numerator":
  "Denominator"
    and (
      exists "Colonoscopy Last 10 Years"
      or exists "FOBT Last Year"
      or exists "FIT-DNA Last 3 Years"
    )

/*
 * Colonoscopy in Last 10 Years:
 * Colonoscopy procedures within lookback period
 */
define "Colonoscopy Last 10 Years":
  [Procedure: code in "Colonoscopy"] Colonoscopy
    where Colonoscopy.status.value = 'completed'
      and Colonoscopy.performed.value during Interval[
        start of "Measurement Period" - 10 years,
        end of "Measurement Period"
      ]
  union
  [Procedure] P
    where (P.code.coding[0].code.value = '73761001'    // Colonoscopy
           or P.code.coding[0].code.value = '446521004') // Colonoscopy flexible
      and P.status.value = 'completed'
      and P.performed.value during Interval[
        start of "Measurement Period" - 10 years,
        end of "Measurement Period"
      ]

/*
 * FOBT in Last Year:
 * Fecal occult blood test within lookback period
 */
define "FOBT Last Year":
  [Observation: code in "FOBT"] FOBT
    where FOBT.status.value in {'final', 'amended', 'corrected'}
      and FOBT.effective.value during Interval[
        start of "Measurement Period" - 1 year,
        end of "Measurement Period"
      ]
  union
  [Observation] O
    where (O.code.coding[0].code.value = '2335-8'     // FOBT
           or O.code.coding[0].code.value = '27396-1') // FOBT qualitative
      and O.status.value in {'final', 'amended', 'corrected'}
      and O.effective.value during Interval[
        start of "Measurement Period" - 1 year,
        end of "Measurement Period"
      ]

/*
 * FIT-DNA in Last 3 Years:
 * FIT-DNA (Cologuard) test within lookback period
 */
define "FIT-DNA Last 3 Years":
  [Observation: code in "FIT-DNA"] FITDNA
    where FITDNA.status.value in {'final', 'amended', 'corrected'}
      and FITDNA.effective.value during Interval[
        start of "Measurement Period" - 3 years,
        end of "Measurement Period"
      ]
  union
  [Observation] O
    where (O.code.coding[0].code.value = '77353-1'    // FIT-DNA cells
           or O.code.coding[0].code.value = '77354-9') // FIT-DNA presence
      and O.status.value in {'final', 'amended', 'corrected'}
      and O.effective.value during Interval[
        start of "Measurement Period" - 3 years,
        end of "Measurement Period"
      ]

/*
 * Most Recent Screening:
 * Identify the most recent screening type and date
 */
define "Most Recent Screening Type":
  case
    when exists "Colonoscopy Last 10 Years" then 'Colonoscopy'
    when exists "FOBT Last Year" then 'FOBT'
    when exists "FIT-DNA Last 3 Years" then 'FIT-DNA'
    else 'None'
  end

define "Most Recent Screening Date":
  Coalesce(
    Last("Colonoscopy Last 10 Years" C sort by performed.value).performed.value,
    Last("FOBT Last Year" F sort by effective.value).effective.value,
    Last("FIT-DNA Last 3 Years" D sort by effective.value).effective.value
  )

/*
 * Care Gaps:
 * Identified gaps in colorectal cancer screening
 */
define "Care Gaps":
  if "Denominator" and not "Numerator" then
    'Patient is due for colorectal cancer screening. Recommend colonoscopy, FOBT, or FIT-DNA test.'
  else if "Denominator" and "Numerator" and "Most Recent Screening Type" = 'FOBT' then
    'Patient screened with FOBT. Annual FOBT required for continued compliance.'
  else if "Denominator" and "Numerator" and "Most Recent Screening Type" = 'FIT-DNA' then
    'Patient screened with FIT-DNA. Next screening due in 3 years from last test.'
  else if "Denominator" and "Numerator" and "Most Recent Screening Type" = 'Colonoscopy' then
    'Patient screened with colonoscopy. Next screening due in 10 years from last procedure.'
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
    mostRecentScreeningType: "Most Recent Screening Type",
    mostRecentScreeningDate: "Most Recent Screening Date",
    careGap: "Care Gaps"
  }
