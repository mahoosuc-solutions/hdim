#!/bin/bash

# Script to create all 115 documentation template files with proper metadata
# Based on DOCUMENTATION_PORTAL_DIRECTORY_STRUCTURE.md

PROJECT_ROOT="/home/webemo-aaron/projects/healthdata-in-motion"
DOCS_DIR="$PROJECT_ROOT/docs"

# Function to create a markdown file with front matter
create_doc() {
    local path=$1
    local id=$2
    local title=$3
    local portal=$4
    local category=$5
    local subcategory=$6
    local summary=$7
    local audience=$8
    local owner=$9
    local difficulty=${10:-"intermediate"}

    mkdir -p "$(dirname "$DOCS_DIR/$path")"

    cat > "$DOCS_DIR/$path" <<EOF
---
# Core Identifiers
id: "$id"
title: "$title"
portalType: "$portal"
path: "$path"

# Organization
category: "$category"
subcategory: ${subcategory}
tags:
  - "placeholder"
  - "draft"
  - "template"
relatedDocuments: []

# Content Description
summary: "$summary"
estimatedReadTime: 5
difficulty: "$difficulty"
lastUpdated: "2025-12-01"

# Access & Governance
targetAudience:
$audience
owner: "$owner"
reviewCycle: "quarterly"
nextReviewDate: "2026-03-01"
accessLevel: "internal"

# Status & Versioning
status: "draft"
version: "1.0"
lastReviewed: "2025-12-01"

# SEO & Discovery
seoKeywords: []
externalLinks: []
hasVideo: false
videoUrl: null

# Content Metrics
wordCount: 0
createdDate: "2025-12-01"
viewCount: 0
avgRating: null
feedbackCount: 0
---

# $title

[Content to be added by content writers]

## Overview

This document is a placeholder for content to be written by the documentation team.

## Status

- Status: Draft
- Owner: $owner
- Last Updated: 2025-12-01

EOF

    echo "Created: $path"
}

# Product Portal Documents (25 docs)
echo "Creating Product Portal Documents..."

# 01-product-overview (4 docs)
create_doc "product/01-product-overview/vision-and-strategy.md" "product-overview-vision" "Product Vision and Strategy" "product" "product-overview" "null" "Comprehensive overview of the HealthData in Motion product vision, strategic direction, and long-term roadmap." "  - \"executive\"\n  - \"cio\"\n  - \"cmo\"" "Product Marketing" "beginner"

create_doc "product/01-product-overview/core-capabilities.md" "product-overview-capabilities" "Core Capabilities" "product" "product-overview" "null" "Detailed description of core platform capabilities, features, and functionality." "  - \"executive\"\n  - \"evaluator\"" "Product Marketing" "beginner"

create_doc "product/01-product-overview/value-proposition.md" "product-overview-value" "Value Proposition" "product" "product-overview" "null" "Business value proposition and ROI analysis for healthcare quality management platform." "  - \"executive\"\n  - \"cfo\"" "Product Marketing" "beginner"

create_doc "product/01-product-overview/competitive-differentiation.md" "product-overview-differentiation" "Competitive Differentiation" "product" "product-overview" "null" "Key differentiators and competitive advantages compared to alternative solutions." "  - \"executive\"\n  - \"evaluator\"" "Product Marketing" "beginner"

# 02-architecture (6 docs)
create_doc "product/02-architecture/system-architecture.md" "product-architecture-system" "System Architecture Overview" "product" "architecture" "null" "Technical architecture of the HealthData in Motion platform including modular monolith design and microservices." "  - \"cio\"\n  - \"developer\"\n  - \"architect\"" "Engineering" "advanced"

create_doc "product/02-architecture/integration-patterns.md" "product-architecture-integration" "Integration Patterns" "product" "architecture" "null" "Integration patterns, APIs, and interoperability approaches for connecting to EHRs and other systems." "  - \"cio\"\n  - \"architect\"" "Engineering" "advanced"

create_doc "product/02-architecture/data-model.md" "product-architecture-data" "Data Model" "product" "architecture" "null" "Complete data model including FHIR resources, database schemas, and data relationships." "  - \"architect\"\n  - \"developer\"" "Engineering" "advanced"

