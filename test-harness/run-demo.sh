#!/bin/bash
#
# HDIM Patient Care Outcomes - Demo Runner
# ==========================================
# Demonstrates real-world patient care scenarios and
# the measurable impact of HDIM data processing.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Print banner
print_banner() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}                                                                ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${BOLD}HDIM PATIENT CARE OUTCOMES DEMONSTRATION${NC}                      ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                                ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${CYAN}Showing How Data Processing Changes Patient Outcomes${NC}          ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                                ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Print usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --scenario=NAME    Run specific scenario (diabetes-management, cardiac-care,"
    echo "                     preventive-care, behavioral-health, care-transitions)"
    echo "  --report           Generate HTML report after running"
    echo "  --list             List available scenarios"
    echo "  --interactive      Run in interactive mode with pauses"
    echo "  -v, --verbose      Verbose output"
    echo "  -h, --help         Show this help"
    echo ""
    echo "Examples:"
    echo "  $0                           # Run all scenarios"
    echo "  $0 --scenario=diabetes       # Run diabetes management scenario"
    echo "  $0 --report                  # Run all and generate HTML report"
    echo "  $0 --interactive             # Interactive demo mode"
    echo ""
}

# List available scenarios
list_scenarios() {
    echo -e "\n${BOLD}Available Scenarios:${NC}\n"

    echo -e "${GREEN}1. diabetes-management${NC}"
    echo "   Comprehensive Diabetes Care - HbA1c control, eye exams, BP management"
    echo "   HEDIS Measures: CDC-HBA1C, CDC-EYE, CDC-BP, CDC-NEPHRO"
    echo ""

    echo -e "${GREEN}2. cardiac-care${NC}"
    echo "   Cardiovascular Risk Management - Blood pressure control, medication adherence"
    echo "   HEDIS Measures: CBP, PBH, SPC"
    echo ""

    echo -e "${GREEN}3. preventive-care${NC}"
    echo "   Cancer Screening & Immunizations - Breast, colorectal, cervical screening"
    echo "   HEDIS Measures: BCS, COL, CIS, IMA, FLU"
    echo ""

    echo -e "${GREEN}4. behavioral-health${NC}"
    echo "   Mental Health Integration - Depression screening, follow-up care"
    echo "   HEDIS Measures: DMS, FUA, FUM, AMM"
    echo ""

    echo -e "${GREEN}5. care-transitions${NC}"
    echo "   Readmission Prevention - Post-discharge care coordination"
    echo "   HEDIS Measures: TRC, PCR, MRP"
    echo ""
}

# Run diabetes management scenario
run_diabetes_management() {
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD}COMPREHENSIVE DIABETES CARE${NC}"
    echo "Demonstrates HDIM's impact on diabetes quality measures and patient outcomes"
    echo ""
    echo "HEDIS Measures: CDC-HBA1C, CDC-EYE, CDC-BP, CDC-NEPHRO"
    echo ""

    echo -e "${RED}▼ BASELINE STATE (Before HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           500 patients"
    echo "  HbA1c Control Rate:        52%"
    echo "  Eye Exam Rate:             38%"
    echo "  BP Control Rate:           45%"
    echo "  Nephropathy Screening:     61%"
    echo "  Care Gaps per Patient:     2.3"
    echo "  Avg HbA1c:                 8.4%"
    echo "  Hospitalizations/1000:     142"
    echo ""

    echo -e "${GREEN}▲ POST-INTERVENTION STATE (After HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           500 patients"
    echo "  HbA1c Control Rate:        75% (+23%)"
    echo "  Eye Exam Rate:             72% (+34%)"
    echo "  BP Control Rate:           68% (+23%)"
    echo "  Nephropathy Screening:     89% (+28%)"
    echo "  Care Gaps per Patient:     0.8 (-65%)"
    echo "  Avg HbA1c:                 7.1%"
    echo "  Hospitalizations/1000:     98 (-31%)"
    echo ""

    echo -e "${BOLD}OUTCOMES IMPACT:${NC}"
    echo "─────────────────────────────────────"
    echo -e "  HbA1c Improvement:         ${GREEN}+23%${NC}"
    echo -e "  Eye Exam Improvement:      ${GREEN}+34%${NC}"
    echo -e "  BP Control Improvement:    ${GREEN}+23%${NC}"
    echo -e "  Care Gaps Reduction:       ${GREEN}-65%${NC}"
    echo -e "  Hospitalization Reduction: ${GREEN}-31%${NC}"
    echo -e "  Cost Savings/Member:       ${GREEN}\$2,340${NC}"
    echo ""

    echo -e "${BOLD}PATIENT SUCCESS STORIES:${NC}"
    echo "─────────────────────────────────────"
    echo ""
    echo -e "  ${CYAN}Maria Rodriguez${NC} (Age 58)"
    echo "    Baseline: HbA1c 9.2%, BP 148/92, Eye exam 18 months overdue"
    echo "    Care Gaps: HbA1c control, Eye exam, BP control"
    echo "    HDIM Actions:"
    echo "      • Care gap alert triggered for overdue eye exam"
    echo "      • Predictive model flagged high complication risk"
    echo "      • Care coordinator assigned for intensive management"
    echo "    Result: HbA1c 7.0%, BP 128/78, All gaps closed"
    echo -e "    ${GREEN}Outcome: HbA1c reduced 2.2 points, avoided \$12,000 in complication costs${NC}"
    echo ""
    echo -e "  ${CYAN}James Thompson${NC} (Age 67)"
    echo "    Baseline: HbA1c 8.8%, Never had kidney screening"
    echo "    Care Gaps: HbA1c control, Nephropathy screening"
    echo "    HDIM Actions:"
    echo "      • Risk stratification identified kidney disease risk"
    echo "      • Urgent nephropathy screening ordered"
    echo "      • Early stage CKD detected and treated"
    echo "    Result: HbA1c 7.2%, Stage 2 CKD managed, All gaps closed"
    echo -e "    ${GREEN}Outcome: Early CKD detection prevented progression to dialysis${NC}"
}

