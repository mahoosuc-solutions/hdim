# HDIM Platform Release Notes - Version v2.7.1-rc1

**Release Date:** 2026-02-15
**Release Type:** Release candidate (Patch)

---

## Release Highlights

- Nx + MCP toolchain pinned and validated locally (`npm run test:mcp`).
- Local CI gate for PR/demo validation (`scripts/ci/local-ci.sh`).
- Release candidate lane, demo readiness gate, and capture checklist established.
- Release artifacts generated for `v2.7.1-rc1` including explicit scope and capture plan.

---

## ✨ New Features

### Feature 1: {FEATURE_NAME}
- **Description:** {DESCRIPTION}
- **Endpoints:** {API_ENDPOINTS}
- **Impact:** {IMPACT}

{REPEAT_FOR_ALL_FEATURES}

---

## 🔄 Changed Features

### Change 1: {CHANGE_NAME}
- **Description:** {DESCRIPTION}
- **Migration Required:** {YES/NO}
- **Impact:** {IMPACT}

{REPEAT_FOR_ALL_CHANGES}

---

## 🛠️ Fixed Issues

- **Issue #{NUMBER}:** {DESCRIPTION}
- **PR #{NUMBER}:** {DESCRIPTION}

{AUTO_GENERATED_FROM_GITHUB_ISSUES}

---

## 🔒 Security & HIPAA Enhancements

### Security Updates
- {SECURITY_UPDATE_1}
- {SECURITY_UPDATE_2}

### HIPAA Compliance
- {HIPAA_ENHANCEMENT_1}
- {HIPAA_ENHANCEMENT_2}

---

## 📊 Performance Improvements

| Component | Metric | Before | After | Improvement |
|-----------|--------|--------|-------|-------------|
| {COMPONENT} | {METRIC} | {BEFORE} | {AFTER} | {PERCENTAGE}% |

---

## 💾 Database Migrations

**Total Migrations:** {COUNT}
**Impacted Services:** {SERVICES}

| Service | Changesets | Rollback Coverage |
|---------|------------|-------------------|
| {SERVICE} | {COUNT} | {PERCENTAGE}% |

{AUTO_GENERATED_FROM_LIQUIBASE}

---

## ⚠️ Breaking Changes

### Breaking Change 1: {CHANGE_NAME}
- **Description:** {DESCRIPTION}
- **Migration Path:** See UPGRADE_GUIDE_v2.7.1-rc1.md
- **Timeline:** {DEPRECATION_TIMELINE}

{REPEAT_FOR_ALL_BREAKING_CHANGES}

---

## 📝 Known Issues

See `KNOWN_ISSUES_v2.7.1-rc1.md` for complete list.

**Critical:**
- {CRITICAL_ISSUE_1}
- {CRITICAL_ISSUE_2}

---

## 📚 Documentation

- **Upgrade Guide:** [UPGRADE_GUIDE_v2.7.1-rc1.md](./UPGRADE_GUIDE_v2.7.1-rc1.md)
- **Version Matrix:** [VERSION_MATRIX_v2.7.1-rc1.md](./VERSION_MATRIX_v2.7.1-rc1.md)
- **Deployment Checklist:** [PRODUCTION_DEPLOYMENT_CHECKLIST_v2.7.1-rc1.md](./PRODUCTION_DEPLOYMENT_CHECKLIST_v2.7.1-rc1.md)
- **Known Issues:** [KNOWN_ISSUES_v2.7.1-rc1.md](./KNOWN_ISSUES_v2.7.1-rc1.md)

---

## 🙏 Contributors

- mahoosuc-solutions <aaron@westbethelmotel.com>
- webemo-aaron <aaron@westbethelmotel.com>

---

## 📦 Artifacts

- **Docker Images:** See VERSION_MATRIX_v2.7.1-rc1.md
- **Release Tag:** `v2.7.1-rc1`
- **GitHub Release:** (create when promoting RC to final release)

---

**Previous Release:** `v2.7.0`