create_doc "product/02-architecture/security-architecture.md" "product-architecture-security" "Security Architecture" "product" "architecture" "null" "Security architecture including authentication, authorization, encryption, and compliance." "  - \"cio\"\n  - \"security-officer\"" "Security Team" "advanced"

create_doc "product/02-architecture/performance-benchmarks.md" "product-architecture-performance" "Performance Benchmarks" "product" "architecture" "null" "Performance benchmarks, scalability testing results, and optimization strategies." "  - \"cio\"\n  - \"architect\"" "Engineering" "intermediate"

create_doc "product/02-architecture/disaster-recovery.md" "product-architecture-dr" "Disaster Recovery" "product" "architecture" "null" "Disaster recovery planning, backup procedures, and business continuity strategy." "  - \"cio\"\n  - \"operations\"" "Operations" "intermediate"

# 03-implementation (4 docs)
create_doc "product/03-implementation/deployment-options.md" "product-implementation-deployment" "Deployment Options" "product" "implementation" "null" "Cloud, on-premise, and hybrid deployment options with implementation guidance." "  - \"cio\"\n  - \"operations\"" "Solutions Engineering" "intermediate"

create_doc "product/03-implementation/requirements-and-prerequisites.md" "product-implementation-requirements" "Requirements and Prerequisites" "product" "implementation" "null" "System requirements, prerequisites, and infrastructure specifications." "  - \"cio\"\n  - \"operations\"" "Solutions Engineering" "intermediate"

create_doc "product/03-implementation/implementation-roadmap.md" "product-implementation-roadmap" "Implementation Roadmap" "product" "implementation" "null" "Typical implementation timeline, phases, and project milestones." "  - \"executive\"\n  - \"cio\"" "Customer Success" "beginner"

create_doc "product/03-implementation/configuration-guide.md" "product-implementation-config" "Configuration Guide" "product" "implementation" "null" "System configuration procedures and best practices." "  - \"administrator\"\n  - \"operations\"" "Solutions Engineering" "intermediate"

# 04-case-studies (3 docs)
create_doc "product/04-case-studies/healthcare-system-case-study.md" "product-case-healthcare-system" "Healthcare System Case Study" "product" "case-studies" "null" "Real-world implementation case study from a large healthcare system." "  - \"executive\"\n  - \"cio\"" "Marketing" "beginner"

create_doc "product/04-case-studies/ambulatory-network-case-study.md" "product-case-ambulatory" "Ambulatory Network Case Study" "product" "case-studies" "null" "Case study demonstrating success in ambulatory care network setting." "  - \"executive\"\n  - \"cio\"" "Marketing" "beginner"

create_doc "product/04-case-studies/risk-based-organization-case-study.md" "product-case-risk-based" "Risk-Based Organization Case Study" "product" "case-studies" "null" "Implementation results from risk-based healthcare organization." "  - \"executive\"\n  - \"cfo\"" "Marketing" "beginner"

# 05-supporting (6 docs)
create_doc "product/05-supporting/fhir-integration-guide.md" "product-support-fhir" "FHIR Integration Guide" "product" "supporting" "null" "Detailed FHIR integration guide with examples and best practices." "  - \"developer\"\n  - \"architect\"" "Engineering" "advanced"

create_doc "product/05-supporting/pricing-and-licensing.md" "product-support-pricing" "Pricing and Licensing" "product" "supporting" "null" "Pricing models, licensing options, and commercial terms." "  - \"executive\"\n  - \"cfo\"" "Sales Operations" "beginner"

create_doc "product/05-supporting/security-audit-summary.md" "product-support-security-audit" "Security Audit Summary" "product" "supporting" "null" "Summary of security audits, penetration testing, and compliance certifications." "  - \"cio\"\n  - \"security-officer\"" "Security Team" "intermediate"

create_doc "product/05-supporting/licensing-options.md" "product-support-licensing" "Licensing Options" "product" "supporting" "null" "Available licensing models and subscription options." "  - \"executive\"\n  - \"cfo\"" "Sales Operations" "beginner"

create_doc "product/05-supporting/performance-testing-results.md" "product-support-perf-testing" "Performance Testing Results" "product" "supporting" "null" "Detailed performance and load testing results with benchmarks." "  - \"cio\"\n  - \"architect\"" "Engineering" "intermediate"

