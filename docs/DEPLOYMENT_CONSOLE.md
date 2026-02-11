# Deployment Console (MFE)

The Deployment Console is a micro frontend that guides on‑prem installs, Docker deployment, seeding, and validation.

## Run Locally

1. Start the ops service (Docker):
```bash
docker compose -f docker-compose.demo.yml up -d ops-service
```

2. Start the MFE host and remote:
```bash
npx nx serve shell-app --devRemotes=mfeDeployment
npx nx serve mfe-deployment --port=4210
```

3. Open the console:
```
http://localhost:4200/deployment
```

## Ops Service Responsibilities

- Start/stop the Docker demo stack.
- Seed data using demo‑seeding service.
- Run validation (`validate-system.sh`).
- Capture Compose logs for troubleshooting.

## Environment Overrides

Optionally set a base URL for the ops service:
```bash
export HDIM_OPS_BASE_URL=http://localhost:4710
```

Then in the browser:
```js
window.__HDIM_OPS_BASE_URL = 'http://localhost:4710';
```
