# Plugin Activation Checklist

**Status:** Plugins installed, awaiting restart to activate
**Date:** January 22, 2026

---

## Pre-Restart Status

✅ **31 plugins installed** from 2 marketplaces
✅ **Installation validated** - All manifests valid
✅ **Plugin reference created** - See `docs/CLAUDE_CODE_PLUGINS.md`
⏳ **Awaiting restart** - Plugins not yet active

---

## Step 1: Install JDT.LS (Java Language Server)

**⚠️ CRITICAL for Java development**

```bash
# macOS (recommended)
brew install jdtls

# Verify installation
which jdtls
jdtls --version
```

**Expected output:**
```
/opt/homebrew/bin/jdtls
Eclipse JDT Language Server vX.X.X
```

**If installation fails:**
See manual installation instructions in `docs/CLAUDE_CODE_PLUGINS.md`

---

## Step 2: Restart Claude Code

**Save all work, then:**

```bash
# Exit Claude Code
exit

# Restart
claude-code
```

**What happens on restart:**
- Claude Code loads all 31 plugins
- New slash commands become available
- LSP servers initialize
- Hooks activate

---

## Step 3: Verify Plugin Loading

### 3.1 Check Available Commands

```bash
/help
```

**Expected new commands:**
- `/commit` - Enhanced git workflow
- `/feature-dev` - Feature development
- `/review-pr` - PR review
- `/hookify` - Custom hooks
- (and ~20+ more)

**❌ If commands missing:**
- Run `/plugin list` to see loaded plugins
- Check `~/.claude/plugins/installed_plugins.json`
- Reinstall: `/plugin install PLUGIN_NAME`

---

### 3.2 Test Java Language Server

**Open a Java file:**
```bash
# Example file
backend/modules/services/gateway-service/src/main/java/com/healthcare/gateway/GatewayServiceApplication.java
```

**Test autocomplete:**
1. Type a partial class name (e.g., `Spring`)
2. Trigger autocomplete (usually Ctrl+Space or automatic)
3. **Expected:** IntelliSense suggestions appear

**❌ If autocomplete doesn't work:**
1. Verify JDT.LS installed: `which jdtls`
2. Check Java version: `java -version` (should be 17+)
3. Restart Claude Code again

---

### 3.3 Test Commit Workflow

**Make a trivial change:**
```bash
echo "# Plugin activation test" >> README.md
```

**Test commit command:**
```bash
/commit
```

**Expected behavior:**
1. Analyzes git changes
2. Shows commit diff
3. Generates AI commit message
4. Creates commit with co-author attribution

**❌ If error occurs:**
- Check git status: `git status`
- Verify plugin loaded: `/plugin list | grep commit-commands`

---

## Step 4: Configure Critical Plugins

### 4.1 Security Guidance (Automatic)

✅ **No configuration needed**

`security-guidance` automatically scans for:
- Command injection
- XSS vulnerabilities
- SQL injection
- HIPAA compliance issues

**Test:** Edit a file with potential security issue, expect warning.

---

### 4.2 GitHub Integration (Optional)

**If using `/review-pr` or `github` plugin:**

```bash
gh auth login
```

**Follow prompts to authenticate**

---

### 4.3 Notion Integration (Optional)

**If using `notion` plugin:**

1. Get Notion API key from https://www.notion.so/my-integrations
2. Configure in Claude Code settings
3. Test: `/notion:create-page`

---

## Step 5: Create Custom Hooks (Recommended)

**Use hookify to prevent common mistakes:**

```bash
/hookify
```

**Recommended hooks for HDIM:**

**1. Prevent direct schema changes:**
```
When: User tries to modify database schema
Action: Remind to use Liquibase migrations
Why: Prevents schema drift
```

**2. Auto-validate entity-migrations:**
```
When: Before Docker build
Action: Run entity-migration validation test
Why: Catch schema mismatches early
```

