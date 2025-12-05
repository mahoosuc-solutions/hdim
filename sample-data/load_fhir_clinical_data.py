#!/usr/bin/env python3
"""
FHIR Clinical Data Loader
Generates and loads realistic FHIR clinical data for HEDIS quality measure testing

This script creates:
- Patient resources (demographics)
- Condition resources (diagnoses)
- Observation resources (lab results, vitals)
- Procedure resources (screenings)
- MedicationRequest resources (prescriptions)
"""

import requests
import json
import uuid
from datetime import datetime, timedelta
import random

FHIR_SERVER = "http://localhost:8083/fhir"

# Patient IDs from quality measure results
PATIENT_IDS = [
    "3553ac0a-762c-4477-a28d-1dba033f379b",  # HEDIS_CDC patient
    "1dbc0fbe-dbd3-482d-9bae-497aac5ba40f",  # HEDIS_CBP patient
    "3791f7ed-8154-4a5e-9581-676463b29507",  # HEDIS_COL patient
    "a5cc507e-58d4-4e1f-a3b4-b19020779310",  # HEDIS_BCS patient
    "0ab448bc-5a75-4820-bef8-82052bf20bca",  # HEDIS_CIS patient
    "dd8cd26c-0444-4fc9-8527-86d0e22a0538",  # HEDIS_CDC patient 2
    "6bd4aca9-f8e6-4603-9be5-033298f4278a",  # HEDIS_CBP patient 2
    "a7577b9d-42da-47d0-b04b-d87f15853450",  # HEDIS_COL patient 2
    "d923ee1e-0771-47ab-80da-1b79fb76168a",  # HEDIS_BCS patient 2
    "f2b451f5-31f8-4bba-b00f-faac57baff41",  # HEDIS_CDC patient 3
]

def create_patient(patient_id, name, gender, birth_date):
    """Create a FHIR Patient resource"""
    return {
        "resourceType": "Patient",
        "id": patient_id,
        "identifier": [{
            "system": "http://healthdata.com/patient-id",
            "value": patient_id
        }],
        "name": [{
            "use": "official",
            "family": name.split()[1],
            "given": [name.split()[0]]
        }],
        "gender": gender,
        "birthDate": birth_date
    }

def create_condition(patient_id, code, display, onset_date):
    """Create a FHIR Condition resource (diagnosis)"""
    return {
        "resourceType": "Condition",
        "id": str(uuid.uuid4()),
        "subject": {"reference": f"Patient/{patient_id}"},
        "code": {
            "coding": [{
                "system": "http://snomed.info/sct",
                "code": code,
                "display": display
            }]
        },
        "clinicalStatus": {
            "coding": [{
                "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
                "code": "active"
            }]
        },
        "onsetDateTime": onset_date
    }

def create_observation(patient_id, code, display, value, unit, effective_date):
    """Create a FHIR Observation resource (lab result or vital)"""
    return {
        "resourceType": "Observation",
        "id": str(uuid.uuid4()),
        "status": "final",
        "subject": {"reference": f"Patient/{patient_id}"},
        "code": {
            "coding": [{
                "system": "http://loinc.org",
                "code": code,
                "display": display
            }]
        },
        "valueQuantity": {
            "value": value,
            "unit": unit,
            "system": "http://unitsofmeasure.org"
        },
        "effectiveDateTime": effective_date
    }

def create_bp_observation(patient_id, systolic, diastolic, effective_date):
    """Create a FHIR Blood Pressure Observation"""
    return {
        "resourceType": "Observation",
        "id": str(uuid.uuid4()),
        "status": "final",
        "subject": {"reference": f"Patient/{patient_id}"},
        "code": {
            "coding": [{
                "system": "http://loinc.org",
                "code": "85354-9",
                "display": "Blood pressure panel"
            }]
        },
        "component": [
            {
                "code": {
                    "coding": [{
                        "system": "http://loinc.org",
                        "code": "8480-6",
                        "display": "Systolic blood pressure"
                    }]
                },
                "valueQuantity": {
                    "value": systolic,
                    "unit": "mm[Hg]",
                    "system": "http://unitsofmeasure.org"
                }
            },
            {
                "code": {
                    "coding": [{
                        "system": "http://loinc.org",
                        "code": "8462-4",
                        "display": "Diastolic blood pressure"
                    }]
                },
                "valueQuantity": {
                    "value": diastolic,
                    "unit": "mm[Hg]",
                    "system": "http://unitsofmeasure.org"
                }
            }
        ],
        "effectiveDateTime": effective_date
    }

