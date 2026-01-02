#!/usr/bin/env npx ts-node
/**
 * HDIM Implementation Timeline Calculator
 *
 * Calculates estimated demo-to-production timeline based on customer profile,
 * integration complexity, and organizational factors.
 *
 * Usage:
 *   npx ts-node timeline-calculator.ts --interactive
 *   npx ts-node timeline-calculator.ts --config customer.json
 *   npx ts-node timeline-calculator.ts --org-type "small-practice" --ehr "athenahealth" --method "fhir-api"
 */

// ============================================================================
// TYPE DEFINITIONS
// ============================================================================

type OrganizationType =
  | 'solo-practice'
  | 'small-practice'
  | 'fqhc'
  | 'rural-hospital'
  | 'small-aco'
  | 'midsize-aco'
  | 'large-health-system'
  | 'ipa';

type EHRSystem =
  | 'none'
  | 'athenahealth'
  | 'epic'
  | 'cerner'
  | 'eclinicalworks'
  | 'nextgen'
  | 'allscripts'
  | 'meditech'
  | 'other-modern'
  | 'legacy';

type IntegrationMethod =
  | 'csv-upload'
  | 'fhir-api'
  | 'smart-on-fhir'
  | 'n8n-workflow'
  | 'private-cloud'
  | 'on-premise';

type ITCapability = 'none' | 'basic' | 'moderate' | 'advanced';

type HDIMTier = 'community' | 'professional' | 'enterprise' | 'enterprise-plus' | 'health-system';

interface CustomerProfile {
  organizationType: OrganizationType;
  organizationName?: string;
  ehrSystem: EHRSystem;
  integrationMethod: IntegrationMethod;
  itCapability: ITCapability;
  hdimTier: HDIMTier;
  patientCount: number;
  providerCount: number;
  siteCount: number;
  dataSourceCount: number;
  qualityPrograms: string[];
  hasExistingFHIR: boolean;
  hasDedicatedIT: boolean;
  requiresSOC2: boolean;
  requiresBAA: boolean;
  pilotFirst: boolean;
}

interface Phase {
  name: string;
  description: string;
  durationDays: { min: number; max: number };
  milestones: string[];
  dependencies: string[];
  risks: string[];
  deliverables: string[];
}

interface TimelineResult {
  customer: CustomerProfile;
  phases: Phase[];
  totalDays: { min: number; max: number };
  complexityScore: number;
  complexityLevel: 'low' | 'medium' | 'high' | 'very-high';
  riskFactors: string[];
  recommendations: string[];
  criticalPath: string[];
  estimatedStartDate?: Date;
  estimatedGoLiveDate?: { earliest: Date; latest: Date };
}

// ============================================================================
// COMPLEXITY SCORING FACTORS
// ============================================================================

const ORG_TYPE_COMPLEXITY: Record<OrganizationType, number> = {
  'solo-practice': 1,
  'small-practice': 2,
  'fqhc': 4,
  'rural-hospital': 3,
  'small-aco': 5,
  'midsize-aco': 7,
  'large-health-system': 10,
  'ipa': 8,
};

const EHR_COMPLEXITY: Record<EHRSystem, number> = {
  'none': 1,
  'athenahealth': 2,
  'epic': 4,
  'cerner': 4,
  'eclinicalworks': 3,
  'nextgen': 3,
  'allscripts': 3,
  'meditech': 5,
  'other-modern': 3,
  'legacy': 6,
};

const METHOD_COMPLEXITY: Record<IntegrationMethod, number> = {
  'csv-upload': 1,
  'fhir-api': 3,
  'smart-on-fhir': 4,
  'n8n-workflow': 5,
  'private-cloud': 7,
  'on-premise': 10,
};

const IT_CAPABILITY_FACTOR: Record<ITCapability, number> = {
  'none': 1.5,
  'basic': 1.2,
  'moderate': 1.0,
  'advanced': 0.8,
};

// ============================================================================
// PHASE DEFINITIONS
// ============================================================================

