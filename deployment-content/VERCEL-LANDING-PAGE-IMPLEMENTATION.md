# HDIM Vercel Landing Page Implementation Guide

Complete step-by-step guide to building and deploying the HDIM landing page showcasing deployment flexibility, customization, and customer ROI.

---

## Phase 1: Setup & Project Structure

### Step 1: Create Project in v0.dev

```bash
# Option A: Use v0.dev web interface
1. Go to v0.dev
2. Create new project
3. Name: "hdim-landing-page"
4. Template: Next.js 16 + React 18 + Tailwind CSS

# Option B: Clone existing & customize
1. Use existing /hdim-admin-portal/landing-page-v0 as base
2. Customize components for HDIM product
```

### Step 2: Project Structure

```
hdim-landing-page/
├── app/
│   ├── layout.tsx                    # Root layout + nav
│   ├── page.tsx                      # Main landing page
│   ├── api/
│   │   ├── demo-request.ts          # Form submission
│   │   └── roi-calculator.ts        # ROI calculation backend
│   └── scenarios/
│       ├── [ehr]/page.tsx           # EHR-specific detail pages
│       └── layout.tsx               # Scenario layout
│
├── components/
│   ├── layout/
│   │   ├── Header.tsx               # Navigation + CTAs
│   │   ├── Footer.tsx               # Links, legal
│   │   └── MobileMenu.tsx           # Mobile navigation
│   │
│   ├── hero/
│   │   ├── HeroSection.tsx          # Main hero
│   │   ├── DeploymentSelector.tsx   # Interactive switcher
│   │   └── HeroAnimation.tsx        # Animated deployment models
│   │
│   ├── problem/
│   │   ├── ProblemSection.tsx       # Pain points
│   │   ├── ProblemCard.tsx          # Individual problem
│   │   └── StatisticsGrid.tsx       # Research-backed stats
│   │
│   ├── solution/
│   │   ├── SolutionSection.tsx      # Core solution
│   │   ├── GatewayArchitecture.tsx  # Architecture visual
│   │   ├── ArchitectureDiagram.tsx  # SVG diagram
│   │   └── BenefitsCard.tsx         # Individual benefit
│   │
│   ├── deployment/
│   │   ├── DeploymentModelsSection.tsx
│   │   ├── ModelCard.tsx            # Pilot/Growth/Enterprise
│   │   ├── ComparisonMatrix.tsx     # Side-by-side comparison
│   │   ├── CostCalculator.tsx       # Interactive cost tool
│   │   └── MigrationPath.tsx        # Upgrade visualization
│   │
│   ├── scenarios/
│   │   ├── ScenariosSection.tsx     # All scenarios intro
│   │   ├── ScenarioCard.tsx         # Individual scenario
│   │   ├── ScenarioTabs.tsx         # Tabbed scenario selector
│   │   └── OutcomeMetrics.tsx       # Results visualization
│   │
│   ├── customization/
│   │   ├── CustomizationSection.tsx # 5-level roadmap
│   │   ├── LevelCard.tsx            # Individual level
│   │   ├── LevelComparison.tsx      # Timeline/cost comparison
│   │   └── ProgressTimeline.tsx     # Expansion journey
│   │
│   ├── features/
│   │   ├── HowItWorksSection.tsx    # 4-step workflow
│   │   ├── WorkflowStep.tsx         # Individual step
│   │   ├── FeatureComparison.tsx    # vs alternatives matrix
│   │   └── ComparisonTable.tsx      # Detailed comparison
│   │
│   ├── social-proof/
│   │   ├── CaseStudiesSection.tsx   # Case study intro
│   │   ├── CaseStudyCard.tsx        # Individual case study
│   │   ├── CustomerQuote.tsx        # Pull quote component
│   │   ├── MetricsShowcase.tsx      # Key stats display
│   │   └── CustomerLogos.tsx        # Organization logos
│   │
│   ├── roi/
│   │   ├── ROICalculator.tsx        # Full calculator
│   │   ├── ROIInputs.tsx            # Form inputs
│   │   ├── ROIOutput.tsx            # Results display
│   │   ├── ROIChart.tsx             # Result visualization
│   │   └── paybackPeriod.tsx        # Payback visualization
│   │
│   ├── trust/
│   │   ├── TrustSection.tsx         # Security/compliance
│   │   ├── CertificationBadges.tsx  # Certification display
│   │   ├── ComplianceChecklist.tsx  # Feature checklist
│   │   └── DataResidency.tsx        # Data location options
│   │
│   ├── pricing/
│   │   ├── PricingSection.tsx       # Pricing intro
│   │   ├── PricingCard.tsx          # Individual pricing tier
│   │   ├── PricingComparison.tsx    # Feature comparison
│   │   ├── PricingToggle.tsx        # Annual/monthly toggle
│   │   └── AddOns.tsx               # Add-on pricing
│   │
│   ├── cta/
│   │   ├── DemoForm.tsx             # Demo booking form
│   │   ├── FormField.tsx            # Form input component
│   │   ├── SuccessMessage.tsx       # Submission confirmation
│   │   ├── CTAButton.tsx            # Button variants
│   │   └── NewsletterSignup.tsx     # Newsletter CTA
│   │
│   └── common/
│       ├── Section.tsx              # Reusable section wrapper
│       ├── Container.tsx            # Width/padding container
│       ├── SectionTitle.tsx         # Heading component
│       ├── SectionSubtitle.tsx      # Subheading component
│       ├── Divider.tsx              # Section divider
│       ├── Badge.tsx                # Label/badge component
│       └── AnimationWrapper.tsx     # Reusable animation container
│
├── lib/
│   ├── constants.ts                 # Copy, metrics, pricing
│   ├── utils.ts                     # Helper functions
│   ├── calculations.ts              # ROI calculator logic
│   ├── validation.ts                # Form validation
│   │
│   └── data/
│       ├── scenarios.ts             # Scenario data
│       ├── case-studies.ts          # Case study details
│       ├── pricing.ts               # Pricing tiers
│       ├── ehr-details.ts           # EHR integration info
│       ├── metrics.ts               # Statistics & metrics
│       ├── faqs.ts                  # FAQ content
│       └── testimonials.ts          # Customer quotes
│
├── styles/
│   ├── globals.css                  # Tailwind + custom
│   ├── animations.css               # Animation definitions
│   └── typography.css               # Font/text styles
│
├── public/
│   ├── images/
│   │   ├── architecture/             # Architecture diagrams (SVG/PNG)
│   │   ├── deployment-models/        # Deployment visuals
│   │   ├── case-studies/             # Customer screenshots
│   │   ├── ehr-logos/                # EHR vendor logos
│   │   ├── icons/                    # Feature icons
│   │   └── hero/                     # Hero section images
│   │
│   └── documents/
│       ├── case-studies.pdf
│       ├── integration-guide.pdf
│       ├── customization-roadmap.pdf
│       └── technical-architecture.pdf
│
├── hooks/
│   ├── useROICalculator.ts          # ROI calculation logic
│   ├── useDeploymentSelector.ts     # Deployment model state
│   ├── useFormState.ts              # Form submission state
│   └── useAnimation.ts              # Reusable animations
│
├── types/
│   ├── index.ts                     # Shared types
│   ├── scenario.ts                  # Scenario types
│   ├── pricing.ts                   # Pricing types
│   ├── roi.ts                       # ROI calculator types
│   └── api.ts                       # API response types
│
├── package.json                      # Dependencies
├── tailwind.config.ts               # Tailwind config
├── next.config.ts                   # Next.js config
├── tsconfig.json                    # TypeScript config
└── .env.local                       # Environment variables
```