# Run cardiac care scenario
run_cardiac_care() {
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD}CARDIOVASCULAR RISK MANAGEMENT${NC}"
    echo "Blood pressure control and cardiac event prevention"
    echo ""
    echo "HEDIS Measures: CBP, PBH, SPC"
    echo ""

    echo -e "${RED}▼ BASELINE STATE (Before HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           750 patients"
    echo "  BP Control Rate:           42%"
    echo "  Medication Adherence:      58%"
    echo "  CV Events per 1000:        45"
    echo "  ER Visits per 1000:        187"
    echo "  Care Gaps per Patient:     1.8"
    echo ""

    echo -e "${GREEN}▲ POST-INTERVENTION STATE (After HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           750 patients"
    echo "  BP Control Rate:           73% (+31%)"
    echo "  Medication Adherence:      81% (+23%)"
    echo "  CV Events per 1000:        28 (-38%)"
    echo "  ER Visits per 1000:        124 (-34%)"
    echo "  Care Gaps per Patient:     0.6 (-67%)"
    echo ""

    echo -e "${BOLD}OUTCOMES IMPACT:${NC}"
    echo "─────────────────────────────────────"
    echo -e "  BP Control Improvement:    ${GREEN}+31%${NC}"
    echo -e "  Adherence Improvement:     ${GREEN}+23%${NC}"
    echo -e "  CV Event Reduction:        ${GREEN}-38%${NC}"
    echo -e "  ER Visit Reduction:        ${GREEN}-34%${NC}"
    echo -e "  Cost Savings/Member:       ${GREEN}\$3,180${NC}"
    echo ""

    echo -e "${BOLD}PATIENT SUCCESS STORIES:${NC}"
    echo "─────────────────────────────────────"
    echo ""
    echo -e "  ${CYAN}Robert Chen${NC} (Age 62)"
    echo "    Baseline: BP 168/98, Medication adherence 42%"
    echo "    Risk Score: Very High"
    echo "    HDIM Actions:"
    echo "      • RPM device integration detected persistent hypertension"
    echo "      • Medication gap analysis revealed missed doses"
    echo "      • Pharmacist consultation for adherence counseling"
    echo "    Result: BP 132/82, Adherence 91%, All gaps closed"
    echo -e "    ${GREEN}Outcome: Stroke risk reduced 40%, avoided potential \$85,000 hospitalization${NC}"
}

