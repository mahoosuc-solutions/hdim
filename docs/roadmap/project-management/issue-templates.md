# GitHub Issue Templates

Standard issue templates for the HDIM project to ensure consistency and completeness.

---

## Feature Template

```markdown
## Feature Description

**User Story**: As a [user type], I want to [action] so that [benefit].

**Business Value**: [Why is this feature important? What problem does it solve?]

**Acceptance Criteria**:
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Technical Specification

**Frontend Changes**:
- Components to create/modify:
  - `ComponentName.tsx`
  - `AnotherComponent.tsx`
- API endpoints to call:
  - `GET /api/v1/resource`
  - `POST /api/v1/resource`

**Backend Changes**:
- Services to modify:
  - `ServiceName.java`
- New API endpoints:
  - `GET /api/v1/resource`
- Database changes:
  - New tables/columns

**Dependencies**:
- Blocked by: #123, #124
- Blocks: #125

## Design

**UI Mockups**: [Link to Figma]

**Screenshots/Wireframes**: [Attach or link]

## Testing Requirements

- [ ] Unit tests written (target: 80% coverage)
- [ ] Integration tests written
- [ ] E2E test scenarios documented
- [ ] Manual testing checklist created

## Documentation

- [ ] API documentation updated
- [ ] User guide updated
- [ ] Developer guide updated (if applicable)

## Estimation

**Story Points**: [1, 2, 3, 5, 8, 13, 21]

**Effort Estimate**: [X days/weeks]

**Complexity**: [Low | Medium | High]

---

**Labels**: `feature`, `[area]`, `[priority]`  
**Milestone**: `[Q1-2026-...]`  
**Assignee**: [@username]
```

---

## Bug Template

```markdown
## Bug Description

**Summary**: Brief description of the bug

**Environment**:
- Browser/OS: [Chrome 120 on Windows 11]
- Service Version: [v1.2.3]
- Environment: [Development | Staging | Production]

## Steps to Reproduce

1. Navigate to...
2. Click on...
3. Enter...
4. Observe...

## Expected Behavior

[What should happen]

## Actual Behavior

[What actually happens]

## Error Messages / Logs

```
[Paste error messages, stack traces, or logs here]
```

## Screenshots

[Attach screenshots if visual bug]

## Impact

**Severity**: [Critical | High | Medium | Low]

**Affected Users**: [All users | Specific role | Specific tenant]

**Workaround**: [Is there a workaround? Describe it]

## Root Cause Analysis

[To be filled by engineer investigating]

## Fix Description

[To be filled when implementing fix]

---

**Labels**: `bug`, `[severity]`, `[area]`  
**Milestone**: [If blocking a release]  
**Assignee**: [@username]
```

---

## Technical Debt Template

```markdown
## Technical Debt Description

**Current State**: [Describe the current implementation]

**Problem**: [Why is this technical debt? What issues does it cause?]

**Proposed Solution**: [How should we refactor this?]

## Impact

**Code Maintainability**: [How does this affect code maintenance?]

**Performance**: [Any performance implications?]

**Security**: [Any security concerns?]

**Developer Productivity**: [How does this slow down development?]

## Benefits of Addressing

- Benefit 1
- Benefit 2
- Benefit 3

## Effort Estimate

**Story Points**: [1, 2, 3, 5, 8, 13]

**Risk**: [Low | Medium | High] (risk of breaking existing functionality)

## Priority

**Business Impact**: [High | Medium | Low]

**Technical Impact**: [High | Medium | Low]

**Recommended Timeline**: [Q1 2026 | Q2 2026 | Can defer]

---

**Labels**: `technical-debt`, `refactoring`, `[area]`  
**Milestone**: [Optional]  
**Assignee**: [@username]
```

---

## Enhancement Template

```markdown
## Enhancement Description

**Current Behavior**: [How does the feature currently work?]

**Proposed Enhancement**: [What improvement are you suggesting?]

**User Story**: As a [user type], I want [enhancement] so that [benefit].

**Business Justification**: [Why should we prioritize this?]

## Detailed Proposal

**Changes Required**:
- Frontend: [List changes]
- Backend: [List changes]
- Infrastructure: [List changes]

**Backward Compatibility**: [Will this break existing functionality?]

## Alternatives Considered

1. **Alternative 1**: [Description]
   - Pros: 
   - Cons:

2. **Alternative 2**: [Description]
   - Pros:
   - Cons:

**Recommended Approach**: [Which alternative and why]

## Estimation

**Story Points**: [1, 2, 3, 5, 8, 13]

**Effort**: [X days]

---

**Labels**: `enhancement`, `[area]`, `[priority]`  
**Milestone**: [Q-YYYY-...]  
**Assignee**: [@username]
```