function generatePhases(profile: CustomerProfile): Phase[] {
  const phases: Phase[] = [];

  // Phase 0: Discovery & Planning
  phases.push({
    name: 'Discovery & Planning',
    description: 'Initial customer discovery, requirements gathering, and implementation planning',
    durationDays: getDiscoveryDuration(profile),
    milestones: [
      'Kickoff meeting completed',
      'Technical requirements documented',
      'Data sources identified',
      'Quality programs confirmed',
      'Implementation timeline agreed',
    ],
    dependencies: [],
    risks: getRisks(profile, 'discovery'),
    deliverables: [
      'Customer profile document',
      'Technical assessment report',
      'Implementation plan',
      'Success criteria defined',
    ],
  });

  // Phase 1: Environment Setup
  phases.push({
    name: 'Environment Setup',
    description: 'Provision HDIM environment, configure tenant, establish security',
    durationDays: getEnvironmentDuration(profile),
    milestones: [
      'Tenant provisioned',
      'User accounts created',
      'SSO configured (if applicable)',
      'Security assessment completed',
      'BAA executed (if required)',
    ],
    dependencies: ['Discovery & Planning'],
    risks: getRisks(profile, 'environment'),
    deliverables: [
      'Configured HDIM tenant',
      'Admin credentials',
      'Security documentation',
      'Access control matrix',
    ],
  });

  // Phase 2: Integration Configuration
  phases.push({
    name: 'Integration Configuration',
    description: 'Configure data integration based on selected method',
    durationDays: getIntegrationDuration(profile),
    milestones: getIntegrationMilestones(profile),
    dependencies: ['Environment Setup'],
    risks: getRisks(profile, 'integration'),
    deliverables: getIntegrationDeliverables(profile),
  });

  // Phase 3: Data Sync & Validation
  phases.push({
    name: 'Data Sync & Validation',
    description: 'Initial data load, quality validation, and measure testing',
    durationDays: getDataSyncDuration(profile),
    milestones: [
      'Initial data sync completed',
      'Data quality validation passed',
      'Patient matching verified',
      'Measure calculations validated',
      'Care gaps identified correctly',
    ],
    dependencies: ['Integration Configuration'],
    risks: getRisks(profile, 'data-sync'),
    deliverables: [
      'Data quality report',
      'Measure validation results',
      'Discrepancy resolution log',
      'Patient matching report',
    ],
  });

  // Phase 4: User Training
  phases.push({
    name: 'User Training',
    description: 'Train clinical and administrative staff on HDIM platform',
    durationDays: getTrainingDuration(profile),
    milestones: [
      'Training materials customized',
      'Admin training completed',
      'Clinical user training completed',
      'Workflow documentation finalized',
      'User acceptance sign-off',
    ],
    dependencies: ['Data Sync & Validation'],
    risks: getRisks(profile, 'training'),
    deliverables: [
      'Training documentation',
      'Quick reference guides',
      'Workflow procedures',
      'Support escalation path',
    ],
  });

  // Phase 5: Pilot (if applicable)
  if (profile.pilotFirst) {
    phases.push({
      name: 'Pilot Deployment',
      description: 'Limited rollout to pilot site/users for validation',
      durationDays: getPilotDuration(profile),
      milestones: [
        'Pilot site selected',
        'Pilot users trained',
        'Pilot period started',
        'Feedback collected',
        'Pilot success criteria met',
      ],
      dependencies: ['User Training'],
      risks: getRisks(profile, 'pilot'),
      deliverables: [
        'Pilot results report',
        'User feedback summary',
        'Issue resolution log',
        'Go-live readiness assessment',
      ],
    });
  }

  // Phase 6: Go-Live
  phases.push({
    name: 'Go-Live',
    description: 'Production deployment and hypercare support period',
    durationDays: getGoLiveDuration(profile),
    milestones: [
      'Go-live checklist completed',
      'Production cutover executed',
      'Hypercare period started',
      'Day 1 issues resolved',
      'Operational handoff completed',
    ],
    dependencies: [profile.pilotFirst ? 'Pilot Deployment' : 'User Training'],
    risks: getRisks(profile, 'go-live'),
    deliverables: [
      'Go-live sign-off',
      'Runbook documentation',
      'Escalation procedures',
      'Support transition plan',
    ],
  });

  // Phase 7: Post-Implementation Review
  phases.push({
    name: 'Post-Implementation Review',
    description: '30-day review, success metrics evaluation, optimization',
    durationDays: { min: 7, max: 14 },
    milestones: [
      '30-day review scheduled',
      'Success metrics evaluated',
      'User satisfaction surveyed',
      'Optimization opportunities identified',
      'Ongoing support plan confirmed',
    ],
    dependencies: ['Go-Live'],
    risks: [],
    deliverables: [
      '30-day review report',
      'ROI analysis',
      'Optimization roadmap',
      'Customer success plan',
    ],
  });

  return phases;
}

