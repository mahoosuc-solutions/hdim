# HDIM Service Validation Registry

`services.yml` is the single source of truth for all 58 HDIM service requirements,
SLO targets, and test coverage tiers.

## Usage

Run smoke validation against the demo stack:

```bash
./load-tests/run-smoke-all.sh
```

## Tiers

| Tier | What runs |
|------|-----------|
| `smoke-only` | k6 HTTP contract checks (1 VU × 3 iterations) |
| `smoke+functional` | Contract checks + `./gradlew testIntegration` (Phase 2) |

## Adding a New Service

Add an entry to `services.yml` following the existing pattern.
The smoke runner picks it up automatically on next run.