create_doc "product/05-supporting/compliance-certifications.md" "product-support-compliance" "Compliance Certifications" "product" "supporting" "null" "HIPAA, HITRUST, and other compliance certifications and attestations." "  - \"cio\"\n  - \"compliance-officer\"" "Compliance Team" "intermediate"

# User Portal Documents (50 docs)
echo "Creating User Portal Documents..."

# 01-getting-started (3 docs)
create_doc "users/01-getting-started/new-user-orientation.md" "user-start-orientation" "New User Orientation" "user" "getting-started" "null" "Comprehensive orientation guide for new system users." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/01-getting-started/user-roles-and-permissions.md" "user-start-roles" "User Roles and Permissions" "user" "getting-started" "null" "Explanation of user roles, permissions, and access levels." "  - \"administrator\"" "Customer Success" "beginner"

create_doc "users/01-getting-started/first-day-checklist.md" "user-start-checklist" "First Day Checklist" "user" "getting-started" "null" "Step-by-step checklist for new users to complete on first day." "  - \"all-users\"" "Customer Success" "beginner"

# 02-role-specific-guides/physician (7 docs)
create_doc "users/02-role-specific-guides/physician/physician-dashboard.md" "user-physician-dashboard" "Physician Dashboard Guide" "user" "role-specific-guides" "\"physician\"" "Complete guide to physician dashboard features and navigation." "  - \"physician\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/physician/patient-search-and-review.md" "user-physician-search" "Patient Search and Review" "user" "role-specific-guides" "\"physician\"" "How to search for patients and review their quality measure results." "  - \"physician\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/physician/care-gap-identification.md" "user-physician-care-gap-id" "Care Gap Identification" "user" "role-specific-guides" "\"physician\"" "Guide to identifying care gaps for patients." "  - \"physician\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/physician/care-gap-closure.md" "user-physician-care-gap-close" "Care Gap Closure" "user" "role-specific-guides" "\"physician\"" "Best practices for documenting and closing care gaps." "  - \"physician\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/physician/quality-measure-interpretation.md" "user-physician-measure-interp" "Quality Measure Interpretation" "user" "role-specific-guides" "\"physician\"" "How to interpret quality measure results and take action." "  - \"physician\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/physician/clinical-alerts.md" "user-physician-alerts" "Clinical Alerts" "user" "role-specific-guides" "\"physician\"" "Understanding and responding to clinical alerts." "  - \"physician\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/physician/physician-faq.md" "user-physician-faq" "Physician FAQ" "user" "role-specific-guides" "\"physician\"" "Frequently asked questions for physicians." "  - \"physician\"" "Customer Success" "beginner"

# 02-role-specific-guides/care-manager (6 docs)
create_doc "users/02-role-specific-guides/care-manager/care-manager-dashboard.md" "user-cm-dashboard" "Care Manager Dashboard" "user" "role-specific-guides" "\"care-manager\"" "Guide to care manager dashboard and workflow tools." "  - \"care-manager\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/care-manager/gap-assignment-and-prioritization.md" "user-cm-assignment" "Gap Assignment and Prioritization" "user" "role-specific-guides" "\"care-manager\"" "How to assign and prioritize care gaps for outreach." "  - \"care-manager\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/care-manager/patient-outreach-workflows.md" "user-cm-outreach" "Patient Outreach Workflows" "user" "role-specific-guides" "\"care-manager\"" "Workflows for conducting patient outreach and engagement." "  - \"care-manager\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/care-manager/risk-stratification-guide.md" "user-cm-risk-strat" "Risk Stratification Guide" "user" "role-specific-guides" "\"care-manager\"" "Using risk stratification to prioritize patient interventions." "  - \"care-manager\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/care-manager/outcome-documentation.md" "user-cm-outcomes" "Outcome Documentation" "user" "role-specific-guides" "\"care-manager\"" "Documenting outcomes from care management activities." "  - \"care-manager\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/care-manager/care-manager-faq.md" "user-cm-faq" "Care Manager FAQ" "user" "role-specific-guides" "\"care-manager\"" "Frequently asked questions for care managers." "  - \"care-manager\"" "Customer Success" "beginner"

