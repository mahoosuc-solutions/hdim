import { chromium, devices } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:3100'
const outDir =
  process.env.OUT_DIR ||
  path.resolve(
    process.cwd(),
    'docs',
    'screenshots',
    'value-proofs',
    new Date().toISOString().slice(0, 10)
  )

fs.mkdirSync(outDir, { recursive: true })

async function captureDesktop(page, route, selector, fileName) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' })
  if (selector) {
    const section = page.locator(selector).first()
    await section.waitFor({ state: 'visible', timeout: 15000 })
    await section.scrollIntoViewIfNeeded()
    await section.screenshot({ path: path.join(outDir, fileName) })
    return
  }
  await page.screenshot({
    path: path.join(outDir, fileName),
    fullPage: true,
  })
}

async function main() {
  const browser = await chromium.launch({ headless: true })

  const desktop = await browser.newContext({
    viewport: { width: 1440, height: 1800 },
  })
  const desktopPage = await desktop.newPage()

  await captureDesktop(desktopPage, '/', 'section#main-content', '01-home-hero.png')
  await captureDesktop(desktopPage, '/', 'section#performance', '02-home-performance-evidence.png')
  await captureDesktop(desktopPage, '/', 'section#customers', '03-home-customer-success.png')
  await captureDesktop(
    desktopPage,
    '/solutions/transitions-of-care',
    'main > section:nth-of-type(1)',
    '04-transitions-hero.png'
  )
  await captureDesktop(
    desktopPage,
    '/solutions/transitions-of-care',
    'section#deployment-models',
    '05-transitions-deployment-options.png'
  )
  await captureDesktop(
    desktopPage,
    '/solutions/transitions-of-care',
    'section:has-text("Pilot KPI Set")',
    '06-transitions-kpi-section.png'
  )
  await captureDesktop(
    desktopPage,
    '/research',
    'div:has(h2:has-text("HEDIS Measure Coverage"))',
    '07-research-hedis-coverage-section.png'
  )
  await captureDesktop(
    desktopPage,
    '/sales',
    'section:has-text("Care Transitions Pilot for Post-Discharge Engagement")',
    '08-sales-pilot-offer-section.png'
  )

  const mobile = await browser.newContext(devices['iPhone 12'])
  const mobilePage = await mobile.newPage()
  await captureDesktop(
    mobilePage,
    '/solutions/transitions-of-care',
    'main > section:nth-of-type(1)',
    '09-transitions-mobile-hero.png'
  )

  await desktop.close()
  await mobile.close()
  await browser.close()

  const files = fs
    .readdirSync(outDir)
    .filter((f) => f.endsWith('.png'))
    .sort()

  const indexPath = path.join(outDir, 'README.md')
  const index = [
    '# Value Screenshot Pack',
    '',
    `Base URL: ${baseUrl}`,
    `Captured: ${new Date().toISOString()}`,
    '',
    '## Files',
    ...files.map((file) => `- ${file}`),
    '',
  ].join('\n')

  fs.writeFileSync(indexPath, index, 'utf8')
  console.log(`Saved ${files.length} screenshots to ${outDir}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
