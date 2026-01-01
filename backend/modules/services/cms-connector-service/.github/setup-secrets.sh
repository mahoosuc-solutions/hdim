#!/bin/bash

# GitHub Actions Secrets Setup Script
# This script helps you generate SSH keys and set up GitHub Actions secrets

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
  echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

print_success() {
  echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
  echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
  echo -e "${RED}✗ $1${NC}"
}

print_info() {
  echo -e "${BLUE}ℹ $1${NC}"
}

# Main script
main() {
  print_header "GitHub Actions Secrets Setup"
  
  echo "This script will help you:"
  echo "1. Generate SSH keys for deployment"
  echo "2. Display secret values for manual GitHub configuration"
  echo "3. Provide commands for GitHub CLI setup (if available)"
  echo ""
  
  # Check prerequisites
  if ! command -v ssh-keygen &> /dev/null; then
    print_error "ssh-keygen not found. Please install OpenSSH."
    exit 1
  fi
  
  # Check if gh CLI is available
  HAS_GH_CLI=false
  if command -v gh &> /dev/null; then
    HAS_GH_CLI=true
    print_success "GitHub CLI found"
  else
    print_warning "GitHub CLI (gh) not found - will show manual setup instructions"
  fi
  
  # Menu
  while true; do
    echo ""
    echo "Select an option:"
    echo "1) Generate SSH keys for development"
    echo "2) Generate SSH keys for staging"
    echo "3) Show Docker Hub setup instructions"
    echo "4) Set all secrets via GitHub CLI (requires gh installed)"
    echo "5) Exit"
    echo ""
    read -p "Enter your choice (1-5): " choice
    
    case $choice in
      1)
        setup_dev_ssh
        ;;
      2)
        setup_staging_ssh
        ;;
      3)
        show_docker_setup
        ;;
      4)
        if [ "$HAS_GH_CLI" = true ]; then
          setup_with_gh_cli
        else
          print_error "GitHub CLI not installed. Install it from https://cli.github.com"
        fi
        ;;
      5)
        print_success "Exiting setup script"
        exit 0
        ;;
      *)
        print_error "Invalid choice. Please select 1-5."
        ;;
    esac
  done
}

setup_dev_ssh() {
  print_header "Generate Development SSH Key"
  
  KEY_PATH="$HOME/.ssh/github_dev"
  
  if [ -f "$KEY_PATH" ]; then
    read -p "Key already exists at $KEY_PATH. Overwrite? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      print_warning "Skipped"
      return
    fi
  fi
  
  ssh-keygen -t ed25519 -f "$KEY_PATH" -C "github-actions-dev" -N ""
  print_success "SSH key generated at $KEY_PATH"
  
  echo ""
  print_info "Public key (add to development server's ~/.ssh/authorized_keys):"
  echo "---"
  cat "${KEY_PATH}.pub"
  echo "---"
  
  echo ""
  print_info "Private key for GitHub secret DEV_DEPLOY_KEY:"
  echo "---"
  cat "$KEY_PATH"
  echo "---"
  
  echo ""
  print_info "Next steps:"
  echo "1. Copy the public key above"
  echo "2. SSH into your development server"
  echo "3. Run: mkdir -p ~/.ssh && echo 'PUBLIC_KEY_HERE' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
  echo "4. Set DEV_DEPLOY_KEY secret in GitHub with the private key above"
}