---

## Documentation Template

```markdown
## Documentation Need

**Document Type**: [User Guide | Developer Guide | API Docs | Architecture Doc | Runbook]

**Target Audience**: [Developers | End Users | Administrators | Executives]

**Purpose**: [Why is this documentation needed?]

## Content Outline

1. Section 1
   - Subsection 1.1
   - Subsection 1.2
2. Section 2
   - Subsection 2.1

## Assets Needed

- [ ] Screenshots
- [ ] Diagrams (architecture, sequence, etc.)
- [ ] Code examples
- [ ] Video tutorial

## Success Criteria

- [ ] Content is accurate and complete
- [ ] Examples are tested and working
- [ ] Reviewed by [stakeholder]
- [ ] Published to [location]

## Estimation

**Effort**: [X hours/days]

---

**Labels**: `documentation`, `[area]`  
**Milestone**: [Optional]  
**Assignee**: [@username]
```

---

## Infrastructure/DevOps Template

```markdown
## Infrastructure Task

**Objective**: [What infrastructure change is needed?]

**Current State**: [Describe current infrastructure]

**Desired State**: [Describe target infrastructure]

## Technical Details

**Resources to Create/Modify**:
- AWS Resources: [List]
- Kubernetes Resources: [List]
- Terraform Modules: [List]

**Configuration Changes**:
- Service: [changes]
- Database: [changes]
- Networking: [changes]

## Security Considerations

- [ ] Security group rules reviewed
- [ ] IAM policies follow least privilege
- [ ] Secrets management (no hardcoded secrets)
- [ ] Compliance requirements met (HIPAA, SOC 2)

## Testing Plan

- [ ] Test in development environment
- [ ] Test in staging environment
- [ ] Load testing (if applicable)
- [ ] Rollback plan documented

## Rollout Plan

**Deployment Method**: [Blue/Green | Rolling | Canary]

**Rollout Steps**:
1. Step 1
2. Step 2
3. Step 3

**Rollback Steps**:
1. Step 1
2. Step 2

## Monitoring

**Metrics to Track**:
- Metric 1
- Metric 2

**Alerts to Configure**:
- Alert 1
- Alert 2

## Estimation

**Story Points**: [1, 2, 3, 5, 8]

**Effort**: [X days]

**Risk Level**: [Low | Medium | High]

---

**Labels**: `infrastructure`, `devops`, `[priority]`  
**Milestone**: [Q-YYYY-...]  
**Assignee**: [@username]
```

---

## Security Template

```markdown
## Security Issue

**Type**: [Vulnerability | Security Enhancement | Compliance Requirement]

**Severity**: [Critical | High | Medium | Low]

**CVE**: [If applicable, link to CVE]

## Issue Description

**Affected Component**: [Service/library name and version]

**Vulnerability Details**: [Description of the security issue]

**Attack Vector**: [How could this be exploited?]

**Impact**: [What's the potential damage?]

## Affected Systems

- [ ] Production
- [ ] Staging
- [ ] Development

**Services Affected**: [List services]

## Remediation

**Recommended Fix**: [How should this be fixed?]

**Patch Available**: [Yes/No - Link if available]

**Workaround**: [Temporary mitigation if fix not ready]

## Testing Requirements

- [ ] Security scanning after fix
- [ ] Penetration testing (if high severity)
- [ ] Regression testing

## Compliance Impact

- [ ] HIPAA impact assessment
- [ ] SOC 2 impact assessment
- [ ] Document in security incident log

## Timeline

**Discovered**: [Date]

**Must Fix By**: [Date based on severity]
- Critical: 24 hours
- High: 7 days
- Medium: 30 days
- Low: 90 days

---

**Labels**: `security`, `[severity]`, `[area]`  
**Milestone**: [Immediate for Critical/High]  
**Assignee**: [@username]  
**Confidential**: [Yes/No]
```

---

## Epic Template

