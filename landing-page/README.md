# HDIM Landing Page

A modern, responsive landing page for the HDIM healthcare quality measurement platform. Built with Next.js 15, React 18, TypeScript, and Tailwind CSS, designed for deployment on Vercel.

## Features

- **Interactive Deployment Selector** - Let visitors explore different deployment models with real-time details
- **ROI Calculator** - Calculate financial impact based on organization type, patient volume, and other factors
- **Case Studies** - Showcase real customer success stories with metrics
- **Customer Scenarios** - Display EHR-specific implementation scenarios (Epic, Cerner, Athena, FHIR)
- **Responsive Design** - Mobile-first approach with Tailwind CSS
- **TypeScript** - Full type safety throughout the codebase
- **Performance Optimized** - Image optimization, code splitting, fast initial load

## Project Structure

```
landing-page/
├── src/
│   ├── app/
│   │   ├── layout.tsx          # Root layout with metadata
│   │   ├── page.tsx            # Main landing page
│   │   └── globals.css         # Global styles
│   ├── components/
│   │   ├── layout/
│   │   │   └── Header.tsx      # Navigation header
│   │   ├── hero/
│   │   │   ├── HeroSection.tsx
│   │   │   └── DeploymentSelector.tsx  # Interactive model selector
│   │   ├── roi/
│   │   │   └── ROICalculator.tsx       # Interactive ROI calculator
│   │   ├── scenarios/
│   │   │   └── ScenarioCard.tsx        # Customer scenario cards
│   │   └── social-proof/
│   │       └── CaseStudyCard.tsx       # Case study cards
│   └── lib/
│       ├── constants.ts         # All copy, metrics, pricing data
│       ├── calculations.ts      # ROI calculation logic
│       └── data/
│           └── ehr-details.ts   # EHR integration details
├── package.json
├── tsconfig.json
├── tailwind.config.js
├── next.config.js
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ (recommended 20 LTS)
- npm or yarn

### Installation

1. Navigate to the landing-page directory:
```bash
cd hdim-master/landing-page
```

2. Install dependencies:
```bash
npm install
```

3. Run the development server:
```bash
npm run dev
```

4. Open [http://localhost:3000](http://localhost:3000) in your browser to see the landing page.

## Development

### Making Changes

All marketing copy is centralized in `src/lib/constants.ts` for easy updates:
- `MESSAGING` - All headline and CTA copy
- `DEPLOYMENT_MODELS` - Deployment option details
- `CUSTOMER_SCENARIOS` - ROI examples
- `CUSTOMIZATION_LEVELS` - Feature roadmap
- `PRICING_TIERS` - Pricing information
- `CASE_STUDIES` - Customer success stories
- `FAQ` - Frequently asked questions

### ROI Calculator

The ROI calculator uses `src/lib/calculations.ts`:
- Customize calculation logic for your business model
- Supports different organization types (solo practice, health system, ACO, payer)
- Returns detailed breakdown of quality bonus, labor savings, implementation costs

### Adding Components

Example component structure:
```typescript
'use client';

import React from 'react';

interface MyComponentProps {
  title: string;
}

export default function MyComponent({ title }: MyComponentProps) {
  return (
    <section className="section">
      <div className="container-lg">
        <h2 className="section-title">{title}</h2>
      </div>
    </section>
  );
}
```

## Building for Production

```bash
npm run build
npm start
```

## Deployment to Vercel

### Option 1: Direct from GitHub

1. Push code to GitHub
2. Go to [vercel.com](https://vercel.com)
3. Import your GitHub repository
4. Vercel will auto-detect Next.js and configure appropriately
5. Deploy with one click

### Option 2: Vercel CLI

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Deploy to production
vercel --prod
```

### Environment Variables

Create `.env.local` for development:
```
NEXT_PUBLIC_API_URL=https://api.hdim.example.com
NEXT_PUBLIC_DEMO_FORM_ENDPOINT=/api/demo-request
NEXT_PUBLIC_GOOGLE_ANALYTICS_ID=G_XXXXXXXXXX
```

For production, add these to Vercel project settings.

## Customization

### Update Copy

Edit `src/lib/constants.ts`:
```typescript
export const MESSAGING = {
  HERO_HEADLINE: 'Your headline here',
  // ... more copy
}
```

### Update Pricing

Edit pricing in `src/lib/constants.ts`:
```typescript
export const PRICING_TIERS = [
  {
    name: 'Pilot',
    price: 500,
    // ... tier details
  },
  // ... more tiers
]
```

### Add New Sections

1. Create new component in `src/components/`
2. Import in `src/app/page.tsx`
3. Add to the page layout

### Update Colors

Edit `tailwind.config.js`:
```typescript
theme: {
  extend: {
    colors: {
      primary: {
        500: '#0ea5e9',
        600: '#0284c7',
        // ... more shades
      },
    },
  },
}
```

## Performance

### Lighthouse Targets
- Performance: 95+
- Accessibility: 90+
- Best Practices: 90+
- SEO: 90+

### Optimization Checklist
- [ ] Images optimized (use Next.js Image component)
- [ ] Bundle size analyzed (`npm run build`)
- [ ] Fonts optimized (using next/font)
- [ ] Analytics configured
- [ ] Meta tags set correctly

## Form Submission

For demo requests and contact forms, configure in `.env.local`:
```
NEXT_PUBLIC_DEMO_FORM_ENDPOINT=/api/demo-request
```

Create API route in `src/app/api/demo-request/route.ts`:
```typescript
export async function POST(request: Request) {
  const body = await request.json();
  // Handle form submission
  return Response.json({ success: true });
}
```

## Analytics

Add Google Analytics in `src/app/layout.tsx`:
```typescript
import Script from 'next/script';

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Script strategy="afterInteractive" src={`https://www.googletagmanager.com/gtag/js?id=${process.env.NEXT_PUBLIC_GOOGLE_ANALYTICS_ID}`} />
        <Script strategy="afterInteractive">
          {`window.dataLayer = window.dataLayer || [];
            function gtag(){dataLayer.push(arguments);}
            gtag('js', new Date());
            gtag('config', '${process.env.NEXT_PUBLIC_GOOGLE_ANALYTICS_ID}');`}
        </Script>
      </body>
    </html>
  );
}
```

## Troubleshooting

### Build fails with type errors
```bash
npm run type-check
```

### Tailwind styles not applying
1. Check that file paths are correct in `tailwind.config.js`
2. Run `npm run build` to verify
3. Clear `.next` folder: `rm -rf .next`

### Images not loading
Use Next.js Image component:
```typescript
import Image from 'next/image';

<Image src="/path/to/image.jpg" alt="Description" width={800} height={600} />
```

## Support

For questions about the landing page:
1. Check `VERCEL-LANDING-PAGE-IMPLEMENTATION.md` in `deployment-content/`
2. Review component source code in `src/components/`
3. Check Tailwind CSS documentation: [tailwindcss.com](https://tailwindcss.com)

## Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [React Documentation](https://react.dev)
- [Tailwind CSS](https://tailwindcss.com)
- [Vercel Deployment](https://vercel.com/docs)
- [TypeScript](https://www.typescriptlang.org)

## License

Copyright © 2024 HDIM. All rights reserved.
