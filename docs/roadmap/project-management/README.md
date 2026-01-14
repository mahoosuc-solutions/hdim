# Project Management Documentation

This directory contains all GitHub project management resources for the HDIM 18-month roadmap.

---

## Files

### 📋 [milestones.md](./milestones.md)
**Purpose**: Complete GitHub milestone structure  
**Content**: 29 milestones from Q1 2026 to Q2 2027  
**Use Case**: Reference for creating milestones in GitHub

### 📝 [issue-templates.md](./issue-templates.md)
**Purpose**: Standardized GitHub issue templates  
**Content**: 7 comprehensive templates (Feature, Bug, Technical Debt, Enhancement, Documentation, Infrastructure, Security, Epic)  
**Use Case**: Copy templates when creating issues

### 📊 [github-issues-q1-2026.csv](./github-issues-q1-2026.csv)
**Purpose**: Ready-to-import Q1 2026 issues  
**Content**: 40+ issues with full details  
**Format**: CSV (Title, Body, Labels, Milestone, Story Points, Assignee)

### 🚀 [import-issues.sh](./import-issues.sh)
**Purpose**: Automated script to import milestones and issues to GitHub  
**Prerequisites**: 
- GitHub CLI (`gh`) installed
- Authenticated (`gh auth login`)
- Repository owner and name configured in script

---

## Quick Start

### 1. Install GitHub CLI

**macOS**:
```bash
brew install gh
```

**Linux**:
```bash
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo gpg --dearmor -o /usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update
sudo apt install gh
```

**Windows**:
```powershell
winget install --id GitHub.cli
```

### 2. Authenticate with GitHub

```bash
gh auth login
```

Follow prompts to authenticate.

### 3. Configure Repository

Edit `import-issues.sh` and update:

```bash
REPO_OWNER="your-org"        # Change to your GitHub org/username
REPO_NAME="hdim"             # Change if your repo has a different name
```

### 4. Run Import Script

```bash
cd /home/webemo-aaron/projects/hdim-master/docs/roadmap/project-management/
./import-issues.sh
```

The script will:
1. ✅ Check prerequisites (gh CLI, authentication)
2. ✅ Create 8 Q1 2026 milestones
3. ✅ Import 40+ issues from CSV
4. ✅ Apply labels and assign to milestones
5. ✅ Report success/error count

**Estimated time**: 2-3 minutes

---

## Manual Import (Alternative)

If you prefer manual import, you can:

### Create Milestones Manually

```bash
gh api /repos/OWNER/REPO/milestones \
  -f title="Q1-2026-Clinical-Portal" \
  -f state="open" \
  -f description="Complete Clinical User Portal" \
  -f due_on="2026-03-15T00:00:00Z"
```

### Create Issues Manually

```bash
gh issue create \
  --repo OWNER/REPO \
  --title "[Feature] Patient Search" \
  --body "..." \
  --label "feature,frontend,P0-Critical" \
  --milestone "Q1-2026-Clinical-Portal"
```

---

## After Import

### 1. Create GitHub Projects

```bash
gh project create --owner your-org --title "Q1 2026 Roadmap"
gh project create --owner your-org --title "Q2 2026 Roadmap"
gh project create --owner your-org --title "Q3 2026 Roadmap"
gh project create --owner your-org --title "Q4 2026 Roadmap"
```

### 2. Set Up Project Boards

For each project, create columns:
- 📋 Backlog
- 📝 To Do
- 🏗️ In Progress
- 👀 In Review
- 🧪 Testing
- ✅ Done

### 3. Link Issues to Projects

```bash
# Add issues to project
gh project item-add PROJECT_ID --owner your-org --url ISSUE_URL
```

### 4. Assign Issues

```bash
gh issue edit ISSUE_NUMBER --add-assignee @username
```

---

## Label Management

### Create Labels (if not exist)

```bash
# Priority labels
gh label create "P0-Critical" --color "d73a4a" --description "Must have, blocks release"
gh label create "P1-High" --color "ff9800" --description "Important for milestone"
gh label create "P2-Medium" --color "fbca04" --description "Should have"
gh label create "P3-Low" --color "0e8a16" --description "Nice to have"

# Type labels
gh label create "feature" --color "1d76db" --description "New feature"
gh label create "enhancement" --color "84b6eb" --description "Improvement"
gh label create "bug" --color "d73a4a" --description "Something isn't working"
gh label create "technical-debt" --color "fef2c0" --description "Code quality"
gh label create "documentation" --color "0075ca" --description "Documentation"
gh label create "security" --color "b60205" --description "Security issue"

# Area labels
gh label create "frontend" --color "5319e7" --description "Frontend code"
gh label create "backend" --color "fbca04" --description "Backend code"
gh label create "infrastructure" --color "006b75" --description "DevOps/infra"
gh label create "ai" --color "d4c5f9" --description "AI/ML features"
gh label create "testing" --color "bfd4f2" --description "Testing"
```

---

## Reporting

### Weekly Milestone Report

```bash
# Get milestone progress
gh api "/repos/OWNER/REPO/milestones" --jq '.[] | select(.title=="Q1-2026-Clinical-Portal") | {title:.title, open_issues:.open_issues, closed_issues:.closed_issues}'

# List open issues in milestone
gh issue list --milestone "Q1-2026-Clinical-Portal" --state open
```

### Sprint Board View

```bash
# View issues by label
gh issue list --label "in-progress"
gh issue list --label "needs-review"
```

---

## Troubleshooting

### Issue: gh command not found
**Solution**: Install GitHub CLI (see step 1)

### Issue: Not authenticated
**Solution**: Run `gh auth login`

### Issue: Permission denied
**Solution**: Ensure you have write access to the repository

### Issue: Milestone already exists
**Solution**: Script will skip existing milestones automatically

### Issue: Rate limiting
**Solution**: Script includes 1-second delay between requests. For large imports, you may need to wait and retry.

---

## Best Practices

1. **Review before import**: Check CSV file for accuracy
2. **Update repo info**: Ensure REPO_OWNER and REPO_NAME are correct
3. **Backup first**: Export existing issues if any
4. **Test with subset**: Import a few issues first to verify
5. **Assign after import**: Bulk assign issues after confirming import
6. **Create projects**: Set up project boards immediately after import
7. **Sprint planning**: Use imported issues for sprint planning

---

## Next Steps

After importing issues:

1. ✅ Review all created issues
2. ✅ Create GitHub Projects
3. ✅ Set up project boards
4. ✅ Assign issues to team members
5. ✅ Schedule Sprint 1 planning meeting
6. ✅ Begin development!

---

## Resources

- [GitHub CLI Documentation](https://cli.github.com/manual/)
- [GitHub Projects Documentation](https://docs.github.com/en/issues/planning-and-tracking-with-projects)
- [Issue Templates](./issue-templates.md)
- [Milestone Structure](./milestones.md)
- [Main Roadmap](../README.md)

---

**Questions?** Contact Product Team at product@hdim.io

**Last Updated**: January 14, 2026
