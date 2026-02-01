/* eslint-disable react-hooks/rules-of-hooks */
// Playwright's 'use' callback is not a React Hook - disable React hooks linting for E2E test fixtures

import { test as base } from '@playwright/test';
import { SalesApiClient } from '../utils/api-helpers';
import { SalesPage } from '../pages/sales-page';
import { LeadsPage } from '../pages/leads-page';
import { PipelinePage } from '../pages/pipeline-page';
import { AccountsPage } from '../pages/accounts-page';
import { SequencesPage } from '../pages/sequences-page';

/**
 * Extended test fixtures for Sales Portal E2E tests
 */
type SalesFixtures = {
  salesPage: SalesPage;
  leadsPage: LeadsPage;
  pipelinePage: PipelinePage;
  accountsPage: AccountsPage;
  sequencesPage: SequencesPage;
  apiClient: SalesApiClient;
};

export const test = base.extend<SalesFixtures>({
  salesPage: async ({ page }, use) => {
    const salesPage = new SalesPage(page);
    await use(salesPage);
  },

  leadsPage: async ({ page }, use) => {
    const leadsPage = new LeadsPage(page);
    await use(leadsPage);
  },

  pipelinePage: async ({ page }, use) => {
    const pipelinePage = new PipelinePage(page);
    await use(pipelinePage);
  },

  accountsPage: async ({ page }, use) => {
    const accountsPage = new AccountsPage(page);
    await use(accountsPage);
  },

  sequencesPage: async ({ page }, use) => {
    const sequencesPage = new SequencesPage(page);
    await use(sequencesPage);
  },

  apiClient: async ({ request }, use) => {
    const apiClient = new SalesApiClient(request);
    await use(apiClient);
  },
});

export { expect } from '@playwright/test';
