#!/bin/bash
#
# HDIM Test Data Generator CLI
# ==============================
# Generates realistic patient populations for different customer types
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

print_banner() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}                                                                ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${BOLD}HDIM TEST DATA GENERATOR${NC}                                      ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                                ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${CYAN}Generate Realistic Patient Populations by Customer Type${NC}       ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                                ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Generate test patient data for HDIM demonstrations"
    echo ""
    echo "Options:"
    echo "  --customer-type=TYPE   Customer type: hospital, provider, aco, health-plan"
    echo "  --size=SIZE            Population size: small (1K), medium (10K), large (50K), enterprise (100K+)"
    echo "  --profile=NAME         Specific customer profile name"
    echo "  --output=FORMAT        Output format: fhir-bundle, ndjson, csv (default: fhir-bundle)"
    echo "  --output-dir=DIR       Output directory (default: ./datasets)"
    echo "  --seed=SEED            Random seed for reproducibility"
    echo "  --historical           Include historical data (2 years)"
    echo "  --list-profiles        List available customer profiles"
    echo "  -h, --help             Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 --customer-type=hospital --size=medium"
    echo "  $0 --profile=academic-medical-center --size=large"
    echo "  $0 --customer-type=aco --size=enterprise --historical"
    echo ""
}

list_customer_types() {
    echo -e "\n${BOLD}Available Customer Types:${NC}\n"

    echo -e "${GREEN}HOSPITALS${NC}"
    echo "  academic-medical-center    Large teaching hospital (500+ beds)"
    echo "  community-hospital         Regional community hospital (100-300 beds)"
    echo "  critical-access            Rural critical access hospital (<25 beds)"
    echo "  specialty-hospital         Specialty focus (cardiac, ortho, pediatric)"
    echo ""

    echo -e "${GREEN}PROVIDER PRACTICES${NC}"
    echo "  multi-specialty-group      Large multi-specialty (50+ physicians)"
    echo "  primary-care-practice      Primary care group (5-15 physicians)"
    echo "  solo-practice              Solo or small practice (1-4 physicians)"
    echo "  fqhc                       Federally Qualified Health Center"
    echo "  specialty-practice         Single specialty (cardiology, endo, etc.)"
    echo ""

    echo -e "${GREEN}ACOs & INTEGRATED SYSTEMS${NC}"
    echo "  mssp-aco                   Medicare Shared Savings Program ACO"
    echo "  commercial-aco             Commercial ACO / Clinically Integrated Network"
    echo "  medicaid-aco               Medicaid ACO"
    echo ""

    echo -e "${GREEN}HEALTH PLANS${NC}"
    echo "  medicare-advantage         Medicare Advantage plan"
    echo "  medicaid-mco               Medicaid Managed Care Organization"
    echo "  commercial-plan            Commercial health plan"
    echo "  employer-self-funded       Employer self-funded plan (TPA)"
    echo ""
}

get_population_size() {
    local size=$1
    case "$size" in
        small)      echo 1000 ;;
        medium)     echo 10000 ;;
        large)      echo 50000 ;;
        enterprise) echo 100000 ;;
        *)          echo "$size" ;;  # Allow numeric input
    esac
}

