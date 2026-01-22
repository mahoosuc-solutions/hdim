# HDIM Claude Code Plugins - Quick Reference

**Last Updated:** January 22, 2026
**Total Plugins:** 31 plugins from 2 marketplaces

This document catalogs all Claude Code plugins installed for HDIM development, organized by priority and use case.

---

## Quick Start

### Using Slash Commands

```bash
# Enhanced git workflow
/commit                    # Interactive commit with AI-generated messages

# Feature development
/feature-dev               # Guided feature implementation workflow

# Code review
/review-pr [PR_NUMBER]     # Comprehensive PR review

# Custom hooks
/hookify                   # Create custom pre/post execution hooks

# Help
/help                      # List all available commands
```

### Active Output Mode

**Current:** `learning-output-style` (Learning mode with explanations)

**Available modes:**
- `/learning` - Learning mode (current)
- `/explanatory` - Explanatory mode
- `/standard` - Standard mode

---

## ⭐ High Priority Plugins (Must Use)

### Java Development

#### `jdtls-lsp` - Java Language Server ⚠️ REQUIRES INSTALLATION

**Status:** Plugin installed, but **JDT.LS server NOT installed**

**What it does:**
- IntelliSense for Java code
- Code navigation (go-to-definition, find references)
- Refactoring support
- Real-time error detection

**Installation Required:**

```bash
# macOS (Homebrew)
brew install jdtls

# Linux (Arch/AUR)
yay -S jdtls

# Manual installation
# 1. Download from: https://download.eclipse.org/jdtls/snapshots/
# 2. Extract to ~/.local/share/jdtls
# 3. Add wrapper script to PATH
```

**Requirements:**
- Java 17+ (JDK, not just JRE) ✅ (HDIM uses Java 21)

