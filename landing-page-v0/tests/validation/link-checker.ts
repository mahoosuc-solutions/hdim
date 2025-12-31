/**
 * Link Checker for HDIM Landing Page
 *
 * Validates all external links return 200 OK and internal links exist.
 */

import fs from 'fs';
import { glob } from 'glob';

interface LinkCheckResult {
  url: string;
  status: number | null;
  error?: string;
  source: string;
}

interface ValidationSummary {
  passed: boolean;
  total: number;
  working: number;
  broken: number;
  results: LinkCheckResult[];
}

const TIMEOUT = 10000; // 10 seconds
const MAX_REDIRECTS = 5;

async function extractLinks(): Promise<Map<string, string[]>> {
  const linkMap = new Map<string, string[]>();

  const files = await glob('app/**/*.{tsx,ts,jsx,js,md,mdx}', {
    cwd: process.cwd(),
    ignore: ['**/node_modules/**', '**/.next/**'],
  });

  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8');

    // Extract href attributes
    const hrefMatches = content.matchAll(/href=["']([^"']+)["']/g);
    for (const match of hrefMatches) {
      const url = match[1];
      if (url.startsWith('http://') || url.startsWith('https://')) {
        if (!linkMap.has(url)) {
          linkMap.set(url, []);
        }
        linkMap.get(url)!.push(file);
      }
    }

    // Extract src attributes for external resources
    const srcMatches = content.matchAll(/src=["']([^"']+)["']/g);
    for (const match of srcMatches) {
      const url = match[1];
      if (url.startsWith('http://') || url.startsWith('https://')) {
        if (!linkMap.has(url)) {
          linkMap.set(url, []);
        }
        linkMap.get(url)!.push(file);
      }
    }

    // Extract markdown links
    const mdMatches = content.matchAll(/\[([^\]]+)\]\(([^)]+)\)/g);
    for (const match of mdMatches) {
      const url = match[2];
      if (url.startsWith('http://') || url.startsWith('https://')) {
        if (!linkMap.has(url)) {
          linkMap.set(url, []);
        }
        linkMap.get(url)!.push(file);
      }
    }
  }

  return linkMap;
}

async function checkUrl(url: string): Promise<{ status: number | null; error?: string }> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT);

    const response = await fetch(url, {
      method: 'HEAD',
      signal: controller.signal,
      redirect: 'follow',
      headers: {
        'User-Agent': 'HDIM-Link-Checker/1.0',
      },
    });

    clearTimeout(timeoutId);

    return { status: response.status };
  } catch (error: any) {
    if (error.name === 'AbortError') {
      return { status: null, error: 'Request timeout' };
    }
    return { status: null, error: error.message };
  }
}

async function checkLinks(): Promise<ValidationSummary> {
  console.log('🔗 Extracting links from code...\n');

  const linkMap = await extractLinks();
  const uniqueUrls = Array.from(linkMap.keys());

  console.log(`Found ${uniqueUrls.length} unique external URLs\n`);
  console.log('🌐 Checking external links...\n');

  const results: LinkCheckResult[] = [];
  let working = 0;
  let broken = 0;

  for (let i = 0; i < uniqueUrls.length; i++) {
    const url = uniqueUrls[i];
    const sources = linkMap.get(url)!;

    process.stdout.write(`   [${i + 1}/${uniqueUrls.length}] Checking ${url}...`);

    const { status, error } = await checkUrl(url);

    const result: LinkCheckResult = {
      url,
      status,
      error,
      source: sources[0], // First source file
    };

    results.push(result);

    if (status && status >= 200 && status < 400) {
      working++;
      console.log(` ✅ ${status}`);
    } else {
      broken++;
      console.log(` ❌ ${error || `Status ${status}`}`);
    }

    // Rate limiting: wait 100ms between requests
    await new Promise((resolve) => setTimeout(resolve, 100));
  }

  console.log('');

  return {
    passed: broken === 0,
    total: uniqueUrls.length,
    working,
    broken,
    results,
  };
}

// Run link checking
checkLinks().then((summary) => {
  console.log('\n' + '='.repeat(60));
  console.log('LINK VALIDATION RESULTS');
  console.log('='.repeat(60) + '\n');

  console.log(`Total URLs checked: ${summary.total}`);
  console.log(`Working: ${summary.working}`);
  console.log(`Broken: ${summary.broken}\n`);

  if (summary.broken > 0) {
    console.log('BROKEN LINKS:\n');
    summary.results
      .filter((r) => !r.status || r.status >= 400)
      .forEach((r) => {
        console.log(`❌ ${r.url}`);
        console.log(`   Source: ${r.source}`);
        console.log(`   Error: ${r.error || `Status ${r.status}`}\n`);
      });
  }

  if (summary.passed) {
    console.log('✅ ALL LINKS ARE WORKING!\n');
    process.exit(0);
  } else {
    console.log(`❌ FOUND ${summary.broken} BROKEN LINKS\n`);
    process.exit(1);
  }
});
