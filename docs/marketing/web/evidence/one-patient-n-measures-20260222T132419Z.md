# One Patient -> N Measures Validation

- Timestamp (UTC): 2026-02-22T13:24:19Z
- Base URL: http://localhost:18080/quality-measure
- Patient ID: 452f4af4-43ba-4ae1-9ce3-572d3d51e615
- Requested Measures (N): 10
- API Mode Used: b
- Successful Evaluations: 7
- Failed Evaluations: 3
- Patient Evaluation Count Endpoint Result: 12
- Total Runtime (ms): 2316
- Claim Validated: false

## Per-Measure Results

| Measure ID | Status | HTTP | Runtime (ms) | Evaluation ID |
|---|---|---:|---:|---|
| HEDIS-BCS | pass | 201 | 170 | ba371e00-704d-46b3-994c-1be8643908ae |
| HEDIS-CBP | pass | 201 | 105 | 0a1edb39-fae5-48f8-b2ad-e9333ff66e97 |
| HEDIS-CCS | pass | 201 | 139 | db8f91fd-c502-402b-8c8c-75e365976efb |
| HEDIS-CDC | pass | 201 | 145 | 1d40fedb-c667-47cd-bfb6-c0eedebcac04 |
| HEDIS-COL | pass | 201 | 320 | 4305d118-e74e-41cb-86f3-6b85e0efcbd7 |
| HEDIS-COU | fail | 500 | 143 |  |
| HEDIS-IMA | pass | 201 | 344 | 41772b29-27cc-48d0-a2a6-b24fe544c0c4 |
| HEDIS-SPD | pass | 201 | 106 | 116acea0-f6a3-4c8f-a986-b162c400df91 |
| HEDIS-FMC | fail | 500 | 75 |  |
| HEDIS-AAB | fail | 500 | 72 |  |

JSON evidence: one-patient-n-measures-20260222T132419Z.json
