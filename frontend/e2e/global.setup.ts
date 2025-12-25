import { chromium, FullConfig } from '@playwright/test';

const SALES_API_URL = process.env.SALES_API_URL || 'http://localhost:8106/sales-automation';
// Must be a valid UUID format for the backend
const TEST_TENANT_ID = '00000000-0000-0000-0000-000000000001';

/**
 * Global setup for Sales Portal E2E tests
 * Seeds test data via API before tests run
 */
async function globalSetup(config: FullConfig) {
  console.log('Setting up E2E test environment...');

  const browser = await chromium.launch();
  const context = await browser.newContext();
  const request = context.request;

  try {
    // Check if backend is available
    const healthCheck = await request.get(`${SALES_API_URL}/actuator/health`);
    if (!healthCheck.ok()) {
      console.warn('Sales API not available, skipping data seeding');
      await browser.close();
      return;
    }

    console.log('Sales API is available, seeding test data...');

    // Seed test leads
    const testLeads = [
      {
        firstName: 'John',
        lastName: 'TestLead',
        email: 'john.testlead@e2etest.com',
        company: 'Test Healthcare Inc',
        phone: '555-0101',
        source: 'WEBSITE',
        status: 'NEW',
        score: 45,
      },
      {
        firstName: 'Jane',
        lastName: 'QualifiedLead',
        email: 'jane.qualified@e2etest.com',
        company: 'Quality Health Systems',
        phone: '555-0102',
        source: 'ROI_CALCULATOR',
        status: 'QUALIFIED',
        score: 85,
      },
      {
        firstName: 'Bob',
        lastName: 'ContactedLead',
        email: 'bob.contacted@e2etest.com',
        company: 'Regional Medical Center',
        phone: '555-0103',
        source: 'REFERRAL',
        status: 'CONTACTED',
        score: 60,
      },
    ];

    for (const lead of testLeads) {
      try {
        await request.post(`${SALES_API_URL}/api/sales/leads`, {
          headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': TEST_TENANT_ID,
          },
          data: lead,
        });
      } catch (error) {
        console.log(`Lead ${lead.email} may already exist`);
      }
    }

    // Seed test accounts
    const testAccounts = [
      {
        name: 'E2E Test Healthcare System',
        type: 'HEALTH_SYSTEM',
        website: 'https://e2etest.healthcare',
        patientCount: 50000,
        ehrCount: 3,
        state: 'CA',
        city: 'San Francisco',
        stage: 'QUALIFIED',
      },
      {
        name: 'Test ACO Partners',
        type: 'ACO',
        website: 'https://testaco.health',
        patientCount: 25000,
        ehrCount: 2,
        state: 'NY',
        city: 'New York',
        stage: 'DEMO',
      },
    ];

    const accountIds: string[] = [];
    for (const account of testAccounts) {
      try {
        const response = await request.post(`${SALES_API_URL}/api/sales/accounts`, {
          headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': TEST_TENANT_ID,
          },
          data: account,
        });
        if (response.ok()) {
          const data = await response.json();
          accountIds.push(data.id);
        }
      } catch (error) {
        console.log(`Account ${account.name} may already exist`);
      }
    }

    // Seed test opportunities
    if (accountIds.length > 0) {
      const testOpportunities = [
        {
          accountId: accountIds[0],
          name: 'E2E Test Deal - Discovery',
          amount: 75000,
          stage: 'DISCOVERY',
          probability: 20,
          expectedCloseDate: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        },
        {
          accountId: accountIds[0],
          name: 'E2E Test Deal - Demo',
          amount: 150000,
          stage: 'DEMO',
          probability: 40,
          expectedCloseDate: new Date(Date.now() + 45 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        },
        {
          accountId: accountIds[1] || accountIds[0],
          name: 'E2E Test Deal - Proposal',
          amount: 200000,
          stage: 'PROPOSAL',
          probability: 60,
          expectedCloseDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        },
      ];

      for (const opportunity of testOpportunities) {
        try {
          await request.post(`${SALES_API_URL}/api/sales/opportunities`, {
            headers: {
              'Content-Type': 'application/json',
              'X-Tenant-ID': TEST_TENANT_ID,
            },
            data: opportunity,
          });
        } catch (error) {
          console.log(`Opportunity ${opportunity.name} may already exist`);
        }
      }
    }

    // Seed test email sequence
    const testSequence = {
      name: 'E2E Test Nurture Sequence',
      description: 'Test sequence for E2E testing',
      type: 'NURTURE',
      active: true,
      steps: [
        {
          stepOrder: 1,
          delayDays: 0,
          subject: 'Welcome to HDIM',
          bodyText: 'Welcome email body',
        },
        {
          stepOrder: 2,
          delayDays: 3,
          subject: 'Follow up',
          bodyText: 'Follow up email body',
        },
      ],
    };

    try {
      await request.post(`${SALES_API_URL}/api/sales/sequences`, {
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-ID': TEST_TENANT_ID,
        },
        data: testSequence,
      });
    } catch (error) {
      console.log('Sequence may already exist');
    }

    console.log('Test data seeding complete!');
  } catch (error) {
    console.error('Error during global setup:', error);
  } finally {
    await browser.close();
  }
}

export default globalSetup;