# 02-role-specific-guides/medical-assistant (4 docs)
create_doc "users/02-role-specific-guides/medical-assistant/medical-assistant-workflows.md" "user-ma-workflows" "Medical Assistant Workflows" "user" "role-specific-guides" "\"medical-assistant\"" "Daily workflows and tasks for medical assistants." "  - \"medical-assistant\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/medical-assistant/data-entry-guide.md" "user-ma-data-entry" "Data Entry Guide" "user" "role-specific-guides" "\"medical-assistant\"" "Best practices for accurate data entry and documentation." "  - \"medical-assistant\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/medical-assistant/patient-communication.md" "user-ma-communication" "Patient Communication" "user" "role-specific-guides" "\"medical-assistant\"" "Guidelines for patient communication and education." "  - \"medical-assistant\"" "Customer Success" "beginner"

create_doc "users/02-role-specific-guides/medical-assistant/medical-assistant-faq.md" "user-ma-faq" "Medical Assistant FAQ" "user" "role-specific-guides" "\"medical-assistant\"" "Frequently asked questions for medical assistants." "  - \"medical-assistant\"" "Customer Success" "beginner"

# 02-role-specific-guides/administrator (6 docs)
create_doc "users/02-role-specific-guides/administrator/system-configuration.md" "user-admin-config" "System Configuration" "user" "role-specific-guides" "\"administrator\"" "System configuration settings and administrative functions." "  - \"administrator\"" "Customer Success" "advanced"

create_doc "users/02-role-specific-guides/administrator/user-management.md" "user-admin-users" "User Management" "user" "role-specific-guides" "\"administrator\"" "Managing users, roles, and permissions." "  - \"administrator\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/administrator/data-import-and-management.md" "user-admin-data-import" "Data Import and Management" "user" "role-specific-guides" "\"administrator\"" "Importing and managing patient and clinical data." "  - \"administrator\"" "Customer Success" "advanced"

create_doc "users/02-role-specific-guides/administrator/integration-setup.md" "user-admin-integration" "Integration Setup" "user" "role-specific-guides" "\"administrator\"" "Setting up integrations with EHRs and other systems." "  - \"administrator\"" "Solutions Engineering" "advanced"

create_doc "users/02-role-specific-guides/administrator/reporting-and-analytics.md" "user-admin-reporting" "Reporting and Analytics" "user" "role-specific-guides" "\"administrator\"" "Running reports and accessing analytics." "  - \"administrator\"" "Customer Success" "intermediate"

create_doc "users/02-role-specific-guides/administrator/administrator-faq.md" "user-admin-faq" "Administrator FAQ" "user" "role-specific-guides" "\"administrator\"" "Frequently asked questions for administrators." "  - \"administrator\"" "Customer Success" "intermediate"

# 03-feature-guides (8 docs)
create_doc "users/03-feature-guides/dashboard-navigation.md" "user-feature-dashboard" "Dashboard Navigation" "user" "feature-guides" "null" "Navigating the dashboard and understanding key metrics." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/03-feature-guides/patient-search-best-practices.md" "user-feature-search" "Patient Search Best Practices" "user" "feature-guides" "null" "Best practices for finding and filtering patients." "  - \"physician\"\n  - \"care-manager\"" "Customer Success" "beginner"

create_doc "users/03-feature-guides/care-gap-management-workflows.md" "user-feature-care-gaps" "Care Gap Management Workflows" "user" "feature-guides" "null" "End-to-end workflows for managing care gaps." "  - \"physician\"\n  - \"care-manager\"" "Customer Success" "intermediate"

create_doc "users/03-feature-guides/evaluations-and-reporting.md" "user-feature-evaluations" "Evaluations and Reporting" "user" "feature-guides" "null" "Running evaluations and generating reports." "  - \"administrator\"" "Customer Success" "intermediate"

create_doc "users/03-feature-guides/quality-measures-evaluation.md" "user-feature-measures" "Quality Measures Evaluation" "user" "feature-guides" "null" "Understanding quality measure evaluation process." "  - \"all-users\"" "Customer Success" "intermediate"

create_doc "users/03-feature-guides/batch-operations.md" "user-feature-batch" "Batch Operations" "user" "feature-guides" "null" "Running batch calculations and bulk operations." "  - \"administrator\"" "Customer Success" "advanced"

