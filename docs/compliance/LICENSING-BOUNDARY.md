# Licensing Boundary and Controlled Content

Purpose
- Define what can be distributed as open source vs. what must remain licensed or customer-supplied.
- Document third-party IP constraints tied to HEDIS, CQL, and clinical code systems.

Not legal advice. This file documents operational guardrails based on vendor terms.

## Controlled content (do not publish without license)

NCQA / HEDIS
- HEDIS measure specifications and related NCQA-owned IP require an NCQA license for any use.
- Commercial use or distribution requires a custom NCQA license and may require NCQA Measure Certification.
- HEDIS MLD and VSD content have their own licensing terms and must not be redistributed without NCQA approval.
- Reference: https://www.ncqa.org/hedis/using-hedis-measures/
- Reference (license notices for licensees):
  - https://wpcdn.ncqa.org/www-prod/wp-content/uploads/Notices-and-Disclaimers-for-Licensees-Products-%E2%80%93-HEDIS-Measure-Specifications.pdf
  - https://wpcdn.ncqa.org/www-prod/wp-content/uploads/Notices-and-Disclaimers-for-Licensees-Products-%E2%80%93-HEDIS-MLD.pdf
  - https://wpcdn.ncqa.org/www-prod/wp-content/uploads/Notices-and-Disclaimers-for-Licensees-Products-%E2%80%93-HEDIS-Research-Use-Only.pdf

HL7 CQL specification
- The HL7 CQL specification text is all-rights-reserved; reproduction is prohibited without permission.
- Use is governed by HL7 IP policy.
- Reference: https://cql.hl7.org/license.html

Third-party code systems referenced in NCQA notices
- CPT (AMA), CDT (ADA), UB codes (AHA), LOINC (Regenstrief), SNOMED CT (IHTSDO), UCUM (Regenstrief), RadLex (RSNA), UMLS (NLM), RxNorm (NLM).
- These may require separate licenses or terms for commercial use. Do not redistribute their datasets.
- If any of these are incorporated or bundled, add a license note in docs/compliance/THIRD_PARTY_NOTICES.md.

## Allowed open-source distribution (with guardrails)
- Core platform code, infrastructure, and tooling not embedding licensed HEDIS measure specs or code sets.
- CQL execution engine code and integrations, provided they do not ship HL7 spec text or NCQA measure content.
- Provider integration guides that describe how to plug in licensed HEDIS packs without embedding them.

## Repository guardrails
- Do not commit or distribute:
  - HEDIS measure specification logic, NCQA VSD/MLD content, or NCQA-authored artifacts.
  - HL7 CQL specification text or other HL7 IP beyond allowed snippets/quotations.
  - Third-party code-system datasets (CPT, LOINC, SNOMED CT, etc.).
- Treat measure packs as customer-supplied artifacts stored outside the open-source repo.

## Compliance actions
- Maintain signed NCQA license(s) for any HEDIS measure content use.
- Include required NCQA notices/disclaimers in any licensed product or report output.
- Track third-party dependencies and their licenses in docs/compliance/THIRD_PARTY_NOTICES.md.
- Validate whether NCQA Measure Certification is required for production deployments.