setup_staging_ssh() {
  print_header "Generate Staging SSH Key"
  
  KEY_PATH="$HOME/.ssh/github_staging"
  
  if [ -f "$KEY_PATH" ]; then
    read -p "Key already exists at $KEY_PATH. Overwrite? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      print_warning "Skipped"
      return
    fi
  fi
  
  ssh-keygen -t ed25519 -f "$KEY_PATH" -C "github-actions-staging" -N ""
  print_success "SSH key generated at $KEY_PATH"
  
  echo ""
  print_info "Public key (add to staging server's ~/.ssh/authorized_keys):"
  echo "---"
  cat "${KEY_PATH}.pub"
  echo "---"
  
  echo ""
  print_info "Private key for GitHub secret STAGING_DEPLOY_KEY:"
  echo "---"
  cat "$KEY_PATH"
  echo "---"
  
  echo ""
  print_info "Next steps:"
  echo "1. Copy the public key above"
  echo "2. SSH into your staging server"
  echo "3. Run: mkdir -p ~/.ssh && echo 'PUBLIC_KEY_HERE' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
  echo "4. Set STAGING_DEPLOY_KEY secret in GitHub with the private key above"
}

show_docker_setup() {
  print_header "Docker Hub Access Token Setup"
  
  echo "To create Docker Hub credentials for GitHub Actions:"
  echo ""
  echo "1. Go to: https://hub.docker.com/settings/security"
  echo "2. Click 'New Access Token'"
  echo "3. Name it: 'github-actions-ci'"
  echo "4. Select permissions: Read, Write, Delete"
  echo "5. Click 'Generate'"
  echo "6. Copy the token (you won't see it again)"
  echo ""
  echo "Then set in GitHub:"
  echo "  DOCKER_USERNAME: Your Docker Hub username"
  echo "  DOCKER_PASSWORD: The access token from step 6"
  echo ""
  print_warning "Never use your Docker Hub password directly - always use an access token"
}

setup_with_gh_cli() {
  print_header "Setup Secrets with GitHub CLI"
  
  echo "This will set all required secrets in your GitHub repository."
  echo ""
  
  # Check authentication
  if ! gh auth status &> /dev/null; then
    print_error "Not authenticated with GitHub CLI"
    echo "Run: gh auth login"
    return
  fi
  
  # Docker secrets
  echo ""
  read -p "Enter Docker Hub username: " docker_user
  read -sp "Enter Docker Hub access token: " docker_token
  echo ""
  
  print_info "Setting Docker secrets..."
  gh secret set DOCKER_USERNAME --body "$docker_user"
  gh secret set DOCKER_PASSWORD --body "$docker_token"
  print_success "Docker secrets set"
  
  # Development environment
  echo ""
  read -p "Enter development server hostname/IP: " dev_host
  read -p "Enter development SSH username: " dev_user
  
  if [ ! -f "$HOME/.ssh/github_dev" ]; then
    print_warning "Dev SSH key not found. Generate it with option 1 first."
    return
  fi
  
  print_info "Setting development environment secrets..."
  gh secret set DEV_DEPLOY_HOST --env development --body "$dev_host"
  gh secret set DEV_DEPLOY_USER --env development --body "$dev_user"
  gh secret set DEV_DEPLOY_KEY --env development --body "$(cat $HOME/.ssh/github_dev)"
  print_success "Development secrets set"
  
  # Staging environment
  echo ""
  read -p "Enter staging server hostname/IP: " staging_host
  read -p "Enter staging SSH username: " staging_user
  
  if [ ! -f "$HOME/.ssh/github_staging" ]; then
    print_warning "Staging SSH key not found. Generate it with option 2 first."
    return
  fi
  
  print_info "Setting staging environment secrets..."
  gh secret set STAGING_DEPLOY_HOST --env staging --body "$staging_host"
  gh secret set STAGING_DEPLOY_USER --env staging --body "$staging_user"
  gh secret set STAGING_DEPLOY_KEY --env staging --body "$(cat $HOME/.ssh/github_staging)"
  print_success "Staging secrets set"
  
  echo ""
  print_success "All secrets configured!"
  echo ""
  print_info "Next steps:"
  echo "1. Create development and staging environments in GitHub Settings > Environments"
  echo "2. Configure protection rules if desired (require approvals)"
  echo "3. Ensure SSH keys are added to deployment servers' authorized_keys files"
  echo "4. Test by pushing to develop branch to trigger deployment"
}

# Run main
main
