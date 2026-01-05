#!/usr/bin/env python3
"""
Load FHIR data directly into HDIM PostgreSQL database
"""
import json
import psycopg2
import uuid
from datetime import datetime
import sys

# Database connection
DB_CONFIG = {
    'host': 'localhost',
    'port': 5435,
    'database': 'fhir_db',
    'user': 'healthdata',
    'password': 'healthdata_password'
}

TENANT_ID = 'default'

def get_connection():
    return psycopg2.connect(**DB_CONFIG)

def parse_fhir_date(date_str):
    """Parse FHIR date string to Python datetime"""
    if not date_str:
        return None
    try:
        if 'T' in date_str:
            return datetime.fromisoformat(date_str.replace('Z', '+00:00'))
        return datetime.strptime(date_str[:10], '%Y-%m-%d')
    except:
        return None

# Global patient ID mapping (FHIR ID -> DB UUID)
patient_id_map = {}

def insert_patient(conn, resource):
    """Insert a Patient resource"""
    global patient_id_map
    cur = conn.cursor()

    # Extract patient data
    fhir_patient_id = resource.get('id', str(uuid.uuid4()))
    db_patient_id = str(uuid.uuid4())

    name = resource.get('name', [{}])[0]
    given = ' '.join(name.get('given', ['Unknown']))
    family = name.get('family', 'Unknown')

    gender = resource.get('gender', 'unknown')
    birth_date = parse_fhir_date(resource.get('birthDate'))

    try:
        cur.execute("""
            INSERT INTO patients (id, tenant_id, resource_type, first_name, last_name, gender, birth_date,
                                  resource_json, version, created_at, last_modified_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (
            db_patient_id, TENANT_ID, 'Patient', given, family, gender, birth_date,
            json.dumps(resource), 1, datetime.now(), datetime.now()
        ))
        # Store the mapping
        patient_id_map[fhir_patient_id] = db_patient_id
        return True
    except Exception as e:
        print(f"Error inserting patient {fhir_patient_id}: {e}")
        return False
    finally:
        cur.close()

def insert_condition(conn, resource):
    """Insert a Condition resource"""
    cur = conn.cursor()

    condition_id = resource.get('id', str(uuid.uuid4()))
    patient_ref = resource.get('subject', {}).get('reference', '')
    fhir_patient_id = patient_ref.split('/')[-1] if patient_ref else ''

    # Get the database patient UUID
    db_patient_id = patient_id_map.get(fhir_patient_id)
    if not db_patient_id:
        return False  # Skip if patient not found

    code = resource.get('code', {})
    coding = code.get('coding', [{}])[0]

    try:
        cur.execute("""
            INSERT INTO conditions (id, tenant_id, resource_type, patient_id, code, code_system, code_display,
                                    clinical_status, verification_status, onset_date, version, created_at, last_modified_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
        """, (
            str(uuid.uuid4()), TENANT_ID, 'Condition', db_patient_id,
            coding.get('code', ''), coding.get('system', ''), coding.get('display', ''),
            resource.get('clinicalStatus', {}).get('coding', [{}])[0].get('code', ''),
            resource.get('verificationStatus', {}).get('coding', [{}])[0].get('code', ''),
            parse_fhir_date(resource.get('onsetDateTime')),
            1, datetime.now(), datetime.now()
        ))
        return True
    except Exception as e:
        print(f"Error inserting condition {condition_id}: {e}")
        return False
    finally:
        cur.close()

def insert_observation(conn, resource):
    """Insert an Observation resource - skipped for now"""
    # Observations table has complex schema, skip for basic demo
    return False

def load_fhir_bundle(filename):
    """Load a FHIR bundle into the database"""
    print(f"Loading FHIR bundle from: {filename}")

    with open(filename) as f:
        bundle = json.load(f)

    entries = bundle.get('entry', [])
    print(f"Found {len(entries)} entries")

    conn = get_connection()

    counts = {'Patient': 0, 'Condition': 0, 'Observation': 0, 'other': 0}

    try:
        for i, entry in enumerate(entries):
            resource = entry.get('resource', {})
            resource_type = resource.get('resourceType')

            if resource_type == 'Patient':
                if insert_patient(conn, resource):
                    counts['Patient'] += 1
            elif resource_type == 'Condition':
                if insert_condition(conn, resource):
                    counts['Condition'] += 1
            elif resource_type == 'Observation':
                if insert_observation(conn, resource):
                    counts['Observation'] += 1
            else:
                counts['other'] += 1

            if (i + 1) % 500 == 0:
                conn.commit()
                print(f"Processed {i + 1} entries...")

        conn.commit()

        print("\n=== Loading Complete ===")
        print(f"Patients loaded: {counts['Patient']}")
        print(f"Conditions loaded: {counts['Condition']}")
        print(f"Observations loaded: {counts['Observation']}")
        print(f"Other resources skipped: {counts['other']}")

    finally:
        conn.close()

if __name__ == '__main__':
    if len(sys.argv) < 2:
        filename = '/home/mahoosuc-solutions/projects/hdim-master/test-harness/gcp-data/academic-medical-center-1000-fhir.json'
    else:
        filename = sys.argv[1]

    load_fhir_bundle(filename)