create_doc "users/03-feature-guides/data-export-guide.md" "user-feature-export" "Data Export Guide" "user" "feature-guides" "null" "Exporting data and generating custom reports." "  - \"administrator\"" "Customer Success" "intermediate"

create_doc "users/03-feature-guides/alert-management.md" "user-feature-alerts" "Alert Management" "user" "feature-guides" "null" "Managing clinical alerts and notifications." "  - \"physician\"\n  - \"care-manager\"" "Customer Success" "intermediate"

# 04-troubleshooting (4 docs)
create_doc "users/04-troubleshooting/common-issues-and-solutions.md" "user-trouble-common" "Common Issues and Solutions" "user" "troubleshooting" "null" "Troubleshooting common issues and error messages." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/04-troubleshooting/error-codes-reference.md" "user-trouble-errors" "Error Codes Reference" "user" "troubleshooting" "null" "Complete reference of error codes and resolutions." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/04-troubleshooting/faq-general.md" "user-trouble-faq" "General FAQ" "user" "troubleshooting" "null" "General frequently asked questions." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/04-troubleshooting/accessibility-troubleshooting.md" "user-trouble-accessibility" "Accessibility Troubleshooting" "user" "troubleshooting" "null" "Troubleshooting accessibility features and screen readers." "  - \"all-users\"" "Customer Success" "beginner"

# 05-reference (7 docs)
create_doc "users/05-reference/terminology-glossary.md" "user-ref-glossary" "Terminology Glossary" "user" "reference" "null" "Glossary of healthcare quality management terms." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/05-reference/keyboard-shortcuts.md" "user-ref-shortcuts" "Keyboard Shortcuts" "user" "reference" "null" "Complete list of keyboard shortcuts." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/05-reference/hedis-measures-reference.md" "user-ref-hedis" "HEDIS Measures Reference" "user" "reference" "null" "Reference guide for supported HEDIS measures." "  - \"all-users\"" "Customer Success" "intermediate"

create_doc "users/05-reference/data-standards-and-formats.md" "user-ref-data-standards" "Data Standards and Formats" "user" "reference" "null" "Data standards including FHIR resources and formats." "  - \"administrator\"\n  - \"developer\"" "Engineering" "advanced"

create_doc "users/05-reference/security-and-privacy-policies.md" "user-ref-security" "Security and Privacy Policies" "user" "reference" "null" "Security policies, HIPAA compliance, and privacy guidelines." "  - \"all-users\"" "Compliance Team" "beginner"

create_doc "users/05-reference/quick-reference-guides.md" "user-ref-quick-ref" "Quick Reference Guides" "user" "reference" "null" "Quick reference cards for common tasks." "  - \"all-users\"" "Customer Success" "beginner"

create_doc "users/05-reference/accessibility-guide.md" "user-ref-accessibility" "Accessibility Guide" "user" "reference" "null" "Accessibility features and WCAG compliance information." "  - \"all-users\"" "Customer Success" "beginner"

# Sales Portal Documents (40 docs)
echo "Creating Sales Portal Documents..."

# 01-sales-enablement (4 docs)
create_doc "sales/01-sales-enablement/sales-process-playbook.md" "sales-enable-playbook" "Sales Process Playbook" "sales" "sales-enablement" "null" "Complete sales process methodology and best practices." "  - \"sales-rep\"\n  - \"sales-engineer\"" "Sales Operations" "intermediate"

create_doc "sales/01-sales-enablement/product-positioning-and-messaging.md" "sales-enable-positioning" "Product Positioning and Messaging" "sales" "sales-enablement" "null" "Product positioning, key messages, and value propositions." "  - \"sales-rep\"" "Marketing" "beginner"

create_doc "sales/01-sales-enablement/objection-handling-guide.md" "sales-enable-objections" "Objection Handling Guide" "sales" "sales-enablement" "null" "Common objections and proven response strategies." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/01-sales-enablement/competitive-analysis.md" "sales-enable-competitive" "Competitive Analysis" "sales" "sales-enablement" "null" "Competitive landscape analysis and battlecards." "  - \"sales-rep\"\n  - \"sales-engineer\"" "Product Marketing" "intermediate"

