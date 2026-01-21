#!/usr/bin/env python3
"""
Import GitHub Issues from CSV

Prerequisites:
1. Install GitHub CLI: https://cli.github.com/
2. Authenticate: gh auth login
3. Python 3.6+ (uses built-in csv module)
"""

import csv
import subprocess
import sys
import json
from pathlib import Path

REPO_OWNER = "webemo-aaron"
REPO_NAME = "hdim"
CSV_FILE = Path(__file__).parent / "github-issues-q1-2026.csv"

# Colors for output
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
RED = '\033[0;31m'
NC = '\033[0m'  # No Color

def print_colored(message, color=NC):
    print(f"{color}{message}{NC}")

def check_gh_cli():
    """Check if GitHub CLI is installed and authenticated."""
    try:
        result = subprocess.run(
            ["gh", "auth", "status"],
            capture_output=True,
            text=True,
            check=True
        )
        print_colored("✓ GitHub CLI authenticated", GREEN)
        return True
    except subprocess.CalledProcessError:
        print_colored("✗ Error: Not authenticated with GitHub", RED)
        print_colored("Run: gh auth login", YELLOW)
        return False
    except FileNotFoundError:
        print_colored("✗ Error: GitHub CLI (gh) is not installed", RED)
        print_colored("Install from: https://cli.github.com/", YELLOW)
        return False

def create_milestone(name, due_date, description):
    """Create a GitHub milestone if it doesn't exist."""
    # Check if milestone exists
    result = subprocess.run(
        ["gh", "api", f"/repos/{REPO_OWNER}/{REPO_NAME}/milestones"],
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        milestones = json.loads(result.stdout)
        for milestone in milestones:
            if milestone["title"] == name:
                print_colored(f"Milestone '{name}' already exists", YELLOW)
                return milestone["number"]
    
    # Create milestone
    print_colored(f"Creating milestone: {name}", GREEN)
    result = subprocess.run(
        ["gh", "api", f"/repos/{REPO_OWNER}/{REPO_NAME}/milestones"],
        method="POST",
        input=json.dumps({
            "title": name,
            "state": "open",
            "description": description,
            "due_on": due_date
        }),
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        milestone = json.loads(result.stdout)
        return milestone["number"]
    else:
        print_colored(f"Failed to create milestone: {result.stderr}", RED)
        return None

def create_issue(title, body, labels, milestone):
    """Create a GitHub issue."""
    # Prepare labels array
    label_list = [l.strip() for l in labels.split(",") if l.strip()]
    
    # Build gh command
    cmd = [
        "gh", "issue", "create",
        "--repo", f"{REPO_OWNER}/{REPO_NAME}",
        "--title", title,
        "--body", body
    ]
    
    # Add labels
    for label in label_list:
        cmd.extend(["--label", label])
    
    # Add milestone if provided
    if milestone and milestone.strip():
        cmd.extend(["--milestone", milestone.strip()])
    
    # Execute command
    result = subprocess.run(
        cmd,
        capture_output=True,
        text=True
    )
    
    if result.returncode == 0:
        # Extract issue URL from output
        output = result.stdout.strip()
        if "https://github.com" in output:
            issue_url = output.split()[-1]
            print_colored(f"✓ Created: {issue_url}", GREEN)
            return True
        print_colored(f"✓ Created", GREEN)
        return True
    else:
        print_colored(f"✗ Failed: {result.stderr}", RED)
        return False

def main():
    print_colored("=== GitHub Issues Import Script ===", GREEN)
    print()
    
    # Check prerequisites
    if not check_gh_cli():
        sys.exit(1)
    
    print_colored(f"Repository: {REPO_OWNER}/{REPO_NAME}", YELLOW)
    print()
    
    # Confirm before proceeding
    response = input("This will create ~40 issues in your repository. Continue? (y/N) ")
    if response.lower() != 'y':
        print("Cancelled.")
        sys.exit(0)
    
    # Create milestones
    print_colored("\nCreating milestones...", GREEN)
    milestones = {
        "Q1-2026-Clinical-Portal": create_milestone(
            "Q1-2026-Clinical-Portal",
            "2026-03-15T00:00:00Z",
            "Complete Clinical User Portal with patient search, care gaps, quality measures, and AI assistant"
        ),
        "Q1-2026-Admin-Portal": create_milestone(
            "Q1-2026-Admin-Portal",
            "2026-03-20T00:00:00Z",
            "Enhanced Admin Portal with service monitoring, audit logs, and user/tenant management"
        ),
        "Q1-2026-Agent-Studio": create_milestone(
            "Q1-2026-Agent-Studio",
            "2026-03-25T00:00:00Z",
            "AI Agent Studio for no-code agent creation and testing"
        ),
        "Q1-2026-Developer-Portal": create_milestone(
            "Q1-2026-Developer-Portal",
            "2026-03-28T00:00:00Z",
            "Developer Portal with API docs, sandbox, and webhook configuration"
        ),
        "Q1-2026-Auth": create_milestone(
            "Q1-2026-Auth",
            "2026-03-10T00:00:00Z",
            "SSO, MFA, RBAC, and session management"
        ),
        "Q1-2026-Infrastructure": create_milestone(
            "Q1-2026-Infrastructure",
            "2026-03-05T00:00:00Z",
            "CI/CD pipelines and monitoring setup"
        ),
        "Q1-2026-Documentation": create_milestone(
            "Q1-2026-Documentation",
            "2026-03-27T00:00:00Z",
            "User guides and API documentation"
        ),
        "Q1-2026-Testing": create_milestone(
            "Q1-2026-Testing",
            "2026-03-26T00:00:00Z",
            "E2E and performance testing"
        ),
    }
    
    print()
    print_colored("Importing issues from CSV...", GREEN)
    
    # Read and import issues
    issue_count = 0
    error_count = 0
    
    with open(CSV_FILE, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        
        for row in reader:
            title = row['Title'].strip()
            body = row['Body'].strip()
            labels = row['Labels'].strip()
            milestone = row['Milestone'].strip()
            
            # Skip empty rows
            if not title:
                continue
            
            print_colored(f"Creating issue: {title[:60]}...", YELLOW)
            
            if create_issue(title, body, labels, milestone):
                issue_count += 1
            else:
                error_count += 1
            
            # Small delay to avoid rate limiting
            import time
            time.sleep(1)
    
    print()
    print_colored("=== Import Complete ===", GREEN)
    print_colored(f"Issues created: {GREEN}{issue_count}{NC}")
    if error_count > 0:
        print_colored(f"Errors: {RED}{error_count}{NC}")
    
    print()
    print_colored("Next steps:", GREEN)
    print(f"1. View issues: gh issue list --repo {REPO_OWNER}/{REPO_NAME}")
    print(f"2. Create project board: gh project create --repo {REPO_OWNER}/{REPO_NAME} --title 'Q1 2026 Roadmap'")
    print("3. Assign issues to team members")
    print("4. Start Sprint 1!")

if __name__ == "__main__":
    main()
