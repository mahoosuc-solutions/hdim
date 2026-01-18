# Team Worktree: visual

**Branch:** feature/visual-algorithm-builder
**Base:** feature/feature/enhanced-measure-builder

## Quick Start

```bash
# Navigate to this directory
cd measure-builder-visual

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
```

## Before Committing

1. Run all tests:
   ```bash
   ./gradlew clean build test
   ```

2. Check code coverage:
   ```bash
   ./gradlew jacocoTestReport
   ```

3. Verify no compiler warnings

4. Commit message format:
   ```
   [TEAM] Brief description

   Detailed explanation of changes

   Fixes #XXX
   ```

## Pushing Changes

```bash
# Push to your feature branch
git push origin feature/visual-algorithm-builder

# Create pull request on GitHub
# Reference the main feature branch
```

## Merging Back

Once your work is ready and approved:

```bash
# Check out the main feature branch
git checkout feature/feature/enhanced-measure-builder

# Pull latest changes
git pull origin feature/feature/enhanced-measure-builder

# Merge your work
git merge --no-ff feature/visual-algorithm-builder

# Push to main feature
git push origin feature/feature/enhanced-measure-builder
```

## Need Help?

- See MEASURE_BUILDER_TDD_SWARM_EXECUTION_GUIDE.md for detailed instructions
- Check GitHub issues for known blockers
- Contact tech lead in #measure-builder-swarm Slack channel
