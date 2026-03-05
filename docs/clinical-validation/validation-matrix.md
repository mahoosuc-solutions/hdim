# Validation Matrix

Maps each phenotype to the tools, measures, and expected outcomes it validates.

## Matrix

| Phenotype | Tool | Expected Outcome | Clinical Basis |
|-----------|------|-------------------|----------------|
| t2dm-managed | `care_gap_list` | 0 gaps | All screenings current per ADA |
| t2dm-managed | `measure_evaluate(HbA1c-Control)` | Compliant | HbA1c 6.8% < 7% target |
| t2dm-managed | `cql_evaluate(Diabetic-Eye-Exam)` | Compliant | Retinopathy screening done |
| t2dm-managed | `patient_risk` | Low | Controlled chronic condition |
| t2dm-unmanaged | `care_gap_list` | 2 gaps | Missing retinopathy + nephropathy |
| t2dm-unmanaged | `care_gap_identify` | retinopathy, nephropathy | ADA screening requirements |
| t2dm-unmanaged | `cql_evaluate(HbA1c-Control)` | Non-compliant | HbA1c 9.2% > 7% target |
| t2dm-unmanaged | `cds_patient_view` | Alert cards | Uncontrolled diabetes triggers CDS |
| chf-polypharmacy | `patient_summary` | 8+ medications | Complex medication regimen |
| chf-polypharmacy | `patient_risk` | High | CHF + polypharmacy + recent ED |
| chf-polypharmacy | `patient_timeline` | Recent ED encounter | 30-day ED visit |
| preventive-gaps | `care_gap_list` | 3+ gaps | Colonoscopy + flu + lipid |
| preventive-gaps | `patient_risk` | Moderate | Screening gaps, no chronic disease |
| healthy-pediatric | `patient_summary` | No active conditions | Healthy child |
| healthy-pediatric | `care_gap_list` | 0 gaps | All immunizations current |
| multi-chronic-elderly | `health_score` | Low | COPD + CKD3 + hypertension |
| multi-chronic-elderly | `patient_risk` | High | Multiple chronic conditions |
| multi-chronic-elderly | `patient_summary` | 3+ conditions | Multi-morbidity |

## Coverage Summary

| Tool Category | Tools Covered | Phenotypes Used |
|---------------|---------------|-----------------|
| FHIR | fhir_read, fhir_search, fhir_create | All (via setup) |
| Patient | patient_summary, patient_timeline, patient_risk, patient_list | All |
| Care Gaps | care_gap_list, care_gap_identify, care_gap_stats, care_gap_population, care_gap_close | t2dm-*, preventive-gaps |
| Quality | measure_evaluate, measure_results, measure_score, measure_population | t2dm-*, multi-chronic |
| CQL | cql_evaluate, cql_batch, cql_result, cql_libraries | t2dm-* |
| CDS | cds_patient_view, health_score, pre_visit_plan | t2dm-unmanaged, multi-chronic |
| DevOps | docker_status, docker_logs, service_dependencies, compose_config, build_status | N/A (Docker-based) |
| Platform | edge_health, platform_health, platform_info, fhir_metadata, service_catalog, dashboard_stats, demo_status, demo_seed | N/A (non-patient) |

**Total:** 40+ tool round-trip tests across 6 phenotypes
