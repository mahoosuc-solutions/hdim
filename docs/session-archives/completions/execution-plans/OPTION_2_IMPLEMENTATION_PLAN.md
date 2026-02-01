# Option 2: Complete Remaining Enhancements - Implementation Plan

**Date**: January 15, 2026  
**Timeline**: 2-3 weeks  
**Total Effort**: 11-17 hours

---

## Implementation Phases

### Phase 1: Integration Testing (4-6 hours)
**Priority**: 🔴 HIGH - Validates production readiness

#### 1.1 Decision Replay Service Integration Tests (2-3 hours)
- Create Testcontainers-based integration test
- Test with real PostgreSQL database
- Test agent service integration (mocked)
- Validate end-to-end replay flow
- Performance testing with batch operations

#### 1.2 QA Per-Agent Statistics Integration Tests (2-3 hours)
- Create Testcontainers-based integration test
- Test with real database and multiple agent types
- Validate statistical calculations with large datasets
- Test filtering and trend calculations
- Performance testing

---

### Phase 2: Minor Enhancements (5-8 hours)
**Priority**: 🟡 MEDIUM - Quality improvements

#### 2.1 Fix Disabled Gateway Tests (1-2 hours)
- Review `ApiGatewayControllerTest.java.disabled`
- Update to use `GatewayForwarder` instead of `RestTemplate`
- Fix test assertions
- Re-enable tests

#### 2.2 Documentation Service Feedback (2-3 hours)
- Review `DocumentFeedbackEntity.java` TODOs
- Complete feedback tracking implementation
- Add feedback aggregation endpoints
- Add tests

#### 2.3 Template Generation Enhancement (2-3 hours) - Optional
- Enhance template-based patient generation
- Better integration of template medications/observations
- Template-specific care gap creation
- Add tests

---

### Phase 3: Documentation (2-3 hours)
**Priority**: 🟡 MEDIUM - Developer experience

#### 3.1 API Documentation (1-2 hours)
- Document Decision Replay Service endpoints
- Document QA Per-Agent Statistics endpoints
- Update OpenAPI/Swagger specs

#### 3.2 Usage Guides (1 hour)
- Create usage examples
- Add code samples
- Update README files

---

## Execution Order

1. ✅ Integration Tests (validates features work)
2. ✅ Fix Gateway Tests (quick win)
3. ✅ Documentation Service (completes feature)
4. ✅ Template Enhancement (optional)
5. ✅ Documentation (improves DX)

---

## Success Criteria

- ✅ All integration tests passing
- ✅ All disabled tests fixed and enabled
- ✅ All TODOs addressed
- ✅ Documentation complete
- ✅ 100% feature completeness

---

## Files to Create/Modify

### New Files
- `DecisionReplayServiceIntegrationTest.java`
- `QAReviewServicePerAgentIntegrationTest.java`
- `DocumentFeedbackService.java` (if needed)
- API documentation updates

### Modified Files
- `ApiGatewayControllerTest.java.disabled` → `ApiGatewayControllerTest.java`
- `DocumentFeedbackEntity.java`
- `SyntheticPatientGenerator.java` (optional)
- Various README files

---

**Status**: Ready to implement  
**Next**: Start with Phase 1 - Integration Testing