**3. HIPAA compliance check:**
```
When: Writing logs or cache code
Action: Scan for PHI in logs/cache
Why: Prevent HIPAA violations
```

**Create hooks:**
```bash
/hookify

# Follow prompts to create each hook
# Or analyze conversation for behaviors to prevent
```

**List configured hooks:**
```bash
/hookify:list
```

---

## Step 6: Update CLAUDE.md

**Add plugin reference to CLAUDE.md:**

Edit `CLAUDE.md` to add:

```markdown
## Claude Code Plugins

**31 plugins installed** - See [Plugin Reference](./docs/CLAUDE_CODE_PLUGINS.md)

**Key Commands:**
- `/commit` - Enhanced git commits
- `/feature-dev` - Feature development workflow
- `/review-pr` - Comprehensive PR review

**Java LSP:** `jdtls-lsp` provides IntelliSense for Java 21
```

**Or use automated update:**
```bash
/revise-claude-md

# Add: "Document newly installed plugins and key commands"
```

---

## Step 7: Team Notification

**Share with team:**

```markdown
🎉 **Claude Code Plugins Activated**

31 new plugins installed for HDIM development:

**Most Useful:**
- `/commit` - AI-generated commit messages
- `/feature-dev` - Guided feature development
- `/review-pr` - Automated PR reviews
- Java IntelliSense (jdtls-lsp)
- HIPAA security scanning (auto)

**Documentation:** docs/CLAUDE_CODE_PLUGINS.md

**Setup Required:** Install jdtls for Java support
```

---

## Validation Checklist

### Pre-Restart
- [x] 31 plugins installed
- [x] Installation validated
- [x] Plugin reference created
- [ ] JDT.LS installed (requires action)

### Post-Restart
- [ ] `/help` shows new commands
- [ ] `/commit` works
- [ ] Java autocomplete works (requires JDT.LS)
- [ ] `/plugin list` shows 31 plugins
- [ ] Security warnings appear when editing files

### Configuration
- [ ] GitHub authenticated (optional)
- [ ] Notion API configured (optional)
- [ ] Custom hooks created (recommended)
- [ ] CLAUDE.md updated with plugin info

---

## Troubleshooting Reference

| Issue | Solution |
|-------|----------|
| Commands not found | Run `/plugin list`, verify plugin loaded |
| Java autocomplete not working | Install JDT.LS: `brew install jdtls` |
| Plugin error on load | Check logs, reinstall plugin |
| Commit command fails | Verify git status, check plugin |
| Security warnings too aggressive | Configure in `hookify` settings |

**Full troubleshooting:** See `docs/CLAUDE_CODE_PLUGINS.md` → Troubleshooting section

---

## Success Criteria

**Minimum viable:**
- ✅ Plugins load without errors
- ✅ `/commit` command works
- ✅ Security scanning active

**Ideal state:**
- ✅ All 31 plugins loaded
- ✅ JDT.LS installed and working
- ✅ Java autocomplete functional
- ✅ Custom hooks configured
- ✅ Team documented

---

## Next Actions (After Validation)

1. **Use plugins in daily workflow:**
   - Use `/commit` for all git commits
   - Use `/feature-dev` for new features
   - Use `/review-pr` before merging PRs

2. **Create HDIM-specific hooks:**
   - Liquibase enforcement
   - Entity-migration validation
   - HIPAA compliance checks

3. **Explore additional plugins:**
   - `/playwright` for E2E testing
   - `/notion` for documentation
   - `/hookify` for custom automation

---

## Quick Reference

**Plugin docs:** `docs/CLAUDE_CODE_PLUGINS.md`
**Restart command:** `exit` then `claude-code`
**Install JDT.LS:** `brew install jdtls`
**Verify plugins:** `/plugin list`
**Test commit:** `/commit`

---

_Created: January 22, 2026_
_Status: Pre-Restart Validation Complete_
