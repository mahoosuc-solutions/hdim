#!/bin/bash
# Create GitHub Labels

REPO_OWNER="webemo-aaron"
REPO_NAME="hdim"

echo "Creating GitHub labels..."

# Priority labels
gh label create "P0-Critical" --repo "${REPO_OWNER}/${REPO_NAME}" --color "d73a4a" --description "Must have, blocks release" --force 2>/dev/null
gh label create "P1-High" --repo "${REPO_OWNER}/${REPO_NAME}" --color "ff9800" --description "Important for milestone" --force 2>/dev/null
gh label create "P2-Medium" --repo "${REPO_OWNER}/${REPO_NAME}" --color "fbca04" --description "Should have" --force 2>/dev/null
gh label create "P3-Low" --repo "${REPO_OWNER}/${REPO_NAME}" --color "0e8a16" --description "Nice to have" --force 2>/dev/null

# Type labels
gh label create "feature" --repo "${REPO_OWNER}/${REPO_NAME}" --color "1d76db" --description "New feature" --force 2>/dev/null
gh label create "enhancement" --repo "${REPO_OWNER}/${REPO_NAME}" --color "84b6eb" --description "Improvement" --force 2>/dev/null
gh label create "bug" --repo "${REPO_OWNER}/${REPO_NAME}" --color "d73a4a" --description "Something isn't working" --force 2>/dev/null
gh label create "technical-debt" --repo "${REPO_OWNER}/${REPO_NAME}" --color "fef2c0" --description "Code quality" --force 2>/dev/null
gh label create "documentation" --repo "${REPO_OWNER}/${REPO_NAME}" --color "0075ca" --description "Documentation" --force 2>/dev/null
gh label create "security" --repo "${REPO_OWNER}/${REPO_NAME}" --color "b60205" --description "Security issue" --force 2>/dev/null
gh label create "infrastructure" --repo "${REPO_OWNER}/${REPO_NAME}" --color "006b75" --description "DevOps/infra" --force 2>/dev/null
gh label create "testing" --repo "${REPO_OWNER}/${REPO_NAME}" --color "bfd4f2" --description "Testing" --force 2>/dev/null

# Area labels
gh label create "frontend" --repo "${REPO_OWNER}/${REPO_NAME}" --color "5319e7" --description "Frontend code" --force 2>/dev/null
gh label create "backend" --repo "${REPO_OWNER}/${REPO_NAME}" --color "fbca04" --description "Backend code" --force 2>/dev/null
gh label create "ai" --repo "${REPO_OWNER}/${REPO_NAME}" --color "d4c5f9" --description "AI/ML features" --force 2>/dev/null
gh label create "api" --repo "${REPO_OWNER}/${REPO_NAME}" --color "1d76db" --description "API changes" --force 2>/dev/null

echo "Labels created!"