// ============================================================================
// DURATION CALCULATORS
// ============================================================================

function getDiscoveryDuration(profile: CustomerProfile): { min: number; max: number } {
  const base = { min: 1, max: 2 };

  if (profile.organizationType === 'large-health-system') {
    return { min: 5, max: 10 };
  }
  if (['midsize-aco', 'ipa'].includes(profile.organizationType)) {
    return { min: 3, max: 5 };
  }
  if (['fqhc', 'small-aco', 'rural-hospital'].includes(profile.organizationType)) {
    return { min: 2, max: 3 };
  }

  return base;
}

function getEnvironmentDuration(profile: CustomerProfile): { min: number; max: number } {
  if (profile.integrationMethod === 'on-premise') {
    return { min: 14, max: 21 };
  }
  if (profile.integrationMethod === 'private-cloud') {
    return { min: 7, max: 14 };
  }
  if (profile.hdimTier === 'health-system') {
    return { min: 3, max: 7 };
  }
  if (profile.requiresSOC2) {
    return { min: 2, max: 5 };
  }

  return { min: 1, max: 2 };
}

function getIntegrationDuration(profile: CustomerProfile): { min: number; max: number } {
  const methodDurations: Record<IntegrationMethod, { min: number; max: number }> = {
    'csv-upload': { min: 0.5, max: 1 },
    'fhir-api': { min: 2, max: 5 },
    'smart-on-fhir': { min: 3, max: 7 },
    'n8n-workflow': { min: 5, max: 10 },
    'private-cloud': { min: 10, max: 21 },
    'on-premise': { min: 21, max: 35 },
  };

  const base = methodDurations[profile.integrationMethod];

  // Adjust for EHR complexity
  const ehrFactor = EHR_COMPLEXITY[profile.ehrSystem] / 3;

  // Adjust for multiple data sources
  const sourceFactor = 1 + (profile.dataSourceCount - 1) * 0.3;

  // Adjust for IT capability
  const itFactor = IT_CAPABILITY_FACTOR[profile.itCapability];

  return {
    min: Math.round(base.min * ehrFactor * sourceFactor * itFactor),
    max: Math.round(base.max * ehrFactor * sourceFactor * itFactor),
  };
}

function getDataSyncDuration(profile: CustomerProfile): { min: number; max: number } {
  const patientFactor = profile.patientCount > 50000 ? 2 : profile.patientCount > 10000 ? 1.5 : 1;
  const sourceFactor = 1 + (profile.dataSourceCount - 1) * 0.25;

  if (profile.integrationMethod === 'csv-upload') {
    return { min: 1, max: 2 };
  }

  return {
    min: Math.round(2 * patientFactor * sourceFactor),
    max: Math.round(5 * patientFactor * sourceFactor),
  };
}

function getTrainingDuration(profile: CustomerProfile): { min: number; max: number } {
  if (profile.organizationType === 'large-health-system') {
    return { min: 5, max: 10 };
  }
  if (['midsize-aco', 'ipa'].includes(profile.organizationType)) {
    return { min: 3, max: 5 };
  }
  if (profile.siteCount > 5) {
    return { min: 3, max: 5 };
  }

  return { min: 1, max: 3 };
}

function getPilotDuration(profile: CustomerProfile): { min: number; max: number } {
  if (profile.organizationType === 'large-health-system') {
    return { min: 14, max: 30 };
  }
  if (['midsize-aco', 'ipa'].includes(profile.organizationType)) {
    return { min: 7, max: 14 };
  }

  return { min: 5, max: 10 };
}

function getGoLiveDuration(profile: CustomerProfile): { min: number; max: number } {
  // Hypercare period
  if (profile.organizationType === 'large-health-system') {
    return { min: 7, max: 14 };
  }
  if (['midsize-aco', 'ipa', 'fqhc'].includes(profile.organizationType)) {
    return { min: 5, max: 7 };
  }

  return { min: 2, max: 5 };
}

// ============================================================================
// MILESTONE & DELIVERABLE GENERATORS
// ============================================================================