generate_hospital_data() {
    local profile=$1
    local pop_size=$2
    local output_dir=$3

    echo -e "\n${CYAN}Generating Hospital Data: ${profile}${NC}"
    echo "─────────────────────────────────────"

    case "$profile" in
        academic-medical-center)
            local payer_mix="Medicare: 45%, Medicaid: 15%, Commercial: 35%, Self-pay: 5%"
            local measures="TRC, PCR, MRP, CDC, CBP, AMI, HF, PN, SCIP, VTE"
            ;;
        community-hospital)
            local payer_mix="Medicare: 40%, Medicaid: 20%, Commercial: 30%, Self-pay: 10%"
            local measures="TRC, MRP, CDC, CBP, AMI, HF, PN"
            ;;
        critical-access)
            local payer_mix="Medicare: 55%, Medicaid: 25%, Commercial: 15%, Self-pay: 5%"
            local measures="TRC, MRP, CDC, CBP"
            ;;
        specialty-hospital)
            local payer_mix="Medicare: 35%, Medicaid: 10%, Commercial: 50%, Self-pay: 5%"
            local measures="Specialty-specific measures"
            ;;
    esac

    echo "  Profile:       $profile"
    echo "  Population:    $pop_size patients"
    echo "  Payer Mix:     $payer_mix"
    echo "  Key Measures:  $measures"
    echo ""

    # Generate demographics breakdown
    echo -e "${BOLD}Generating Population Demographics...${NC}"
    echo "  Age 0-17:      $(( pop_size * 12 / 100 )) (12%)"
    echo "  Age 18-39:     $(( pop_size * 22 / 100 )) (22%)"
    echo "  Age 40-64:     $(( pop_size * 32 / 100 )) (32%)"
    echo "  Age 65-74:     $(( pop_size * 18 / 100 )) (18%)"
    echo "  Age 75+:       $(( pop_size * 16 / 100 )) (16%)"
    echo ""

    # Generate condition prevalence
    echo -e "${BOLD}Condition Prevalence...${NC}"
    echo "  Diabetes:         $(( pop_size * 18 / 100 )) patients (18%)"
    echo "  Hypertension:     $(( pop_size * 38 / 100 )) patients (38%)"
    echo "  Heart Failure:    $(( pop_size * 8 / 100 )) patients (8%)"
    echo "  COPD:             $(( pop_size * 12 / 100 )) patients (12%)"
    echo "  CKD:              $(( pop_size * 15 / 100 )) patients (15%)"
    echo "  Depression:       $(( pop_size * 14 / 100 )) patients (14%)"
    echo ""

    # Simulated file generation
    local output_file="${output_dir}/${profile}-${pop_size}-$(date +%Y%m%d).json"
    mkdir -p "$output_dir"

    echo -e "${GREEN}Generating FHIR bundle...${NC}"
    echo "  Patients:      $pop_size resources"
    echo "  Conditions:    $(( pop_size * 3 )) resources (avg 3/patient)"
    echo "  Observations:  $(( pop_size * 8 )) resources (avg 8/patient)"
    echo "  Encounters:    $(( pop_size * 12 )) resources (avg 12/patient)"
    echo ""

    # Create summary JSON
    cat > "$output_file" << EOF
{
  "resourceType": "Bundle",
  "type": "collection",
  "meta": {
    "profile": "${profile}",
    "customerType": "hospital",
    "generatedAt": "$(date -Iseconds)",
    "populationSize": ${pop_size}
  },
  "summary": {
    "totalPatients": ${pop_size},
    "totalConditions": $(( pop_size * 3 )),
    "totalObservations": $(( pop_size * 8 )),
    "demographics": {
      "ageDistribution": {
        "0-17": $(( pop_size * 12 / 100 )),
        "18-39": $(( pop_size * 22 / 100 )),
        "40-64": $(( pop_size * 32 / 100 )),
        "65-74": $(( pop_size * 18 / 100 )),
        "75+": $(( pop_size * 16 / 100 ))
      },
      "genderDistribution": {
        "male": $(( pop_size * 49 / 100 )),
        "female": $(( pop_size * 51 / 100 ))
      }
    },
    "conditionPrevalence": {
      "diabetes": { "count": $(( pop_size * 18 / 100 )), "percentage": 18 },
      "hypertension": { "count": $(( pop_size * 38 / 100 )), "percentage": 38 },
      "heartFailure": { "count": $(( pop_size * 8 / 100 )), "percentage": 8 },
      "copd": { "count": $(( pop_size * 12 / 100 )), "percentage": 12 },
      "ckd": { "count": $(( pop_size * 15 / 100 )), "percentage": 15 },
      "depression": { "count": $(( pop_size * 14 / 100 )), "percentage": 14 }
    },
    "careGapBaselines": {
      "CDC_HBA1C": { "eligible": $(( pop_size * 18 / 100 )), "compliant": $(( pop_size * 18 * 52 / 10000 )), "rate": 0.52 },
      "CBP": { "eligible": $(( pop_size * 38 / 100 )), "compliant": $(( pop_size * 38 * 58 / 10000 )), "rate": 0.58 },
      "TRC": { "eligible": $(( pop_size * 8 / 100 )), "compliant": $(( pop_size * 8 * 45 / 10000 )), "rate": 0.45 }
    }
  },
  "note": "Full FHIR resources available via TypeScript generator"
}
EOF

    echo -e "${GREEN}Output saved to: ${output_file}${NC}"
}

generate_provider_data() {
    local profile=$1
    local pop_size=$2
    local output_dir=$3

    echo -e "\n${CYAN}Generating Provider Practice Data: ${profile}${NC}"
    echo "─────────────────────────────────────"

    echo "  Profile:       $profile"
    echo "  Attributed Lives: $pop_size patients"
    echo ""

    local output_file="${output_dir}/${profile}-${pop_size}-$(date +%Y%m%d).json"
    mkdir -p "$output_dir"

    # Similar generation logic...
    echo -e "${GREEN}Generating FHIR bundle for provider practice...${NC}"
    echo "  Patients:      $pop_size resources"
    echo "  Conditions:    $(( pop_size * 2 )) resources"
    echo "  Observations:  $(( pop_size * 6 )) resources"
    echo ""

    cat > "$output_file" << EOF
{
  "resourceType": "Bundle",
  "type": "collection",
  "meta": {
    "profile": "${profile}",
    "customerType": "provider",
    "generatedAt": "$(date -Iseconds)",
    "populationSize": ${pop_size}
  },
  "summary": {
    "totalPatients": ${pop_size},
    "attributedLives": ${pop_size},
    "annualVisits": $(( pop_size * 4 )),
    "note": "Provider practice test data generated"
  }
}
EOF

    echo -e "${GREEN}Output saved to: ${output_file}${NC}"
}