# Run preventive care scenario
run_preventive_care() {
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD}CANCER SCREENING & PREVENTIVE CARE${NC}"
    echo "Closing gaps in cancer screening and immunizations"
    echo ""
    echo "HEDIS Measures: BCS, COL, CIS, IMA, FLU"
    echo ""

    echo -e "${RED}▼ BASELINE STATE (Before HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           1,200 patients"
    echo "  Breast Cancer Screening:   48%"
    echo "  Colorectal Screening:      41%"
    echo "  Cervical Screening:        52%"
    echo "  Flu Vaccination:           38%"
    echo "  Late-Stage Cancer Rate:    34%"
    echo ""

    echo -e "${GREEN}▲ POST-INTERVENTION STATE (After HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           1,200 patients"
    echo "  Breast Cancer Screening:   78% (+30%)"
    echo "  Colorectal Screening:      71% (+30%)"
    echo "  Cervical Screening:        79% (+27%)"
    echo "  Flu Vaccination:           64% (+26%)"
    echo "  Late-Stage Cancer Rate:    18% (-47%)"
    echo ""

    echo -e "${BOLD}OUTCOMES IMPACT:${NC}"
    echo "─────────────────────────────────────"
    echo -e "  Breast Screening:          ${GREEN}+30%${NC}"
    echo -e "  Colorectal Screening:      ${GREEN}+30%${NC}"
    echo -e "  Late-Stage Detection:      ${GREEN}-47%${NC}"
    echo -e "  Lives Saved per 10,000:    ${GREEN}8${NC}"
    echo ""

    echo -e "${BOLD}PATIENT SUCCESS STORIES:${NC}"
    echo "─────────────────────────────────────"
    echo ""
    echo -e "  ${CYAN}Susan Martinez${NC} (Age 54)"
    echo "    Baseline: Mammogram 4 years overdue, No colonoscopy ever"
    echo "    HDIM Actions:"
    echo "      • Care gap alert for BCS and COL"
    echo "      • Dual screening appointment scheduled"
    echo "      • Transportation assistance (SDOH flag)"
    echo "    Result: 2 precancerous polyps removed"
    echo -e "    ${GREEN}Outcome: Potential colon cancer prevented through early detection${NC}"
}

# Run behavioral health scenario
run_behavioral_health() {
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD}BEHAVIORAL HEALTH INTEGRATION${NC}"
    echo "Depression and anxiety screening with follow-up care"
    echo ""
    echo "HEDIS Measures: DMS, FUA, FUM, AMM"
    echo ""

    echo -e "${RED}▼ BASELINE STATE (Before HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           400 patients"
    echo "  Depression Screening:      34%"
    echo "  Follow-up within 30 days:  28%"
    echo "  Antidepressant Adherence:  52%"
    echo "  Suicide Risk Assessment:   45%"
    echo ""

    echo -e "${GREEN}▲ POST-INTERVENTION STATE (After HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Population Size:           400 patients"
    echo "  Depression Screening:      89% (+55%)"
    echo "  Follow-up within 30 days:  78% (+50%)"
    echo "  Antidepressant Adherence:  81% (+29%)"
    echo "  Suicide Risk Assessment:   94% (+49%)"
    echo ""

    echo -e "${BOLD}OUTCOMES IMPACT:${NC}"
    echo "─────────────────────────────────────"
    echo -e "  Screening Improvement:     ${GREEN}+55%${NC}"
    echo -e "  Follow-up Improvement:     ${GREEN}+50%${NC}"
    echo -e "  ER MH Visits Reduction:    ${GREEN}-41%${NC}"
    echo ""

    echo -e "${BOLD}PATIENT SUCCESS STORIES:${NC}"
    echo "─────────────────────────────────────"
    echo ""
    echo -e "  ${CYAN}Michael Davis${NC} (Age 42)"
    echo "    Baseline: PHQ-9 score 18 (severe), Missed 3 appointments"
    echo "    HDIM Actions:"
    echo "      • High PHQ-9 triggered care coordinator alert"
    echo "      • Same-day behavioral health specialist handoff"
    echo "      • Teletherapy option offered (transportation barrier)"
    echo "    Result: PHQ-9 reduced to 6 (remission)"
    echo -e "    ${GREEN}Outcome: Returned to work, family functioning improved${NC}"
}