---

## Phase 2: Component Implementation

### Hero Section (Highest Priority)

**File: `components/hero/HeroSection.tsx`**

```tsx
'use client';

import React, { useState } from 'react';
import DeploymentSelector from './DeploymentSelector';
import HeroAnimation from './HeroAnimation';

export default function HeroSection() {
  const [selectedModel, setSelectedModel] = useState('pilot');

  return (
    <section className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 pt-20 pb-16">
      <div className="max-w-6xl mx-auto px-4">
        {/* Headlines */}
        <div className="text-center mb-12">
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
            Calculate Clinical Value in Real-Time,<br />
            <span className="text-indigo-600">Your Way</span>
          </h1>
          <p className="text-xl text-gray-700 max-w-3xl mx-auto mb-8">
            FHIR-native quality measures without data copying. Deploy anywhere—from single-node to enterprise.
            Get real answers in less time with full customization to your needs.
          </p>
        </div>

        {/* Deployment Selector */}
        <DeploymentSelector
          selected={selectedModel}
          onSelect={setSelectedModel}
        />

        {/* Animated Deployment Models */}
        <HeroAnimation model={selectedModel} />

        {/* CTAs */}
        <div className="flex gap-4 justify-center mt-12">
          <button className="px-8 py-3 bg-indigo-600 text-white rounded-lg font-semibold hover:bg-indigo-700">
            Schedule Demo
          </button>
          <button className="px-8 py-3 border-2 border-indigo-600 text-indigo-600 rounded-lg font-semibold hover:bg-indigo-50">
            Learn More
          </button>
        </div>

        {/* Social Proof */}
        <div className="mt-16 text-center">
          <p className="text-gray-600 mb-4">Trusted by healthcare organizations</p>
          {/* Customer logos here */}
        </div>
      </div>
    </section>
  );
}
```

