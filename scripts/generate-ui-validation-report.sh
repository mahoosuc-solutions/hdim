#!/bin/bash
#
# UI Implementation Validation Report Generator
# Validates Angular UI against customer success criteria, best practices, and HIPAA compliance
#
# Usage: bash scripts/generate-ui-validation-report.sh [--html] [--pdf]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/hdim-backend-tests/apps/clinical-portal"
REPORT_DIR="$PROJECT_ROOT/validation-reports"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  HDIM UI Implementation Validation${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""

# Create report directory
mkdir -p "$REPORT_DIR"

# Generate timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/ui-validation-report_$TIMESTAMP.txt"
JSON_FILE="$REPORT_DIR/ui-validation-results_$TIMESTAMP.json"
HTML_FILE="$REPORT_DIR/ui-validation-report_$TIMESTAMP.html"

echo -e "${YELLOW}📊 Analyzing Frontend Implementation...${NC}"
echo ""

# ============================================================================
# Manual Validation (File-Based)
# ============================================================================

echo -e "${BLUE}Running Component Analysis...${NC}"

# Count components
TOTAL_COMPONENTS=$(find "$FRONTEND_DIR/src/app" -name "*.component.ts" 2>/dev/null | wc -l)
TOTAL_SERVICES=$(find "$FRONTEND_DIR/src/app" -name "*.service.ts" 2>/dev/null | wc -l)
TOTAL_MODULES=$(find "$FRONTEND_DIR/src/app" -name "*.module.ts" 2>/dev/null | wc -l)
TOTAL_PAGES=$(find "$FRONTEND_DIR/src/app/pages" -maxdepth 1 -type d 2>/dev/null | wc -l)

echo "  Components: $TOTAL_COMPONENTS"
echo "  Services: $TOTAL_SERVICES"
echo "  Modules: $TOTAL_MODULES"
echo "  Pages: $((TOTAL_PAGES - 1))"  # Subtract parent directory
echo ""

# ============================================================================
# Feature Coverage Analysis
# ============================================================================

echo -e "${BLUE}Checking Feature Coverage...${NC}"

declare -A FEATURES
FEATURES["Care Gap Management"]=$([ -f "$FRONTEND_DIR/src/app/pages/care-gaps/care-gap-manager.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Patient Health Overview"]=$([ -f "$FRONTEND_DIR/src/app/pages/patient-health-overview/patient-health-overview.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Quality Measure Evaluation"]=$([ -f "$FRONTEND_DIR/src/app/pages/evaluations/evaluations.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Care Recommendations"]=$([ -f "$FRONTEND_DIR/src/app/pages/care-recommendations/care-recommendations.component.ts" ] && echo "✓" || echo "✗")
FEATURES["MA Dashboard"]=$([ -f "$FRONTEND_DIR/src/app/pages/dashboard/ma-dashboard/ma-dashboard.component.ts" ] && echo "✓" || echo "✗")
FEATURES["RN Dashboard"]=$([ -f "$FRONTEND_DIR/src/app/pages/dashboard/rn-dashboard/rn-dashboard.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Provider Dashboard"]=$([ -f "$FRONTEND_DIR/src/app/pages/dashboard/provider-dashboard/provider-dashboard.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Measure Builder"]=$([ -f "$FRONTEND_DIR/src/app/pages/measure-builder/measure-builder.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Reports & Analytics"]=$([ -f "$FRONTEND_DIR/src/app/pages/reports/reports.component.ts" ] && echo "✓" || echo "✗")
FEATURES["Patient Search"]=$([ -f "$FRONTEND_DIR/src/app/pages/patients/patients.component.ts" ] && echo "✓" || echo "✗")

FEATURE_COUNT=0
for feature in "${!FEATURES[@]}"; do
  status="${FEATURES[$feature]}"
  if [ "$status" == "✓" ]; then
    echo -e "  ${GREEN}$status${NC} $feature"
    ((FEATURE_COUNT++))
  else
    echo -e "  ${RED}$status${NC} $feature"
  fi
done

FEATURE_COVERAGE=$(echo "scale=1; $FEATURE_COUNT * 100 / ${#FEATURES[@]}" | bc)
echo ""
echo -e "  Feature Coverage: ${FEATURE_COUNT}/${#FEATURES[@]} (${FEATURE_COVERAGE}%)"
echo ""

# ============================================================================
# HIPAA Compliance Checks
# ============================================================================

echo -e "${BLUE}Checking HIPAA Compliance...${NC}"

declare -A HIPAA_CHECKS
HIPAA_CHECKS["Auth Guard"]=$(grep -r "canActivate" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
HIPAA_CHECKS["Auth Service"]=$([ -f "$FRONTEND_DIR/src/app/core/services/auth.service.ts" ] && echo "1" || echo "0")
HIPAA_CHECKS["Audit Logging"]=$(grep -r "audit" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
HIPAA_CHECKS["HTTPS Only"]=$(grep -r "http://" "$FRONTEND_DIR/src" 2>/dev/null | grep -v "localhost" | wc -l)
HIPAA_CHECKS["Console.log"]=$(grep -r "console\.log" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
HIPAA_CHECKS["localStorage PHI"]=$(grep -r "localStorage" "$FRONTEND_DIR/src/app" 2>/dev/null | grep -i -E "patient|phi|ssn|mrn" | wc -l)

echo "  Auth Guards: ${HIPAA_CHECKS["Auth Guard"]} found"
echo "  Auth Service: $([ "${HIPAA_CHECKS["Auth Service"]}" == "1" ] && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}")"
echo "  Audit References: ${HIPAA_CHECKS["Audit Logging"]} instances"
echo "  Insecure HTTP: $([ "${HIPAA_CHECKS["HTTPS Only"]}" == "0" ] && echo -e "${GREEN}0 (Good)${NC}" || echo -e "${RED}${HIPAA_CHECKS["HTTPS Only"]} (Bad)${NC}")"
echo "  Console.log: $([ "${HIPAA_CHECKS["Console.log"]}" == "0" ] && echo -e "${GREEN}0 (Good)${NC}" || echo -e "${YELLOW}${HIPAA_CHECKS["Console.log"]} (Remove for prod)${NC}")"
echo "  PHI in localStorage: $([ "${HIPAA_CHECKS["localStorage PHI"]}" == "0" ] && echo -e "${GREEN}0 (Good)${NC}" || echo -e "${RED}${HIPAA_CHECKS["localStorage PHI"]} (CRITICAL)${NC}")"
echo ""

# ============================================================================
# Best Practices Checks
# ============================================================================

echo -e "${BLUE}Checking Best Practices...${NC}"

# Responsive design
RESPONSIVE_COUNT=$(grep -r "@media\|fxLayout\|mat-grid" "$FRONTEND_DIR/src" 2>/dev/null | wc -l)
echo "  Responsive CSS: $RESPONSIVE_COUNT instances"

# Accessibility
ARIA_COUNT=$(grep -r "aria-label\|aria-labelledby" "$FRONTEND_DIR/src" 2>/dev/null | wc -l)
SEMANTIC_HTML=$(grep -r "<nav\|<main\|<section\|<header\|<footer" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
echo "  ARIA labels: $ARIA_COUNT instances"
echo "  Semantic HTML5: $SEMANTIC_HTML instances"

# Error handling
ERROR_HANDLING=$(grep -r "catchError\|ErrorHandler" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
LOADING_STATES=$(grep -r "loading\|spinner" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
echo "  Error handling: $ERROR_HANDLING instances"
echo "  Loading states: $LOADING_STATES instances"

# Performance
LAZY_LOADING=$(grep -r "loadChildren" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
ON_PUSH=$(grep -r "ChangeDetectionStrategy.OnPush" "$FRONTEND_DIR/src/app" 2>/dev/null | wc -l)
echo "  Lazy-loaded routes: $LAZY_LOADING"
echo "  OnPush components: $ON_PUSH"
echo ""

# ============================================================================
# Generate Text Report
# ============================================================================

{
  echo "════════════════════════════════════════════════════════════════"
  echo "  HDIM UI IMPLEMENTATION VALIDATION REPORT"
  echo "════════════════════════════════════════════════════════════════"
  echo ""
  echo "Generated: $(date)"
  echo "Frontend Directory: $FRONTEND_DIR"
  echo ""

  echo "────────────────────────────────────────────────────────────────"
  echo "CODEBASE STATISTICS"
  echo "────────────────────────────────────────────────────────────────"
  echo ""
  echo "  Components: $TOTAL_COMPONENTS"
  echo "  Services: $TOTAL_SERVICES"
  echo "  Modules: $TOTAL_MODULES"
  echo "  Pages: $((TOTAL_PAGES - 1))"
  echo ""

  echo "────────────────────────────────────────────────────────────────"
  echo "FEATURE COVERAGE: ${FEATURE_COVERAGE}%"
  echo "────────────────────────────────────────────────────────────────"
  echo ""
  for feature in "${!FEATURES[@]}"; do
    echo "  ${FEATURES[$feature]} $feature"
  done
  echo ""

  echo "────────────────────────────────────────────────────────────────"
  echo "HIPAA COMPLIANCE CHECKS"
  echo "────────────────────────────────────────────────────────────────"
  echo ""
  echo "  Auth Guards: ${HIPAA_CHECKS["Auth Guard"]}"
  echo "  Auth Service: $([ "${HIPAA_CHECKS["Auth Service"]}" == "1" ] && echo "✓ Present" || echo "✗ Missing")"
  echo "  Audit Logging: ${HIPAA_CHECKS["Audit Logging"]} references"
  echo "  Insecure HTTP: ${HIPAA_CHECKS["HTTPS Only"]} instances"
  echo "  Console.log: ${HIPAA_CHECKS["Console.log"]} instances (remove for production)"
  echo "  PHI in localStorage: ${HIPAA_CHECKS["localStorage PHI"]} instances (CRITICAL if >0)"
  echo ""

  echo "────────────────────────────────────────────────────────────────"
  echo "BEST PRACTICES"
  echo "────────────────────────────────────────────────────────────────"
  echo ""
  echo "  Responsive Design: $RESPONSIVE_COUNT instances"
  echo "  Accessibility (ARIA): $ARIA_COUNT labels"
  echo "  Semantic HTML5: $SEMANTIC_HTML elements"
  echo "  Error Handling: $ERROR_HANDLING implementations"
  echo "  Loading States: $LOADING_STATES instances"
  echo "  Lazy Loading: $LAZY_LOADING routes"
  echo "  OnPush Change Detection: $ON_PUSH components"
  echo ""

  echo "────────────────────────────────────────────────────────────────"
  echo "RECOMMENDATIONS"
  echo "────────────────────────────────────────────────────────────────"
  echo ""

  if [ "$FEATURE_COVERAGE" == "100.0" ]; then
    echo "  ✓ All core features implemented"
  else
    echo "  • Complete missing features to reach 100% coverage"
  fi

  if [ "${HIPAA_CHECKS["localStorage PHI"]}" -gt 0 ]; then
    echo "  ⚠ CRITICAL: Remove PHI from localStorage (HIPAA violation)"
  fi

  if [ "${HIPAA_CHECKS["Console.log"]}" -gt 10 ]; then
    echo "  • Remove console.log() statements before production"
  fi

  if [ "$ARIA_COUNT" -lt 10 ]; then
    echo "  • Improve accessibility with more ARIA labels"
  fi

  if [ "$LAZY_LOADING" -lt 3 ]; then
    echo "  • Implement lazy loading for better performance"
  fi

  echo ""
  echo "════════════════════════════════════════════════════════════════"

} > "$REPORT_FILE"

echo -e "${GREEN}✓ Report generated: $REPORT_FILE${NC}"

# ============================================================================
# Generate HTML Report
# ============================================================================

if [[ " $* " =~ " --html " ]]; then
  echo -e "${BLUE}Generating HTML report...${NC}"

  cat > "$HTML_FILE" <<EOF
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>HDIM UI Validation Report - $TIMESTAMP</title>
  <style>
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
      background: #f5f5f5;
    }
    .header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 30px;
      border-radius: 10px;
      margin-bottom: 30px;
    }
    .header h1 {
      margin: 0 0 10px 0;
    }
    .header p {
      margin: 5px 0;
      opacity: 0.9;
    }
    .card {
      background: white;
      border-radius: 8px;
      padding: 25px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .card h2 {
      margin-top: 0;
      color: #333;
      border-bottom: 2px solid #667eea;
      padding-bottom: 10px;
    }
    .stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 15px;
      margin: 20px 0;
    }
    .stat-box {
      background: #f8f9fa;
      padding: 15px;
      border-radius: 6px;
      border-left: 4px solid #667eea;
    }
    .stat-box .label {
      font-size: 0.9em;
      color: #666;
      margin-bottom: 5px;
    }
    .stat-box .value {
      font-size: 1.8em;
      font-weight: bold;
      color: #333;
    }
    .feature-list {
      list-style: none;
      padding: 0;
    }
    .feature-list li {
      padding: 10px;
      margin: 5px 0;
      background: #f8f9fa;
      border-radius: 4px;
      display: flex;
      align-items: center;
    }
    .feature-list li.pass {
      border-left: 4px solid #28a745;
    }
    .feature-list li.fail {
      border-left: 4px solid #dc3545;
    }
    .icon {
      margin-right: 10px;
      font-weight: bold;
      font-size: 1.2em;
    }
    .icon.pass { color: #28a745; }
    .icon.fail { color: #dc3545; }
    .progress-bar {
      background: #e9ecef;
      border-radius: 10px;
      height: 30px;
      overflow: hidden;
      margin: 15px 0;
    }
    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #28a745 0%, #20c997 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: bold;
      transition: width 0.3s ease;
    }
    .alert {
      padding: 15px;
      border-radius: 6px;
      margin: 10px 0;
    }
    .alert.critical {
      background: #f8d7da;
      border-left: 4px solid #dc3545;
      color: #721c24;
    }
    .alert.warning {
      background: #fff3cd;
      border-left: 4px solid #ffc107;
      color: #856404;
    }
    .alert.success {
      background: #d4edda;
      border-left: 4px solid #28a745;
      color: #155724;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin: 15px 0;
    }
    th, td {
      padding: 12px;
      text-align: left;
      border-bottom: 1px solid #dee2e6;
    }
    th {
      background: #f8f9fa;
      font-weight: 600;
    }
    .footer {
      text-align: center;
      color: #666;
      margin-top: 40px;
      padding: 20px;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>🏥 HDIM UI Implementation Validation Report</h1>
    <p><strong>Generated:</strong> $(date)</p>
    <p><strong>Frontend:</strong> Clinical Portal</p>
  </div>

  <div class="card">
    <h2>📊 Codebase Statistics</h2>
    <div class="stats">
      <div class="stat-box">
        <div class="label">Components</div>
        <div class="value">$TOTAL_COMPONENTS</div>
      </div>
      <div class="stat-box">
        <div class="label">Services</div>
        <div class="value">$TOTAL_SERVICES</div>
      </div>
      <div class="stat-box">
        <div class="label">Modules</div>
        <div class="value">$TOTAL_MODULES</div>
      </div>
      <div class="stat-box">
        <div class="label">Pages</div>
        <div class="value">$((TOTAL_PAGES - 1))</div>
      </div>
    </div>
  </div>

  <div class="card">
    <h2>✨ Feature Coverage</h2>
    <div class="progress-bar">
      <div class="progress-fill" style="width: ${FEATURE_COVERAGE}%;">
        ${FEATURE_COVERAGE}%
      </div>
    </div>
    <ul class="feature-list">
EOF

  for feature in "${!FEATURES[@]}"; do
    status="${FEATURES[$feature]}"
    if [ "$status" == "✓" ]; then
      echo "      <li class=\"pass\"><span class=\"icon pass\">✓</span>$feature</li>" >> "$HTML_FILE"
    else
      echo "      <li class=\"fail\"><span class=\"icon fail\">✗</span>$feature</li>" >> "$HTML_FILE"
    fi
  done

  cat >> "$HTML_FILE" <<EOF
    </ul>
  </div>

  <div class="card">
    <h2>🔒 HIPAA Compliance</h2>
EOF

  if [ "${HIPAA_CHECKS["localStorage PHI"]}" -gt 0 ]; then
    echo "    <div class=\"alert critical\"><strong>⚠ CRITICAL:</strong> PHI detected in localStorage (${HIPAA_CHECKS["localStorage PHI"]} instances). This violates HIPAA encryption requirements.</div>" >> "$HTML_FILE"
  fi

  if [ "${HIPAA_CHECKS["HTTPS Only"]}" -gt 0 ]; then
    echo "    <div class=\"alert critical\"><strong>⚠ CRITICAL:</strong> Insecure HTTP endpoints found (${HIPAA_CHECKS["HTTPS Only"]} instances). All PHI must be transmitted over HTTPS.</div>" >> "$HTML_FILE"
  fi

  cat >> "$HTML_FILE" <<EOF
    <table>
      <tr>
        <th>Check</th>
        <th>Result</th>
        <th>Status</th>
      </tr>
      <tr>
        <td>Auth Guards</td>
        <td>${HIPAA_CHECKS["Auth Guard"]} found</td>
        <td>$([ "${HIPAA_CHECKS["Auth Guard"]}" -gt 0 ] && echo "✓ Pass" || echo "✗ Fail")</td>
      </tr>
      <tr>
        <td>Auth Service</td>
        <td>$([ "${HIPAA_CHECKS["Auth Service"]}" == "1" ] && echo "Present" || echo "Missing")</td>
        <td>$([ "${HIPAA_CHECKS["Auth Service"]}" == "1" ] && echo "✓ Pass" || echo "✗ Fail")</td>
      </tr>
      <tr>
        <td>Audit Logging</td>
        <td>${HIPAA_CHECKS["Audit Logging"]} references</td>
        <td>$([ "${HIPAA_CHECKS["Audit Logging"]}" -gt 0 ] && echo "✓ Pass" || echo "⚠ Review")</td>
      </tr>
      <tr>
        <td>HTTPS Only</td>
        <td>${HIPAA_CHECKS["HTTPS Only"]} insecure instances</td>
        <td>$([ "${HIPAA_CHECKS["HTTPS Only"]}" == "0" ] && echo "✓ Pass" || echo "✗ Fail")</td>
      </tr>
      <tr>
        <td>PHI in localStorage</td>
        <td>${HIPAA_CHECKS["localStorage PHI"]} instances</td>
        <td>$([ "${HIPAA_CHECKS["localStorage PHI"]}" == "0" ] && echo "✓ Pass" || echo "✗ CRITICAL")</td>
      </tr>
    </table>
  </div>

  <div class="card">
    <h2>⚡ Best Practices</h2>
    <table>
      <tr>
        <th>Category</th>
        <th>Metric</th>
        <th>Count</th>
      </tr>
      <tr>
        <td>Responsive Design</td>
        <td>CSS media queries / flex layouts</td>
        <td>$RESPONSIVE_COUNT</td>
      </tr>
      <tr>
        <td>Accessibility</td>
        <td>ARIA labels</td>
        <td>$ARIA_COUNT</td>
      </tr>
      <tr>
        <td>Accessibility</td>
        <td>Semantic HTML5 elements</td>
        <td>$SEMANTIC_HTML</td>
      </tr>
      <tr>
        <td>Error Handling</td>
        <td>catchError / ErrorHandler</td>
        <td>$ERROR_HANDLING</td>
      </tr>
      <tr>
        <td>UX</td>
        <td>Loading states</td>
        <td>$LOADING_STATES</td>
      </tr>
      <tr>
        <td>Performance</td>
        <td>Lazy-loaded routes</td>
        <td>$LAZY_LOADING</td>
      </tr>
      <tr>
        <td>Performance</td>
        <td>OnPush components</td>
        <td>$ON_PUSH</td>
      </tr>
    </table>
  </div>

  <div class="footer">
    <p>Generated by HDIM UI Validation System</p>
    <p>For questions, see <code>scripts/generate-ui-validation-report.sh</code></p>
  </div>
</body>
</html>
EOF

  echo -e "${GREEN}✓ HTML report generated: $HTML_FILE${NC}"
fi

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Validation Complete!${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo "Reports:"
echo "  - Text: $REPORT_FILE"
if [[ " $* " =~ " --html " ]]; then
  echo "  - HTML: $HTML_FILE"
  echo ""
  echo "Open HTML report:"
  echo "  xdg-open $HTML_FILE  # Linux"
  echo "  open $HTML_FILE      # macOS"
fi
echo ""
