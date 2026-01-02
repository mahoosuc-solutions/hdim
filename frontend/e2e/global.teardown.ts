import { chromium, FullConfig } from '@playwright/test';

const SALES_API_URL = process.env.SALES_API_URL || 'http://localhost:8106/sales-automation';
// Must be a valid UUID format for the backend
const TEST_TENANT_ID = '00000000-0000-0000-0000-000000000001';

/**
 * Global teardown for Sales Portal E2E tests
 * Cleans up test data after tests complete
 */
async function globalTeardown(config: FullConfig) {
  console.log('Cleaning up E2E test environment...');

  const browser = await chromium.launch();
  const context = await browser.newContext();
  const request = context.request;

  try {
    // Check if backend is available
    const healthCheck = await request.get(`${SALES_API_URL}/actuator/health`);
    if (!healthCheck.ok()) {
      console.warn('Sales API not available, skipping cleanup');
      await browser.close();
      return;
    }

    // Get and delete test leads
    const leadsResponse = await request.get(`${SALES_API_URL}/api/sales/leads`, {
      headers: { 'X-Tenant-ID': TEST_TENANT_ID },
    });

    if (leadsResponse.ok()) {
      const leadsData = await leadsResponse.json();
      const leads = leadsData.content || [];
      for (const lead of leads) {
        if (lead.email?.includes('e2etest.com')) {
          await request.delete(`${SALES_API_URL}/api/sales/leads/${lead.id}`, {
            headers: { 'X-Tenant-ID': TEST_TENANT_ID },
          });
        }
      }
    }

    // Get and delete test opportunities
    const oppsResponse = await request.get(`${SALES_API_URL}/api/sales/opportunities`, {
      headers: { 'X-Tenant-ID': TEST_TENANT_ID },
    });

    if (oppsResponse.ok()) {
      const oppsData = await oppsResponse.json();
      const opportunities = oppsData.content || [];
      for (const opp of opportunities) {
        if (opp.name?.startsWith('E2E Test')) {
          await request.delete(`${SALES_API_URL}/api/sales/opportunities/${opp.id}`, {
            headers: { 'X-Tenant-ID': TEST_TENANT_ID },
          });
        }
      }
    }

    // Get and delete test accounts
    const accountsResponse = await request.get(`${SALES_API_URL}/api/sales/accounts`, {
      headers: { 'X-Tenant-ID': TEST_TENANT_ID },
    });

    if (accountsResponse.ok()) {
      const accountsData = await accountsResponse.json();
      const accounts = accountsData.content || [];
      for (const account of accounts) {
        if (account.name?.startsWith('E2E Test') || account.name?.startsWith('Test ACO')) {
          await request.delete(`${SALES_API_URL}/api/sales/accounts/${account.id}`, {
            headers: { 'X-Tenant-ID': TEST_TENANT_ID },
          });
        }
      }
    }

    // Get and delete test sequences
    const seqResponse = await request.get(`${SALES_API_URL}/api/sales/sequences`, {
      headers: { 'X-Tenant-ID': TEST_TENANT_ID },
    });

    if (seqResponse.ok()) {
      const seqData = await seqResponse.json();
      const sequences = seqData.content || [];
      for (const seq of sequences) {
        if (seq.name?.startsWith('E2E Test')) {
          await request.delete(`${SALES_API_URL}/api/sales/sequences/${seq.id}`, {
            headers: { 'X-Tenant-ID': TEST_TENANT_ID },
          });
        }
      }
    }

    console.log('Cleanup complete!');
  } catch (error) {
    console.error('Error during cleanup:', error);
  } finally {
    await browser.close();
  }
}

export default globalTeardown;
