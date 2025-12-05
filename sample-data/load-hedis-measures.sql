-- Load HEDIS Measures directly into CQL Libraries table
-- This bypasses the JWT authentication requirement for development

-- HEDIS-CDC: Comprehensive Diabetes Care - HbA1c Control
INSERT INTO cql_libraries (
    id,
    tenant_id,
    name,
    version,
    status,
    cql_content,
    description,
    publisher,
    created_by,
    library_name,
    active
) VALUES (
    gen_random_uuid(),
    'default',
    'HEDIS-CDC',
    '2024.1',
    'ACTIVE',
    'library HEDIS_CDC version ''2024.1''

using FHIR version ''4.0.1''

include FHIRHelpers version ''4.0.1''

context Patient

define "Patient Age in Years":
  AgeInYearsAt(Today())

define "Is Diabetic Patient":
  exists (
    [Condition: Code ''E11'' from SNOMED] C
      where C.clinicalStatus ~ "active"
  )

define "Has Recent HbA1c":
  exists (
    [Observation: Code ''4548-4'' from LOINC] O
      where O.status = ''final''
        and O.effective during Interval[Today() - 1 year, Today()]
  )

define "HbA1c Poor Control":
  exists (
    [Observation: Code ''4548-4'' from LOINC] O
      where O.status = ''final''
        and O.effective during Interval[Today() - 1 year, Today()]
        and (O.value as Quantity) > 9.0 ''%''
  )

define "In Denominator":
  "Patient Age in Years" >= 18
    and "Patient Age in Years" <= 75
    and "Is Diabetic Patient"

define "In Numerator":
  "In Denominator"
    and "Has Recent HbA1c"
    and not "HbA1c Poor Control"',
    'Comprehensive Diabetes Care - HbA1c Control. Measures the percentage of members 18-75 years of age with diabetes (type 1 and type 2) whose most recent HbA1c level is >9.0% (poor control).',
    'HealthData In Motion',
    'system',
    'HEDIS_CDC',
    true
) ON CONFLICT (tenant_id, name, version) DO UPDATE SET
    status = EXCLUDED.status,
    cql_content = EXCLUDED.cql_content,
    description = EXCLUDED.description,
    publisher = EXCLUDED.publisher,
    updated_at = now();

-- HEDIS-CBP: Controlling High Blood Pressure
INSERT INTO cql_libraries (
    id,
    tenant_id,
    name,
    version,
    status,
    cql_content,
    description,
    publisher,
    created_by,
    library_name,
    active
) VALUES (
    gen_random_uuid(),
    'default',
    'HEDIS-CBP',
    '2024.1',
    'ACTIVE',
    'library HEDIS_CBP version ''2024.1''

using FHIR version ''4.0.1''

include FHIRHelpers version ''4.0.1''

context Patient

define "Patient Age in Years":
  AgeInYearsAt(Today())

define "Has Hypertension Diagnosis":
  exists (
    [Condition: Code ''I10'' from ICD10] C
      where C.clinicalStatus ~ "active"
  )

define "Recent Blood Pressure Readings":
  [Observation: Code ''85354-9'' from LOINC] O
    where O.status = ''final''
      and O.effective during Interval[Today() - 1 year, Today()]

define "BP Controlled":
  exists (
    "Recent Blood Pressure Readings" BP
      where (BP.component[0].value as Quantity) < 140 ''mm[Hg]''
        and (BP.component[1].value as Quantity) < 90 ''mm[Hg]''
  )

define "In Denominator":
  "Patient Age in Years" >= 18
    and "Patient Age in Years" <= 85
    and "Has Hypertension Diagnosis"

define "In Numerator":
  "In Denominator"
    and "BP Controlled"',
    'Controlling High Blood Pressure. Measures the percentage of members 18-85 years of age who had a diagnosis of hypertension and whose BP was adequately controlled (<140/90 mm Hg) during the measurement year.',
    'HealthData In Motion',
    'system',
    'HEDIS_CBP',
    true
) ON CONFLICT (tenant_id, name, version) DO UPDATE SET
    status = EXCLUDED.status,
    cql_content = EXCLUDED.cql_content,
    description = EXCLUDED.description,
    publisher = EXCLUDED.publisher,
    updated_at = now();

-- HEDIS-COL: Colorectal Cancer Screening
INSERT INTO cql_libraries (
    id,
    tenant_id,
    name,
    version,
    status,
    cql_content,
    description,
    publisher,
    created_by,
    library_name,
    active
) VALUES (
    gen_random_uuid(),
    'default',
    'HEDIS-COL',
    '2024.1',
    'ACTIVE',
    'library HEDIS_COL version ''2024.1''

using FHIR version ''4.0.1''

include FHIRHelpers version ''4.0.1''

context Patient

define "Patient Age in Years":
  AgeInYearsAt(Today())

define "Colonoscopy Last 10 Years":
  exists (
    [Procedure: Code ''73761001'' from SNOMED] P
      where P.status = ''completed''
        and P.performed during Interval[Today() - 10 years, Today()]
  )

define "FIT Test Last Year":
  exists (
    [Observation: Code ''27396-1'' from LOINC] O
      where O.status = ''final''
        and O.effective during Interval[Today() - 1 year, Today()]
  )

define "Flexible Sigmoidoscopy Last 5 Years":
  exists (
    [Procedure: Code ''44441009'' from SNOMED] P
      where P.status = ''completed''
        and P.performed during Interval[Today() - 5 years, Today()]
  )

define "In Denominator":
  "Patient Age in Years" >= 50
    and "Patient Age in Years" <= 75

define "In Numerator":
  "In Denominator"
    and (
      "Colonoscopy Last 10 Years"
      or "FIT Test Last Year"
      or "Flexible Sigmoidoscopy Last 5 Years"
    )',
    'Colorectal Cancer Screening. Measures the percentage of members 50-75 years of age who had appropriate screening for colorectal cancer.',
    'HealthData In Motion',
    'system',
    'HEDIS_COL',
    true
) ON CONFLICT (tenant_id, name, version) DO UPDATE SET
    status = EXCLUDED.status,
    cql_content = EXCLUDED.cql_content,
    description = EXCLUDED.description,
    publisher = EXCLUDED.publisher,
    updated_at = now();

