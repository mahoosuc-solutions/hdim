# HDIM Landing Page

Modern, conversion-optimized landing page for HealthData-in-Motion (HDIM) healthcare quality platform.

## Validation Status

[![Landing Page Validation](https://github.com/webemo-aaron/hdim/actions/workflows/landing-page-validation.yml/badge.svg?branch=master)](https://github.com/webemo-aaron/hdim/actions/workflows/landing-page-validation.yml)

This repo enforces landing-page quality checks for changes under `landing-page-v0/`:

- `npm run validate:ci` (aggregated CI gate)
- `npm run validate:content` (includes `/sales` metadata contract checks)
- `npm run validate:links`
- `npm run lint`
- `npm run build`

## Features

- **Modern Design**: Gradient hero, animated elements, responsive layout
- **Conversion-Optimized**: Multiple CTAs, social proof, trust badges
- **Healthcare-Focused**: Compliance badges (HIPAA, SOC2, HITRUST), industry terminology
- **Performance**: Next.js 16, optimized images, lazy loading
- **Analytics Ready**: Vercel Analytics integration, Drift chat placeholder

## Tech Stack

- **Framework**: Next.js 16 (App Router)
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **Analytics**: Vercel Analytics
- **Deployment**: Vercel

## Getting Started

### Prerequisites

- Node.js 20+
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

### Development

The development server runs at `http://localhost:3000`

## Deployment to Vercel

### Option 1: Vercel CLI

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Deploy to production
vercel --prod
```

### Option 2: GitHub Integration

1. Push this directory to a GitHub repository
2. Import the repository in Vercel dashboard
3. Vercel will auto-deploy on every push

## Customization

### Colors

Edit `tailwind.config.ts` to change the color scheme:

```typescript
colors: {
  primary: {
    DEFAULT: '#0D4F8B',  // Main brand color
    // ... shades
  },
  accent: {
    DEFAULT: '#4B9CD3',  // Accent color
    // ... shades
  },
}
```

### Content

Edit `app/page.tsx` to update:

- Headlines and copy
- Feature descriptions
- Testimonials
- Stats and metrics

### Adding Drift Chat

1. Get your Drift embed code from Drift dashboard
2. Add to `app/layout.tsx` using Next.js Script component:

```tsx
import Script from 'next/script'

// In the body:
<Script id="drift-widget" strategy="lazyOnload">
  {`
    // Drift widget code here
  `}
</Script>
```

### Adding Interactive Demo

Embed Navattic or Storylane demo in the hero section:

```tsx
<iframe
  src="https://capture.navattic.com/YOUR_DEMO_ID"
  className="w-full h-96 rounded-xl"
  frameBorder="0"
/>
```

## Project Structure

```
landing-page-v0/
├── app/
│   ├── globals.css      # Global styles + Tailwind
│   ├── layout.tsx       # Root layout + metadata
│   └── page.tsx         # Main landing page
├── public/              # Static assets
├── next.config.js       # Next.js configuration
├── tailwind.config.ts   # Tailwind configuration
├── vercel.json          # Vercel deployment config
└── package.json         # Dependencies
```

## Performance Targets

- Lighthouse Performance: 90+
- First Contentful Paint: <1.5s
- Largest Contentful Paint: <2.5s
- Cumulative Layout Shift: <0.1

## License

Private - HealthData-in-Motion

---

Built with Next.js and Tailwind CSS