**Documentation:** [Eclipse JDT.LS](https://github.com/eclipse-jdtls/eclipse.jdt.ls)

---

### Security & Compliance

#### `security-guidance` - HIPAA Security Warnings

**What it does:**
- Automatic security scanning when editing files
- Warns about command injection, XSS, SQL injection
- OWASP Top 10 vulnerability detection
- Critical for PHI handling compliance

**Usage:** Automatic (runs as pre-edit hook)

---

### Code Quality

#### `code-review` - Automated Code Review

**What it does:**
- Automated code review on file changes
- Detects code smells, anti-patterns
- Suggests improvements

**Command:** `/code-review`

---

#### `pr-review-toolkit` - Comprehensive PR Review

**What it does:**
- Multi-agent PR review system
- Checks code quality, tests, silent failures, type design
- Generates detailed review reports

**Command:** `/review-pr [PR_NUMBER]`

**Agents:**
- `code-reviewer` - Code quality & style
- `pr-test-analyzer` - Test coverage analysis
- `silent-failure-hunter` - Error handling review
- `type-design-analyzer` - Type design quality
- `comment-analyzer` - Documentation review

---

#### `code-simplifier` - Code Simplification

**What it does:**
- Identifies over-engineered code
- Suggests simplifications
- Reduces complexity

**Usage:** Part of code review workflow

---

### Workflow Automation

#### `commit-commands` - Enhanced Git Workflow

**What it does:**
- AI-generated commit messages
- Follows repository commit style
- Interactive staging
- Automatic PR creation

**Commands:**
- `/commit` - Create commit with AI message
- `/commit-push-pr` - Commit, push, and create PR
- `/clean_gone` - Clean deleted remote branches

**Example:**
```bash
/commit
# Analyzes changes, suggests commit message, creates commit
```

---

#### `feature-dev` - Guided Feature Development

**What it does:**
- Structured feature implementation workflow
- Codebase exploration → architecture design → implementation → review
- Multi-agent system for complex features

**Command:** `/feature-dev`

**Agents:**
- `code-explorer` - Analyze existing codebase
- `code-architect` - Design feature architecture
- `code-reviewer` - Review implementation

---

## 🟡 Medium Priority Plugins (Very Useful)

### Testing

#### `playwright` - E2E Testing

**What it does:**
- Playwright test generation and execution
- Useful for Angular frontend testing

**Command:** `/playwright`

---

### Language Support

#### `typescript-lsp` - TypeScript Language Server

**What it does:**
- TypeScript IntelliSense for Angular development
- Code navigation and refactoring

**Supported:** `.ts`, `.tsx` files

---

#### `pyright-lsp` - Python Language Server

**What it does:**
- Python IntelliSense for utility scripts
- Type checking and code navigation

**Supported:** `.py` files

---

### Custom Automation

#### `hookify` - Custom Hooks

**What it does:**
- Create custom pre/post execution hooks
- Prevent unwanted behaviors
- Automate repetitive tasks

**Commands:**
- `/hookify` - Create hooks from conversation analysis
- `/hookify:list` - List configured hooks
- `/hookify:configure` - Enable/disable hooks

**Example Use Cases:**
- Prevent direct database schema changes (force Liquibase)
- Auto-run tests before commits
- Validate HIPAA compliance before file writes

---

#### `ralph-loop` / `ralph-wiggum` - Ralph Wiggum Technique

**What it does:**
- Iterative problem-solving technique
- Useful for complex debugging

**Commands:**
- `/ralph-loop` - Start Ralph loop
- `/cancel-ralph` - Stop active loop

---

### Documentation

#### `claude-md-management` - CLAUDE.md Management

**What it does:**
- Automatically update CLAUDE.md with learnings
- Version tracking for documentation

**Command:** `/revise-claude-md`

---

#### `notion` - Notion Integration

**What it does:**
- Sync documentation to Notion
- Create tasks, databases, pages

**Commands:**
- `/notion:create-page` - Create Notion page
- `/notion:create-task` - Create task
- `/notion:tasks:build` - Build task from page

**Setup Required:** Notion API key

---

## 🟢 Low Priority Plugins (Optional)

### External Integrations

#### `github` - GitHub Integration

**What it does:**
- Enhanced GitHub PR/issue management
- Uses `gh` CLI under the hood

**Setup Required:** GitHub token

---

#### `linear` - Linear Project Management

**What it does:**
- Linear issue tracking integration

**Setup Required:** Linear API key

---

#### `greptile` - Advanced Code Search

**What it does:**
- AI-powered code search
- Semantic code understanding

---

#### `vercel` - Vercel Deployment

**What it does:**
- Deploy to Vercel
- View deployment logs

**Commands:**
- `/vercel:deploy`
- `/vercel:logs`

**Note:** May not be needed for HDIM (backend-focused)

---

#### `stripe` - Stripe Integration

**What it does:**
- Stripe payment testing
- Error code explanations

**Commands:**
- `/stripe:explain-error`
- `/stripe:test-cards`

**Note:** Not needed for HDIM (healthcare, not payments)

---

#### `firebase` - Firebase Integration

**What it does:**
- Firebase deployment and management

**Note:** May not be needed for HDIM

---

### Development Tools

#### `agent-sdk-dev` - Agent SDK Development

**What it does:**
- Create Claude Agent SDK applications

**Command:** `/new-sdk-app`

---

#### `plugin-dev` - Plugin Development

**What it does:**
- Create and validate Claude Code plugins

**Command:** `/create-plugin`

**Agents:**
- `agent-creator` - Create agents
- `plugin-validator` - Validate plugins
- `skill-reviewer` - Review skills

---

#### `huggingface-skills` - HuggingFace Integration

**What it does:**
- HuggingFace model integration
- ML workflows

---

#### `pinecone` - Vector Database Integration

**What it does:**
- Pinecone vector database operations
- Semantic search

**Commands:**
- `/pinecone:query`
- `/pinecone:help`

---

### Output Modes

#### `learning-output-style` ⭐ (Currently Active)

**What it does:**
- Learning mode with educational explanations
- Interactive learning approach
- Prompts user contributions for key decisions

---

#### `explanatory-output-style`

**What it does:**
- Explanatory mode with technical insights
- Detailed implementation explanations

---

#### `frontend-design` - Frontend Design Workflows

**What it does:**
- Production-grade frontend design
- Avoids generic AI aesthetics
- High design quality components

---

## Migration & Utilities

#### `claude-opus-4-5-migration`

**What it does:**
- Migrate prompts/code to Opus 4.5
- Model string updates

---

#### `claude-code-setup` - Setup Wizard

**What it does:**
- Initial Claude Code configuration
- Setup assistant

---

#### `superpowers` - Skill Discovery System

**What it does:**
- Skill invocation framework
- Discover and use available skills

**Note:** This is a meta-plugin that enables the skill system

---

## Post-Installation Setup

### ⚠️ Critical: Install JDT.LS for Java Support

```bash
# macOS
brew install jdtls

# Verify installation
which jdtls
jdtls --version
```

### Optional: Configure External Integrations

**GitHub (for `github` plugin):**
```bash
gh auth login
```

**Notion (for `notion` plugin):**
1. Get API key from Notion
2. Configure in Claude Code settings

---

## Next Steps

### 1. Restart Claude Code

Plugins are installed but **not yet loaded**. Exit and restart Claude Code to activate them.

```bash
# Exit Claude Code
exit

# Restart Claude Code
claude-code
```

---

### 2. Post-Restart Validation Checklist

After restarting, verify plugins loaded correctly:

**Basic Validation:**
```bash
/help                      # Should show new commands like /commit, /feature-dev, /review-pr
```

**Test Critical Plugins:**

**a) Test commit workflow:**
```bash
# Make a trivial change
echo "# test" >> README.md

# Test commit command
/commit

# Expected: AI analyzes changes, suggests commit message, creates commit
```