function getIntegrationMilestones(profile: CustomerProfile): string[] {
  const common = [
    'Integration method confirmed',
    'Authentication configured',
    'Test connection successful',
  ];

  const methodSpecific: Record<IntegrationMethod, string[]> = {
    'csv-upload': [
      'CSV template provided',
      'Sample file validated',
      'Upload process documented',
    ],
    'fhir-api': [
      'OAuth credentials obtained',
      'FHIR endpoint configured',
      'Resource mapping completed',
      'Test queries validated',
    ],
    'smart-on-fhir': [
      'SMART app registered',
      'Launch context configured',
      'EHR admin approval obtained',
      'Scopes validated',
    ],
    'n8n-workflow': [
      'n8n instance deployed',
      'Workflow designed',
      'Data transformations tested',
      'Schedule configured',
    ],
    'private-cloud': [
      'Cloud environment provisioned',
      'Network connectivity established',
      'Security groups configured',
      'Database replication set up',
    ],
    'on-premise': [
      'Hardware provisioned',
      'Network configured',
      'Security hardened',
      'Backup/DR configured',
      'Monitoring established',
    ],
  };

  return [...common, ...methodSpecific[profile.integrationMethod]];
}

function getIntegrationDeliverables(profile: CustomerProfile): string[] {
  const common = [
    'Integration configuration document',
    'Data mapping specification',
  ];

  const methodSpecific: Record<IntegrationMethod, string[]> = {
    'csv-upload': ['CSV template file', 'Upload instructions'],
    'fhir-api': ['API credentials', 'FHIR endpoint documentation', 'Query library'],
    'smart-on-fhir': ['App registration details', 'Launch configuration'],
    'n8n-workflow': ['Workflow export', 'Schedule documentation', 'Error handling procedures'],
    'private-cloud': ['Infrastructure diagram', 'Runbook', 'DR plan'],
    'on-premise': ['Architecture document', 'Operations manual', 'Security assessment'],
  };

  return [...common, ...methodSpecific[profile.integrationMethod]];
}

// ============================================================================
// RISK ASSESSMENT
// ============================================================================

function getRisks(profile: CustomerProfile, phase: string): string[] {
  const risks: string[] = [];

  if (phase === 'discovery') {
    if (profile.itCapability === 'none') {
      risks.push('Limited IT support may slow technical discussions');
    }
    if (profile.dataSourceCount > 3) {
      risks.push('Multiple data sources require comprehensive mapping');
    }
  }

  if (phase === 'environment') {
    if (profile.requiresSOC2) {
      risks.push('SOC2 review may extend timeline');
    }
    if (profile.integrationMethod === 'on-premise') {
      risks.push('On-premise deployment requires extended infrastructure setup');
    }
  }

  if (phase === 'integration') {
    if (profile.ehrSystem === 'legacy') {
      risks.push('Legacy EHR may require custom data extraction');
    }
    if (profile.ehrSystem === 'epic' || profile.ehrSystem === 'cerner') {
      risks.push('EHR app marketplace approval may extend timeline by 2-4 weeks');
    }
    if (!profile.hasExistingFHIR) {
      risks.push('FHIR capability may need to be enabled at EHR level');
    }
  }

  if (phase === 'data-sync') {
    if (profile.patientCount > 50000) {
      risks.push('Large patient volume may require incremental sync strategy');
    }
    if (profile.qualityPrograms.length > 2) {
      risks.push('Multiple quality programs increase validation complexity');
    }
  }

  if (phase === 'training') {
    if (profile.siteCount > 5) {
      risks.push('Multi-site training requires coordination and may extend timeline');
    }
    if (!profile.hasDedicatedIT) {
      risks.push('Lack of dedicated IT may slow issue resolution during training');
    }
  }

  if (phase === 'go-live') {
    if (profile.organizationType === 'large-health-system') {
      risks.push('Large organization requires phased rollout strategy');
    }
  }

  return risks;
}

// ============================================================================
// COMPLEXITY CALCULATION
// ============================================================================

