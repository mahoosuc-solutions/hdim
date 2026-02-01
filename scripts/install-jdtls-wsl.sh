#!/usr/bin/env bash
#
# Install Eclipse JDT.LS (Java Language Server) on WSL2 Ubuntu
#
# Requirements:
#   - Java 17+ (HDIM has Java 21 ✓)
#   - curl or wget
#
# Installation:
#   chmod +x scripts/install-jdtls-wsl.sh
#   ./scripts/install-jdtls-wsl.sh
#

set -euo pipefail

JDTLS_HOME="$HOME/.local/share/jdtls"
JDTLS_BIN="$HOME/.local/bin/jdtls"
JDTLS_URL="https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz"

echo "================================================"
echo "Installing Eclipse JDT.LS for Claude Code"
echo "================================================"
echo ""

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java not found. Install Java 17+ first."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Error: Java 17+ required. Found Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $JAVA_VERSION detected"
echo ""

# Create directories
echo "Creating installation directories..."
mkdir -p "$JDTLS_HOME"
mkdir -p "$(dirname "$JDTLS_BIN")"

# Download JDT.LS
echo "Downloading JDT.LS..."
cd "$JDTLS_HOME"

if command -v wget &> /dev/null; then
    wget -q --show-progress "$JDTLS_URL" -O jdt-language-server-latest.tar.gz
elif command -v curl &> /dev/null; then
    curl -L --progress-bar "$JDTLS_URL" -o jdt-language-server-latest.tar.gz
else
    echo "❌ Error: wget or curl required"
    exit 1
fi

# Extract
echo "Extracting JDT.LS..."
tar -xzf jdt-language-server-latest.tar.gz
rm jdt-language-server-latest.tar.gz

# Create wrapper script
echo "Creating jdtls wrapper script..."
cat > "$JDTLS_BIN" << 'EOF'
#!/usr/bin/env bash
JDTLS_HOME="$HOME/.local/share/jdtls"

# Find the launcher jar
LAUNCHER_JAR=$(find "$JDTLS_HOME/plugins" -name 'org.eclipse.equinox.launcher_*.jar' | head -1)

if [ -z "$LAUNCHER_JAR" ]; then
    echo "Error: JDT.LS launcher not found in $JDTLS_HOME/plugins"
    exit 1
fi

# Detect OS for config
case "$(uname -s)" in
    Linux*)     CONFIG_DIR="config_linux";;
    Darwin*)    CONFIG_DIR="config_mac";;
    CYGWIN*|MINGW*|MSYS*) CONFIG_DIR="config_win";;
    *)          CONFIG_DIR="config_linux";;
esac

# Set workspace directory (first arg or default)
WORKSPACE="${1:-$HOME/.jdtls-workspace}"

# Launch JDT.LS
exec java \
  -Declipse.application=org.eclipse.jdt.ls.core.id1 \
  -Dosgi.bundles.defaultStartLevel=4 \
  -Declipse.product=org.eclipse.jdt.ls.core.product \
  -Dlog.level=ALL \
  -Xmx1G \
  --add-modules=ALL-SYSTEM \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  -jar "$LAUNCHER_JAR" \
  -configuration "$JDTLS_HOME/$CONFIG_DIR" \
  -data "$WORKSPACE" \
  "${@:2}"
EOF

chmod +x "$JDTLS_BIN"

# Add to PATH if not already there
if [[ ":$PATH:" != *":$HOME/.local/bin:"* ]]; then
    echo "Adding ~/.local/bin to PATH..."

    # Detect shell
    if [ -n "$BASH_VERSION" ]; then
        SHELL_RC="$HOME/.bashrc"
    elif [ -n "$ZSH_VERSION" ]; then
        SHELL_RC="$HOME/.zshrc"
    else
        SHELL_RC="$HOME/.profile"
    fi

    echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$SHELL_RC"
    export PATH="$HOME/.local/bin:$PATH"

    echo "✅ Added to PATH in $SHELL_RC"
    echo "   Run: source $SHELL_RC"
fi

echo ""
echo "================================================"
echo "✅ Installation Complete!"
echo "================================================"
echo ""
echo "Installation details:"
echo "  JDTLS Home:  $JDTLS_HOME"
echo "  Binary:      $JDTLS_BIN"
echo "  Java:        $(java -version 2>&1 | head -1)"
echo ""
echo "Verify installation:"
echo "  which jdtls"
echo "  jdtls --version"
echo ""
echo "Next steps:"
echo "  1. Restart your terminal (or run: source ~/.bashrc)"
echo "  2. Restart Claude Code to activate jdtls-lsp plugin"
echo "  3. Open a .java file to test autocomplete"
echo ""