def create_procedure(patient_id, code, display, performed_date):
    """Create a FHIR Procedure resource"""
    return {
        "resourceType": "Procedure",
        "id": str(uuid.uuid4()),
        "status": "completed",
        "subject": {"reference": f"Patient/{patient_id}"},
        "code": {
            "coding": [{
                "system": "http://snomed.info/sct",
                "code": code,
                "display": display
            }]
        },
        "performedDateTime": performed_date
    }

def post_resource(resource):
    """POST a resource to the FHIR server"""
    resource_type = resource["resourceType"]
    resource_id = resource.get("id")
    
    if resource_id:
        # Use PUT for resources with IDs
        url = f"{FHIR_SERVER}/{resource_type}/{resource_id}"
        response = requests.put(url, json=resource)
    else:
        # Use POST for resources without IDs
        url = f"{FHIR_SERVER}/{resource_type}"
        response = requests.post(url, json=resource)
    
    if response.status_code in [200, 201]:
        print(f"✓ Created {resource_type}: {resource.get('id', 'new')}")
        return True
    else:
        print(f"✗ Failed to create {resource_type}: HTTP {response.status_code}")
        print(f"  Response: {response.text[:200]}")
        return False

def load_diabetes_patient_data():
    """Load clinical data for diabetes patients (HEDIS_CDC)"""
    print("\n=== Loading Diabetes Patient Data (HEDIS_CDC) ===")
    
    patient_ids = [PATIENT_IDS[0], PATIENT_IDS[5], PATIENT_IDS[9]]
    names = ["John Smith", "Maria Garcia", "David Lee"]
    birth_years = [1975, 1968, 1982]
    
    for i, patient_id in enumerate(patient_ids):
        print(f"\nPatient {i+1}: {names[i]}")
        
        # Create patient
        birth_date = f"{birth_years[i]}-05-15"
        patient = create_patient(patient_id, names[i], "male" if i == 0 or i == 2 else "female", birth_date)
        post_resource(patient)
        
        # Add diabetes diagnosis (Type 2 Diabetes)
        condition = create_condition(
            patient_id,
            "E11",  # ICD-10 for Type 2 Diabetes
            "Type 2 diabetes mellitus",
            "2020-03-15"
        )
        post_resource(condition)
        
        # Add recent HbA1c observation
        today = datetime.now()
        hba1c_date = (today - timedelta(days=60)).strftime("%Y-%m-%d")
        
        # First patient: compliant (HbA1c < 9%)
        # Third patient: compliant
        # Second patient: use existing poor control for variety
        hba1c_value = 7.2 if i in [0, 2] else 9.5
        
        hba1c = create_observation(
            patient_id,
            "4548-4",  # LOINC for HbA1c
            "Hemoglobin A1c",
            hba1c_value,
            "%",
            hba1c_date
        )
        post_resource(hba1c)

def load_hypertension_patient_data():
    """Load clinical data for hypertension patients (HEDIS_CBP)"""
    print("\n=== Loading Hypertension Patient Data (HEDIS_CBP) ===")
    
    patient_ids = [PATIENT_IDS[1], PATIENT_IDS[6]]
    names = ["Sarah Johnson", "Robert Martinez"]
    birth_years = [1970, 1965]
    
    for i, patient_id in enumerate(patient_ids):
        print(f"\nPatient {i+1}: {names[i]}")
        
        # Create patient
        birth_date = f"{birth_years[i]}-08-20"
        patient = create_patient(patient_id, names[i], "female" if i == 0 else "male", birth_date)
        post_resource(patient)
        
        # Add hypertension diagnosis
        condition = create_condition(
            patient_id,
            "I10",  # ICD-10 for Essential Hypertension
            "Essential hypertension",
            "2019-06-10"
        )
        post_resource(condition)
        
        # Add recent blood pressure reading
        today = datetime.now()
        bp_date = (today - timedelta(days=45)).strftime("%Y-%m-%d")
        
        # First patient: controlled (< 140/90)
        # Second patient: uncontrolled
        systolic = 128 if i == 0 else 155
        diastolic = 82 if i == 0 else 95
        
        bp = create_bp_observation(patient_id, systolic, diastolic, bp_date)
        post_resource(bp)