function calculateComplexity(profile: CustomerProfile): {
  score: number;
  level: 'low' | 'medium' | 'high' | 'very-high';
} {
  let score = 0;

  // Organization type (0-10)
  score += ORG_TYPE_COMPLEXITY[profile.organizationType];

  // EHR complexity (0-6)
  score += EHR_COMPLEXITY[profile.ehrSystem];

  // Integration method (0-10)
  score += METHOD_COMPLEXITY[profile.integrationMethod];

  // Data sources (0-5)
  score += Math.min(profile.dataSourceCount * 1.5, 5);

  // Quality programs (0-3)
  score += Math.min(profile.qualityPrograms.length, 3);

  // IT capability adjustment
  score *= IT_CAPABILITY_FACTOR[profile.itCapability];

  // Additional factors
  if (!profile.hasExistingFHIR && profile.integrationMethod !== 'csv-upload') {
    score += 3;
  }
  if (profile.requiresSOC2) {
    score += 2;
  }
  if (profile.patientCount > 100000) {
    score += 3;
  } else if (profile.patientCount > 50000) {
    score += 2;
  } else if (profile.patientCount > 10000) {
    score += 1;
  }

  // Normalize to 0-100 scale
  const normalizedScore = Math.min(Math.round(score * 2.5), 100);

  let level: 'low' | 'medium' | 'high' | 'very-high';
  if (normalizedScore <= 25) {
    level = 'low';
  } else if (normalizedScore <= 50) {
    level = 'medium';
  } else if (normalizedScore <= 75) {
    level = 'high';
  } else {
    level = 'very-high';
  }

  return { score: normalizedScore, level };
}

// ============================================================================
// RECOMMENDATIONS
// ============================================================================

function generateRecommendations(profile: CustomerProfile, phases: Phase[]): string[] {
  const recommendations: string[] = [];

  // Method recommendations
  if (profile.integrationMethod === 'csv-upload' && profile.patientCount > 5000) {
    recommendations.push('Consider upgrading to FHIR API for automated data sync at this patient volume');
  }

  if (profile.ehrSystem === 'epic' || profile.ehrSystem === 'cerner') {
    recommendations.push('Start EHR app marketplace approval process during Discovery phase to avoid delays');
  }

  // IT capability recommendations
  if (profile.itCapability === 'none') {
    recommendations.push('Assign a technical point of contact or consider HDIM managed services');
  }

  // Scale recommendations
  if (profile.siteCount > 5) {
    recommendations.push('Use train-the-trainer approach for efficient multi-site rollout');
  }

  if (profile.patientCount > 50000) {
    recommendations.push('Implement incremental data sync strategy with validation checkpoints');
  }

  // Pilot recommendations
  if (!profile.pilotFirst && profile.organizationType === 'large-health-system') {
    recommendations.push('Consider pilot deployment to validate workflows before full rollout');
  }

  // Data quality recommendations
  if (profile.dataSourceCount > 3) {
    recommendations.push('Establish master data management strategy for patient matching across sources');
  }

  // Support recommendations
  if (profile.hdimTier === 'community' && profile.organizationType !== 'solo-practice') {
    recommendations.push('Consider Professional tier for dedicated support during implementation');
  }

  return recommendations;
}

// ============================================================================
// MAIN CALCULATOR
// ============================================================================

export function calculateTimeline(profile: CustomerProfile, startDate?: Date): TimelineResult {
  const phases = generatePhases(profile);
  const complexity = calculateComplexity(profile);
  const recommendations = generateRecommendations(profile, phases);

  // Calculate total duration
  const totalMin = phases.reduce((sum, p) => sum + p.durationDays.min, 0);
  const totalMax = phases.reduce((sum, p) => sum + p.durationDays.max, 0);

  // Collect all risks
  const allRisks = phases.flatMap(p => p.risks).filter((r, i, arr) => arr.indexOf(r) === i);

  // Build critical path
  const criticalPath = phases.map(p => p.name);

  // Calculate dates if start date provided
  let estimatedGoLiveDate: { earliest: Date; latest: Date } | undefined;
  if (startDate) {
    const earliest = new Date(startDate);
    earliest.setDate(earliest.getDate() + totalMin);
    const latest = new Date(startDate);
    latest.setDate(latest.getDate() + totalMax);
    estimatedGoLiveDate = { earliest, latest };
  }

  return {
    customer: profile,
    phases,
    totalDays: { min: totalMin, max: totalMax },
    complexityScore: complexity.score,
    complexityLevel: complexity.level,
    riskFactors: allRisks,
    recommendations,
    criticalPath,
    estimatedStartDate: startDate,
    estimatedGoLiveDate,
  };
}

// ============================================================================
// OUTPUT FORMATTERS
// ============================================================================

