# One Patient -> N Measures Validation

- Timestamp (UTC): 2026-02-22T13:48:02Z
- Base URL: http://localhost:18080/quality-measure
- Patient ID: 452f4af4-43ba-4ae1-9ce3-572d3d51e615
- Requested Measures (N): 10
- API Mode Used: b
- Successful Evaluations: 7
- Failed Evaluations: 3
- Patient Evaluation Count Endpoint Result: 29
- Total Runtime (ms): 10635
- Claim Validated: false

## Per-Measure Results

| Measure ID | Status | HTTP | Runtime (ms) | Evaluation ID |
|---|---|---:|---:|---|
| HEDIS-BCS | pass | 201 | 4502 | 196e0691-5c01-4d01-8349-cb17394bb07e |
| HEDIS-CBP | pass | 201 | 4058 | e4357fdd-40a6-469c-bc84-0e5b504c901f |
| HEDIS-CCS | pass | 201 | 387 | 9aceb340-5403-4ba3-81f9-cffcb6db9443 |
| HEDIS-CDC | pass | 201 | 192 | 84e031e4-2c3c-4c10-a81d-28fc52818a65 |
| HEDIS-COL | pass | 201 | 127 | 4262736b-8f69-4ff0-b27f-4ef61251936c |
| HEDIS-COU | fail | 500 | 234 |  |
| HEDIS-IMA | pass | 201 | 122 | 5228bede-ee63-437a-a811-17044dcfe6e8 |
| HEDIS-SPD | pass | 201 | 106 | cf312bfa-62d6-478f-b042-1672fe7c735e |
| HEDIS-FMC | fail | 500 | 75 |  |
| HEDIS-AAB | fail | 500 | 67 |  |

JSON evidence: one-patient-n-measures-20260222T134802Z.json
