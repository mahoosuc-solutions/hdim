# HDIM Ops Server

Lightweight HTTP service that orchestrates on-prem deployment steps for the MFE Deployment Console.

## Endpoints
- `GET /ops/status` — Returns docker compose status + seeding log tail.
- `POST /ops/command` — Executes a whitelisted command (`start`, `stop`, `seed`, `validate`, `capture-logs`).

## Local Run
```bash
node tools/ops-server/server.js
```

## Docker Run
```bash
docker build -t hdim-ops-server -f tools/ops-server/Dockerfile .
docker run --rm -p 4710:4710 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(pwd):/workspace \
  hdim-ops-server
```