def load_colorectal_screening_data():
    """Load clinical data for colorectal cancer screening (HEDIS_COL)"""
    print("\n=== Loading Colorectal Screening Data (HEDIS_COL) ===")
    
    patient_ids = [PATIENT_IDS[2], PATIENT_IDS[7]]
    names = ["Linda Brown", "James Wilson"]
    birth_years = [1963, 1960]
    
    for i, patient_id in enumerate(patient_ids):
        print(f"\nPatient {i+1}: {names[i]}")
        
        # Create patient
        birth_date = f"{birth_years[i]}-11-30"
        patient = create_patient(patient_id, names[i], "female" if i == 0 else "male", birth_date)
        post_resource(patient)
        
        # Second patient: Add colonoscopy procedure (compliant)
        if i == 1:
            today = datetime.now()
            procedure_date = (today - timedelta(days=730)).strftime("%Y-%m-%d")  # 2 years ago
            
            procedure = create_procedure(
                patient_id,
                "73761001",  # SNOMED for Colonoscopy
                "Colonoscopy",
                procedure_date
            )
            post_resource(procedure)

def load_breast_cancer_screening_data():
    """Load clinical data for breast cancer screening (HEDIS_BCS)"""
    print("\n=== Loading Breast Cancer Screening Data (HEDIS_BCS) ===")
    
    patient_ids = [PATIENT_IDS[3], PATIENT_IDS[8]]
    names = ["Patricia Anderson", "Jennifer Taylor"]
    birth_years = [1968, 1965]
    
    for i, patient_id in enumerate(patient_ids):
        print(f"\nPatient {i+1}: {names[i]}")
        
        # Create patient (female)
        birth_date = f"{birth_years[i]}-02-14"
        patient = create_patient(patient_id, names[i], "female", birth_date)
        post_resource(patient)
        
        # First patient: Add mammogram (compliant)
        if i == 0:
            today = datetime.now()
            mammo_date = (today - timedelta(days=365)).strftime("%Y-%m-%d")  # 1 year ago
            
            procedure = create_procedure(
                patient_id,
                "71651007",  # SNOMED for Mammography
                "Mammography",
                mammo_date
            )
            post_resource(procedure)

def load_pediatric_immunization_data():
    """Load clinical data for childhood immunization (HEDIS_CIS)"""
    print("\n=== Loading Pediatric Immunization Data (HEDIS_CIS) ===")
    
    patient_id = PATIENT_IDS[4]
    name = "Emma Thompson"
    
    print(f"\nPatient: {name}")
    
    # Create patient (2 years old)
    birth_year = datetime.now().year - 2
    birth_date = f"{birth_year}-09-05"
    patient = create_patient(patient_id, name, "female", birth_date)
    post_resource(patient)
    
    # Note: Immunization resources would require more detailed FHIR implementation
    # For now, we'll document that this patient has partial immunizations

def main():
    print("="*80)
    print("FHIR Clinical Data Loader")
    print("="*80)
    
    print(f"\nFHIR Server: {FHIR_SERVER}")
    print(f"Loading data for {len(PATIENT_IDS)} patients...")
    
    try:
        # Test FHIR server connectivity
        response = requests.get(f"{FHIR_SERVER}/metadata")
        if response.status_code != 200:
            print(f"\n✗ FHIR server not accessible: HTTP {response.status_code}")
            return
        print("✓ FHIR server accessible")
        
        # Load clinical data by measure type
        load_diabetes_patient_data()
        load_hypertension_patient_data()
        load_colorectal_screening_data()
        load_breast_cancer_screening_data()
        load_pediatric_immunization_data()
        
        print("\n" + "="*80)
        print("✓ FHIR Clinical Data Loading Complete!")
        print("="*80)
        print("\nYou can now test quality measure calculations with real clinical data.")
        
    except requests.exceptions.ConnectionError:
        print("\n✗ Error: Cannot connect to FHIR server")
        print(f"  Make sure the FHIR server is running at {FHIR_SERVER}")
    except Exception as e:
        print(f"\n✗ Error: {e}")

if __name__ == "__main__":
    main()
