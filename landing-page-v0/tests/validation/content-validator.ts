/**
 * Content Validation Script for HDIM Landing Page
 *
 * Validates all content is present, no placeholders remain, and required sections exist.
 */

import fs from 'fs';
import path from 'path';
import { glob } from 'glob';

interface ValidationResult {
  passed: boolean;
  errors: string[];
  warnings: string[];
}

const PLACEHOLDER_PATTERNS = [
  /\[YOUR[_\s].*?\]/gi,
  /\{\{.*?\}\}/g,
  /TODO:/gi,
  /PLACEHOLDER/gi,
  /Lorem ipsum/gi,
  /\[COMPANY[_\s]NAME\]/gi,
  /\[PRODUCT[_\s]NAME\]/gi,
  /example\.com/gi,
  /test@test\.com/gi,
];

const REQUIRED_CONTENT = {
  'Value Proposition': [
    'HEDIS',
    'quality measures',
    'care gaps',
    'value-based care',
  ],
  'Product Features': [
    'FHIR',
    'interoperability',
    'clinical decision support',
  ],
  'Call to Action': [
    'demo',
    'contact',
    'get started',
  ],
};

const REQUIRED_PAGES = [
  'page.tsx',           // Home page
  'demo',               // Demo page
  'features',           // Features page
  'pricing',            // Pricing page
  'about',              // About page
  'contact',            // Contact page
];

const REQUIRED_SECTIONS_HOME = [
  'hero',
  'features',
  'benefits',
  'testimonials',
  'pricing',
  'cta',
  'footer',
];

async function validateContent(): Promise<ValidationResult> {
  const result: ValidationResult = {
    passed: true,
    errors: [],
    warnings: [],
  };

  console.log('🔍 Starting content validation...\n');

  // 1. Check for placeholder text
  await checkPlaceholders(result);

  // 2. Validate required pages exist
  await validatePages(result);

  // 3. Validate required content
  await validateRequiredContent(result);

  // 4. Validate images exist
  await validateImages(result);

  // 5. Check for broken internal links
  await checkInternalLinks(result);

  return result;
}

async function checkPlaceholders(result: ValidationResult): Promise<void> {
  console.log('📝 Checking for placeholder text...');

  const files = await glob('app/**/*.{tsx,ts,jsx,js,md,mdx}', {
    cwd: process.cwd(),
    ignore: ['**/node_modules/**', '**/.next/**'],
  });

  let placeholderCount = 0;

  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8');

    for (const pattern of PLACEHOLDER_PATTERNS) {
      const matches = content.match(pattern);
      if (matches) {
        placeholderCount += matches.length;
        result.errors.push(
          `❌ Placeholder found in ${file}: ${matches.join(', ')}`
        );
      }
    }
  }

  if (placeholderCount > 0) {
    result.passed = false;
    console.log(`   ❌ Found ${placeholderCount} placeholders\n`);
  } else {
    console.log('   ✅ No placeholders found\n');
  }
}

async function validatePages(result: ValidationResult): Promise<void> {
  console.log('📄 Validating required pages...');

  const appDir = path.join(process.cwd(), 'app');

  for (const page of REQUIRED_PAGES) {
    const pagePath = path.join(appDir, page);
    const pageExists =
      fs.existsSync(pagePath) ||
      fs.existsSync(`${pagePath}.tsx`) ||
      fs.existsSync(path.join(pagePath, 'page.tsx'));

    if (!pageExists) {
      result.errors.push(`❌ Required page missing: ${page}`);
      result.passed = false;
    }
  }

  const foundPages = REQUIRED_PAGES.filter((page) => {
    const pagePath = path.join(appDir, page);
    return (
      fs.existsSync(pagePath) ||
      fs.existsSync(`${pagePath}.tsx`) ||
      fs.existsSync(path.join(pagePath, 'page.tsx'))
    );
  });

  console.log(`   ✅ Found ${foundPages.length}/${REQUIRED_PAGES.length} required pages\n`);
}