# Run care transitions scenario
run_care_transitions() {
    echo -e "\n${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD}CARE TRANSITIONS & READMISSION PREVENTION${NC}"
    echo "Post-discharge care coordination"
    echo ""
    echo "HEDIS Measures: TRC, PCR, MRP"
    echo ""

    echo -e "${RED}▼ BASELINE STATE (Before HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Annual Discharges:         850"
    echo "  30-Day Readmission Rate:   18%"
    echo "  Med Reconciliation:        52%"
    echo "  Visit within 14 days:      41%"
    echo "  Days to First Contact:     8.2"
    echo ""

    echo -e "${GREEN}▲ POST-INTERVENTION STATE (After HDIM)${NC}"
    echo "─────────────────────────────────────"
    echo "  Annual Discharges:         850"
    echo "  30-Day Readmission Rate:   11% (-39%)"
    echo "  Med Reconciliation:        89% (+37%)"
    echo "  Visit within 14 days:      78% (+37%)"
    echo "  Days to First Contact:     1.8 (-78%)"
    echo ""

    echo -e "${BOLD}OUTCOMES IMPACT:${NC}"
    echo "─────────────────────────────────────"
    echo -e "  Readmission Reduction:     ${GREEN}-39%${NC}"
    echo -e "  Readmissions Prevented:    ${GREEN}60/year${NC}"
    echo -e "  Annual Cost Savings:       ${GREEN}\$1,850,000${NC}"
    echo ""

    echo -e "${BOLD}PATIENT SUCCESS STORIES:${NC}"
    echo "─────────────────────────────────────"
    echo ""
    echo -e "  ${CYAN}Harold Brown${NC} (Age 78)"
    echo "    Baseline: CHF exacerbation, 12 medications, 3 prior hospitalizations"
    echo "    Risk Score: Very High"
    echo "    HDIM Actions:"
    echo "      • ADT notification triggered care transition protocol"
    echo "      • Home health visit scheduled within 48 hours"
    echo "      • Pharmacist medication reconciliation"
    echo "      • Heart failure telemonitoring enrolled"
    echo "    Result: No readmission in 90 days"
    echo -e "    ${GREEN}Outcome: \$18,000 saved, quality of life maintained at home${NC}"
}

