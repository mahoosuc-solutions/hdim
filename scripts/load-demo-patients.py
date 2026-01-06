#!/usr/bin/env python3
"""
Load Demo Patients with Historical Data

This script creates the 5 demo patients with their full clinical histories,
properly updating patient references to match server-generated UUIDs.
"""

import json
import requests
import sys
from pathlib import Path

# Configuration
FHIR_SERVICE_URL = "http://localhost:8085/fhir"
TENANT_ID = "DEMO_TENANT"

HEADERS = {
    "Content-Type": "application/fhir+json",
    "X-Tenant-ID": TENANT_ID,
    "X-User-ID": "00000000-0000-0000-0000-000000000001",
    "X-Auth-User-Id": "00000000-0000-0000-0000-000000000001",
    "X-Auth-Username": "demo-admin",
    "X-Auth-Tenant-Ids": TENANT_ID,
    "X-Auth-Roles": "ADMIN",
    "X-Auth-Validated": "gateway-dev-mode"
}

# Actual patient UUIDs from server (already created)
PATIENT_IDS = {
    "maria-garcia": "0ff05f22-e32c-4c33-8f8a-35abf7b516e7",
    "robert-chen": "7cc5c483-b586-4ae7-ac49-eec39b67493c",
    "angela-williams": "16c8df0d-df20-41f3-9e8a-7119c44943b3",
    "james-thompson": "522e2c9e-28ef-4b8f-8db1-23bf4a21c7bf",
    "patricia-davis": "c402c047-342b-431a-8b17-f2795a43f521"
}

def create_resource(resource_type, resource, description):
    """Create a FHIR resource via API."""
    # Remove ID to let server generate it
    resource.pop('id', None)

    url = f"{FHIR_SERVICE_URL}/{resource_type}"
    response = requests.post(url, headers=HEADERS, json=resource)

    if response.status_code in [200, 201]:
        result = response.json()
        print(f"  OK - Created {description}: {result.get('id', 'N/A')}")
        return result.get('id')
    else:
        print(f"  FAILED - {description}: {response.status_code}")
        try:
            error = response.json()
            print(f"    Error: {error.get('error', response.text[:200])}")
        except:
            print(f"    Response: {response.text[:200]}")
        return None

def update_patient_reference(resource, patient_key):
    """Update patient/subject reference to use actual patient UUID."""
    patient_id = PATIENT_IDS.get(patient_key)
    if not patient_id:
        print(f"  WARNING: Unknown patient key: {patient_key}")
        return

    # Update subject reference (used by Procedure, Observation, etc.)
    if 'subject' in resource:
        resource['subject']['reference'] = f"Patient/{patient_id}"

    # Update patient reference (used by Immunization, Encounter, etc.)
    if 'patient' in resource:
        resource['patient']['reference'] = f"Patient/{patient_id}"

def load_bundle(file_path, patient_key, description):
    """Load a bundle of resources."""
    with open(file_path) as f:
        bundle = json.load(f)

    if 'entry' not in bundle:
        print(f"  WARNING: No entries in bundle {file_path}")
        return

    print(f"  Loading {description} ({len(bundle['entry'])} resources)...")
    for entry in bundle['entry']:
        resource = entry.get('resource', {})
        resource_type = resource.get('resourceType', 'Unknown')
        update_patient_reference(resource, patient_key)
        create_resource(resource_type, resource, f"{resource_type}")

def main():
    base_dir = Path(__file__).parent.parent / "demo" / "fhir-resources"

    print("=" * 60)
    print("Loading Demo Patient Clinical Data")
    print("=" * 60)

    # Maria Garcia - Colonoscopy
    print("\n--- Maria Garcia (Colonoscopy Gap - OPEN) ---")
    with open(base_dir / "maria-garcia" / "procedure-colonoscopy-2019.json") as f:
        proc = json.load(f)
    update_patient_reference(proc, "maria-garcia")
    create_resource("Procedure", proc, "2019 Colonoscopy")

    # Robert Chen - Diabetes
    print("\n--- Robert Chen (Diabetes Gaps - MIXED) ---")
    with open(base_dir / "robert-chen" / "condition-diabetes.json") as f:
        cond = json.load(f)
    update_patient_reference(cond, "robert-chen")
    create_resource("Condition", cond, "Diabetes Condition")

    load_bundle(base_dir / "robert-chen" / "observations-hba1c.json", "robert-chen", "HbA1c Observations")
    load_bundle(base_dir / "robert-chen" / "procedure-eye-exam.json", "robert-chen", "Eye Exams")

    # Angela Williams - Breast Cancer Screening
    print("\n--- Angela Williams (BCS Gap - CLOSED) ---")
    load_bundle(base_dir / "angela-williams" / "procedures-mammogram.json", "angela-williams", "Mammograms")

    # James Thompson - Cardiovascular
    print("\n--- James Thompson (CBP/SPC Gaps - OPEN) ---")
    load_bundle(base_dir / "james-thompson" / "conditions.json", "james-thompson", "Conditions")
    load_bundle(base_dir / "james-thompson" / "observations-bp.json", "james-thompson", "BP Observations")

    # Patricia Davis - Preventive Care
    print("\n--- Patricia Davis (AWC/FLU Gaps - CLOSED) ---")
    load_bundle(base_dir / "patricia-davis" / "encounters-wellness.json", "patricia-davis", "Wellness Encounters")
    with open(base_dir / "patricia-davis" / "immunization-flu.json") as f:
        imm = json.load(f)
    update_patient_reference(imm, "patricia-davis")
    create_resource("Immunization", imm, "Flu Vaccine")

    print("\n" + "=" * 60)
    print("Demo Patient Data Loading Complete!")
    print("=" * 60)

if __name__ == "__main__":
    main()
