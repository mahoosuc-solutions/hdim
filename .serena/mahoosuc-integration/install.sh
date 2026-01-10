#!/bin/bash
# HDIM Mahoosuc Integration Installer
# Installs HDIM commands and skills into Mahoosuc Operating System

set -e

echo "🔧 HDIM Mahoosuc Integration Installer"
echo "======================================"
echo ""

# Check if Mahoosuc directories exist
if [ ! -d ~/.claude ]; then
    echo "⚠️  Mahoosuc Operating System not found at ~/.claude"
    echo "   Creating directory structure..."
    mkdir -p ~/.claude/commands ~/.claude/skills
fi

# Install commands
echo "📦 Installing HDIM commands..."
mkdir -p ~/.claude/commands/hdim
cp commands/*.md ~/.claude/commands/hdim/

COMMANDS_INSTALLED=$(ls ~/.claude/commands/hdim/*.md | wc -l)
echo "   ✅ Installed $COMMANDS_INSTALLED commands"

# Install skills
echo "📦 Installing HDIM skills..."
mkdir -p ~/.claude/skills/hdim
cp skills/*.md ~/.claude/skills/hdim/

SKILLS_INSTALLED=$(ls ~/.claude/skills/hdim/*.md | wc -l)
echo "   ✅ Installed $SKILLS_INSTALLED skills"

echo ""
echo "======================================"
echo "✅ Installation Complete!"
echo ""
echo "📋 Installed Components:"
echo "   Commands: $COMMANDS_INSTALLED"
echo "   Skills:   $SKILLS_INSTALLED"
echo ""
echo "📚 Available Commands:"
echo "   /hdim-validate        - Run validation checks"
echo "   /hdim-service-create  - Create new service"
echo "   /hdim-memory          - Access Serena memories"
echo "   /hdim-service         - Manage services"
echo ""
echo "🎯 Available Skills:"
echo "   hdim-dev - HDIM development workflow"
echo ""
echo "📖 Next Steps:"
echo "   1. Read: .serena/mahoosuc-integration/INTEGRATION_GUIDE.md"
echo "   2. Quick Ref: .serena/mahoosuc-integration/QUICK_REFERENCE.md"
echo "   3. Examples: .serena/mahoosuc-integration/EXAMPLE_WORKFLOWS.md"
echo "   4. Test: /hdim-validate"
echo ""
echo "🚀 Ready to use HDIM commands in your Mahoosuc environment!"
