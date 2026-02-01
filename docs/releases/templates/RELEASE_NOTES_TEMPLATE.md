# HDIM Platform Release Notes - Version {VERSION}

**Release Date:** {DATE}
**Release Type:** {TYPE} (Major/Minor/Patch)

---

## 🎯 Release Highlights

{AUTO_GENERATED_FROM_GIT_LOG}

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
- **Migration Path:** See UPGRADE_GUIDE_{VERSION}.md
- **Timeline:** {DEPRECATION_TIMELINE}

{REPEAT_FOR_ALL_BREAKING_CHANGES}

---

## 📝 Known Issues

See `KNOWN_ISSUES_{VERSION}.md` for complete list.

**Critical:**
- {CRITICAL_ISSUE_1}
- {CRITICAL_ISSUE_2}

---

## 📚 Documentation

- **Upgrade Guide:** [UPGRADE_GUIDE_{VERSION}.md](./UPGRADE_GUIDE_{VERSION}.md)
- **Version Matrix:** [VERSION_MATRIX_{VERSION}.md](./VERSION_MATRIX_{VERSION}.md)
- **Deployment Checklist:** [PRODUCTION_DEPLOYMENT_CHECKLIST_{VERSION}.md](./PRODUCTION_DEPLOYMENT_CHECKLIST_{VERSION}.md)
- **Known Issues:** [KNOWN_ISSUES_{VERSION}.md](./KNOWN_ISSUES_{VERSION}.md)

---

## 🙏 Contributors

{AUTO_GENERATED_FROM_GIT_CONTRIBUTORS}

---

## 📦 Artifacts

- **Docker Images:** See VERSION_MATRIX_{VERSION}.md
- **Release Tag:** `{VERSION}`
- **GitHub Release:** https://github.com/{ORG}/hdim/releases/tag/{VERSION}

---

**Previous Release:** [{PREVIOUS_VERSION}](../{ PREVIOUS_VERSION}/RELEASE_NOTES_{PREVIOUS_VERSION}.md)
