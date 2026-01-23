# GitHub Issues - Phase 2 Summary

**Created**: January 23, 2026
**Purpose**: Track 95+ incomplete features identified in HDIM Production Release Plan

---

## Overview

Created **15 GitHub issues** to track all incomplete work identified during codebase analysis. These issues enable sprint planning, technical debt management, and release readiness assessment.

**GitHub Repository**: https://github.com/webemo-aaron/hdim

---

## Issues Created

### 🔴 CRITICAL Priority (3 issues)

#### Issue #314 - Clinical Audit Dashboard Backend APIs
- **URL**: https://github.com/webemo-aaron/hdim/issues/314
- **Endpoints**: 9 API endpoints
- **Impact**: Patient safety (clinical decision review)
- **Blocks**: Clinical decision support, AI recommendation validation
- **Sprint**: Next Sprint (Week 1-2)

#### Issue #316 - Clinical Workflow WebSocket Notifications
- **URL**: https://github.com/webemo-aaron/hdim/issues/316
- **Impact**: Patient safety (real-time vital sign alerts)
- **Blocks**: Provider notifications for critical vital signs
- **Sprint**: Next Sprint (Week 2-3)

#### Issue #323 - Quality Measure CQL Engine Integration
- **URL**: https://github.com/webemo-aaron/hdim/issues/323
- **Impact**: Custom HEDIS measures cannot be evaluated
- **Blocks**: Non-standard quality measures, client-specific measures
- **Sprint**: Next Sprint (Week 3-4)

---

### 🟠 HIGH Priority (7 issues)

#### Issue #313 - QA Audit Dashboard Backend APIs
- **URL**: https://github.com/webemo-aaron/hdim/issues/313
- **Endpoints**: 12 API endpoints
- **Impact**: QA review workflow
- **Sprint**: Next Sprint (Week 1-2)

#### Issue #315 - MPI Audit Dashboard Backend APIs
- **URL**: https://github.com/webemo-aaron/hdim/issues/315
- **Endpoints**: 8 API endpoints
- **Impact**: Patient identity management
- **Sprint**: Next Sprint (Week 1-2)

#### Issue #321 - Clinical Workflow Patient Name Resolution
- **URL**: https://github.com/webemo-aaron/hdim/issues/321
- **Impact**: UI displays null for patient names/room numbers
- **Sprint**: Next Sprint (Week 2)

#### Issue #322 - Patient Event Handler Merge Chain Resolution
- **URL**: https://github.com/webemo-aaron/hdim/issues/322
- **Impact**: Multi-level patient merges don't work
- **Sprint**: Next Sprint (Week 3)

#### Issue #324 - Clinical Decision Service - 10 TODO Placeholders
- **URL**: https://github.com/webemo-aaron/hdim/issues/324
- **Impact**: Clinical decision support incomplete
- **Sprint**: Next Sprint (Week 3-4)

#### Issue #325 - AI Audit Event Store - 6 Missing Features
- **URL**: https://github.com/webemo-aaron/hdim/issues/325
- **Impact**: HIPAA compliance gaps, AI governance
- **Sprint**: Next Sprint (Week 2-4)

#### Issue #327 - [EPIC] Frontend-Backend API Integration
- **URL**: https://github.com/webemo-aaron/hdim/issues/327
- **Scope**: 65+ TODO API calls across frontend
- **Sprint**: Multiple sprints (2-3 sprints)

---

### 🟡 MEDIUM Priority (4 issues)

#### Issue #317 - Demo Orchestrator WebSocket Publishing
- **URL**: https://github.com/webemo-aaron/hdim/issues/317
- **Impact**: Live demo visualizations
- **Sprint**: Future (Next Quarter)

#### Issue #318 - AI Audit Dashboard Real-Time Events
- **URL**: https://github.com/webemo-aaron/hdim/issues/318
- **Impact**: Real-time audit monitoring
- **Sprint**: Future (Next Quarter)

#### Issue #320 - Patient Service Age Range Filtering
- **URL**: https://github.com/webemo-aaron/hdim/issues/320
- **Impact**: Age-based patient searches
- **Sprint**: Future

#### Issue #319 - [ENHANCEMENT] Demo Seeding Progress WebSocket Upgrade
- **URL**: https://github.com/webemo-aaron/hdim/issues/319
- **Impact**: Optimization (polling currently works)
- **Sprint**: Future (Low priority)

---

### 🟢 LOW Priority (1 issue)

#### Issue #326 - [TECH-DEBT] Remove Deprecated Code
- **URL**: https://github.com/webemo-aaron/hdim/issues/326
- **Impact**: Code cleanliness, maintenance
- **Sprint**: Future (Tech debt sprint)

---

## Issue Breakdown by Category