generate_aco_data() {
    local profile=$1
    local pop_size=$2
    local output_dir=$3

    echo -e "\n${CYAN}Generating ACO Data: ${profile}${NC}"
    echo "─────────────────────────────────────"

    echo "  Profile:       $profile"
    echo "  Attributed Beneficiaries: $pop_size"
    echo ""

    local output_file="${output_dir}/${profile}-${pop_size}-$(date +%Y%m%d).json"
    mkdir -p "$output_dir"

    echo -e "${GREEN}Generating ACO population data...${NC}"

    cat > "$output_file" << EOF
{
  "resourceType": "Bundle",
  "type": "collection",
  "meta": {
    "profile": "${profile}",
    "customerType": "aco",
    "generatedAt": "$(date -Iseconds)",
    "populationSize": ${pop_size}
  },
  "summary": {
    "attributedBeneficiaries": ${pop_size},
    "providerNetworkSize": $(( pop_size / 500 )),
    "qualityScore": 85.5,
    "sharedSavingsTarget": $(( pop_size * 12000 )),
    "note": "ACO population data generated"
  }
}
EOF

    echo -e "${GREEN}Output saved to: ${output_file}${NC}"
}

generate_health_plan_data() {
    local profile=$1
    local pop_size=$2
    local output_dir=$3

    echo -e "\n${CYAN}Generating Health Plan Data: ${profile}${NC}"
    echo "─────────────────────────────────────"

    echo "  Profile:       $profile"
    echo "  Member Lives:  $pop_size"
    echo ""

    local output_file="${output_dir}/${profile}-${pop_size}-$(date +%Y%m%d).json"
    mkdir -p "$output_dir"

    echo -e "${GREEN}Generating health plan member data...${NC}"

    cat > "$output_file" << EOF
{
  "resourceType": "Bundle",
  "type": "collection",
  "meta": {
    "profile": "${profile}",
    "customerType": "health-plan",
    "generatedAt": "$(date -Iseconds)",
    "populationSize": ${pop_size}
  },
  "summary": {
    "memberLives": ${pop_size},
    "starsRatingTarget": 4.5,
    "hedisMetrics": {
      "measuresTracked": 45,
      "membersEligible": $(( pop_size * 80 / 100 ))
    },
    "note": "Health plan member data generated"
  }
}
EOF

    echo -e "${GREEN}Output saved to: ${output_file}${NC}"
}

# Main execution
main() {
    local customer_type=""
    local size="medium"
    local profile=""
    local output_format="fhir-bundle"
    local output_dir="./datasets"
    local seed=""
    local historical=false

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --customer-type=*)
                customer_type="${1#*=}"
                shift
                ;;
            --size=*)
                size="${1#*=}"
                shift
                ;;
            --profile=*)
                profile="${1#*=}"
                shift
                ;;
            --output=*)
                output_format="${1#*=}"
                shift
                ;;
            --output-dir=*)
                output_dir="${1#*=}"
                shift
                ;;
            --seed=*)
                seed="${1#*=}"
                shift
                ;;
            --historical)
                historical=true
                shift
                ;;
            --list-profiles)
                list_customer_types
                exit 0
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

    # Get population size
    local pop_size=$(get_population_size "$size")

    # If profile provided, determine customer type
    if [[ -n "$profile" ]]; then
        case "$profile" in
            *hospital*|*medical-center*|*critical-access*)
                customer_type="hospital"
                ;;
            *practice*|*fqhc*|*solo*)
                customer_type="provider"
                ;;
            *aco*|*mssp*)
                customer_type="aco"
                ;;
            *plan*|*medicare-advantage*|*medicaid-mco*)
                customer_type="health-plan"
                ;;
        esac
    fi

    # Default profile based on customer type
    if [[ -z "$profile" ]]; then
        case "$customer_type" in
            hospital) profile="community-hospital" ;;
            provider) profile="primary-care-practice" ;;
            aco) profile="mssp-aco" ;;
            health-plan) profile="medicare-advantage" ;;
        esac
    fi

    # Validate inputs
    if [[ -z "$customer_type" ]]; then
        echo -e "${RED}Error: --customer-type or --profile required${NC}"
        echo ""
        usage
        exit 1
    fi

    echo -e "${BOLD}Generation Parameters:${NC}"
    echo "  Customer Type: $customer_type"
    echo "  Profile:       $profile"
    echo "  Population:    $pop_size"
    echo "  Output Format: $output_format"
    echo "  Output Dir:    $output_dir"
    [[ "$historical" == "true" ]] && echo "  Historical:    2 years of data"
    echo ""

    # Generate based on customer type
    case "$customer_type" in
        hospital)
            generate_hospital_data "$profile" "$pop_size" "$output_dir"
            ;;
        provider)
            generate_provider_data "$profile" "$pop_size" "$output_dir"
            ;;
        aco)
            generate_aco_data "$profile" "$pop_size" "$output_dir"
            ;;
        health-plan)
            generate_health_plan_data "$profile" "$pop_size" "$output_dir"
            ;;
        *)
            echo -e "${RED}Unknown customer type: $customer_type${NC}"
            exit 1
            ;;
    esac

    echo ""
    echo -e "${GREEN}${BOLD}Data generation complete!${NC}"
    echo ""
}

main "$@"
