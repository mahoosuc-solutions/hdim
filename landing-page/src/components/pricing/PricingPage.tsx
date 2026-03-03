'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { PRICING_TIERS, CALENDAR_URL, EMAIL_SALES } from '@/lib/constants';

// Extend shared PRICING_TIERS with page-specific presentation fields.
// Core pricing (name, price, period, description, commitment, features, cta, highlighted)
// comes from constants.ts — single source of truth for price changes.
const TIER_EXTENSIONS: Record<string, {
  badge: string | null;
  ctaHref: string;
  members: string;
  roi: string;
  extraFeatures: string[];
  notIncluded: string[];
}> = {
  Pilot: {
    badge: null,
    ctaHref: `mailto:${EMAIL_SALES}?subject=Pilot Inquiry`,
    members: 'Up to 50K members',
    roi: 'Typical ROI: 10–20×',
    extraFeatures: [
      'Care gap detection & prioritization',
      'Email + Slack notifications',
      'Community + email support',
    ],
    notIncluded: [
      'Predictive gap detection',
      'Multi-tenant / multi-payer',
      'CDS Hooks / SMART on FHIR',
      'Custom CQL measures',
    ],
  },
  Annual: {
    badge: 'Most Popular',
    ctaHref: CALENDAR_URL,
    members: '50K–500K members',
    roi: 'Typical ROI: 50–150×',
    extraFeatures: [
      'HL7 v2 + bulk data ingestion',
      'Real-time quality bonus tracking dashboard',
      'AI provider narratives (3× engagement)',
      'CDS Hooks for in-workflow EHR alerts',
      'Advanced ROI calculator + board reporting',
      'Quarterly business reviews',
    ],
    notIncluded: [
      'SMART on FHIR app integration',
      'On-premise air-gapped deployment',
    ],
  },
  Enterprise: {
    badge: null,
    ctaHref: `mailto:${EMAIL_SALES}?subject=Enterprise Inquiry`,
    members: '500K+ members',
    roi: 'Typical ROI: 100×+',
    extraFeatures: [
      'Kubernetes auto-scaling deployment',
      'SMART on FHIR app integration',
      'Multi-region deployment',
      'Readmission risk + cost forecasting AI',
      'BI tool connectors (Tableau, Power BI)',
      'Custom integrations (non-FHIR sources)',
      'Dedicated customer success manager',
      'White-label option',
    ],
    notIncluded: [],
  },
};

const TIERS = PRICING_TIERS.map((tier) => {
  const ext = TIER_EXTENSIONS[tier.name];
  const priceDisplay = tier.price ? `$${tier.price.toLocaleString()}` : 'Custom';
  const periodDisplay = tier.price ? tier.period : '';
  return {
    name: tier.name,
    badge: ext?.badge ?? null,
    price: priceDisplay,
    period: periodDisplay,
    commitment: tier.commitment ?? '',
    description: tier.description,
    cta: tier.cta,
    ctaHref: ext?.ctaHref ?? '#',
    highlighted: tier.highlighted,
    members: ext?.members ?? '',
    roi: ext?.roi ?? '',
    features: [...tier.features, ...(ext?.extraFeatures ?? [])],
    notIncluded: ext?.notIncluded ?? [],
  };
});

const FAQ_ITEMS = [
  {
    q: 'How long does it take to go live?',
    a: 'Most pilots go live in 3–4 weeks. The integration timeline is: Week 1 — FHIR credential validation and data extraction test; Week 2 — Full data load with 10K sample patients; Week 3–4 — Production deployment and monitoring. Epic integrations are typically fastest (2–3 weeks) due to standardized FHIR R4.',
  },
  {
    q: 'How is pricing structured?',
    a: 'Pilot tier is $2,500/month for a 3-month proof-of-concept engagement. Annual tier is $8,500/month on a 12-month contract. We also offer per-member-per-month (PMPM) pricing for large plans: $0.20/PMPM is typical for 100K+ member plans, which often works out lower than flat-rate. Enterprise is custom based on member volume, integrations, and deployment model.',
  },
  {
    q: 'What EHR systems do you integrate with?',
    a: 'HDIM integrates with all major EHRs via FHIR R4: Epic, Oracle Health (Cerner), InterSystems HealthShare, Meditech, Athenahealth, Allscripts/Veradigm, eClinicalWorks, and NextGen. Any FHIR R4-compliant server works regardless of vendor.',
  },
  {
    q: 'Is this HIPAA-compliant?',
    a: 'Yes. HDIM deploys on your infrastructure — PHI never leaves your network. We provide a HIPAA Business Associate Agreement (BAA) with every tier. All services include TLS encryption, AES-256 at rest, row-level tenant isolation, and 100% API audit logging per HIPAA §164.312(b).',
  },
  {
    q: 'How do you calculate ROI?',
    a: 'Every 1-point improvement in quality score = $500K–$750K in annual quality bonuses for a 100K-member plan. HDIM pilots typically achieve 5–10 point improvements, generating $2.5M–$7.5M in incremental bonuses on a $30K–$100K annual investment. Our ROI Calculator on the home page lets you run your own numbers.',
  },
  {
    q: 'Can we start with a subset of our member population?',
    a: 'Yes. Pilots typically start with 5K–50K members for validation, then expand. Data is loaded incrementally — we can go from 0 to 100K members in under 5 minutes with our bulk ingestion pipeline.',
  },
];

