# Provider Compliance Checklist

Purpose
- Help providers deploy the platform while respecting third-party licensing constraints.
- Clarify which content must be licensed vs. which is open source.

## Licensing prerequisites
- NCQA HEDIS license in place for any use of HEDIS measure specifications, VSD, or MLD.
- Confirm whether NCQA Measure Certification is required for the intended deployment.
- Confirm licenses for third-party code systems if used commercially (CPT, CDT, UB, SNOMED CT, LOINC, UCUM, UMLS, RxNorm, RadLex).
- License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md).

## Allowed in open-source distribution
- Core platform services and integrations that do not embed NCQA HEDIS content.
- CQL execution tooling and adapters (without HL7 spec text or NCQA measure content).
- Deployment templates (Docker, Kubernetes) without licensed content embedded.

## Prohibited content in open-source repos
- HEDIS measure specifications, NCQA VSD/MLD content, or NCQA-authored artifacts.
- HL7 CQL specification text beyond minimal, permitted quotations.
- Third-party code-system datasets (CPT, SNOMED CT, LOINC, UMLS, RxNorm, UB, CDT, UCUM, RadLex).

## Required notices and branding
- Include required NCQA notices and disclaimers when displaying HEDIS results.
- Use registered trademarks with the correct symbols and footnotes where required.
- Maintain a "Copyright Notices & Disclaimers" page in provider UI or reports.

## Data handling and audit readiness
- Ensure PHI handling aligns with HIPAA and applicable local regulations.
- Keep an internal record of licensed content versions (HEDIS year, VSD/MLD versions).
- Retain proof of license and certification documentation.

## Operational steps before go-live
- Validate that measure packs are supplied by the provider or licensed integrator.
- Confirm no licensed datasets are bundled in container images or code repos.
- Review docs/compliance/THIRD_PARTY_NOTICES.md for dependency licensing.
- Run license inventory scripts and resolve unknowns:
  - `python3 scripts/compliance/update-third-party-notices.py`
  - `python3 scripts/compliance/verify-third-party-licenses.py`
  - Ensure docs/compliance/LICENSE_ALLOWLIST.txt is empty for production releases.

## References
- NCQA HEDIS licensing guidance: https://www.ncqa.org/hedis/using-hedis-measures/
- HL7 CQL license page: https://cql.hl7.org/license.html
- Project boundary rules: docs/compliance/LICENSING-BOUNDARY.md
