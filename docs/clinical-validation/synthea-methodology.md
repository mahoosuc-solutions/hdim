# Synthea Methodology and Provenance

## What Is Synthea?

Synthea is an open-source synthetic patient generator developed by MITRE Corporation. It creates realistic (but not real) patient medical records in FHIR R4 format.

**Repository:** https://github.com/synthetichealth/synthea
**License:** Apache 2.0

## Peer-Reviewed Validation

Synthea's clinical validity is established by peer-reviewed research:

- **Walonoski J, et al.** "Synthea: An approach, method, and software mechanism for generating synthetic patients and the synthetic electronic health care record." *Journal of the American Medical Informatics Association (JAMIA)*, 25(3):230-238, 2018. DOI: 10.1093/jamia/ocx079

- **Walonoski J, et al.** "Synthea Novel coronavirus (COVID-19) model and synthetic data set." *Intelligence-Based Medicine*, 2020.

## Regulatory Adoption

Synthea is used by major healthcare organizations and regulators:

- **ONC (Office of the National Coordinator for Health IT):** Uses Synthea for interoperability testing and certification
- **CMS (Centers for Medicare & Medicaid Services):** Synthetic data for Blue Button 2.0 API testing
- **HL7 FHIR:** Reference implementation test data
- **MITRE:** Internal research and development

## Our Approach

### Base Generation

Synthea generates clinically valid FHIR R4 Bundles using disease progression models informed by epidemiological data. Each module (diabetes, COPD, etc.) implements published clinical guidelines and disease natural history.

### Hand-Crafted Overlays

We apply hand-crafted overlays on top of Synthea's base data to add platform-specific test targets:

- **Care gaps:** Open/closed/overdue gaps linked to quality measures
- **CQL targets:** Known expected outcomes for Clinical Quality Language evaluation
- **Quality measure periods:** Measurement year boundaries
- **CDS hook triggers:** Conditions that should fire clinical decision support cards

### Why Overlays?

Synthea generates clinically valid medical histories, but doesn't know about HDIM's specific care gap detection, quality measure evaluation, or CQL library implementations. Overlays bridge this gap while preserving Synthea's validated clinical foundation.

## Reproducibility

- Synthea uses deterministic seeds — same seed produces same patient
- Bundles are committed to version control (CI doesn't need Synthea installed)
- `generate.sh` wrapper documents exact Synthea parameters per phenotype
- `manifest.json` maps phenotype → bundle → overlay → expected outcomes