export default function PricingPage() {
  const [openFaq, setOpenFaq] = useState<number | null>(null);

  return (
    <main id="main-content">
      {/* Hero */}
      <section className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-20">
        <div className="container-lg">
          <div className="max-w-4xl">
            <Link
              href="/"
              className="inline-flex items-center text-blue-200 hover:text-white mb-6 transition"
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
              Home
            </Link>
            <h1 className="text-4xl md:text-5xl font-bold mb-4">
              Pricing That Pays for Itself in Week Two
            </h1>
            <p className="text-xl text-blue-100 max-w-3xl">
              Every 1-point quality improvement = $500K–$750K in annual bonuses.
              HDIM pilots average 5–10 point improvements. The math is straightforward.
            </p>
          </div>
        </div>
      </section>

      {/* ROI Proof Bar */}
      <section className="bg-white border-b py-10">
        <div className="container-lg">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-center">
            <div>
              <div className="text-3xl font-bold text-blue-600">35–40%</div>
              <div className="text-sm text-gray-600 mt-1">projected gap closure improvement</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-green-600">$1M+</div>
              <div className="text-sm text-gray-600 mt-1">projected incremental quality bonus</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-indigo-600">4 weeks</div>
              <div className="text-sm text-gray-600 mt-1">to live (vs. 8–12 with legacy vendors)</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-purple-600">100×+</div>
              <div className="text-sm text-gray-600 mt-1">typical ROI on implementation cost</div>
            </div>
          </div>
          <p className="text-xs text-gray-400 text-center mt-6">
            Based on modeled outcomes for a 100K-member health plan with 2-point quality score improvement.
            Actual results vary by organization. See{' '}
            <Link href="/terms" className="underline hover:text-gray-500">Terms</Link> for details.
          </p>
        </div>
      </section>

      {/* Pricing Tiers */}
      <section className="section bg-gray-50">
        <div className="container-lg">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-3">Choose Your Path</h2>
            <p className="text-gray-600 max-w-2xl mx-auto">
              Start with a pilot to validate ROI, scale to annual when you're ready.
              No lock-in. No per-seat fees. No surprise infrastructure costs.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 max-w-6xl mx-auto">
            {TIERS.map((tier) => (
              <div
                key={tier.name}
                className={`rounded-2xl p-8 flex flex-col ${
                  tier.highlighted
                    ? 'bg-blue-600 text-white shadow-2xl scale-[1.02] ring-4 ring-blue-300'
                    : 'bg-white text-gray-900 shadow-lg'
                }`}
              >
                {/* Badge */}
                <div className="mb-4 h-6">
                  {tier.badge && (
                    <span className={`text-xs font-bold uppercase tracking-wide px-3 py-1 rounded-full ${
                      tier.highlighted ? 'bg-blue-400 text-white' : 'bg-blue-100 text-blue-700'
                    }`}>
                      {tier.badge}
                    </span>
                  )}
                </div>

                {/* Name + Price */}
                <h3 className={`text-2xl font-bold mb-1 ${tier.highlighted ? 'text-white' : 'text-gray-900'}`}>
                  {tier.name}
                </h3>
                <div className="mb-1">
                  <span className={`text-4xl font-extrabold ${tier.highlighted ? 'text-white' : 'text-gray-900'}`}>
                    {tier.price}
                  </span>
                  {tier.period && (
                    <span className={`text-lg ml-1 ${tier.highlighted ? 'text-blue-200' : 'text-gray-500'}`}>
                      {tier.period}
                    </span>
                  )}
                </div>
                <div className={`text-sm mb-1 ${tier.highlighted ? 'text-blue-200' : 'text-gray-500'}`}>
                  {tier.commitment}
                </div>
                <div className={`text-sm font-medium mb-2 ${tier.highlighted ? 'text-blue-100' : 'text-blue-600'}`}>
                  {tier.members}
                </div>
                <div className={`text-xs font-bold mb-4 ${tier.highlighted ? 'text-green-300' : 'text-green-600'}`}>
                  {tier.roi}
                </div>

                <p className={`text-sm mb-6 ${tier.highlighted ? 'text-blue-100' : 'text-gray-600'}`}>
                  {tier.description}
                </p>

                {/* CTA */}
                <a
                  href={tier.ctaHref}
                  {...(tier.ctaHref.startsWith('http') ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                  className={`w-full text-center py-3 px-6 rounded-xl font-bold transition mb-8 ${
                    tier.highlighted
                      ? 'bg-white text-blue-600 hover:bg-blue-50'
                      : 'bg-blue-600 text-white hover:bg-blue-700'
                  }`}
                >
                  {tier.cta}
                </a>

                {/* Features */}
                <div className="flex-1">
                  <ul className="space-y-3 mb-6">
                    {tier.features.map((f) => (
                      <li key={f} className="flex items-start gap-2 text-sm">
                        <svg
                          className={`w-4 h-4 mt-0.5 flex-shrink-0 ${tier.highlighted ? 'text-green-300' : 'text-green-500'}`}
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                        <span className={tier.highlighted ? 'text-blue-100' : 'text-gray-700'}>
                          {f}
                        </span>
                      </li>
                    ))}
                  </ul>

                  {tier.notIncluded.length > 0 && (
                    <ul className="space-y-2">
                      {tier.notIncluded.map((f) => (
                        <li key={f} className="flex items-start gap-2 text-sm">
                          <svg
                            className={`w-4 h-4 mt-0.5 flex-shrink-0 ${tier.highlighted ? 'text-blue-400' : 'text-gray-300'}`}
                            fill="currentColor"
                            viewBox="0 0 20 20"
                          >
                            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                          </svg>
                          <span className={tier.highlighted ? 'text-blue-400' : 'text-gray-400'}>
                            {f}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* PMPM Callout */}
      <section className="bg-white border-y py-12">
        <div className="container-lg max-w-3xl mx-auto text-center">
          <h3 className="text-xl font-bold text-gray-900 mb-3">
            Large Plan? Ask About PMPM Pricing
          </h3>
          <p className="text-gray-600 mb-4">
            Health plans with 100K+ members can often get better economics through
            per-member-per-month pricing. At <strong>$0.20 PMPM</strong>, a 200K-member plan
            pays $40K/year — less than the flat annual rate — while capturing $2M+ in
            incremental quality bonuses.
          </p>
          <a
            href={`mailto:${EMAIL_SALES}?subject=PMPM Pricing Inquiry`}
            className="inline-flex items-center text-blue-600 font-semibold hover:underline"
          >
            Talk to sales about PMPM
            <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </a>
        </div>
      </section>

      {/* FAQ */}
      <section className="section bg-gray-50">
        <div className="container-lg max-w-3xl mx-auto">
          <h2 className="text-3xl font-bold text-gray-900 mb-10 text-center">
            Frequently Asked Questions
          </h2>
          <div className="space-y-4">
            {FAQ_ITEMS.map((item, idx) => (
              <div key={idx} className="bg-white rounded-xl shadow-sm overflow-hidden">
                <button
                  onClick={() => setOpenFaq(openFaq === idx ? null : idx)}
                  className="w-full flex items-center justify-between p-6 text-left"
                >
                  <span className="font-semibold text-gray-900">{item.q}</span>
                  <svg
                    className={`w-5 h-5 text-gray-500 flex-shrink-0 transition-transform ${openFaq === idx ? 'rotate-180' : ''}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                {openFaq === idx && (
                  <div className="px-6 pb-6">
                    <p className="text-gray-600 text-sm leading-relaxed">{item.a}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section id="contact" className="section bg-blue-600 text-white">
        <div className="container-lg max-w-2xl mx-auto text-center">
          <h2 className="text-3xl font-bold mb-4">Ready to Run the Math?</h2>
          <p className="text-blue-100 mb-8">
            15-minute call. We'll show you exactly what your quality bonus capture
            looks like with HDIM — no estimates, real model based on your member size
            and current gap closure rate.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <a href={CALENDAR_URL} target="_blank" rel="noopener noreferrer" className="bg-white text-blue-600 font-bold py-3 px-8 rounded-xl hover:bg-blue-50 transition text-center">
              Schedule 15-Minute Call
            </a>
            <Link
              href="/#roi"
              className="border border-blue-300 text-white font-bold py-3 px-8 rounded-xl hover:bg-blue-500 transition"
            >
              Run ROI Calculator
            </Link>
          </div>
          <p className="text-blue-200 text-sm mt-6">
            No commitment required. Pilot can start within 2 weeks of contract signature.
          </p>
        </div>
      </section>
    </main>
  );
}