```markdown
## Epic Description

**Epic Name**: [Descriptive name]

**Business Objective**: [High-level business goal]

**User Impact**: [Who benefits and how?]

**Timeline**: [Q1 2026 | Q2 2026 | etc.]

## User Stories

This epic includes the following user stories:

- [ ] #123 - User story 1
- [ ] #124 - User story 2
- [ ] #125 - User story 3

## Technical Scope

**Frontend Work**:
- New applications/components
- Major UI changes

**Backend Work**:
- New services
- API changes
- Database schema changes

**Infrastructure Work**:
- New infrastructure components
- Configuration changes

## Success Metrics

**KPIs**:
- Metric 1: Target value
- Metric 2: Target value
- Metric 3: Target value

**User Satisfaction**: [How will we measure?]

## Dependencies

**Requires**:
- Epic #XX to be completed
- Third-party service integration

**Enables**:
- Epic #YY can start after this

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Risk 1 | High | High | Mitigation plan |
| Risk 2 | Low | Medium | Mitigation plan |

## Total Estimation

**Story Points**: [Sum of all stories]

**Effort**: [X weeks/months]

**Team Size**: [X engineers]

---

**Labels**: `epic`, `[area]`, `[priority]`  
**Milestone**: `[Q-YYYY]`  
**Project**: [Link to GitHub Project]
```

---

## Issue Labeling Guidelines

### Priority Labels

| Label | Description | SLA |
|-------|-------------|-----|
| `P0-Critical` | Blocks release, major functionality down | Fix immediately |
| `P1-High` | Important for milestone | Fix within sprint |
| `P2-Medium` | Should have | Fix in next 2 sprints |
| `P3-Low` | Nice to have | Backlog |

### Type Labels

| Label | Description |
|-------|-------------|
| `feature` | New functionality |
| `enhancement` | Improvement to existing feature |
| `bug` | Something isn't working |
| `technical-debt` | Code quality/refactoring |
| `documentation` | Documentation only |
| `security` | Security-related |
| `performance` | Performance optimization |

### Area Labels

| Label | Description |
|-------|-------------|
| `frontend` | Frontend code (React, Angular) |
| `backend` | Backend code (Java, Spring Boot) |
| `infrastructure` | DevOps, Kubernetes, AWS |
| `ai` | AI/ML features |
| `api` | API changes |
| `database` | Database schema or queries |
| `testing` | Test code |

### Status Labels

| Label | Description |
|-------|-------------|
| `blocked` | Cannot proceed, needs unblocking |
| `needs-design` | Needs UI/UX design |
| `needs-review` | Needs code review |
| `needs-testing` | Needs QA testing |
| `ready` | Ready to be picked up |
| `in-progress` | Currently being worked on |

---

## Automation

### Issue Auto-Labeling

Use GitHub Actions to auto-label issues:

```yaml
# .github/workflows/issue-labeler.yml
name: Issue Labeler
on:
  issues:
    types: [opened]

jobs:
  label:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/github-script@v6
        with:
          script: |
            const issue = context.payload.issue;
            const labels = [];
            
            // Auto-label based on title
            if (issue.title.includes('[Bug]')) labels.push('bug');
            if (issue.title.includes('[Feature]')) labels.push('feature');
            if (issue.title.includes('[Security]')) labels.push('security');
            
            // Auto-label based on body content
            if (issue.body.includes('frontend')) labels.push('frontend');
            if (issue.body.includes('backend')) labels.push('backend');
            
            if (labels.length > 0) {
              github.rest.issues.addLabels({
                owner: context.repo.owner,
                repo: context.repo.repo,
                issue_number: issue.number,
                labels: labels
              });
            }
```

---

## Creating Issues from This Template

### Using GitHub CLI

```bash
# Create a feature issue
gh issue create \
  --title "[Feature] Patient Search Autocomplete" \
  --body-file .github/ISSUE_TEMPLATE/feature.md \
  --label "feature,frontend,P1-High" \
  --milestone "Q1-2026-Clinical-Portal" \
  --assignee "john-doe"

# Bulk create issues from CSV
while IFS=, read -r title body label milestone; do
  gh issue create --title "$title" --body "$body" --label "$label" --milestone "$milestone"
done < issues.csv
```

### Using GitHub API

```python
import requests

def create_issue(title, body, labels, milestone):
    url = f"https://api.github.com/repos/{owner}/{repo}/issues"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json"
    }
    data = {
        "title": title,
        "body": body,
        "labels": labels,
        "milestone": milestone
    }
    response = requests.post(url, json=data, headers=headers)
    return response.json()
```

---

## Best Practices

1. **Be Specific**: Use clear, descriptive titles
2. **Provide Context**: Include "why" not just "what"
3. **Link Related Issues**: Use "Relates to #123" or "Closes #123"
4. **Update Regularly**: Keep issues updated with progress
5. **Use Checklists**: Break down complex issues into smaller tasks
6. **Add Screenshots**: Visual aids help with UI issues
7. **Tag People**: @ mention reviewers or stakeholders
8. **Set Milestones**: Always assign to a milestone if planned work
9. **Estimate Effort**: Use story points for planning
10. **Close When Done**: Don't leave completed issues open

---

**Last Updated**: January 14, 2026  
**Maintained By**: Engineering Team
