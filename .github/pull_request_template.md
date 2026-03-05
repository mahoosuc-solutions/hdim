## Summary

- What changed:
- Why:
- Risk level: low / medium / high

## Validation

- [ ] Relevant local tests run and passing
- [ ] If frontend session/auth code changed, ran `npm --prefix apps/clinical-portal run test:session-flow`
- [ ] If session flow UI changed, ran `npm --prefix apps/clinical-portal run e2e:session-flow` (or attached reason if not run)
- [ ] If `landing-page/**` changed, ran `npm --prefix landing-page run validate:ci`
- [ ] If `mcp-edge-*` code changed, ran `npm run test:mcp-edge`

## Merge Gate Checklist

- [ ] PR targets `master` branch
- [ ] Required checks are green, including:
  - [ ] `MCP Release Gate / release-gate`
  - [ ] `Frontend Session Flow E2E / session-flow` (when `apps/clinical-portal/**` is affected)
  - [ ] `Landing Page Validation` (when `landing-page/**` is affected)
  - [ ] `Frontend Session Flow E2E / session-flow-external-auth` (for auth/login config changes; CI runs this conditionally)
  - [ ] `Frontend Session Flow E2E / auth-callback` (for auth/login config changes; CI runs this conditionally)
- [ ] Branch protection expectations verified against `docs/runbooks/CI_BRANCH_PROTECTION_CHECKLIST.md`

## Deployment Notes

- [ ] No deployment impact
- [ ] Deployment/runbook updates included (if needed)
- [ ] Rollback approach documented (if needed)
