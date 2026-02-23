# One Patient -> N Measures Validation

- Timestamp (UTC): 2026-02-23T00:44:45Z
- Base URL: http://localhost:18080/quality-measure
- Patient ID: 96d946b2-98f8-4e1c-bf96-5592e3a2c94b
- Requested Measures (N): 7
- API Mode Used: b
- Successful Evaluations: 6
- Failed Evaluations: 1
- Patient Evaluation Count Endpoint Result: 51
- Total Runtime (ms): 1922
- Claim Validated: false

## Per-Measure Results

| Measure ID | Status | HTTP | Runtime (ms) | Evaluation ID |
|---|---|---:|---:|---|
| HEDIS-BCS | pass | 201 | 654 | 84a69f23-0328-4be0-b441-bfd037698bfa |
| HEDIS-CBP | pass | 201 | 147 | 5399a9b2-9a4b-493b-abee-32d6c5505a07 |
| HEDIS-CCS | pass | 201 | 125 | a0ac7817-f03b-4719-98c1-3d5e54c6796b |
| HEDIS-CDC | pass | 201 | 144 | 5987a289-d064-4a1c-9721-f92ed3d0d00b |
| HEDIS-COL | pass | 201 | 143 | d64bccc2-4a4c-4631-b6b0-6e6df72f83ba |
| HEDIS-EED | pass | 201 | 122 | 9d5023d2-032d-483c-9904-89f7299c5caa |
| HEDIS-SPC | fail | 500 | 118 |  |

JSON evidence: one-patient-n-measures-20260223T004445Z.json