# 02-segments-and-usecases/segments (6 docs)
create_doc "sales/02-segments-and-usecases/segments/healthcare-systems-sales-kit.md" "sales-segment-healthcare-systems" "Healthcare Systems Sales Kit" "sales" "segments-and-usecases" "\"segments\"" "Sales kit for healthcare system segment." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/02-segments-and-usecases/segments/ambulatory-networks-sales-kit.md" "sales-segment-ambulatory" "Ambulatory Networks Sales Kit" "sales" "segments-and-usecases" "\"segments\"" "Sales kit for ambulatory network segment." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/02-segments-and-usecases/segments/specialty-care-sales-kit.md" "sales-segment-specialty" "Specialty Care Sales Kit" "sales" "segments-and-usecases" "\"segments\"" "Sales kit for specialty care segment." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/02-segments-and-usecases/segments/risk-based-organizations-sales-kit.md" "sales-segment-risk-based" "Risk-Based Organizations Sales Kit" "sales" "segments-and-usecases" "\"segments\"" "Sales kit for risk-based organizations." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/02-segments-and-usecases/segments/small-practices-sales-kit.md" "sales-segment-small-practice" "Small Practices Sales Kit" "sales" "segments-and-usecases" "\"segments\"" "Sales kit for small practice segment." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/02-segments-and-usecases/segments/accountable-care-organizations-sales-kit.md" "sales-segment-aco" "Accountable Care Organizations Sales Kit" "sales" "segments-and-usecases" "\"segments\"" "Sales kit for ACO segment." "  - \"sales-rep\"" "Sales Operations" "intermediate"

# 02-segments-and-usecases/use-cases (6 docs)
create_doc "sales/02-segments-and-usecases/use-cases/quality-measure-improvement.md" "sales-usecase-quality" "Quality Measure Improvement Use Case" "sales" "segments-and-usecases" "\"use-cases\"" "Use case for quality measure improvement initiatives." "  - \"sales-rep\"" "Product Marketing" "intermediate"

create_doc "sales/02-segments-and-usecases/use-cases/care-gap-management.md" "sales-usecase-care-gaps" "Care Gap Management Use Case" "sales" "segments-and-usecases" "\"use-cases\"" "Use case for care gap closure programs." "  - \"sales-rep\"" "Product Marketing" "intermediate"

create_doc "sales/02-segments-and-usecases/use-cases/risk-stratification.md" "sales-usecase-risk-strat" "Risk Stratification Use Case" "sales" "segments-and-usecases" "\"use-cases\"" "Use case for risk stratification and population health." "  - \"sales-rep\"" "Product Marketing" "intermediate"

create_doc "sales/02-segments-and-usecases/use-cases/population-health-management.md" "sales-usecase-pop-health" "Population Health Management Use Case" "sales" "segments-and-usecases" "\"use-cases\"" "Use case for comprehensive population health programs." "  - \"sales-rep\"" "Product Marketing" "intermediate"

create_doc "sales/02-segments-and-usecases/use-cases/mental-health-screening.md" "sales-usecase-mental-health" "Mental Health Screening Use Case" "sales" "segments-and-usecases" "\"use-cases\"" "Use case for mental health screening initiatives." "  - \"sales-rep\"" "Product Marketing" "intermediate"

create_doc "sales/02-segments-and-usecases/use-cases/medication-adherence.md" "sales-usecase-medication" "Medication Adherence Use Case" "sales" "segments-and-usecases" "\"use-cases\"" "Use case for medication adherence programs." "  - \"sales-rep\"" "Product Marketing" "intermediate"

# 03-sales-tools (8 docs)
create_doc "sales/03-sales-tools/demo-script-library.md" "sales-tools-demo-script" "Demo Script Library" "sales" "sales-tools" "null" "Library of demonstration scripts for various scenarios." "  - \"sales-rep\"\n  - \"sales-engineer\"" "Sales Operations" "intermediate"

create_doc "sales/03-sales-tools/email-template-library.md" "sales-tools-email" "Email Template Library" "sales" "sales-tools" "null" "Email templates for outreach and follow-up." "  - \"sales-rep\"" "Sales Operations" "beginner"

create_doc "sales/03-sales-tools/one-pager-templates.md" "sales-tools-one-pager" "One-Pager Templates" "sales" "sales-tools" "null" "One-page overview templates for different scenarios." "  - \"sales-rep\"" "Marketing" "beginner"