export function formatTimelineText(result: TimelineResult): string {
  const lines: string[] = [];

  lines.push('═'.repeat(80));
  lines.push('HDIM IMPLEMENTATION TIMELINE');
  lines.push('═'.repeat(80));
  lines.push('');

  // Customer summary
  lines.push('CUSTOMER PROFILE');
  lines.push('─'.repeat(40));
  lines.push(`Organization: ${result.customer.organizationName || 'Not specified'}`);
  lines.push(`Type: ${result.customer.organizationType}`);
  lines.push(`EHR System: ${result.customer.ehrSystem}`);
  lines.push(`Integration Method: ${result.customer.integrationMethod}`);
  lines.push(`IT Capability: ${result.customer.itCapability}`);
  lines.push(`HDIM Tier: ${result.customer.hdimTier}`);
  lines.push(`Patients: ${result.customer.patientCount.toLocaleString()}`);
  lines.push(`Providers: ${result.customer.providerCount}`);
  lines.push(`Sites: ${result.customer.siteCount}`);
  lines.push(`Data Sources: ${result.customer.dataSourceCount}`);
  lines.push(`Quality Programs: ${result.customer.qualityPrograms.join(', ') || 'None specified'}`);
  lines.push('');

  // Complexity assessment
  lines.push('COMPLEXITY ASSESSMENT');
  lines.push('─'.repeat(40));
  const complexityBar = '█'.repeat(Math.floor(result.complexityScore / 5)) +
                        '░'.repeat(20 - Math.floor(result.complexityScore / 5));
  lines.push(`Score: ${result.complexityScore}/100 [${complexityBar}]`);
  lines.push(`Level: ${result.complexityLevel.toUpperCase()}`);
  lines.push('');

  // Timeline summary
  lines.push('TIMELINE SUMMARY');
  lines.push('─'.repeat(40));
  lines.push(`Estimated Duration: ${result.totalDays.min}-${result.totalDays.max} business days`);
  lines.push(`                    (~${Math.ceil(result.totalDays.min / 5)}-${Math.ceil(result.totalDays.max / 5)} weeks)`);
  if (result.estimatedGoLiveDate) {
    lines.push(`Start Date: ${result.estimatedStartDate?.toLocaleDateString()}`);
    lines.push(`Go-Live: ${result.estimatedGoLiveDate.earliest.toLocaleDateString()} - ${result.estimatedGoLiveDate.latest.toLocaleDateString()}`);
  }
  lines.push('');

  // Phase breakdown
  lines.push('PHASE BREAKDOWN');
  lines.push('─'.repeat(40));
  let dayCounter = 0;
  for (const phase of result.phases) {
    const startDay = dayCounter + 1;
    const endDayMin = dayCounter + phase.durationDays.min;
    const endDayMax = dayCounter + phase.durationDays.max;
    dayCounter = endDayMax;

    lines.push('');
    lines.push(`▶ ${phase.name.toUpperCase()}`);
    lines.push(`  Duration: ${phase.durationDays.min}-${phase.durationDays.max} days (Day ${startDay} → Day ${endDayMin}-${endDayMax})`);
    lines.push(`  ${phase.description}`);
    lines.push('');
    lines.push('  Milestones:');
    for (const milestone of phase.milestones) {
      lines.push(`    ☐ ${milestone}`);
    }
    if (phase.deliverables.length > 0) {
      lines.push('');
      lines.push('  Deliverables:');
      for (const deliverable of phase.deliverables) {
        lines.push(`    📄 ${deliverable}`);
      }
    }
    if (phase.risks.length > 0) {
      lines.push('');
      lines.push('  ⚠️ Risks:');
      for (const risk of phase.risks) {
        lines.push(`    • ${risk}`);
      }
    }
  }
  lines.push('');

  // Risk summary
  if (result.riskFactors.length > 0) {
    lines.push('RISK FACTORS');
    lines.push('─'.repeat(40));
    for (const risk of result.riskFactors) {
      lines.push(`  ⚠️ ${risk}`);
    }
    lines.push('');
  }

  // Recommendations
  if (result.recommendations.length > 0) {
    lines.push('RECOMMENDATIONS');
    lines.push('─'.repeat(40));
    for (const rec of result.recommendations) {
      lines.push(`  💡 ${rec}`);
    }
    lines.push('');
  }

  lines.push('═'.repeat(80));
  lines.push(`Generated: ${new Date().toISOString()}`);
  lines.push('═'.repeat(80));

  return lines.join('\n');
}

export function formatTimelineJSON(result: TimelineResult): string {
  return JSON.stringify(result, null, 2);
}