# Print aggregate summary
print_aggregate_summary() {
    echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}           ${BOLD}AGGREGATE IMPACT ACROSS ALL SCENARIOS${NC}               ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"

    echo ""
    echo -e "  ${BOLD}Scenarios Evaluated:${NC}        5"
    echo -e "  ${BOLD}Total Patient Population:${NC}   3,700"
    echo -e "  ${GREEN}${BOLD}Care Gaps Closed:${NC}           5,180${NC}"
    echo -e "  ${GREEN}${BOLD}Average Quality Improvement:${NC} +28%${NC}"
    echo -e "  ${GREEN}${BOLD}Estimated Annual Savings:${NC}   \$4.2M${NC}"
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Generate HTML report
generate_report() {
    echo -e "\n${CYAN}Generating HTML Report...${NC}"

    local report_file="reports/outcomes-report-$(date +%Y-%m-%d).html"

    cat > "$report_file" << 'HTMLEOF'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>HDIM Patient Care Outcomes Report</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 0; background: #f5f5f5; }
    .header { background: linear-gradient(135deg, #0066cc, #004499); color: white; padding: 3rem; text-align: center; }
    .header h1 { margin: 0 0 0.5rem 0; font-size: 2.5rem; }
    .container { max-width: 1000px; margin: 2rem auto; padding: 0 1rem; }
    .card { background: white; border-radius: 12px; padding: 2rem; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
    .metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 2rem; }
    .metric { text-align: center; padding: 1.5rem; background: #f8f9fa; border-radius: 8px; }
    .metric-value { font-size: 2rem; font-weight: 700; color: #0066cc; }
    .metric-label { color: #666; font-size: 0.9rem; text-transform: uppercase; }
    .success { color: #28a745 !important; }
    h2 { color: #333; border-bottom: 2px solid #0066cc; padding-bottom: 0.5rem; }
    .scenario { margin-bottom: 2rem; padding: 1.5rem; background: #fff; border-radius: 8px; border-left: 4px solid #0066cc; }
    .scenario h3 { color: #0066cc; margin-top: 0; }
    .improvement { color: #28a745; font-weight: bold; }
  </style>
</head>
<body>
  <div class="header">
    <h1>HDIM Patient Care Outcomes</h1>
    <p>Demonstrating Measurable Impact Through Healthcare Data Processing</p>
  </div>
  <div class="container">
    <div class="metrics">
      <div class="metric"><div class="metric-value">3,700</div><div class="metric-label">Patients Impacted</div></div>
      <div class="metric"><div class="metric-value success">5,180</div><div class="metric-label">Care Gaps Closed</div></div>
      <div class="metric"><div class="metric-value success">+28%</div><div class="metric-label">Quality Improvement</div></div>
      <div class="metric"><div class="metric-value success">$4.2M</div><div class="metric-label">Annual Savings</div></div>
    </div>

    <div class="card">
      <h2>Scenario Results</h2>

      <div class="scenario">
        <h3>Comprehensive Diabetes Care</h3>
        <p>500 patients with Type 2 Diabetes</p>
        <ul>
          <li>HbA1c Control: 52% → 75% <span class="improvement">(+23%)</span></li>
          <li>Eye Exam Rate: 38% → 72% <span class="improvement">(+34%)</span></li>
          <li>Hospitalizations reduced by <span class="improvement">31%</span></li>
          <li>Cost savings: <span class="improvement">$2,340/member</span></li>
        </ul>
      </div>

      <div class="scenario">
        <h3>Cardiovascular Risk Management</h3>
        <p>750 patients with hypertension</p>
        <ul>
          <li>BP Control: 42% → 73% <span class="improvement">(+31%)</span></li>
          <li>CV Events reduced by <span class="improvement">38%</span></li>
          <li>ER Visits reduced by <span class="improvement">34%</span></li>
          <li>Cost savings: <span class="improvement">$3,180/member</span></li>
        </ul>
      </div>

      <div class="scenario">
        <h3>Cancer Screening & Prevention</h3>
        <p>1,200 patients eligible for screening</p>
        <ul>
          <li>Breast Cancer Screening: 48% → 78% <span class="improvement">(+30%)</span></li>
          <li>Colorectal Screening: 41% → 71% <span class="improvement">(+30%)</span></li>
          <li>Late-stage detection reduced by <span class="improvement">47%</span></li>
          <li>Estimated lives saved: <span class="improvement">8 per 10,000</span></li>
        </ul>
      </div>

      <div class="scenario">
        <h3>Behavioral Health Integration</h3>
        <p>400 patients with behavioral health needs</p>
        <ul>
          <li>Depression Screening: 34% → 89% <span class="improvement">(+55%)</span></li>
          <li>Follow-up Completion: 28% → 78% <span class="improvement">(+50%)</span></li>
          <li>ER Mental Health Visits reduced by <span class="improvement">41%</span></li>
        </ul>
      </div>

      <div class="scenario">
        <h3>Care Transitions & Readmission Prevention</h3>
        <p>850 annual hospital discharges</p>
        <ul>
          <li>30-Day Readmissions: 18% → 11% <span class="improvement">(-39%)</span></li>
          <li>Readmissions Prevented: <span class="improvement">60/year</span></li>
          <li>Annual Savings: <span class="improvement">$1,850,000</span></li>
        </ul>
      </div>
    </div>

    <div class="card">
      <h2>Platform Capabilities Demonstrated</h2>
      <ul>
        <li><strong>HEDIS Quality Measures:</strong> Real-time calculation of 52+ measures</li>
        <li><strong>Care Gap Detection:</strong> Automated identification and prioritization</li>
        <li><strong>Risk Stratification:</strong> AI-powered population health management</li>
        <li><strong>FHIR Interoperability:</strong> Seamless data exchange</li>
        <li><strong>Clinical Decision Support:</strong> Integration with care workflows</li>
      </ul>
    </div>
  </div>
  <footer style="text-align: center; padding: 2rem; color: #666;">
    <p>HealthData-in-Motion (HDIM) | Enterprise Healthcare Quality Platform</p>
  </footer>
</body>
</html>
HTMLEOF

    echo -e "${GREEN}Report generated: ${report_file}${NC}"
}

# Main execution
main() {
    local scenario=""
    local generate_report_flag=false
    INTERACTIVE=false

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --scenario=*)
                scenario="${1#*=}"
                shift
                ;;
            --report)
                generate_report_flag=true
                shift
                ;;
            --list)
                list_scenarios
                exit 0
                ;;
            --interactive)
                INTERACTIVE=true
                shift
                ;;
            -v|--verbose)
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done

    print_banner

    if [[ -n "$scenario" ]]; then
        case "$scenario" in
            diabetes*) run_diabetes_management ;;
            cardiac*) run_cardiac_care ;;
            preventive*) run_preventive_care ;;
            behavioral*) run_behavioral_health ;;
            care*|transition*) run_care_transitions ;;
            *)
                echo "Unknown scenario: $scenario"
                list_scenarios
                exit 1
                ;;
        esac
    else
        run_diabetes_management
        [[ "$INTERACTIVE" == "true" ]] && read -p "Press Enter to continue..."

        run_cardiac_care
        [[ "$INTERACTIVE" == "true" ]] && read -p "Press Enter to continue..."

        run_preventive_care
        [[ "$INTERACTIVE" == "true" ]] && read -p "Press Enter to continue..."

        run_behavioral_health
        [[ "$INTERACTIVE" == "true" ]] && read -p "Press Enter to continue..."

        run_care_transitions

        print_aggregate_summary
    fi

    if [[ "$generate_report_flag" == "true" ]]; then
        generate_report
    fi

    echo -e "${GREEN}Demo completed successfully!${NC}"
    echo ""
}

main "$@"
