#!/usr/bin/env python3

import json
import requests
import sys
import os
from pathlib import Path

# Colors for output
RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
NC = '\033[0m'  # No Color

# Configuration
CQL_ENGINE_URL = "http://localhost:8081/cql-engine/api/v1/cql/libraries"
FHIR_SERVER_URL = "http://localhost:8083/fhir"
CQL_USERNAME = "cql-service-user"
CQL_PASSWORD = "cql-service-dev-password-change-in-prod"
TENANT_ID = "default"

# Get script directory
SCRIPT_DIR = Path(__file__).parent

# Counters
measures_success = 0
measures_failed = 0
patients_success = 0
patients_failed = 0

print("=========================================")
print("Loading Sample Data into Clinical Portal")
print("=========================================")
print()

# Check if files exist
hedis_file = SCRIPT_DIR / "hedis-measures.json"
patients_file = SCRIPT_DIR / "sample-patients.json"

if not hedis_file.exists():
    print(f"{RED}Error: hedis-measures.json not found{NC}")
    sys.exit(1)

if not patients_file.exists():
    print(f"{RED}Error: sample-patients.json not found{NC}")
    sys.exit(1)

# Load HEDIS measures
print(f"{YELLOW}Loading HEDIS measures...{NC}")
print("-------------------------------------")

try:
    with open(hedis_file, 'r') as f:
        measures = json.load(f)

    for measure in measures:
        name = measure.get('name', 'Unknown')
        version = measure.get('version', 'Unknown')

        print(f"Loading {name} v{version}... ", end='', flush=True)

        try:
            response = requests.post(
                CQL_ENGINE_URL,
                auth=(CQL_USERNAME, CQL_PASSWORD),
                headers={
                    "X-Tenant-ID": TENANT_ID,
                    "Content-Type": "application/json"
                },
                json=measure,
                timeout=10
            )

            if 200 <= response.status_code < 300:
                print(f"{GREEN}SUCCESS{NC}")
                measures_success += 1
            else:
                print(f"{RED}FAILED (HTTP {response.status_code}){NC}")
                print(f"  Response: {response.text}")
                measures_failed += 1
        except Exception as e:
            print(f"{RED}FAILED{NC}")
            print(f"  Error: {str(e)}")
            measures_failed += 1

except Exception as e:
    print(f"{RED}Error reading hedis-measures.json: {str(e)}{NC}")
    sys.exit(1)

print()
print(f"Measures Loaded: {GREEN}{measures_success}{NC}")
print(f"Measures Failed: {RED}{measures_failed}{NC}")
print()

# Load sample patients
print(f"{YELLOW}Loading sample patients...{NC}")
print("-------------------------------------")

try:
    with open(patients_file, 'r') as f:
        bundle = json.load(f)

    patients = [entry['resource'] for entry in bundle.get('entry', [])]

    for patient in patients:
        identifier = patient.get('identifier', [{}])[0].get('value', 'Unknown')
        name_obj = patient.get('name', [{}])[0]
        given = name_obj.get('given', [''])[0]
        family = name_obj.get('family', '')
        full_name = f"{given} {family}"

        print(f"Loading {full_name} ({identifier})... ", end='', flush=True)

        try:
            response = requests.post(
                f"{FHIR_SERVER_URL}/Patient",
                headers={
                    "Content-Type": "application/fhir+json"
                },
                json=patient,
                timeout=10
            )

            if 200 <= response.status_code < 300:
                print(f"{GREEN}SUCCESS{NC}")
                patients_success += 1
            else:
                print(f"{RED}FAILED (HTTP {response.status_code}){NC}")
                print(f"  Response: {response.text}")
                patients_failed += 1
        except Exception as e:
            print(f"{RED}FAILED{NC}")
            print(f"  Error: {str(e)}")
            patients_failed += 1

except Exception as e:
    print(f"{RED}Error reading sample-patients.json: {str(e)}{NC}")
    sys.exit(1)

print()
print(f"Patients Loaded: {GREEN}{patients_success}{NC}")
print(f"Patients Failed: {RED}{patients_failed}{NC}")
print()

# Summary
print("=========================================")
print("Summary")
print("=========================================")
print(f"Total Measures Loaded: {GREEN}{measures_success}{NC} / {measures_success + measures_failed}")
print(f"Total Patients Loaded: {GREEN}{patients_success}{NC} / {patients_success + patients_failed}")
print()

if measures_failed == 0 and patients_failed == 0:
    print(f"{GREEN}All data loaded successfully!{NC}")
    sys.exit(0)
else:
    print(f"{RED}Some data failed to load. Please check the errors above.{NC}")
    sys.exit(1)
