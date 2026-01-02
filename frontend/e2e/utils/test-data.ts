/**
 * Test data factories and constants for Sales Portal E2E tests
 */

// Must be a valid UUID format for the backend
export const TEST_TENANT_ID = '00000000-0000-0000-0000-000000000001';

// Lead test data
export const testLeads = {
  newLead: {
    firstName: 'New',
    lastName: 'TestLead',
    email: 'new.testlead@e2etest.com',
    company: 'New Test Company',
    phone: '555-1001',
    source: 'WEBSITE',
    status: 'NEW',
    score: 30,
  },
  qualifiedLead: {
    firstName: 'Jane',
    lastName: 'QualifiedLead',
    email: 'jane.qualified@e2etest.com',
    company: 'Quality Health Systems',
    phone: '555-0102',
    source: 'ROI_CALCULATOR',
    status: 'QUALIFIED',
    score: 85,
  },
  contactedLead: {
    firstName: 'Bob',
    lastName: 'ContactedLead',
    email: 'bob.contacted@e2etest.com',
    company: 'Regional Medical Center',
    phone: '555-0103',
    source: 'REFERRAL',
    status: 'CONTACTED',
    score: 60,
  },
  convertedLead: {
    firstName: 'Converted',
    lastName: 'Lead',
    email: 'converted.lead@e2etest.com',
    company: 'Converted Corp',
    phone: '555-1002',
    source: 'CONFERENCE',
    status: 'CONVERTED',
    score: 90,
  },
};

// Account test data
export const testAccounts = {
  healthSystem: {
    name: 'E2E Test Healthcare System',
    type: 'HEALTH_SYSTEM',
    website: 'https://e2etest.healthcare',
    patientCount: 50000,
    ehrCount: 3,
    state: 'CA',
    city: 'San Francisco',
    stage: 'QUALIFIED',
  },
  aco: {
    name: 'Test ACO Partners',
    type: 'ACO',
    website: 'https://testaco.health',
    patientCount: 25000,
    ehrCount: 2,
    state: 'NY',
    city: 'New York',
    stage: 'DEMO',
  },
  payer: {
    name: 'Test Payer Corp',
    type: 'PAYER',
    website: 'https://testpayer.com',
    patientCount: 100000,
    ehrCount: 5,
    state: 'TX',
    city: 'Houston',
    stage: 'PROPOSAL',
  },
};

// Opportunity test data
export const testOpportunities = {
  discoveryDeal: {
    name: 'E2E Test Deal - Discovery',
    amount: 75000,
    stage: 'DISCOVERY',
    probability: 20,
  },
  demoDeal: {
    name: 'E2E Test Deal - Demo',
    amount: 150000,
    stage: 'DEMO',
    probability: 40,
  },
  proposalDeal: {
    name: 'E2E Test Deal - Proposal',
    amount: 200000,
    stage: 'PROPOSAL',
    probability: 60,
  },
  negotiationDeal: {
    name: 'E2E Test Deal - Negotiation',
    amount: 300000,
    stage: 'NEGOTIATION',
    probability: 80,
  },
  atRiskDeal: {
    name: 'E2E Test Deal - At Risk',
    amount: 100000,
    stage: 'DEMO',
    probability: 25,
  },
};

// Email sequence test data
export const testSequences = {
  nurture: {
    name: 'E2E Test Nurture Sequence',
    description: 'Test nurture sequence for E2E testing',
    type: 'NURTURE',
    active: true,
    steps: [
      { stepOrder: 1, delayDays: 0, subject: 'Welcome', bodyText: 'Welcome email' },
      { stepOrder: 2, delayDays: 3, subject: 'Follow up', bodyText: 'Follow up email' },
    ],
  },
  followUp: {
    name: 'E2E Test Follow-up Sequence',
    description: 'Test follow-up sequence',
    type: 'FOLLOW_UP',
    active: false,
    steps: [
      { stepOrder: 1, delayDays: 1, subject: 'Checking in', bodyText: 'Check in email' },
    ],
  },
};

// Enum values for testing
export const leadStatuses = ['NEW', 'CONTACTED', 'QUALIFIED', 'UNQUALIFIED', 'CONVERTED', 'LOST'];
export const leadSources = ['WEBSITE', 'ROI_CALCULATOR', 'REFERRAL', 'CONFERENCE', 'COLD_OUTREACH'];
export const opportunityStages = ['DISCOVERY', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST'];
export const accountStages = ['PROSPECT', 'QUALIFIED', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST'];
export const accountTypes = ['ACO', 'HEALTH_SYSTEM', 'PAYER', 'HIE', 'FQHC', 'CLINIC', 'OTHER'];
export const sequenceTypes = ['NURTURE', 'FOLLOW_UP', 'ONBOARDING', 'RE_ENGAGEMENT', 'WELCOME'];

// Helper functions
export function generateUniqueEmail(prefix: string): string {
  const timestamp = Date.now();
  return `${prefix}.${timestamp}@e2etest.com`;
}

export function getExpectedCloseDate(daysFromNow: number): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  return date.toISOString().split('T')[0];
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatNumber(num: number): string {
  return new Intl.NumberFormat('en-US').format(num);
}
