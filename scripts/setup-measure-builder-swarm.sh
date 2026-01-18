#!/bin/bash

#############################################################################
# Measure Builder TDD Swarm - Worktree Setup Script
# This script sets up git worktrees for parallel team development
#
# Usage: bash ./scripts/setup-measure-builder-swarm.sh
#############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKTREE_BASE="${PROJECT_ROOT}/.."
MAIN_BRANCH="feature/enhanced-measure-builder"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Measure Builder TDD Swarm - Worktree Setup              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to check git status
check_git_status() {
    echo -e "${YELLOW}Checking git status...${NC}"
    cd "$PROJECT_ROOT"

    if [[ -n $(git status -s) ]]; then
        echo -e "${RED}Error: Working directory has uncommitted changes${NC}"
        echo "Please commit or stash changes before proceeding"
        exit 1
    fi
    echo -e "${GREEN}✓ Working directory clean${NC}"
}

# Function to create main feature branch
create_main_branch() {
    echo ""
    echo -e "${YELLOW}Creating main feature branch...${NC}"

    cd "$PROJECT_ROOT"

    # Check if branch already exists
    if git rev-parse --verify "$MAIN_BRANCH" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Main branch already exists: $MAIN_BRANCH${NC}"
        git checkout "$MAIN_BRANCH"
    else
        git checkout -b "$MAIN_BRANCH"
        echo -e "${GREEN}✓ Created main branch: $MAIN_BRANCH${NC}"
    fi
}

