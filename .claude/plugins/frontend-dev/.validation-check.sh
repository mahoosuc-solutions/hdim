#!/bin/bash
# Validation script for frontend-dev plugin

echo "🔍 Frontend Dev Plugin Validation"
echo "=================================="
echo ""

# Check plugin.json
echo "✓ Checking plugin.json..."
if [ -f "plugin.json" ]; then
    jq '.' plugin.json > /dev/null 2>&1 && echo "  ✅ Valid JSON" || echo "  ❌ Invalid JSON"
else
    echo "  ❌ plugin.json not found"
fi

# Count components
echo ""
echo "📊 Component Count:"
echo "  Agents:   $(ls -1 agents/*.md 2>/dev/null | wc -l)"
echo "  Commands: $(ls -1 commands/*.md 2>/dev/null | wc -l)"
echo "  Skills:   $(ls -1 skills/*.md 2>/dev/null | wc -l)"
echo "  Docs:     $(ls -1 *.md 2>/dev/null | wc -l)"

# Check required files
echo ""
echo "📁 Required Files:"
for file in "plugin.json" "README.md" "QUICK_REFERENCE.md" "EXAMPLES.md" "CHANGELOG.md"; do
    if [ -f "$file" ]; then
        echo "  ✅ $file"
    else
        echo "  ❌ $file (missing)"
    fi
done

# Check agents
echo ""
echo "🤖 Agents:"
for agent in "code-reviewer" "accessibility-analyzer" "test-coverage-analyzer" "performance-analyzer"; do
    if [ -f "agents/${agent}.md" ]; then
        echo "  ✅ $agent"
    else
        echo "  ❌ $agent (missing)"
    fi
done

# Check commands
echo ""
echo "⚡ Commands:"
for cmd in "build" "test" "lint" "generate" "e2e" "feature-dev"; do
    if [ -f "commands/${cmd}.md" ]; then
        echo "  ✅ $cmd"
    else
        echo "  ❌ $cmd (missing)"
    fi
done

# Check skills
echo ""
echo "📚 Skills:"
for skill in "react-patterns" "testing-strategy" "mui-customization" "zustand-patterns"; do
    if [ -f "skills/${skill}.md" ]; then
        echo "  ✅ $skill"
    else
        echo "  ❌ $skill (missing)"
    fi
done

echo ""
echo "=================================="
echo "✅ Validation complete!"
