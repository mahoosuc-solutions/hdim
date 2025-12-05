#!/bin/bash

# Create Duplicate Patient Records for MPI Demo
# This script adds duplicate records to the FHIR server for testing the MPI feature

FHIR_URL="http://localhost:8000/api/fhir"

echo "Creating duplicate patient records for MPI demo..."
echo "=================================================="

# Duplicate 1: Perfect duplicate of John Doe (ID 1) - 100% match except MRN
echo ""
echo "1. Creating perfect duplicate of John Doe..."
curl -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-9001"
    }],
    "active": true,
    "name": [{
      "use": "official",
      "family": "Doe",
      "given": ["John"]
    }],
    "gender": "male",
    "birthDate": "1959-11-14"
  }' 2>/dev/null | python3 -c "import json, sys; d=json.load(sys.stdin); print(f'Created patient ID: {d.get(\"id\", \"unknown\")}')"

# Duplicate 2: Name variation of Jane Smith (ID 2) - 90% match
echo ""
echo "2. Creating name variation of Jane Smith..."
curl -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-9002"
    }],
    "active": true,
    "name": [{
      "use": "official",
      "family": "Smith",
      "given": ["Jane", "Marie"]
    }],
    "gender": "female",
    "birthDate": "1979-11-14"
  }' 2>/dev/null | python3 -c "import json, sys; d=json.load(sys.stdin); print(f'Created patient ID: {d.get(\"id\", \"unknown\")}')"

# Duplicate 3: Typo in Robert Johnson (ID 3) - 88% match
echo ""
echo "3. Creating typo variation of Robert Johnson..."
curl -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-9003"
    }],
    "active": true,
    "name": [{
      "use": "official",
      "family": "Jonson",
      "given": ["Robert"]
    }],
    "gender": "male",
    "birthDate": "1952-11-14"
  }' 2>/dev/null | python3 -c "import json, sys; d=json.load(sys.stdin); print(f'Created patient ID: {d.get(\"id\", \"unknown\")}')"

# Duplicate 4: Another perfect duplicate with different MRN
echo ""
echo "4. Creating another duplicate with spelling variation..."
curl -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-9004"
    }],
    "active": true,
    "name": [{
      "use": "official",
      "family": "Doe",
      "given": ["Jon"]
    }],
    "gender": "male",
    "birthDate": "1959-11-14"
  }' 2>/dev/null | python3 -c "import json, sys; d=json.load(sys.stdin); print(f'Created patient ID: {d.get(\"id\", \"unknown\")}')"

# Duplicate 5: Middle name variation
echo ""
echo "5. Creating middle name variation..."
curl -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-9005"
    }],
    "active": true,
    "name": [{
      "use": "official",
      "family": "Smith",
      "given": ["Jane", "M"]
    }],
    "gender": "female",
    "birthDate": "1979-11-14"
  }' 2>/dev/null | python3 -c "import json, sys; d=json.load(sys.stdin); print(f'Created patient ID: {d.get(\"id\", \"unknown\")}')"

echo ""
echo "=================================================="
echo "✅ Duplicate patient records created successfully!"
echo ""
echo "Expected duplicates:"
echo "  - John Doe (MRN-0001) has 2 duplicates: MRN-9001 (perfect), MRN-9004 (Jon Doe)"
echo "  - Jane Smith (MRN-0002) has 2 duplicates: MRN-9002 (Jane Marie), MRN-9005 (Jane M)"
echo "  - Robert Johnson (MRN-0003) has 1 duplicate: MRN-9003 (Jonson typo)"
echo ""
echo "Now:"
echo "  1. Navigate to http://localhost:4200/patients"
echo "  2. Click 'Detect Duplicates' button"
echo "  3. Watch the MPI algorithm link the duplicates!"
echo ""