### Audit Dashboards (3 issues)
- #313 - QA Audit Dashboard (12 endpoints)
- #314 - Clinical Audit Dashboard (9 endpoints)
- #315 - MPI Audit Dashboard (8 endpoints)
- **Total**: 29 backend API endpoints

### Real-Time Features (4 issues)
- #316 - Clinical Workflow WebSocket (CRITICAL - patient safety)
- #317 - Demo Orchestrator WebSocket
- #318 - AI Audit Dashboard SSE/WebSocket
- #319 - Demo Seeding WebSocket (enhancement)

### Business Logic Stubs (6 issues)
- #320 - Age Range Filtering
- #321 - Patient Name Resolution
- #322 - Merge Chain Resolution
- #323 - CQL Engine Integration (CRITICAL)
- #324 - Clinical Decision Service (10 TODOs)
- #325 - AI Audit Event Store (6 features)

### Tech Debt (1 issue)
- #326 - Remove Deprecated Code

### Meta Issues (1 EPIC)
- #327 - Frontend-Backend Integration (65+ API calls)

---

## Sprint Planning Recommendations

### Sprint 1 (Week 1-2) - Audit Infrastructure
**Focus**: Complete critical audit dashboard APIs
**Issues**: #313, #314, #315
**Estimated Effort**: 40-50 hours
**Deliverables**:
- 29 backend API endpoints
- 3 functional audit dashboards
- HIPAA audit compliance

### Sprint 2 (Week 2-3) - Real-Time & Clinical Features
**Focus**: Patient safety and real-time notifications
**Issues**: #316, #321, #323
**Estimated Effort**: 30-40 hours
**Deliverables**:
- WebSocket vital sign alerts
- Patient name resolution
- CQL engine integration

### Sprint 3 (Week 3-4) - Business Logic Completion
**Focus**: Complete TODO placeholders
**Issues**: #322, #324, #325
**Estimated Effort**: 40-50 hours
**Deliverables**:
- Patient merge chain resolution
- Clinical decision service complete
- AI audit event store features

### Future Sprints
**Focus**: Enhancements and tech debt
**Issues**: #317, #318, #319, #320, #326, #327
**Estimated Effort**: 80-100 hours

---

## Issue Template

Created standardized issue template for future incomplete features:
- **File**: `.github/ISSUE_TEMPLATE/incomplete-feature.md`
- **Usage**: Tracks missing/incomplete functionality with impact assessment
- **Sections**: Description, Location, Impact, Acceptance Criteria, References

---

## Metrics

| Metric | Count |
|--------|-------|
| **Total Issues Created** | 15 |
| **Critical Priority** | 3 |
| **High Priority** | 7 |
| **Medium Priority** | 4 |
| **Low Priority** | 1 |
| **Backend API Endpoints** | 29+ |
| **Frontend TODO API Calls** | 65+ |
| **WebSocket Integrations** | 4 |
| **Business Logic TODOs** | 20+ |
| **Deprecated Code Items** | 5 |

---

## Estimated Total Effort

| Category | Effort (Hours) |
|----------|----------------|
| Audit Dashboard APIs | 40-50 |
| Real-Time Features | 30-40 |
| Business Logic Completion | 40-50 |
| Frontend-Backend Integration | 120-160 |
| Enhancements & Tech Debt | 40-60 |
| **TOTAL** | **270-360 hours** |

**Approximate Timeline**: 3-4 months (3-4 developers, 2-week sprints)

---

## Success Criteria

### Phase 1 Complete (v1.0 MVP)
- [ ] All CRITICAL issues resolved (#314, #316, #323)
- [ ] All audit dashboards functional (#313, #314, #315)
- [ ] Patient safety features working (vital sign alerts, patient name resolution)
- [ ] CQL engine integration complete

### Phase 2 Complete (v1.1 Audit & Analytics)
- [ ] All HIGH priority issues resolved
- [ ] Real-time event streaming implemented
- [ ] AI audit event store features complete
- [ ] Clinical decision service complete

### Phase 3 Complete (v2.0 Full Release)
- [ ] All issues resolved
- [ ] No TODO comments in codebase
- [ ] Deprecated code removed
- [ ] Full frontend-backend integration

---

## References

- **Plan Document**: `/home/webemo-aaron/.claude/plans/agile-snuggling-sphinx.md`
- **GitHub Repository**: https://github.com/webemo-aaron/hdim
- **Issue Template**: `.github/ISSUE_TEMPLATE/incomplete-feature.md`

---

## Next Steps

1. **Review Issues**: Prioritize with product team
2. **Assign Issues**: Distribute to development team
3. **Sprint Planning**: Schedule work across 3-4 sprints
4. **Progress Tracking**: Use GitHub Projects for visibility
5. **Release Planning**: Determine v1.0, v1.1, v2.0 scope

---

**Created By**: Claude Sonnet 4.5
**Date**: January 23, 2026
**Context**: Phase 2 of HDIM Production Release Plan
