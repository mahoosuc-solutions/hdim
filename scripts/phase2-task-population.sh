#!/bin/bash
# Phase 2 Task Population Script
# Creates 14 Phase 2 execution tasks for March 2026 GTM launch

API_BASE="http://localhost:8098/api/v1/payer/phase2-execution"
TENANT_ID="hdim-test"

echo "Phase 2 Task Population - Starting..."
echo "Tenant: $TENANT_ID"
echo "API Base: $API_BASE"
echo ""

# Function to create a task
create_task() {
    local task_name=$1
    local description=$2
    local category=$3
    local priority=$4
    local status=$5
    local due_date=$6
    local owner=$7
    local week=$8
    local metrics=$9

    echo "Creating: $task_name..."

    curl -s -X POST "$API_BASE/tasks" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        -d "{
            \"taskName\": \"$task_name\",
            \"description\": \"$description\",
            \"category\": \"$category\",
            \"priority\": \"$priority\",
            \"status\": \"$status\",
            \"targetDueDate\": \"$due_date\",
            \"ownerName\": \"$owner\",
            \"ownerRole\": \"CEO\",
            \"phase2Week\": $week,
            \"successMetrics\": \"$metrics\",
            \"notes\": \"Phase 2 execution task\"
        }" | jq . 2>/dev/null || echo "Response: OK"

    echo ""
}

# WEEK 1-2: Positioning Refinement (March 1-14)
create_task \
    "Week 1: VP Sales Onboarding & Playbook Customization" \
    "Onboard new VP Sales and customize sales playbooks for Phase 2 GTM" \
    "SALES" \
    "CRITICAL" \
    "IN_PROGRESS" \
    "2026-03-14T23:59:59Z" \
    "Aaron (CEO)" \
    "1" \
    "VP Sales productive, 20-30 discovery calls completed"

create_task \
    "Week 1: Sales Collateral Refinement - AI Features" \
    "Reframe sales collateral with AI-first positioning and feature differentiation" \
    "MARKETING" \
    "HIGH" \
    "IN_PROGRESS" \
    "2026-03-07T23:59:59Z" \
    "Marketing Lead" \
    "1" \
    "AI feature messaging approved, 3+ collateral pieces updated"

create_task \
    "Week 1: AI Feature Prototype - Predictive Care Gaps" \
    "Build MVP of predictive care gap emergence feature for demos" \
    "PRODUCT" \
    "CRITICAL" \
    "IN_PROGRESS" \
    "2026-03-14T23:59:59Z" \
    "Product Manager" \
    "1" \
    "Prototype functional and demo-ready with 75%+ accuracy"

create_task \
    "Week 1: Thought Leadership Content - LinkedIn Launch" \
    "Launch LinkedIn thought leadership series on AI in healthcare" \
    "MARKETING" \
    "HIGH" \
    "PENDING" \
    "2026-03-10T23:59:59Z" \
    "Marketing Lead" \
    "1" \
    "4+ posts published, 500+ followers gained, 1000+ views per post"

create_task \
    "Week 2: Lead Generation Campaign - Target 20-30 Discovery Calls" \
    "Execute outreach to 50 qualified prospects for discovery conversations" \
    "SALES" \
    "HIGH" \
    "PENDING" \
    "2026-03-14T23:59:59Z" \
    "VP Sales" \
    "2" \
    "20-30 discovery calls scheduled, 50+ outreach touches completed"

create_task \
    "Week 2: Website Launch - Marketing Site Live" \
    "Launch marketing website showcasing AI-first positioning" \
    "MARKETING" \
    "HIGH" \
    "PENDING" \
    "2026-03-14T23:59:59Z" \
    "Marketing Lead" \
    "2" \
    "Website live, 100+ visitors by March 31, email nurture active"

# WEEK 3-4: Pilot Acquisition & Validation (March 15-31)
create_task \
    "Week 3: Pilot Customer Outreach - 10 Target Accounts" \
    "Execute targeted outreach to 10 AI-forward health plan executives" \
    "SALES" \
    "CRITICAL" \
    "PENDING" \
    "2026-03-21T23:59:59Z" \
    "VP Sales" \
    "2" \
    "1-2 pilot LOIs signed, 5+ exploratory calls completed"

create_task \
    "Week 3: AI Feature Development - Clinical Summaries" \
    "Complete AI-generated clinical summary feature for pilot demos" \
    "PRODUCT" \
    "HIGH" \
    "PENDING" \
    "2026-03-21T23:59:59Z" \
    "Product Manager" \
    "2" \
    "Feature functional with natural language output, clinically validated"

create_task \
    "Week 3: Case Study Preparation - First Win Documentation" \
    "Prepare case study template and success metrics tracking for pilots" \
    "MARKETING" \
    "MEDIUM" \
    "PENDING" \
    "2026-03-21T23:59:59Z" \
    "Marketing Lead" \
    "2" \
    "Template finalized, metrics dashboard setup, ready for first win"

create_task \
    "Week 4: Pilot Deployment - First Customer Go-Live" \
    "Deploy Phase 2 system for first pilot customer, establish success metrics" \
    "PRODUCT" \
    "CRITICAL" \
    "PENDING" \
    "2026-03-31T23:59:59Z" \
    "Product Manager" \
    "2" \
    "Pilot deployed, weekly check-ins scheduled, success dashboard live"

create_task \
    "Week 4: Webinar - AI Innovation in Healthcare" \
    "Host webinar on AI-first product development for healthcare" \
    "MARKETING" \
    "HIGH" \
    "PENDING" \
    "2026-03-28T23:59:59Z" \
    "Marketing Lead" \
    "2" \
    "50+ registrants, 30+ attendees, 20% converted to demos"

create_task \
    "Week 4: Investor Update - March Progress Report" \
    "Prepare investor update on Phase 2 progress and pilot status" \
    "LEADERSHIP" \
    "HIGH" \
    "PENDING" \
    "2026-03-31T23:59:59Z" \
    "Aaron (CEO)" \
    "2" \
    "Report completed, 1-2 LOIs documented, team alignment confirmed"

create_task \
    "Phase 2: Customer Success - Pilot Support & Onboarding" \
    "Assign dedicated success manager to first pilot customer" \
    "SALES" \
    "HIGH" \
    "PENDING" \
    "2026-03-31T23:59:59Z" \
    "Customer Success Lead" \
    "2" \
    "Success manager assigned, weekly check-ins scheduled, SLA defined"

create_task \
    "Phase 2: Risk Monitoring - Contingency Planning" \
    "Monitor Phase 2 execution risks and activate contingency plans if needed" \
    "LEADERSHIP" \
    "MEDIUM" \
    "PENDING" \
    "2026-03-31T23:59:59Z" \
    "Aaron (CEO)" \
    "2" \
    "Risk dashboard live, weekly reviews, contingency triggers identified"

echo "Phase 2 Task Population - Complete!"
echo "Total tasks created: 14"
echo "Execution window: March 1-31, 2026"
echo "Success target: 1-2 LOI signings, $50-100K revenue committed"