### Deployment Selector Component

**File: `components/hero/DeploymentSelector.tsx`**

```tsx
'use client';

interface DeploymentOption {
  id: string;
  name: string;
  description: string;
  patients: string;
  timeline: string;
  cost: string;
}

const options: DeploymentOption[] = [
  {
    id: 'pilot',
    name: 'Pilot',
    description: 'Single-node Docker deployment',
    patients: 'Up to 50K',
    timeline: '2-3 weeks',
    cost: '$500/month'
  },
  {
    id: 'growth',
    name: 'Growth',
    description: 'Multi-node clustered deployment',
    patients: '50K-500K',
    timeline: '4-8 weeks',
    cost: '$2.5K/month'
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    description: 'Kubernetes enterprise deployment',
    patients: '500K+',
    timeline: '8-12 weeks',
    cost: '$5-15K/month'
  }
];

export default function DeploymentSelector({ selected, onSelect }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 my-12">
      {options.map((option) => (
        <button
          key={option.id}
          onClick={() => onSelect(option.id)}
          className={`p-6 rounded-lg border-2 transition ${
            selected === option.id
              ? 'border-indigo-600 bg-indigo-50'
              : 'border-gray-200 bg-white hover:border-indigo-300'
          }`}
        >
          <h3 className="text-xl font-bold mb-2">{option.name}</h3>
          <p className="text-gray-600 mb-4">{option.description}</p>
          <div className="space-y-2 text-sm">
            <p><strong>Patients:</strong> {option.patients}</p>
            <p><strong>Timeline:</strong> {option.timeline}</p>
            <p><strong>Cost:</strong> {option.cost}</p>
          </div>
        </button>
      ))}
    </div>
  );
}
```

### ROI Calculator Component

**File: `components/roi/ROICalculator.tsx`**

```tsx
'use client';

import { useState } from 'react';
import { calculateROI } from '@/lib/calculations';

export default function ROICalculator() {
  const [inputs, setInputs] = useState({
    patientPopulation: 250000,
    currentFTE: 5,
    eHRCount: 1,
    expectedGapClosure: 15
  });

  const [results, setResults] = useState(null);

  const handleCalculate = () => {
    const roi = calculateROI(inputs);
    setResults(roi);
  };

  return (
    <section className="py-16 bg-white">
      <div className="max-w-4xl mx-auto px-4">
        <h2 className="text-4xl font-bold mb-12 text-center">Calculate Your ROI</h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Inputs */}
          <div className="bg-gray-50 p-8 rounded-lg">
            <h3 className="text-xl font-bold mb-6">Your Information</h3>

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-semibold mb-2">
                  Patient Population
                </label>
                <input
                  type="number"
                  value={inputs.patientPopulation}
                  onChange={(e) => setInputs({
                    ...inputs,
                    patientPopulation: parseInt(e.target.value)
                  })}
                  className="w-full px-4 py-2 border border-gray-300 rounded"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold mb-2">
                  Current FTE on Quality
                </label>
                <input
                  type="number"
                  step="0.5"
                  value={inputs.currentFTE}
                  onChange={(e) => setInputs({
                    ...inputs,
                    currentFTE: parseFloat(e.target.value)
                  })}
                  className="w-full px-4 py-2 border border-gray-300 rounded"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold mb-2">
                  Number of EHRs
                </label>
                <input
                  type="number"
                  value={inputs.eHRCount}
                  onChange={(e) => setInputs({
                    ...inputs,
                    eHRCount: parseInt(e.target.value)
                  })}
                  className="w-full px-4 py-2 border border-gray-300 rounded"
                />
              </div>

              <button
                onClick={handleCalculate}
                className="w-full bg-indigo-600 text-white py-3 rounded font-semibold hover:bg-indigo-700"
              >
                Calculate ROI
              </button>
            </div>
          </div>

          {/* Results */}
          {results && (
            <div className="bg-green-50 p-8 rounded-lg border-2 border-green-300">
              <h3 className="text-xl font-bold mb-6 text-green-900">Your Results</h3>

              <div className="space-y-6">
                <div>
                  <p className="text-sm text-gray-600">Annual Labor Savings</p>
                  <p className="text-3xl font-bold text-green-600">
                    ${(results.laborSavings / 1000).toFixed(0)}K
                  </p>
                </div>

                <div>
                  <p className="text-sm text-gray-600">Quality Bonus Potential</p>
                  <p className="text-3xl font-bold text-green-600">
                    ${(results.qualityBonus / 1000).toFixed(0)}K
                  </p>
                </div>

                <div>
                  <p className="text-sm text-gray-600">Payback Period</p>
                  <p className="text-3xl font-bold text-green-600">
                    {results.paybackMonths} months
                  </p>
                </div>

                <div className="pt-4 border-t-2 border-green-300">
                  <p className="text-sm text-gray-600">3-Year Total Benefit</p>
                  <p className="text-4xl font-bold text-green-700">
                    ${(results.totalBenefit / 1000000).toFixed(1)}M
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
```

