## Summary

- What changed:
- Why:
- Risk level: low / medium / high

## Validation

- [ ] Relevant local tests run and passing
- [ ] If frontend session/auth code changed, ran `npm --prefix frontend run test:session-flow`
- [ ] If session flow UI changed, ran `npm --prefix frontend run e2e:session-flow` (or attached reason if not run)

## Merge Gate Checklist

- [ ] PR targets protected branch (`main` or `develop`) when applicable
- [ ] Required checks are green, including:
  - [ ] `MCP Release Gate / release-gate`
  - [ ] `Frontend Session Flow E2E / session-flow` (when `frontend/**` is affected)
  - [ ] `Frontend Session Flow E2E / session-flow-external-auth` (for auth/login config changes; CI runs this conditionally)
  - [ ] `Frontend Session Flow E2E / auth-callback` (for auth/login config changes; CI runs this conditionally)
- [ ] Branch protection expectations verified against `docs/runbooks/CI_BRANCH_PROTECTION_CHECKLIST.md`

## Deployment Notes

- [ ] No deployment impact
- [ ] Deployment/runbook updates included (if needed)
- [ ] Rollback approach documented (if needed)
