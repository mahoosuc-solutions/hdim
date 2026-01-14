#!/bin/bash
# Import GitHub Issues from CSV

# Prerequisites:
# 1. Install GitHub CLI: https://cli.github.com/
# 2. Authenticate: gh auth login
# 3. Set your repository owner and name below

REPO_OWNER="webemo-aaron"
REPO_NAME="hdim"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== GitHub Issues Import Script ===${NC}"
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}Error: GitHub CLI (gh) is not installed${NC}"
    echo "Install from: https://cli.github.com/"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${RED}Error: Not authenticated with GitHub${NC}"
    echo "Run: gh auth login"
    exit 1
fi

echo -e "${YELLOW}Repository: ${REPO_OWNER}/${REPO_NAME}${NC}"
echo ""

# Confirm before proceeding
read -p "This will create ~40 issues in your repository. Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi

# Function to create milestone if it doesn't exist
create_milestone() {
    local milestone_name=$1
    local due_date=$2
    local description=$3
    
    # Check if milestone exists
    if gh api "/repos/${REPO_OWNER}/${REPO_NAME}/milestones" --jq ".[].title" | grep -q "^${milestone_name}$"; then
        echo -e "${YELLOW}Milestone '${milestone_name}' already exists${NC}"
    else
        echo -e "${GREEN}Creating milestone: ${milestone_name}${NC}"
        gh api "/repos/${REPO_OWNER}/${REPO_NAME}/milestones" \
            -f title="${milestone_name}" \
            -f state="open" \
            -f description="${description}" \
            -f due_on="${due_date}"
    fi
}

# Create Q1 2026 milestones
echo -e "${GREEN}Creating milestones...${NC}"
create_milestone "Q1-2026-Clinical-Portal" "2026-03-15T00:00:00Z" "Complete Clinical User Portal with patient search, care gaps, quality measures, and AI assistant"
create_milestone "Q1-2026-Admin-Portal" "2026-03-20T00:00:00Z" "Enhanced Admin Portal with service monitoring, audit logs, and user/tenant management"
create_milestone "Q1-2026-Agent-Studio" "2026-03-25T00:00:00Z" "AI Agent Studio for no-code agent creation and testing"
create_milestone "Q1-2026-Developer-Portal" "2026-03-28T00:00:00Z" "Developer Portal with API docs, sandbox, and webhook configuration"
create_milestone "Q1-2026-Auth" "2026-03-10T00:00:00Z" "SSO, MFA, RBAC, and session management"
create_milestone "Q1-2026-Infrastructure" "2026-03-05T00:00:00Z" "CI/CD pipelines and monitoring setup"
create_milestone "Q1-2026-Documentation" "2026-03-27T00:00:00Z" "User guides and API documentation"
create_milestone "Q1-2026-Testing" "2026-03-26T00:00:00Z" "E2E and performance testing"

echo ""
echo -e "${GREEN}Importing issues from CSV...${NC}"

# Read CSV and create issues
ISSUE_COUNT=0
ERROR_COUNT=0

# Skip header line
tail -n +2 "github-issues-q1-2026.csv" | while IFS=',' read -r title body labels milestone story_points assignee; do
    # Remove quotes from fields
    title=$(echo "$title" | sed 's/^"//;s/"$//')
    body=$(echo "$body" | sed 's/^"//;s/"$//')
    labels=$(echo "$labels" | sed 's/^"//;s/"$//')
    milestone=$(echo "$milestone" | sed 's/^"//;s/"$//')
    
    echo -e "${YELLOW}Creating issue: ${title}${NC}"
    
    # Prepare labels array
    IFS=',' read -ra LABEL_ARRAY <<< "$labels"
    LABEL_FLAGS=""
    for label in "${LABEL_ARRAY[@]}"; do
        LABEL_FLAGS="${LABEL_FLAGS} --label \"${label}\""
    done
    
    # Create issue
    if eval gh issue create \
        --repo "${REPO_OWNER}/${REPO_NAME}" \
        --title "${title}" \
        --body "${body}" \
        ${LABEL_FLAGS} \
        --milestone "${milestone}"; then
        ISSUE_COUNT=$((ISSUE_COUNT + 1))
        echo -e "${GREEN}✓ Created${NC}"
    else
        ERROR_COUNT=$((ERROR_COUNT + 1))
        echo -e "${RED}✗ Failed${NC}"
    fi
    
    # Add small delay to avoid rate limiting
    sleep 1
done

echo ""
echo -e "${GREEN}=== Import Complete ===${NC}"
echo -e "Issues created: ${GREEN}${ISSUE_COUNT}${NC}"
if [ $ERROR_COUNT -gt 0 ]; then
    echo -e "Errors: ${RED}${ERROR_COUNT}${NC}"
fi

echo ""
echo -e "${GREEN}Next steps:${NC}"
echo "1. View issues: gh issue list --repo ${REPO_OWNER}/${REPO_NAME}"
echo "2. Create project board: gh project create --repo ${REPO_OWNER}/${REPO_NAME} --title 'Q1 2026 Roadmap'"
echo "3. Assign issues to team members"
echo "4. Start Sprint 1!"
