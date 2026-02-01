import { test, expect } from '@playwright/test';
import { WebSocketServer, WebSocket } from 'ws';

test.describe('HDIM Demo Walkthrough', () => {
  let wss: WebSocketServer | null = null;
  const clients = new Set<WebSocket>();

  test.beforeAll(async () => {
    wss = new WebSocketServer({ port: 7070 });
    wss.on('connection', (socket) => {
      clients.add(socket);
      socket.on('close', () => clients.delete(socket));
    });
  });

  test.afterAll(async () => {
    clients.forEach((client) => client.close());
    wss?.close();
  });

  const sendStep = (stepId: string) => {
    const payload = JSON.stringify({ type: 'storyboard.step', stepId });
    clients.forEach((client) => {
      if (client.readyState === 1) {
        client.send(payload);
      }
    });
  };

  test('follows the demo storyboard with screenshots', async ({ page }) => {
    test.setTimeout(10 * 60 * 1000);

    await page.goto('/login?demo=true&storyboard=1&storyboardWs=ws://localhost:7070', {
      waitUntil: 'domcontentloaded',
    });

    const demoButton = page.locator('.demo-button');
    if (await demoButton.count()) {
      await demoButton.first().click();
    } else {
      await page.fill('input[formcontrolname="username"]', 'demo_admin');
      await page.fill('input[formcontrolname="password"]', 'demo123');
      await page.click('button[type="submit"]');
    }

    await page.waitForURL('**/dashboard', { timeout: 60000 });
    sendStep('dashboard-overview');
    await page.waitForTimeout(1200);
    await page.screenshot({
      path: test.info().outputPath('demo-walkthrough/01-dashboard.png'),
      fullPage: true,
    });

    await page.goto('/care-gaps', { waitUntil: 'domcontentloaded' });
    sendStep('hero-patient');
    await page.waitForTimeout(1200);
    const mariaRow = page.getByText('Maria Garcia').first();
    if (await mariaRow.count()) {
      await mariaRow.click();
    }
    await page.screenshot({
      path: test.info().outputPath('demo-walkthrough/02-care-gaps.png'),
      fullPage: true,
    });

    sendStep('intervention');
    const logIntervention = page.getByRole('button', { name: /log intervention/i });
    if (await logIntervention.count()) {
      await logIntervention.first().click();
      await page.waitForTimeout(800);
    }
    await page.screenshot({
      path: test.info().outputPath('demo-walkthrough/03-intervention.png'),
      fullPage: true,
    });

    await page.goto('/results', { waitUntil: 'domcontentloaded' });
    sendStep('quality-impact');
    await page.waitForTimeout(1200);
    await page.screenshot({
      path: test.info().outputPath('demo-walkthrough/04-quality-impact.png'),
      fullPage: true,
    });

    await page.goto('/visualization/live-monitor', { waitUntil: 'domcontentloaded' });
    sendStep('technical');
    await page.waitForTimeout(1200);
    await page.screenshot({
      path: test.info().outputPath('demo-walkthrough/05-live-monitor.png'),
      fullPage: true,
    });

    await page.goto('/ai-assistant', { waitUntil: 'domcontentloaded' });
    sendStep('summary');
    await page.waitForTimeout(1200);
    await page.screenshot({
      path: test.info().outputPath('demo-walkthrough/06-ai-assistant.png'),
      fullPage: true,
    });

    expect(true).toBe(true);
  });
});
