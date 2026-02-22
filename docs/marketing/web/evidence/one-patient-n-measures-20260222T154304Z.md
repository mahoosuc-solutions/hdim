# One Patient -> N Measures Validation

- Timestamp (UTC): 2026-02-22T15:43:04Z
- Base URL: http://localhost:18080/quality-measure
- Patient ID: 452f4af4-43ba-4ae1-9ce3-572d3d51e615
- Requested Measures (N): 10
- API Mode Used: b
- Successful Evaluations: 7
- Failed Evaluations: 3
- Patient Evaluation Count Endpoint Result: 41
- Total Runtime (ms): 2334
- Claim Validated: false

## Per-Measure Results

| Measure ID | Status | HTTP | Runtime (ms) | Evaluation ID |
|---|---|---:|---:|---|
| HEDIS-BCS | pass | 201 | 131 | e7d44757-42f8-4238-9659-07c09ca51d5b |
| HEDIS-CBP | pass | 201 | 114 | 627ee9cd-687c-4f44-9c83-5ef14af6f16e |
| HEDIS-CCS | pass | 201 | 154 | 9676329d-eb2d-4ec3-98e4-facffdfc5d8e |
| HEDIS-CDC | pass | 201 | 117 | 4350ef34-f1d3-45d0-920f-56da237ea807 |
| HEDIS-COL | pass | 201 | 127 | 1df63229-39f3-4323-ae3e-7d804de2c48d |
| HEDIS-COU | fail | 500 | 126 |  |
| HEDIS-IMA | pass | 201 | 129 | 9c013a52-85df-40b3-81e0-6aa0b38c5a54 |
| HEDIS-SPD | pass | 201 | 120 | c4376a6e-dc4e-43a4-b6cb-eb997dc10b89 |
| HEDIS-FMC | fail | 500 | 150 |  |
| HEDIS-AAB | fail | 500 | 123 |  |

JSON evidence: one-patient-n-measures-20260222T154304Z.json