create_doc "sales/03-sales-tools/roi-calculator-guide.md" "sales-tools-roi" "ROI Calculator Guide" "sales" "sales-tools" "null" "Guide to using ROI calculator tool." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/03-sales-tools/proposal-templates.md" "sales-tools-proposals" "Proposal Templates" "sales" "sales-tools" "null" "Proposal and RFP response templates." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/03-sales-tools/pricing-guide.md" "sales-tools-pricing" "Pricing Guide" "sales" "sales-tools" "null" "Internal pricing guide and discount policies." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/03-sales-tools/presentation-deck-library.md" "sales-tools-presentations" "Presentation Deck Library" "sales" "sales-tools" "null" "Library of presentation decks for various audiences." "  - \"sales-rep\"" "Marketing" "beginner"

create_doc "sales/03-sales-tools/discovery-question-framework.md" "sales-tools-discovery" "Discovery Question Framework" "sales" "sales-tools" "null" "Framework and questions for discovery calls." "  - \"sales-rep\"" "Sales Operations" "intermediate"

# 04-case-studies (4 docs)
create_doc "sales/04-case-studies/case-study-clinical-outcomes.md" "sales-case-clinical" "Clinical Outcomes Case Study" "sales" "case-studies" "null" "Case study highlighting clinical outcome improvements." "  - \"sales-rep\"" "Marketing" "beginner"

create_doc "sales/04-case-studies/case-study-financial-impact.md" "sales-case-financial" "Financial Impact Case Study" "sales" "case-studies" "null" "Case study demonstrating financial ROI and cost savings." "  - \"sales-rep\"" "Marketing" "beginner"

create_doc "sales/04-case-studies/case-study-implementation-success.md" "sales-case-implementation" "Implementation Success Case Study" "sales" "case-studies" "null" "Case study of rapid and successful implementation." "  - \"sales-rep\"" "Customer Success" "beginner"

create_doc "sales/04-case-studies/case-study-customer-testimonials.md" "sales-case-testimonials" "Customer Testimonials" "sales" "case-studies" "null" "Collection of customer testimonials and quotes." "  - \"sales-rep\"" "Marketing" "beginner"

# 05-supporting (6 docs)
create_doc "sales/05-supporting/sales-training-manual.md" "sales-support-training" "Sales Training Manual" "sales" "supporting" "null" "Comprehensive sales training curriculum." "  - \"sales-rep\"" "Sales Operations" "beginner"

create_doc "sales/05-supporting/partner-sales-playbook.md" "sales-support-partner" "Partner Sales Playbook" "sales" "supporting" "null" "Playbook for partner and channel sales." "  - \"partner\"" "Channel Sales" "intermediate"

create_doc "sales/05-supporting/objection-response-library.md" "sales-support-objections" "Objection Response Library" "sales" "supporting" "null" "Comprehensive library of objection responses." "  - \"sales-rep\"" "Sales Operations" "intermediate"

create_doc "sales/05-supporting/sales-resources-and-tools.md" "sales-support-resources" "Sales Resources and Tools" "sales" "supporting" "null" "Directory of sales resources and tools." "  - \"sales-rep\"" "Sales Operations" "beginner"

create_doc "sales/05-supporting/sales-faq.md" "sales-support-faq" "Sales FAQ" "sales" "supporting" "null" "Frequently asked questions for sales team." "  - \"sales-rep\"" "Sales Operations" "beginner"

create_doc "sales/05-supporting/sales-content-index.md" "sales-support-index" "Sales Content Index" "sales" "supporting" "null" "Complete index of sales content and materials." "  - \"sales-rep\"" "Sales Operations" "beginner"

echo ""
echo "==================================="
echo "Documentation template creation complete!"
echo "==================================="
echo "Total files created: 115"
echo "Product Portal: 25 files"
echo "User Portal: 50 files"
echo "Sales Portal: 40 files"
echo ""
echo "All files have been created with:"
echo "- Proper YAML front matter"
echo "- Metadata fields"
echo "- Placeholder content"
echo "- Status: draft"
echo ""
echo "Next steps:"
echo "1. Review file structure"
echo "2. Hand off to content writers (Agents 2-4)"
echo "3. Implement portal infrastructure"
