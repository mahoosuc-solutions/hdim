# Clinical Portal Recovery Plan

**Date:** November 21, 2025  
**Owner:** Platform + Frontend Enablement  
**Goal:** Restore a reliable build/run/test loop for the Clinical Portal so developers can ship UI updates and run Playwright grading suites with confidence.

---

## 1. Environment Configuration Hardening
1. **Inventory required variables** – `API_GATEWAY_URL`, `CQL_ENGINE_URL`, `QUALITY_MEASURE_URL`, `FHIR_SERVER_URL`, `DEFAULT_TENANT_ID`, `GA_MEASUREMENT_ID`, `SENTRY_DSN`. Document defaults in `.env.portal.sample` (see helper file).  
2. **Single source of truth** – Source the helper script before `nx serve`/`nx build`/`npm run e2e…` to guarantee consistent values locally and in CI.  
3. **TypeScript config alignment** – Ensure `tsconfig.app.json` (already updated) and any spec configs include `"types": ["node"]` so environment files can safely reference `process`.  
4. **Validation hook** – Add an NPM script (`lint:env`) that checks required envs are set; run it inside CI before UI builds.

## 2. Local Build & Serve Verification
1. `nx build clinical-portal --configuration=production` to confirm the Angular app compiles with current envs.  
2. `nx serve clinical-portal --configuration=production` (or equivalent) to verify runtime behavior and catch missing injected tokens.  
3. Capture any build warnings in `BUILD_STATUS.md` so regressions remain visible.

## 3. Backend/Service Dependencies
1. Use `docker-compose.local.yml` (or lightweight mocks) to boot API gateway, CQL engine, quality-measure service, and FHIR facade.  
2. When running tests, either point env vars at the mock stack or supply Playwright route intercepts so the UI never waits on unavailable upstreams.  
3. Document health-check URLs + credentials for each dependency inside `CLINICAL_PORTAL_QUICK_REFERENCE.md` (future work).

## 4. Playwright & WebServer Reliability
1. Pre-build step – Run `nx build clinical-portal` before invoking Playwright so its `webServer` start doesnt spend extra minutes compiling.  
2. Update `apps/clinical-portal-e2e/playwright.config.ts` to allow `reuseExistingServer: true` so we can start `nx serve` once and reuse across tests.  
3. Run diagnostics via `npx playwright test --config apps/clinical-portal-e2e/playwright.config.ts --trace on --reporter=list`. Capture trace artifacts for failures.  
4. Add watchdog to fail fast if the web server logs TypeScript errors (we saw `TS2591` earlier); pipeline should surface the log snippet directly.

## 5. CI Guardrails
1. Add GitHub Actions (or Nx Cloud) job that runs: `npm ci`, `npm run lint`, `nx test clinical-portal`, `nx build clinical-portal`, and finally `npm run e2e:clinical-portal:cli`.  
2. Cache `node_modules` and `.angular/cache` to keep Playwright startup under the timeout threshold.  
3. Upload Playwright report + traces as artifacts for UI reviewers.  
4. Post status to `BUILD_STATUS.md` and Slack channel #clinical-portal when failures occur.

## 6. Owners & Next Milestones
- **Dana (FE Lead):** Environment helper + build verification (Due Nov 25).  
- **Ravi (Platform):** Docker compose refresh & service docs (Due Nov 26).  
- **Maya (QE):** Playwright config updates + CI wiring (Due Nov 27).  
- **Outcome:** By Nov 29, Playwright suite should pass locally and in CI within 8 minutes with deterministic envs.