**b) Test Java LSP (requires JDT.LS installation):**
- Open any `.java` file (e.g., `backend/modules/services/gateway-service/src/main/java/com/healthcare/gateway/GatewayServiceApplication.java`)
- Type a class name and trigger autocomplete
- Expected: IntelliSense suggestions appear

**c) Verify plugin list:**
```bash
/plugin list               # Should show ~31 plugins
```

---

### 3. Configure for HDIM Workflow

**Enable HIPAA compliance checks:**
- `security-guidance` automatically scans for vulnerabilities
- No configuration needed

**Set up commit workflow:**
```bash
/commit                    # Test on a sample change
```

**Create custom hooks (optional):**
```bash
/hookify                   # Create hooks to prevent unwanted behaviors

# Example hooks to create:
# - Prevent direct schema changes (force Liquibase)
# - Auto-run entity-migration validation before builds
# - Validate no PHI in logs
```

---

## Troubleshooting

### Plugin Not Loading

**Check plugin list:**
```bash
/plugin list
```

**Check installation:**
```bash
cat ~/.claude/plugins/installed_plugins.json
```

**Reinstall plugin:**
```bash
/plugin install PLUGIN_NAME
```

---

### JDT.LS Not Working

**Verify installation:**
```bash
which jdtls
jdtls --version
```

**Check Java version:**
```bash
java -version
# Should be Java 17+
```

**Reinstall:**
```bash
brew reinstall jdtls
```

---

### Command Not Found

**After restart, if commands like `/commit` don't work:**

1. Verify plugin loaded: `/plugin list`
2. Check plugin cache: `ls ~/.claude/plugins/cache/`
3. Reinstall: `/plugin install commit-commands`

---

## Plugin Storage Locations

```
~/.claude/plugins/
├── installed_plugins.json           # Installation registry
├── known_marketplaces.json          # Marketplace sources
├── cache/                           # Plugin code cache
│   ├── claude-code-plugins/         # 13 plugins
│   └── claude-plugins-official/     # 29 plugins
└── marketplaces/                    # Marketplace clones
    ├── claude-code-plugins/
    └── claude-plugins-official/
```

---

## Additional Resources

**Plugin Marketplaces:**
- [claude-code-plugins](https://github.com/anthropics/claude-code) (Official)
- [claude-plugins-official](https://github.com/anthropics/claude-plugins-official) (Official)

**Documentation:**
- [Claude Code Plugin Development](https://github.com/anthropics/claude-code/blob/main/docs/plugins.md)
- [Eclipse JDT.LS](https://github.com/eclipse-jdtls/eclipse.jdt.ls)

---

## Summary

**✅ Installed:** 31 plugins
**⚠️ Action Required:** Install JDT.LS for Java support
**🔄 Next Step:** Restart Claude Code to activate plugins

**Most Critical for HDIM:**
1. `jdtls-lsp` - Java IntelliSense (install JDT.LS first)
2. `security-guidance` - HIPAA compliance
3. `commit-commands` - Git workflow
4. `code-review` - Code quality
5. `feature-dev` - Feature development

---

_Last Updated: January 22, 2026_
