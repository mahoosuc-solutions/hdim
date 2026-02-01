# Third-Party License Tracking

This directory stores license texts or vendor notices that are not already captured by standard OSS licenses or package registries.

## What goes here
- Vendor notices (NCQA HEDIS, HL7 CQL spec constraints).
- Non-OSS licenses or data-set terms (UMLS, RxNorm, CPT, etc.).
- Any license text required to be redistributed with binaries or documentation.

## License texts included in this folder
- `docs/compliance/LICENSES/Apache-2.0.txt`
- `docs/compliance/LICENSES/MIT.txt`
- `docs/compliance/LICENSES/ISC.txt`
- `docs/compliance/LICENSES/BSD-2-Clause.txt`
- `docs/compliance/LICENSES/BSD-3-Clause.txt`
- `docs/compliance/LICENSES/0BSD.txt`
- `docs/compliance/LICENSES/EPL-2.0.txt`
- `docs/compliance/LICENSES/EPL-1.0.txt`
- `docs/compliance/LICENSES/Bouncy-Castle-License.txt`
- `docs/compliance/LICENSES/GSAP-Standard-License.txt`

## How to update the inventory

Run the updater
- `python3 scripts/compliance/update-third-party-notices.py`
 - `python3 scripts/compliance/verify-third-party-licenses.py`

Allowlist for unresolved licenses
- `docs/compliance/LICENSE_ALLOWLIST.txt`


1) Update direct dependency lists
- Frontend: use package.json as the source of direct dependencies.
- Backend: use backend/gradle/libs.versions.toml for the catalog of shared libraries.

2) Verify licenses
- For each dependency, confirm the license from the upstream project (repo LICENSE, package metadata, or official vendor page).
- Record the license type in docs/compliance/THIRD_PARTY_NOTICES.md.
- If a license requires redistribution of full text or a special notice, save it in this folder and reference it in the notices file.

3) Vendor-licensed content
- If the project uses NCQA HEDIS content, include the required notices/disclaimers in customer-facing reports or UI.
- Do not commit NCQA or other licensed content into the open-source repo.

## Suggested validation checks
- Confirm no HEDIS measure specs, VSD, or MLD content is committed to the repo.
- Confirm no HL7 CQL spec text is distributed outside allowed excerpts.
- Confirm third-party code-system datasets are not embedded (CPT, LOINC, SNOMED CT, UCUM, UB, CDT, UMLS, RxNorm, RadLex).