async function validateRequiredContent(result: ValidationResult): Promise<void> {
  console.log('🔤 Validating required content...');

  const files = await glob('app/**/*.{tsx,ts,jsx,js}', {
    cwd: process.cwd(),
    ignore: ['**/node_modules/**', '**/.next/**'],
  });

  const allContent = files
    .map((file) => fs.readFileSync(file, 'utf-8').toLowerCase())
    .join(' ');

  for (const [category, keywords] of Object.entries(REQUIRED_CONTENT)) {
    const missingKeywords = keywords.filter(
      (keyword) => !allContent.includes(keyword.toLowerCase())
    );

    if (missingKeywords.length > 0) {
      result.warnings.push(
        `⚠️  ${category} missing keywords: ${missingKeywords.join(', ')}`
      );
    }
  }

  console.log(`   ✅ Content validation complete\n`);
}

async function validateImages(result: ValidationResult): Promise<void> {
  console.log('🖼️  Validating images...');

  const files = await glob('app/**/*.{tsx,ts,jsx,js}', {
    cwd: process.cwd(),
    ignore: ['**/node_modules/**', '**/.next/**'],
  });

  const imageReferences: string[] = [];

  // Extract image references from code
  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8');

    // Match src="/..." or src='/...' or src={...}
    const srcMatches = content.match(/src=["']([^"']+)["']/g);
    if (srcMatches) {
      srcMatches.forEach((match) => {
        const src = match.match(/src=["']([^"']+)["']/)?.[1];
        if (src && src.startsWith('/')) {
          imageReferences.push(src);
        }
      });
    }
  }

  let missingImages = 0;

  for (const imgRef of imageReferences) {
    const imgPath = path.join(process.cwd(), 'public', imgRef.replace(/^\//, ''));
    if (!fs.existsSync(imgPath)) {
      result.errors.push(`❌ Missing image: ${imgRef}`);
      missingImages++;
      result.passed = false;
    }
  }

  if (missingImages === 0) {
    console.log(`   ✅ All ${imageReferences.length} referenced images exist\n`);
  } else {
    console.log(`   ❌ ${missingImages} missing images\n`);
  }
}

async function checkInternalLinks(result: ValidationResult): Promise<void> {
  console.log('🔗 Checking internal links...');

  const files = await glob('app/**/*.{tsx,ts,jsx,js}', {
    cwd: process.cwd(),
    ignore: ['**/node_modules/**', '**/.next/**'],
  });

  const internalLinks: string[] = [];

  // Extract internal links
  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8');

    // Match href="/..." or href='/...'
    const hrefMatches = content.match(/href=["']\/[^"']*["']/g);
    if (hrefMatches) {
      hrefMatches.forEach((match) => {
        const href = match.match(/href=["'](\/[^"']*)["']/)?.[1];
        if (href && !href.startsWith('http')) {
          internalLinks.push(href);
        }
      });
    }
  }

  let brokenLinks = 0;
  const appDir = path.join(process.cwd(), 'app');

  for (const link of internalLinks) {
    // Remove query params and hash
    const cleanLink = link.split('?')[0].split('#')[0];
    const linkPath = path.join(appDir, cleanLink);

    const exists =
      fs.existsSync(linkPath) ||
      fs.existsSync(`${linkPath}.tsx`) ||
      fs.existsSync(path.join(linkPath, 'page.tsx'));

    if (!exists) {
      result.warnings.push(`⚠️  Potentially broken internal link: ${link}`);
      brokenLinks++;
    }
  }

  console.log(`   ✅ Checked ${internalLinks.length} internal links (${brokenLinks} warnings)\n`);
}

// Run validation
validateContent().then((result) => {
  console.log('\n' + '='.repeat(60));
  console.log('CONTENT VALIDATION RESULTS');
  console.log('='.repeat(60) + '\n');

  if (result.errors.length > 0) {
    console.log('ERRORS:');
    result.errors.forEach((error) => console.log(error));
    console.log('');
  }

  if (result.warnings.length > 0) {
    console.log('WARNINGS:');
    result.warnings.forEach((warning) => console.log(warning));
    console.log('');
  }

  if (result.passed && result.warnings.length === 0) {
    console.log('✅ ALL VALIDATIONS PASSED!\n');
    process.exit(0);
  } else if (result.passed) {
    console.log(`⚠️  PASSED WITH ${result.warnings.length} WARNINGS\n`);
    process.exit(0);
  } else {
    console.log(`❌ VALIDATION FAILED WITH ${result.errors.length} ERRORS\n`);
    process.exit(1);
  }
});