export function formatTimelineMarkdown(result: TimelineResult): string {
  const lines: string[] = [];

  lines.push(`# HDIM Implementation Timeline`);
  lines.push('');
  lines.push(`> Generated: ${new Date().toLocaleDateString()}`);
  lines.push('');

  // Customer Profile Table
  lines.push('## Customer Profile');
  lines.push('');
  lines.push('| Attribute | Value |');
  lines.push('|-----------|-------|');
  lines.push(`| Organization | ${result.customer.organizationName || 'Not specified'} |`);
  lines.push(`| Type | ${result.customer.organizationType} |`);
  lines.push(`| EHR System | ${result.customer.ehrSystem} |`);
  lines.push(`| Integration | ${result.customer.integrationMethod} |`);
  lines.push(`| HDIM Tier | ${result.customer.hdimTier} |`);
  lines.push(`| Patients | ${result.customer.patientCount.toLocaleString()} |`);
  lines.push(`| Providers | ${result.customer.providerCount} |`);
  lines.push(`| Sites | ${result.customer.siteCount} |`);
  lines.push('');

  // Complexity
  lines.push('## Complexity Assessment');
  lines.push('');
  lines.push(`**Score:** ${result.complexityScore}/100`);
  lines.push(`**Level:** ${result.complexityLevel.toUpperCase()}`);
  lines.push('');

  // Timeline Summary
  lines.push('## Timeline Summary');
  lines.push('');
  lines.push(`**Estimated Duration:** ${result.totalDays.min}-${result.totalDays.max} business days (~${Math.ceil(result.totalDays.min / 5)}-${Math.ceil(result.totalDays.max / 5)} weeks)`);
  if (result.estimatedGoLiveDate) {
    lines.push(`**Go-Live Window:** ${result.estimatedGoLiveDate.earliest.toLocaleDateString()} - ${result.estimatedGoLiveDate.latest.toLocaleDateString()}`);
  }
  lines.push('');

  // Phase table
  lines.push('## Phase Breakdown');
  lines.push('');
  lines.push('| Phase | Duration | Key Deliverables |');
  lines.push('|-------|----------|------------------|');
  for (const phase of result.phases) {
    const deliverables = phase.deliverables.slice(0, 2).join(', ');
    lines.push(`| ${phase.name} | ${phase.durationDays.min}-${phase.durationDays.max} days | ${deliverables} |`);
  }
  lines.push('');

  // Detailed phases
  lines.push('## Detailed Phase Plans');
  lines.push('');
  for (const phase of result.phases) {
    lines.push(`### ${phase.name}`);
    lines.push('');
    lines.push(`*${phase.description}*`);
    lines.push('');
    lines.push(`**Duration:** ${phase.durationDays.min}-${phase.durationDays.max} days`);
    lines.push('');
    lines.push('**Milestones:**');
    for (const m of phase.milestones) {
      lines.push(`- [ ] ${m}`);
    }
    lines.push('');
    if (phase.deliverables.length > 0) {
      lines.push('**Deliverables:**');
      for (const d of phase.deliverables) {
        lines.push(`- ${d}`);
      }
      lines.push('');
    }
    if (phase.risks.length > 0) {
      lines.push('**Risks:**');
      for (const r of phase.risks) {
        lines.push(`- ⚠️ ${r}`);
      }
      lines.push('');
    }
  }

  // Recommendations
  if (result.recommendations.length > 0) {
    lines.push('## Recommendations');
    lines.push('');
    for (const rec of result.recommendations) {
      lines.push(`- 💡 ${rec}`);
    }
    lines.push('');
  }

  lines.push('---');
  lines.push('*Generated by HDIM Implementation Planner*');

  return lines.join('\n');
}

// ============================================================================
// CLI INTERFACE
// ============================================================================

function printUsage(): void {
  console.log(`
HDIM Implementation Timeline Calculator

Usage:
  npx ts-node timeline-calculator.ts [options]

Options:
  --interactive, -i     Run in interactive mode
  --config <file>       Load customer profile from JSON file
  --output <format>     Output format: text, json, markdown (default: text)
  --start-date <date>   Start date for timeline (YYYY-MM-DD)

  Quick mode options:
  --org-type <type>     Organization type
  --ehr <system>        EHR system
  --method <method>     Integration method
  --patients <count>    Patient count
  --providers <count>   Provider count
  --sites <count>       Site count

Organization Types:
  solo-practice, small-practice, fqhc, rural-hospital,
  small-aco, midsize-aco, large-health-system, ipa

EHR Systems:
  none, athenahealth, epic, cerner, eclinicalworks,
  nextgen, allscripts, meditech, other-modern, legacy

Integration Methods:
  csv-upload, fhir-api, smart-on-fhir, n8n-workflow,
  private-cloud, on-premise

Examples:
  npx ts-node timeline-calculator.ts --interactive
  npx ts-node timeline-calculator.ts --config customer.json --output markdown
  npx ts-node timeline-calculator.ts --org-type small-practice --ehr athenahealth --method fhir-api
`);
}