# Function to create worktree
create_worktree() {
    local WORKTREE_NAME=$1
    local FEATURE_BRANCH=$2
    local WORKTREE_PATH="${WORKTREE_BASE}/measure-builder-${WORKTREE_NAME}"

    echo ""
    echo -e "${YELLOW}Setting up worktree: ${WORKTREE_NAME}${NC}"
    echo "  Path: $WORKTREE_PATH"
    echo "  Branch: feature/$FEATURE_BRANCH"

    # Remove existing worktree if it exists
    if [ -d "$WORKTREE_PATH" ]; then
        echo -e "${YELLOW}  Removing existing worktree...${NC}"
        git -C "$PROJECT_ROOT" worktree remove "$WORKTREE_PATH" 2>/dev/null || true
        rm -rf "$WORKTREE_PATH"
    fi

    # Create branch if it doesn't exist
    if git -C "$PROJECT_ROOT" rev-parse --verify "feature/$FEATURE_BRANCH" >/dev/null 2>&1; then
        echo -e "${YELLOW}  Branch already exists, using existing...${NC}"
    else
        echo -e "${YELLOW}  Creating new branch from main...${NC}"
        git -C "$PROJECT_ROOT" branch "feature/$FEATURE_BRANCH" "$MAIN_BRANCH"
    fi

    # Create worktree
    git -C "$PROJECT_ROOT" worktree add "$WORKTREE_PATH" "feature/$FEATURE_BRANCH"

    echo -e "${GREEN}✓ Worktree created: $WORKTREE_NAME${NC}"

    # Create README in worktree
    cat > "$WORKTREE_PATH/TEAM_README.md" << EOF
# Team Worktree: $WORKTREE_NAME

**Branch:** feature/$FEATURE_BRANCH
**Base:** feature/$MAIN_BRANCH

## Quick Start

\`\`\`bash
# Navigate to this directory
cd measure-builder-$WORKTREE_NAME

# Install dependencies (first time only)
npm install

# Run tests in watch mode
npm run test:watch

# Build the project
npm run build

# Run linter
npm run lint

# Format code
npm run format
\`\`\`

## Before Committing

1. Run all tests:
   \`\`\`bash
   ./gradlew clean build test
   \`\`\`

2. Check code coverage:
   \`\`\`bash
   ./gradlew jacocoTestReport
   \`\`\`

3. Verify no compiler warnings

4. Commit message format:
   \`\`\`
   [TEAM] Brief description

   Detailed explanation of changes

   Fixes #XXX
   \`\`\`

## Pushing Changes

\`\`\`bash
# Push to your feature branch
git push origin feature/$FEATURE_BRANCH

# Create pull request on GitHub
# Reference the main feature branch
\`\`\`

## Merging Back

Once your work is ready and approved:

\`\`\`bash
# Check out the main feature branch
git checkout feature/$MAIN_BRANCH

# Pull latest changes
git pull origin feature/$MAIN_BRANCH

# Merge your work
git merge --no-ff feature/$FEATURE_BRANCH

# Push to main feature
git push origin feature/$MAIN_BRANCH
\`\`\`

## Need Help?

- See MEASURE_BUILDER_TDD_SWARM_EXECUTION_GUIDE.md for detailed instructions
- Check GitHub issues for known blockers
- Contact tech lead in #measure-builder-swarm Slack channel
EOF

    echo -e "${GREEN}  Created TEAM_README.md${NC}"
}

# Function to print worktree status
print_worktree_status() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   Worktree Setup Complete                                 ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}All worktrees created successfully!${NC}"
    echo ""
    echo "Worktree Locations:"
    cd "$PROJECT_ROOT"
    git worktree list
    echo ""
    echo "Next Steps for Each Team:"
    echo ""
    echo "1. Visual Algorithm Builder:"
    echo -e "   ${YELLOW}cd ${WORKTREE_BASE}/measure-builder-visual${NC}"
    echo "   npm install && npm run test:watch"
    echo ""
    echo "2. Slider Components:"
    echo -e "   ${YELLOW}cd ${WORKTREE_BASE}/measure-builder-sliders${NC}"
    echo "   npm install && npm run test:watch"
    echo ""
    echo "3. Integration Tests:"
    echo -e "   ${YELLOW}cd ${WORKTREE_BASE}/measure-builder-tests${NC}"
    echo "   npm install && npm run test:watch"
    echo ""
    echo "4. Performance Optimization:"
    echo -e "   ${YELLOW}cd ${WORKTREE_BASE}/measure-builder-perf${NC}"
    echo "   npm install && npm run test:watch"
    echo ""
    echo -e "${BLUE}Documentation:${NC}"
    echo "  - See MEASURE_BUILDER_TDD_SWARM_EXECUTION_GUIDE.md"
    echo "  - Daily standup at 9:00 AM"
    echo "  - Communication: #measure-builder-swarm"
    echo ""
}

# Function to create team assignment file
create_team_assignments() {
    echo ""
    echo -e "${YELLOW}Creating team assignments document...${NC}"

    local ASSIGNMENTS_FILE="${PROJECT_ROOT}/MEASURE_BUILDER_TEAM_ASSIGNMENTS.md"

    cat > "$ASSIGNMENTS_FILE" << 'EOF'
# Measure Builder TDD Swarm - Team Assignments

**Project:** Enhanced Visual Measure Builder (Phases 2-4)
**Duration:** 4 weeks
**Methodology:** Test-Driven Development with Git Worktrees
**Start Date:** January 17, 2026

---

## Team Structure

### Team 1: Visual Algorithm Builder - SVG Rendering
**Worktree:** `measure-builder-visual`
**Branch:** `feature/visual-algorithm-builder`
**Lead:** [Developer Name]
**Duration:** 6 hours (Week 1)

**Deliverables:**
- [ ] VisualAlgorithmBuilderComponent (SVG rendering)
- [ ] Population block rendering (all types)
- [ ] Color-coded blocks (initial, denominator, numerator, exclusion, exception)
- [ ] Connection lines between blocks
- [ ] Block hover effects and tooltips
- [ ] 40+ unit tests with ≥85% coverage
- [ ] Zero compilation warnings
- [ ] API documentation

**Files to Create/Modify:**
```
apps/clinical-portal/src/app/pages/measure-builder/
├── components/visual-algorithm-builder/
│   ├── visual-algorithm-builder.component.ts (500 lines)
│   ├── visual-algorithm-builder.component.html (200 lines)
│   ├── visual-algorithm-builder.component.scss (300 lines)
│   └── visual-algorithm-builder.component.spec.ts (800 lines)
```

**Test Coverage Target:** 40+ tests
**Performance Target:** Render 50 blocks in < 500ms

---

### Team 2: Visual Algorithm Builder - Drag & Drop
**Worktree:** `measure-builder-visual`
**Branch:** `feature/visual-algorithm-builder`
**Lead:** [Developer Name]
**Duration:** 6 hours (Week 1)

**Deliverables:**
- [ ] Drag-and-drop block repositioning
- [ ] Connection line updates on drag
- [ ] Context menu (edit, duplicate, delete)
- [ ] Connection creation UI
- [ ] Undo/redo integration
- [ ] 35+ unit tests with ≥85% coverage
- [ ] Keyboard shortcuts support

**Files to Extend:**
```
apps/clinical-portal/src/app/pages/measure-builder/
├── components/visual-algorithm-builder/
│   ├── population-block/ (child component)
│   ├── block-connection/ (child component)
│   └── context-menu/ (child component)
```

**Test Coverage Target:** 35+ tests
**Features:**
- Drag blocks 50px (triggers update)
- Create connections via UI
- Delete blocks with confirmation

---

### Team 3: Range & Threshold Sliders
**Worktree:** `measure-builder-sliders`
**Branch:** `feature/interactive-sliders`
**Lead:** [Developer Name]
**Duration:** 5 hours (Week 2)

**Deliverables:**
- [ ] RangeSliderComponent (dual-value: age, BMI)
- [ ] ThresholdSliderComponent (single-value: HbA1c, BP)
- [ ] Preset functionality
- [ ] Warning/critical indicators
- [ ] Real-time value updates
- [ ] 30+ unit tests with ≥85% coverage
- [ ] CQL generation integration

**Files to Create:**
```
apps/clinical-portal/src/app/pages/measure-builder/
├── components/measure-config-slider/
│   ├── sliders/
│   │   ├── range-slider/
│   │   │   ├── range-slider.component.ts
│   │   │   ├── range-slider.component.html
│   │   │   ├── range-slider.component.scss
│   │   │   └── range-slider.component.spec.ts
│   │   └── threshold-slider/
│   │       ├── threshold-slider.component.ts
│   │       ├── threshold-slider.component.html
│   │       ├── threshold-slider.component.scss
│   │       └── threshold-slider.component.spec.ts
```

**Test Coverage Target:** 30+ tests
**Performance Target:** < 100ms per slider update

---

### Team 4: Distribution & Period Sliders
**Worktree:** `measure-builder-sliders`
**Branch:** `feature/interactive-sliders`
**Lead:** [Developer Name]
**Duration:** 5 hours (Week 2)

**Deliverables:**
- [ ] DistributionSliderComponent (component weights)
- [ ] PeriodSelectorComponent (time periods)
- [ ] Weight distribution validation (sum = 100%)
- [ ] Custom period support
- [ ] 30+ unit tests with ≥85% coverage
- [ ] CQL generation for composite measures

**Files to Create:**
```
apps/clinical-portal/src/app/pages/measure-builder/
├── components/measure-config-slider/
│   └── sliders/
│       ├── distribution-slider/
│       └── period-selector/
```

**Test Coverage Target:** 30+ tests
**Constraints:**
- Distribution total must equal 100%
- Period must be within bounds
- Custom periods validated

---

### Team 5: Integration & E2E Tests
**Worktree:** `measure-builder-tests`
**Branch:** `feature/integration-tests`
**Lead:** [QA Lead]
**Duration:** 8 hours (Week 3)

**Deliverables:**
- [ ] E2E test suite for complete workflows
- [ ] API integration tests
- [ ] Database integration tests
- [ ] Service integration tests
- [ ] Undo/redo integration tests
- [ ] 50+ E2E tests
- [ ] Cross-browser compatibility testing

**Test Scenarios:**
1. Create measure end-to-end
2. Edit measure and publish
3. Drag blocks and update connections
4. Adjust sliders and verify CQL
5. Undo/redo operations
6. Export/import measures
7. Test with sample patients
8. Performance under load

**Files to Create:**
```
apps/clinical-portal/e2e/
├── measure-builder/
│   ├── create-measure.spec.ts
│   ├── edit-measure.spec.ts
│   ├── algorithm-builder.spec.ts
│   ├── sliders.spec.ts
│   └── undo-redo.spec.ts

apps/clinical-portal/src/app/pages/measure-builder/
├── tests/
│   ├── integration/
│   ├── services/
│   └── components/
```

**Test Coverage Target:** 50+ E2E tests
**Success Criteria:**
- All workflows pass on Chrome, Firefox, Safari
- No flaky tests
- < 5 second average test duration

---

### Team 6: Performance & Optimization
**Worktree:** `measure-builder-perf`
**Branch:** `feature/performance-optimization`
**Lead:** [Performance Lead]
**Duration:** 6 hours (Week 4)

**Deliverables:**
- [ ] Rendering performance optimization
- [ ] Memory leak detection and fixes
- [ ] CQL generation optimization
- [ ] Slider update performance
- [ ] Build size optimization
- [ ] 20+ performance tests
- [ ] Performance benchmarks

**Performance Targets:**
- Render 50 blocks: < 500ms
- Slider update: < 100ms
- CQL generation: < 100ms for 20 components
- Memory growth: < 10% for 100 operations
- Bundle size: < 2MB (gzipped)

**Files to Create/Modify:**
```
apps/clinical-portal/src/app/pages/measure-builder/
├── services/
│   ├── performance.service.ts
│   ├── performance.service.spec.ts
├── performance-tests/
│   ├── rendering.perf.spec.ts
│   ├── slider-updates.perf.spec.ts
│   └── cql-generation.perf.spec.ts
```

**Tools:**
- Chrome DevTools Performance tab
- Angular DevTools
- Lighthouse
- WebPageTest

**Optimization Techniques:**
- OnPush change detection
- Memoization
- Virtual scrolling (if needed)
- Code splitting
- Tree shaking

---

## Daily Standup Template

**Time:** 9:00 AM
**Format:** Slack thread + Optional video call

### Template
```
Team [Number]: [Team Name]
Lead: [Name]

✅ Completed Yesterday:
- [Feature/Test]
- [Feature/Test]

🎯 Working On Today:
- [Feature/Test]
- [Feature/Test]

🚧 Blockers:
- [Blocker 1 - if any]

📊 Metrics:
- Tests: X/Y passing
- Coverage: X%
```

---

## Merge Strategy

### Order of Merging:
1. Team 1-2: Visual Algorithm Builder
2. Team 3-4: Slider Components
3. Team 5: Integration Tests
4. Team 6: Performance Optimization

### Merge Command:
```bash
git merge --no-ff feature/[TEAM_BRANCH]
```

### Validation After Each Merge:
```bash
./gradlew clean build test
./gradlew integrationTest
./gradlew sonarqube
```

---

## Definition of Done Checklist

### For Each Component:

**Code Quality:**
- [ ] All tests passing
- [ ] Code coverage ≥85% (verified by JaCoCo)
- [ ] No compiler warnings
- [ ] No SonarQube critical issues
- [ ] Code formatted (prettier/eslint)
- [ ] JSDoc comments on public methods

**Testing:**
- [ ] Unit tests for all public methods
- [ ] Integration tests for API endpoints
- [ ] Edge cases covered
- [ ] Error scenarios tested
- [ ] Performance benchmarks passed

**Documentation:**
- [ ] README.md updated
- [ ] API documentation (Swagger if applicable)
- [ ] Code comments for complex logic
- [ ] Architecture diagrams updated
- [ ] Example usage included

**Security & Compliance:**
- [ ] No hardcoded secrets
- [ ] HIPAA compliance verified
- [ ] Input validation implemented
- [ ] Error messages don't leak info
- [ ] Security review passed

**Performance:**
- [ ] Performance targets met
- [ ] No memory leaks
- [ ] Rendering smooth (60fps)
- [ ] Bundle size reasonable
- [ ] Build time acceptable

---

## Communication

### Slack Channel: #measure-builder-swarm

**Channels:**
- `#measure-builder-swarm` - Main channel for all updates
- `#measure-builder-blockers` - Critical blockers only
- Thread replies for detailed discussions

**Daily Standup:** 9:00 AM in thread

### GitHub

**Labels:**
- `measure-builder-phase2` - Visual Algorithm
- `measure-builder-phase3` - Sliders
- `measure-builder-phase4` - Integration
- `blocker` - Critical blockers
- `perf` - Performance work

**Issues:**
- Create GitHub issue for each blocker
- Link to Slack for visibility

---

## Success Metrics

**Week 1 (Phase 2):**
- [ ] Visual algorithm builder 90% complete
- [ ] 60+ tests passing
- [ ] Code coverage ≥80%
- [ ] Drag-and-drop working

**Week 2 (Phase 3):**
- [ ] All slider types implemented
- [ ] 100+ tests passing
- [ ] Code coverage ≥85%
- [ ] Real-time CQL updates working

**Week 3 (Phase 4):**
- [ ] Integration tests passing
- [ ] E2E workflows validated
- [ ] Performance targets met
- [ ] 150+ tests passing

**Week 4 (Finalization):**
- [ ] All code merged
- [ ] Full integration testing passing
- [ ] Ready for production
- [ ] Deployed to staging

---

## Resources & References

- [MEASURE_BUILDER_TDD_SWARM_EXECUTION_GUIDE.md](./MEASURE_BUILDER_TDD_SWARM_EXECUTION_GUIDE.md) - Complete execution guide
- [MEASURE_BUILDER_API_REFERENCE.md](./MEASURE_BUILDER_API_REFERENCE.md) - API documentation
- [Angular Testing Guide](https://angular.io/guide/testing)
- [Jasmine Documentation](https://jasmine.github.io/)
- [NX Monorepo](https://nx.dev/)

---

## Questions or Issues?

1. Check the execution guide first
2. Ask in #measure-builder-swarm
3. Create GitHub issue if it's a blocker
4. Contact tech lead for major decisions

---

_Document Created: January 17, 2026_
_Framework: Angular 17_
_Build Tool: NX Monorepo_
EOF

    echo -e "${GREEN}✓ Team assignments document created${NC}"
}

# Main execution
main() {
    check_git_status
    create_main_branch

    # Create worktrees for each team
    create_worktree "visual" "visual-algorithm-builder"
    create_worktree "sliders" "interactive-sliders"
    create_worktree "tests" "integration-tests"
    create_worktree "perf" "performance-optimization"

    # Create team assignments
    create_team_assignments

    # Print final status
    print_worktree_status
}

# Run main
main