---

## Phase 3: Data & Content

### Create Constants File

**File: `lib/constants.ts`**

```ts
export const CASE_STUDIES = [
  {
    id: 'epic-hospital',
    name: 'Regional Health System (Epic)',
    organization: '5 hospitals, 200K patients',
    challenge: 'Multi-hospital quality coordination',
    solution: 'HDIM Enterprise deployment with Epic integration',
    timeline: '8 weeks',
    results: {
      hedisImprovement: '+1.5 points',
      qualityBonus: '$2.3M',
      laborSavings: '80%',
      timeSavingsPerPatient: '15 min → 2 min'
    },
    quote: 'We went from spreadsheets to real-time insights.',
    ehr: 'Epic'
  },
  // ... more case studies
];

export const DEPLOYMENT_MODELS = [
  {
    id: 'pilot',
    name: 'Pilot',
    patientRange: 'Up to 50K',
    timeline: '2-3 weeks',
    monthlyPrice: 500,
    infrastructure: 'Single server',
    useCase: 'Testing, POC, small practices'
  },
  // ... more models
];

export const CUSTOMIZATION_LEVELS = [
  {
    level: 1,
    name: 'Pre-Built Measures',
    features: ['52 HEDIS measures', 'Basic dashboards', 'FHIR import'],
    timeline: 'Included',
    cost: 0,
    description: 'Deploy and start calculating measures immediately'
  },
  // ... more levels
];
```

### Create ROI Calculation Logic

**File: `lib/calculations.ts`**

```ts
interface ROIInputs {
  patientPopulation: number;
  currentFTE: number;
  eHRCount: number;
  expectedGapClosure: number;
}

interface ROIResults {
  laborSavings: number;
  qualityBonus: number;
  paybackMonths: number;
  totalBenefit: number;
}

export function calculateROI(inputs: ROIInputs): ROIResults {
  // Labor savings calculation
  const fteCost = 100000; // Average healthcare FTE cost
  const laborSavingsPercent = 0.3; // 30% efficiency improvement typical
  const laborSavings = inputs.currentFTE * fteCost * laborSavingsPercent;

  // Quality bonus estimation
  // Varies by Star rating improvement (assumes 1-2 point improvement)
  const bonusPerPoint = inputs.patientPopulation < 50000
    ? 50000  // Smaller orgs
    : inputs.patientPopulation < 500000
    ? 500000  // Mid-market
    : 2000000; // Large enterprises

  const qualityBonus = bonusPerPoint * 1.5; // Assume 1.5 point improvement

  // Implementation cost
  const implementationCost = inputs.patientPopulation < 50000 ? 5000 : 20000;
  const annualPlatformCost = inputs.patientPopulation < 50000 ? 6000 : 30000;

  const paybackMonths = Math.round(
    (implementationCost + annualPlatformCost) / ((laborSavings + qualityBonus) / 12)
  );

  const totalBenefit = (laborSavings + qualityBonus) * 3 - (implementationCost + annualPlatformCost * 3);

  return {
    laborSavings,
    qualityBonus,
    paybackMonths,
    totalBenefit
  };
}
```

---

## Phase 4: Vercel Deployment

### Setup Environment Variables

**File: `.env.local`**

```env
# API endpoints
NEXT_PUBLIC_API_URL=https://api.hdim.example.com
NEXT_PUBLIC_DEMO_FORM_ENDPOINT=/api/demo-request

# Analytics
NEXT_PUBLIC_GOOGLE_ANALYTICS_ID=G_XXXXXXXXXX
NEXT_PUBLIC_HOTJAR_ID=xxx

# Feature flags
NEXT_PUBLIC_ENABLE_ROI_CALCULATOR=true
NEXT_PUBLIC_ENABLE_SCENARIO_PAGES=true
```

