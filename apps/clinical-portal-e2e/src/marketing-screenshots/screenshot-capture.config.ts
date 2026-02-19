import { Page } from '@playwright/test';
import path from 'path';

export const SCREENSHOT_CONFIG = {
  viewport: { width: 1920, height: 1080 },
  waitAfterNavigation: 2500,
  waitAfterInteraction: 1000,
  fullPage: false, // Viewport-sized for video consistency
};

const SCREENSHOTS_BASE_DIR = path.resolve(
  __dirname,
  '../../../../landing-page-v0/remotion/public/screenshots'
);

/**
 * Capture a marketing screenshot with standardized naming and timing.
 *
 * Screenshots are saved directly into the Remotion public directory
 * so they can be referenced via staticFile() without a copy step.
 *
 * @param page       Playwright page object
 * @param role       Role slug (e.g., 'care-manager', 'cmo')
 * @param step       Two-digit step number (01-10)
 * @param description Kebab-case description for the filename
 */
export async function captureScreenshot(
  page: Page,
  role: string,
  step: string,
  description: string
): Promise<string> {
  // Wait for any loading spinners to clear
  await waitForLoadingComplete(page);

  // Brief pause for animations to settle
  await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);

  const filename = `${role}-${step}-${description}.png`;
  const dir = path.join(SCREENSHOTS_BASE_DIR, role);
  const fullPath = path.join(dir, filename);

  await page.screenshot({
    path: fullPath,
    fullPage: SCREENSHOT_CONFIG.fullPage,
  });

  // Return the path relative to remotion/public/ for staticFile()
  return `screenshots/${role}/${filename}`;
}

/**
 * Wait for loading indicators to disappear and dismiss overlays before capturing.
 */
async function waitForLoadingComplete(page: Page): Promise<void> {
  const spinners = page.locator(
    '.loading-overlay, mat-progress-spinner, mat-spinner, .mat-progress-spinner, .mat-mdc-progress-spinner'
  );

  try {
    // Wait up to 5 seconds for spinners to vanish
    await spinners.first().waitFor({ state: 'hidden', timeout: 5000 });
  } catch {
    // No spinners visible — that's fine
  }

  // Also wait for skeleton loaders
  const skeletons = page.locator('.skeleton, .placeholder-glow, [class*="skeleton"]');
  try {
    await skeletons.first().waitFor({ state: 'hidden', timeout: 3000 });
  } catch {
    // No skeletons — fine
  }

  // Dismiss any guided tour overlays, CDK dialogs, snackbars, or banners
  await dismissOverlays(page);
}

/**
 * Remove all overlay elements that obstruct screenshots:
 * - Guided tour overlay
 * - CDK overlay backdrops and panes (Angular Material dialogs)
 * - Snackbar notifications
 */
async function dismissOverlays(page: Page): Promise<void> {
  try {
    await page.evaluate(() => {
      // Dismiss guided tour overlay
      document.querySelectorAll('app-tour-overlay, .tour-overlay, .tour-backdrop').forEach(el => el.remove());

      // Dismiss CDK overlays (Angular Material dialogs/modals)
      document.querySelectorAll('.cdk-overlay-backdrop').forEach(el => el.remove());
      document.querySelectorAll('.cdk-overlay-pane').forEach(el => {
        const pane = el as HTMLElement;
        // Remove dialog panes but keep tooltips/menus
        if (pane.querySelector('mat-dialog-container, mat-snack-bar-container, mat-bottom-sheet-container')) {
          el.remove();
        }
      });

      // Dismiss snackbar notifications
      document.querySelectorAll('mat-snack-bar-container, .mat-mdc-snack-bar-container, snack-bar-container').forEach(el => el.remove());

      // Hide any help panel that might be open
      document.querySelectorAll('app-help-panel, .help-panel-overlay').forEach(el => {
        (el as HTMLElement).style.display = 'none';
      });
    });
  } catch {
    // Page might have navigated — safe to ignore
  }
}

/**
 * Navigate and capture in one call.
 * Sets up auth if not already done, navigates, waits, and screenshots.
 */
export async function navigateAndCapture(
  page: Page,
  url: string,
  role: string,
  step: string,
  description: string,
  options?: {
    waitAfter?: number;
    clickSelector?: string;
    hoverSelector?: string;
  }
): Promise<string> {
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterNavigation);

  if (options?.clickSelector) {
    await page.locator(options.clickSelector).first().click();
    await page.waitForTimeout(SCREENSHOT_CONFIG.waitAfterInteraction);
  }

  if (options?.hoverSelector) {
    await page.locator(options.hoverSelector).first().hover();
    await page.waitForTimeout(400);
  }

  if (options?.waitAfter) {
    await page.waitForTimeout(options.waitAfter);
  }

  return captureScreenshot(page, role, step, description);
}
