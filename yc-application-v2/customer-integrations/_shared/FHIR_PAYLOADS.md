# Shared FHIR Payload Examples

> Reusable FHIR R4 payload examples for customer integration scenarios. All examples conform to US Core Implementation Guide.

## Table of Contents

1. [Patient Resource](#patient-resource)
2. [Condition Resource](#condition-resource)
3. [Medication Request](#medication-request)
4. [Observation - Lab Results](#observation-lab-results)
5. [Observation - Vitals](#observation-vitals)
6. [Immunization Resource](#immunization-resource)
7. [Encounter Resource](#encounter-resource)
8. [Complete Patient Bundle](#complete-patient-bundle)
9. [Bulk Export Format](#bulk-export-format)

---

## Patient Resource

### Basic Patient (Demographics Only)

```json
{
  "resourceType": "Patient",
  "id": "patient-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient"]
  },
  "identifier": [
    {
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-12345"
    }
  ],
  "active": true,
  "name": [
    {
      "use": "official",
      "family": "Garcia",
      "given": ["Maria", "Elena"]
    }
  ],
  "telecom": [
    {
      "system": "phone",
      "value": "555-123-4567",
      "use": "home"
    },
    {
      "system": "email",
      "value": "maria.garcia@email.com"
    }
  ],
  "gender": "female",
  "birthDate": "1968-03-15",
  "address": [
    {
      "use": "home",
      "line": ["123 Main Street", "Apt 4B"],
      "city": "Springfield",
      "state": "MA",
      "postalCode": "01101"
    }
  ],
  "communication": [
    {
      "language": {
        "coding": [
          {
            "system": "urn:ietf:bcp:47",
            "code": "en",
            "display": "English"
          }
        ]
      },
      "preferred": true
    }
  ]
}
```

### Patient with Extensions (Race, Ethnicity)

```json
{
  "resourceType": "Patient",
  "id": "patient-002",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient"]
  },
  "extension": [
    {
      "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race",
      "extension": [
        {
          "url": "ombCategory",
          "valueCoding": {
            "system": "urn:oid:2.16.840.1.113883.6.238",
            "code": "2106-3",
            "display": "White"
          }
        },
        {
          "url": "text",
          "valueString": "White"
        }
      ]
    },
    {
      "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity",
      "extension": [
        {
          "url": "ombCategory",
          "valueCoding": {
            "system": "urn:oid:2.16.840.1.113883.6.238",
            "code": "2186-5",
            "display": "Not Hispanic or Latino"
          }
        },
        {
          "url": "text",
          "valueString": "Not Hispanic or Latino"
        }
      ]
    }
  ],
  "identifier": [
    {
      "system": "http://hospital.example.org/mrn",
      "value": "MRN-67890"
    }
  ],
  "name": [
    {
      "family": "Johnson",
      "given": ["Robert", "James"]
    }
  ],
  "gender": "male",
  "birthDate": "1955-07-22"
}
```

---

## Condition Resource

### Diabetes Mellitus Type 2

```json
{
  "resourceType": "Condition",
  "id": "condition-diabetes-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition"]
  },
  "clinicalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        "code": "active",
        "display": "Active"
      }
    ]
  },
  "verificationStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-ver-status",
        "code": "confirmed",
        "display": "Confirmed"
      }
    ]
  },
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/condition-category",
          "code": "problem-list-item",
          "display": "Problem List Item"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "44054006",
        "display": "Diabetes mellitus type 2"
      },
      {
        "system": "http://hl7.org/fhir/sid/icd-10-cm",
        "code": "E11.9",
        "display": "Type 2 diabetes mellitus without complications"
      }
    ],
    "text": "Type 2 Diabetes Mellitus"
  },
  "subject": {
    "reference": "Patient/patient-001",
    "display": "Maria Garcia"
  },
  "onsetDateTime": "2018-06-15",
  "recordedDate": "2018-06-15"
}
```

### Hypertension

```json
{
  "resourceType": "Condition",
  "id": "condition-htn-001",
  "clinicalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        "code": "active"
      }
    ]
  },
  "verificationStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-ver-status",
        "code": "confirmed"
      }
    ]
  },
  "code": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "38341003",
        "display": "Hypertensive disorder"
      },
      {
        "system": "http://hl7.org/fhir/sid/icd-10-cm",
        "code": "I10",
        "display": "Essential (primary) hypertension"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "onsetDateTime": "2019-02-10"
}
```

### Depression (for BH measures)

```json
{
  "resourceType": "Condition",
  "id": "condition-depression-001",
  "clinicalStatus": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        "code": "active"
      }
    ]
  },
  "code": {
    "coding": [
      {
        "system": "http://snomed.info/sct",
        "code": "35489007",
        "display": "Depressive disorder"
      },
      {
        "system": "http://hl7.org/fhir/sid/icd-10-cm",
        "code": "F32.9",
        "display": "Major depressive disorder, single episode, unspecified"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "onsetDateTime": "2023-01-15"
}
```

---

## Medication Request

### Metformin (Diabetes)

```json
{
  "resourceType": "MedicationRequest",
  "id": "medrx-metformin-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest"]
  },
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "code": "860974",
        "display": "Metformin Hydrochloride 500 MG Oral Tablet"
      }
    ],
    "text": "Metformin 500mg"
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "authoredOn": "2023-06-01",
  "requester": {
    "reference": "Practitioner/practitioner-001",
    "display": "Dr. Sarah Martinez"
  },
  "dosageInstruction": [
    {
      "text": "Take 1 tablet by mouth twice daily with meals",
      "timing": {
        "repeat": {
          "frequency": 2,
          "period": 1,
          "periodUnit": "d"
        }
      },
      "route": {
        "coding": [
          {
            "system": "http://snomed.info/sct",
            "code": "26643006",
            "display": "Oral route"
          }
        ]
      },
      "doseAndRate": [
        {
          "doseQuantity": {
            "value": 500,
            "unit": "mg",
            "system": "http://unitsofmeasure.org",
            "code": "mg"
          }
        }
      ]
    }
  ],
  "dispenseRequest": {
    "numberOfRepeatsAllowed": 3,
    "quantity": {
      "value": 180,
      "unit": "tablets"
    },
    "expectedSupplyDuration": {
      "value": 90,
      "unit": "days",
      "system": "http://unitsofmeasure.org",
      "code": "d"
    }
  }
}
```

### Lisinopril (Hypertension)

```json
{
  "resourceType": "MedicationRequest",
  "id": "medrx-lisinopril-001",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "code": "314076",
        "display": "Lisinopril 10 MG Oral Tablet"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "authoredOn": "2023-06-01",
  "dosageInstruction": [
    {
      "text": "Take 1 tablet by mouth once daily",
      "timing": {
        "repeat": {
          "frequency": 1,
          "period": 1,
          "periodUnit": "d"
        }
      }
    }
  ]
}
```

### Statin (Cardiovascular)

```json
{
  "resourceType": "MedicationRequest",
  "id": "medrx-statin-001",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "code": "617311",
        "display": "Atorvastatin 20 MG Oral Tablet"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "authoredOn": "2023-06-01"
}
```

---

## Observation Lab Results

### HbA1c (Diabetes Control)

```json
{
  "resourceType": "Observation",
  "id": "obs-hba1c-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab"]
  },
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "laboratory",
          "display": "Laboratory"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "4548-4",
        "display": "Hemoglobin A1c/Hemoglobin.total in Blood"
      }
    ],
    "text": "HbA1c"
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "effectiveDateTime": "2024-09-15T10:30:00Z",
  "issued": "2024-09-15T14:00:00Z",
  "valueQuantity": {
    "value": 7.2,
    "unit": "%",
    "system": "http://unitsofmeasure.org",
    "code": "%"
  },
  "referenceRange": [
    {
      "low": {
        "value": 4.0,
        "unit": "%"
      },
      "high": {
        "value": 5.6,
        "unit": "%"
      },
      "text": "Normal: 4.0-5.6%"
    }
  ],
  "interpretation": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
          "code": "H",
          "display": "High"
        }
      ]
    }
  ]
}
```

### LDL Cholesterol

```json
{
  "resourceType": "Observation",
  "id": "obs-ldl-001",
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "laboratory"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "2089-1",
        "display": "Cholesterol in LDL [Mass/volume] in Serum or Plasma"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "effectiveDateTime": "2024-09-15",
  "valueQuantity": {
    "value": 118,
    "unit": "mg/dL",
    "system": "http://unitsofmeasure.org",
    "code": "mg/dL"
  }
}
```

### Creatinine (Kidney Function)

```json
{
  "resourceType": "Observation",
  "id": "obs-creatinine-001",
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "laboratory"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "2160-0",
        "display": "Creatinine [Mass/volume] in Serum or Plasma"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "effectiveDateTime": "2024-09-15",
  "valueQuantity": {
    "value": 1.1,
    "unit": "mg/dL"
  }
}
```

### PHQ-9 Depression Screening

```json
{
  "resourceType": "Observation",
  "id": "obs-phq9-001",
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "survey"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "44261-6",
        "display": "Patient Health Questionnaire 9 item (PHQ-9) total score"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "effectiveDateTime": "2024-10-01",
  "valueQuantity": {
    "value": 12,
    "unit": "{score}"
  },
  "interpretation": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
          "code": "H",
          "display": "High"
        }
      ],
      "text": "Moderate depression (10-14)"
    }
  ]
}
```

---

## Observation Vitals

### Blood Pressure

```json
{
  "resourceType": "Observation",
  "id": "obs-bp-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-blood-pressure"]
  },
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "vital-signs",
          "display": "Vital Signs"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "85354-9",
        "display": "Blood pressure panel with all children optional"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "effectiveDateTime": "2024-10-15T09:30:00Z",
  "component": [
    {
      "code": {
        "coding": [
          {
            "system": "http://loinc.org",
            "code": "8480-6",
            "display": "Systolic blood pressure"
          }
        ]
      },
      "valueQuantity": {
        "value": 128,
        "unit": "mmHg",
        "system": "http://unitsofmeasure.org",
        "code": "mm[Hg]"
      }
    },
    {
      "code": {
        "coding": [
          {
            "system": "http://loinc.org",
            "code": "8462-4",
            "display": "Diastolic blood pressure"
          }
        ]
      },
      "valueQuantity": {
        "value": 82,
        "unit": "mmHg",
        "system": "http://unitsofmeasure.org",
        "code": "mm[Hg]"
      }
    }
  ]
}
```

### BMI

```json
{
  "resourceType": "Observation",
  "id": "obs-bmi-001",
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "vital-signs"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "39156-5",
        "display": "Body mass index (BMI)"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-001"
  },
  "effectiveDateTime": "2024-10-15",
  "valueQuantity": {
    "value": 28.5,
    "unit": "kg/m2",
    "system": "http://unitsofmeasure.org",
    "code": "kg/m2"
  }
}
```

---

## Immunization Resource

### Influenza Vaccine

```json
{
  "resourceType": "Immunization",
  "id": "imm-flu-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization"]
  },
  "status": "completed",
  "vaccineCode": {
    "coding": [
      {
        "system": "http://hl7.org/fhir/sid/cvx",
        "code": "141",
        "display": "Influenza, seasonal, injectable"
      }
    ],
    "text": "Flu shot 2024-2025"
  },
  "patient": {
    "reference": "Patient/patient-001"
  },
  "occurrenceDateTime": "2024-10-01",
  "primarySource": true,
  "lotNumber": "FLU2024-A123",
  "expirationDate": "2025-03-31",
  "site": {
    "coding": [
      {
        "system": "http://terminology.hl7.org/CodeSystem/v3-ActSite",
        "code": "LA",
        "display": "Left arm"
      }
    ]
  },
  "performer": [
    {
      "actor": {
        "reference": "Practitioner/practitioner-001"
      }
    }
  ]
}
```

### Pneumococcal Vaccine

```json
{
  "resourceType": "Immunization",
  "id": "imm-pneumo-001",
  "status": "completed",
  "vaccineCode": {
    "coding": [
      {
        "system": "http://hl7.org/fhir/sid/cvx",
        "code": "133",
        "display": "Pneumococcal conjugate PCV 13"
      }
    ]
  },
  "patient": {
    "reference": "Patient/patient-001"
  },
  "occurrenceDateTime": "2023-11-15",
  "primarySource": true
}
```

### COVID-19 Vaccine

```json
{
  "resourceType": "Immunization",
  "id": "imm-covid-001",
  "status": "completed",
  "vaccineCode": {
    "coding": [
      {
        "system": "http://hl7.org/fhir/sid/cvx",
        "code": "208",
        "display": "COVID-19, mRNA, LNP-S, PF, 30 mcg/0.3 mL dose"
      }
    ],
    "text": "Pfizer-BioNTech COVID-19 Vaccine"
  },
  "patient": {
    "reference": "Patient/patient-001"
  },
  "occurrenceDateTime": "2024-09-15",
  "primarySource": true
}
```

---

## Encounter Resource

### Office Visit

```json
{
  "resourceType": "Encounter",
  "id": "enc-office-001",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter"]
  },
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "ambulatory"
  },
  "type": [
    {
      "coding": [
        {
          "system": "http://www.ama-assn.org/go/cpt",
          "code": "99214",
          "display": "Office or other outpatient visit, established patient"
        }
      ]
    }
  ],
  "subject": {
    "reference": "Patient/patient-001"
  },
  "participant": [
    {
      "individual": {
        "reference": "Practitioner/practitioner-001",
        "display": "Dr. Sarah Martinez"
      }
    }
  ],
  "period": {
    "start": "2024-10-15T09:00:00Z",
    "end": "2024-10-15T09:30:00Z"
  },
  "reasonCode": [
    {
      "coding": [
        {
          "system": "http://snomed.info/sct",
          "code": "185349003",
          "display": "Encounter for check up"
        }
      ],
      "text": "Annual wellness visit"
    }
  ],
  "diagnosis": [
    {
      "condition": {
        "reference": "Condition/condition-diabetes-001"
      },
      "use": {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/diagnosis-role",
            "code": "billing"
          }
        ]
      }
    }
  ]
}
```

---

## Complete Patient Bundle

### Diabetic Patient with Full Clinical Data

```json
{
  "resourceType": "Bundle",
  "id": "bundle-patient-complete-001",
  "type": "collection",
  "timestamp": "2024-10-15T15:00:00Z",
  "entry": [
    {
      "fullUrl": "urn:uuid:patient-001",
      "resource": {
        "resourceType": "Patient",
        "id": "patient-001",
        "identifier": [{"system": "http://hospital.example.org/mrn", "value": "MRN-12345"}],
        "name": [{"family": "Garcia", "given": ["Maria"]}],
        "gender": "female",
        "birthDate": "1968-03-15"
      }
    },
    {
      "fullUrl": "urn:uuid:condition-001",
      "resource": {
        "resourceType": "Condition",
        "id": "condition-001",
        "clinicalStatus": {"coding": [{"code": "active"}]},
        "code": {"coding": [{"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "E11.9", "display": "Type 2 diabetes"}]},
        "subject": {"reference": "Patient/patient-001"}
      }
    },
    {
      "fullUrl": "urn:uuid:condition-002",
      "resource": {
        "resourceType": "Condition",
        "id": "condition-002",
        "clinicalStatus": {"coding": [{"code": "active"}]},
        "code": {"coding": [{"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "I10", "display": "Essential hypertension"}]},
        "subject": {"reference": "Patient/patient-001"}
      }
    },
    {
      "fullUrl": "urn:uuid:medication-001",
      "resource": {
        "resourceType": "MedicationRequest",
        "id": "medication-001",
        "status": "active",
        "intent": "order",
        "medicationCodeableConcept": {"coding": [{"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "860974", "display": "Metformin 500 MG"}]},
        "subject": {"reference": "Patient/patient-001"},
        "authoredOn": "2024-06-01"
      }
    },
    {
      "fullUrl": "urn:uuid:observation-hba1c",
      "resource": {
        "resourceType": "Observation",
        "id": "observation-hba1c",
        "status": "final",
        "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4", "display": "HbA1c"}]},
        "subject": {"reference": "Patient/patient-001"},
        "effectiveDateTime": "2024-09-15",
        "valueQuantity": {"value": 7.2, "unit": "%"}
      }
    },
    {
      "fullUrl": "urn:uuid:observation-bp",
      "resource": {
        "resourceType": "Observation",
        "id": "observation-bp",
        "status": "final",
        "code": {"coding": [{"system": "http://loinc.org", "code": "85354-9"}]},
        "subject": {"reference": "Patient/patient-001"},
        "effectiveDateTime": "2024-10-15",
        "component": [
          {"code": {"coding": [{"code": "8480-6"}]}, "valueQuantity": {"value": 128, "unit": "mmHg"}},
          {"code": {"coding": [{"code": "8462-4"}]}, "valueQuantity": {"value": 82, "unit": "mmHg"}}
        ]
      }
    },
    {
      "fullUrl": "urn:uuid:encounter-001",
      "resource": {
        "resourceType": "Encounter",
        "id": "encounter-001",
        "status": "finished",
        "class": {"code": "AMB"},
        "subject": {"reference": "Patient/patient-001"},
        "period": {"start": "2024-10-15T09:00:00Z", "end": "2024-10-15T09:30:00Z"}
      }
    }
  ]
}
```

---

## Bulk Export Format

### NDJSON Patient File (patients.ndjson)

```
{"resourceType":"Patient","id":"p001","name":[{"family":"Garcia","given":["Maria"]}],"gender":"female","birthDate":"1968-03-15"}
{"resourceType":"Patient","id":"p002","name":[{"family":"Johnson","given":["Robert"]}],"gender":"male","birthDate":"1955-07-22"}
{"resourceType":"Patient","id":"p003","name":[{"family":"Smith","given":["Jennifer"]}],"gender":"female","birthDate":"1972-11-08"}
{"resourceType":"Patient","id":"p004","name":[{"family":"Williams","given":["Michael"]}],"gender":"male","birthDate":"1980-04-25"}
{"resourceType":"Patient","id":"p005","name":[{"family":"Brown","given":["Sarah"]}],"gender":"female","birthDate":"1990-09-12"}
```

### NDJSON Condition File (conditions.ndjson)

```
{"resourceType":"Condition","id":"c001","clinicalStatus":{"coding":[{"code":"active"}]},"code":{"coding":[{"system":"http://hl7.org/fhir/sid/icd-10-cm","code":"E11.9"}]},"subject":{"reference":"Patient/p001"}}
{"resourceType":"Condition","id":"c002","clinicalStatus":{"coding":[{"code":"active"}]},"code":{"coding":[{"system":"http://hl7.org/fhir/sid/icd-10-cm","code":"I10"}]},"subject":{"reference":"Patient/p001"}}
{"resourceType":"Condition","id":"c003","clinicalStatus":{"coding":[{"code":"active"}]},"code":{"coding":[{"system":"http://hl7.org/fhir/sid/icd-10-cm","code":"J45.909"}]},"subject":{"reference":"Patient/p002"}}
```

---

## Usage Notes

### For Integration Developers

1. All examples use US Core R4 profiles
2. Code systems are fully qualified with system URIs
3. Examples include common value sets (SNOMED, ICD-10, LOINC, RxNorm, CVX)
4. Timestamps are in ISO 8601 format with timezone

### For Customer Scenarios

Reference these examples in customer integration documents:

```markdown
See: [FHIR_PAYLOADS.md#diabetes-mellitus-type-2](../_shared/FHIR_PAYLOADS.md#diabetes-mellitus-type-2)
```

### Customization

When creating customer-specific examples:
1. Replace patient identifiers and demographics
2. Adjust clinical data to match the scenario
3. Update dates to reflect realistic timelines
4. Add organization-specific identifier systems

---

*FHIR Payloads Version: 1.0*
*FHIR Version: R4*
*Profiles: US Core STU4*
*Last Updated: December 2025*