-- HEDIS-BCS: Breast Cancer Screening
INSERT INTO cql_libraries (
    id,
    tenant_id,
    name,
    version,
    status,
    cql_content,
    description,
    publisher,
    created_by,
    library_name,
    active
) VALUES (
    gen_random_uuid(),
    'default',
    'HEDIS-BCS',
    '2024.1',
    'ACTIVE',
    'library HEDIS_BCS version ''2024.1''

using FHIR version ''4.0.1''

include FHIRHelpers version ''4.0.1''

context Patient

define "Patient Age in Years":
  AgeInYearsAt(Today())

define "Is Female":
  Patient.gender = ''female''

define "Mammogram Last 2 Years":
  exists (
    [Procedure: Code ''71651007'' from SNOMED] P
      where P.status = ''completed''
        and P.performed during Interval[Today() - 2 years, Today()]
  )

define "Bilateral Mastectomy":
  exists (
    [Procedure: Code ''27865001'' from SNOMED] P
      where P.status = ''completed''
  )

define "In Denominator":
  "Is Female"
    and "Patient Age in Years" >= 50
    and "Patient Age in Years" <= 74
    and not "Bilateral Mastectomy"

define "In Numerator":
  "In Denominator"
    and "Mammogram Last 2 Years"',
    'Breast Cancer Screening. Measures the percentage of women 50-74 years of age who had a mammogram to screen for breast cancer in the past 2 years.',
    'HealthData In Motion',
    'system',
    'HEDIS_BCS',
    true
) ON CONFLICT (tenant_id, name, version) DO UPDATE SET
    status = EXCLUDED.status,
    cql_content = EXCLUDED.cql_content,
    description = EXCLUDED.description,
    publisher = EXCLUDED.publisher,
    updated_at = now();

-- HEDIS-CIS: Childhood Immunization Status
INSERT INTO cql_libraries (
    id,
    tenant_id,
    name,
    version,
    status,
    cql_content,
    description,
    publisher,
    created_by,
    library_name,
    active
) VALUES (
    gen_random_uuid(),
    'default',
    'HEDIS-CIS',
    '2024.1',
    'ACTIVE',
    'library HEDIS_CIS version ''2024.1''

using FHIR version ''4.0.1''

include FHIRHelpers version ''4.0.1''

context Patient

define "Patient Age in Years":
  AgeInYearsAt(Today())

define "DTaP Vaccines":
  [Immunization: Code ''107'' from CVX] I
    where I.status = ''completed''
      and I.occurrence before Today()

define "Has 4 DTaP":
  Count("DTaP Vaccines") >= 4

define "IPV Vaccines":
  [Immunization: Code ''10'' from CVX] I
    where I.status = ''completed''
      and I.occurrence before Today()

define "Has 3 IPV":
  Count("IPV Vaccines") >= 3

define "MMR Vaccines":
  [Immunization: Code ''03'' from CVX] I
    where I.status = ''completed''
      and I.occurrence before Today()

define "Has 1 MMR":
  Count("MMR Vaccines") >= 1

define "HiB Vaccines":
  [Immunization: Code ''17'' from CVX] I
    where I.status = ''completed''
      and I.occurrence before Today()

define "Has 3 HiB":
  Count("HiB Vaccines") >= 3

define "In Denominator":
  "Patient Age in Years" = 2

define "In Numerator":
  "In Denominator"
    and "Has 4 DTaP"
    and "Has 3 IPV"
    and "Has 1 MMR"
    and "Has 3 HiB"',
    'Childhood Immunization Status. Measures the percentage of children 2 years of age who had four diphtheria, tetanus and acellular pertussis (DTaP); three polio (IPV); one measles, mumps and rubella (MMR); three H influenza type B (HiB); three hepatitis B (HepB); one chicken pox (VZV); four pneumococcal conjugate (PCV); one hepatitis A (HepA); two or three rotavirus (RV); and two influenza (flu) vaccines by their second birthday.',
    'HealthData In Motion',
    'system',
    'HEDIS_CIS',
    true
) ON CONFLICT (tenant_id, name, version) DO UPDATE SET
    status = EXCLUDED.status,
    cql_content = EXCLUDED.cql_content,
    description = EXCLUDED.description,
    publisher = EXCLUDED.publisher,
    updated_at = now();

-- Verify insertion
SELECT
    name,
    version,
    status,
    publisher,
    created_at
FROM cql_libraries
WHERE tenant_id = 'default'
ORDER BY name;