// Example profiles for quick testing
export const EXAMPLE_PROFILES: Record<string, CustomerProfile> = {
  'solo-practice': {
    organizationType: 'solo-practice',
    organizationName: 'Dr. Martinez Family Medicine',
    ehrSystem: 'none',
    integrationMethod: 'csv-upload',
    itCapability: 'none',
    hdimTier: 'community',
    patientCount: 1200,
    providerCount: 1,
    siteCount: 1,
    dataSourceCount: 1,
    qualityPrograms: ['MIPS'],
    hasExistingFHIR: false,
    hasDedicatedIT: false,
    requiresSOC2: false,
    requiresBAA: true,
    pilotFirst: false,
  },
  'small-practice': {
    organizationType: 'small-practice',
    organizationName: 'Riverside Primary Care',
    ehrSystem: 'athenahealth',
    integrationMethod: 'fhir-api',
    itCapability: 'basic',
    hdimTier: 'professional',
    patientCount: 4500,
    providerCount: 5,
    siteCount: 2,
    dataSourceCount: 2,
    qualityPrograms: ['MIPS'],
    hasExistingFHIR: true,
    hasDedicatedIT: false,
    requiresSOC2: false,
    requiresBAA: true,
    pilotFirst: false,
  },
  'fqhc': {
    organizationType: 'fqhc',
    organizationName: 'Community Health Partners',
    ehrSystem: 'eclinicalworks',
    integrationMethod: 'n8n-workflow',
    itCapability: 'moderate',
    hdimTier: 'enterprise',
    patientCount: 22000,
    providerCount: 35,
    siteCount: 8,
    dataSourceCount: 4,
    qualityPrograms: ['UDS', 'MIPS'],
    hasExistingFHIR: true,
    hasDedicatedIT: true,
    requiresSOC2: false,
    requiresBAA: true,
    pilotFirst: true,
  },
  'midsize-aco': {
    organizationType: 'midsize-aco',
    organizationName: 'Metro Health Alliance',
    ehrSystem: 'epic',
    integrationMethod: 'smart-on-fhir',
    itCapability: 'advanced',
    hdimTier: 'enterprise-plus',
    patientCount: 42000,
    providerCount: 120,
    siteCount: 25,
    dataSourceCount: 6,
    qualityPrograms: ['ACO REACH', 'MIPS'],
    hasExistingFHIR: true,
    hasDedicatedIT: true,
    requiresSOC2: true,
    requiresBAA: true,
    pilotFirst: true,
  },
  'large-health-system': {
    organizationType: 'large-health-system',
    organizationName: 'Regional Medical Center',
    ehrSystem: 'epic',
    integrationMethod: 'private-cloud',
    itCapability: 'advanced',
    hdimTier: 'health-system',
    patientCount: 180000,
    providerCount: 500,
    siteCount: 45,
    dataSourceCount: 10,
    qualityPrograms: ['MIPS', 'ACO REACH', 'Commercial Payer'],
    hasExistingFHIR: true,
    hasDedicatedIT: true,
    requiresSOC2: true,
    requiresBAA: true,
    pilotFirst: true,
  },
};

// Main execution
if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length === 0 || args.includes('--help') || args.includes('-h')) {
    printUsage();
    process.exit(0);
  }

  // Quick demo with example profile
  if (args.includes('--demo')) {
    const profileName = args[args.indexOf('--demo') + 1] || 'small-practice';
    const profile = EXAMPLE_PROFILES[profileName] || EXAMPLE_PROFILES['small-practice'];
    const result = calculateTimeline(profile, new Date());
    console.log(formatTimelineText(result));
    process.exit(0);
  }

  // For full CLI implementation, use readline or a CLI framework
  console.log('Run with --demo <profile> to see example output');
  console.log('Available profiles:', Object.keys(EXAMPLE_PROFILES).join(', '));
}