### Configure Next.js

**File: `next.config.ts`**

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**",
      },
    ],
  },
  headers: async () => [
    {
      source: "/(.*)",
      headers: [
        {
          key: "Cache-Control",
          value: "public, max-age=3600, stale-while-revalidate=86400",
        },
      ],
    },
  ],
  experimental: {
    scrollRestoration: true,
  },
};

export default nextConfig;
```

### Deploy to Vercel

```bash
# 1. Connect to Vercel CLI
npm install -g vercel
vercel login

# 2. Deploy from project root
vercel deploy

# 3. Set production environment
vercel env add NEXT_PUBLIC_API_URL
vercel env add NEXT_PUBLIC_GOOGLE_ANALYTICS_ID

# 4. Deploy to production
vercel deploy --prod

# 5. Set custom domain
vercel domains add hdim.io
```

---

## Phase 5: Analytics & Tracking

### Setup Conversion Tracking

**File: `lib/analytics.ts`**

```ts
export const trackEvent = (event: string, data?: Record<string, any>) => {
  if (typeof window !== 'undefined' && window.gtag) {
    window.gtag('event', event, data);
  }
};

export const trackPageView = (path: string) => {
  trackEvent('page_view', { page_path: path });
};

export const trackDemoRequest = (organization: string, ehr: string) => {
  trackEvent('demo_request', { organization, ehr });
};

export const trackROICalculation = (population: number, result: number) => {
  trackEvent('roi_calculated', { population, result });
};
```

### Add Tracking to Components

```tsx
import { trackDemoRequest } from '@/lib/analytics';

function DemoForm() {
  const handleSubmit = async (formData) => {
    trackDemoRequest(formData.organization, formData.ehr);
    // ... submit form
  };
}
```

---

## Phase 6: Testing & Optimization

### Performance Testing

```bash
# Run Lighthouse audit
npm run build
npx lighthouse https://hdim.io --view

# Test locally
npm run dev
# Visit http://localhost:3000
```

### SEO Optimization

**File: `app/layout.tsx`**

```tsx
export const metadata: Metadata = {
  title: 'HDIM - Healthcare Quality Measurement Platform',
  description: 'Real-time clinical quality measures. Deploy anywhere, integrate any EHR, customize infinitely.',
  openGraph: {
    title: 'HDIM - Real-Time Healthcare Quality Measurement',
    description: 'Calculate clinical value in real-time without data copying',
    image: 'https://hdim.io/og-image.png',
    url: 'https://hdim.io',
  },
};
```

---

## Phase 7: Post-Launch

### Monitor Performance

Track these metrics:
- ✅ Lighthouse score (target: 90+)
- ✅ Core Web Vitals (LCP < 2.5s, FID < 100ms, CLS < 0.1)
- ✅ ROI calculator engagement rate
- ✅ Demo request conversion rate
- ✅ Scenario page view depth

### Content Updates

Regularly update:
- Case studies (add new ones quarterly)
- Pricing (if changed)
- Integration timeline estimates
- Customer metrics

### A/B Testing

Test variations of:
- CTA button placement & copy
- Headline messaging
- Feature order in comparison matrix
- Pricing tier emphasis

---

## Implementation Timeline

**Week 1**: Project setup, component structure, design system
**Week 2**: Hero section, deployment selector, animation
**Week 3**: ROI calculator, case studies section
**Week 4**: Remaining sections (integration, pricing, trust)
**Week 5**: Content population, imagery, copywriting
**Week 6**: Testing, optimization, performance tuning
**Week 7**: Vercel deployment, analytics setup
**Week 8**: Post-launch monitoring, content refinement

---

## Key Success Metrics

**Launch Targets**:
- ✅ Lighthouse score: 95+
- ✅ Landing page CTR: > 5%
- ✅ Demo form conversion: > 2%
- ✅ Time on site: > 3 minutes
- ✅ ROI calculator engagement: > 30% of visitors

---

## Resources & References

- [Next.js Documentation](https://nextjs.org/docs)
- [Tailwind CSS Documentation](https://tailwindcss.com)
- [Vercel Deployment Guide](https://vercel.com/docs)
- [Web Vitals Optimization](https://web.dev/vitals/)
- [Existing landing page code](../../hdim-admin-portal/landing-page-v0/)

---

This implementation guide provides everything needed to build a professional, conversion-optimized landing page that showcases HDIM's deployment flexibility, customer value, and ROI potential.
