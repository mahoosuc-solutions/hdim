'use client'

import { Download, Play, Calendar, Rocket, ArrowRight } from 'lucide-react'

type CTAVariant = 'full' | 'compact' | 'inline'

interface ProgressiveCTAProps {
  variant?: CTAVariant
  inlineLabel?: string
  inlineHref?: string
}

const CTA_TIERS = [
  {
    id: 'one-pager',
    icon: Download,
    label: 'Download One-Pager',
    description: 'Two-page platform overview with ROI data',
    href: '/downloads',
    style: 'bg-white text-primary border-2 border-primary hover:bg-primary-50',
  },
  {
    id: 'overview',
    icon: Play,
    label: 'Watch Overview',
    description: '5-minute platform walkthrough video',
    href: '/demo',
    style: 'bg-white text-primary border-2 border-primary hover:bg-primary-50',
  },
  {
    id: 'demo',
    icon: Calendar,
    label: 'Schedule Demo',
    description: '30-minute live demo with your data',
    href: '/schedule',
    style: 'bg-primary text-white hover:bg-primary-600',
  },
  {
    id: 'pilot',
    icon: Rocket,
    label: 'Start Pilot',
    description: '90-day deployment with dedicated success team',
    href: '/contact',
    style: 'bg-gradient-to-r from-primary to-accent text-white hover:opacity-90',
  },
]

export function ProgressiveCTA({ variant = 'full', inlineLabel, inlineHref }: ProgressiveCTAProps) {
  if (variant === 'inline') {
    return (
      <a
        href={inlineHref || '/schedule'}
        className="inline-flex items-center gap-2 px-6 py-3 bg-primary text-white rounded-lg font-semibold hover:bg-primary-600 transition-all duration-200 transform hover:scale-105"
      >
        {inlineLabel || 'Schedule Demo'}
        <ArrowRight className="w-4 h-4" />
      </a>
    )
  }

  if (variant === 'compact') {
    const compactTiers = CTA_TIERS.filter(t => t.id === 'demo' || t.id === 'pilot')
    return (
      <div className="flex flex-col sm:flex-row gap-4 justify-center">
        {compactTiers.map((tier) => (
          <a
            key={tier.id}
            href={tier.href}
            className={`inline-flex items-center justify-center gap-2 px-8 py-4 rounded-lg font-semibold transition-all duration-200 transform hover:scale-105 shadow-lg ${tier.style}`}
          >
            <tier.icon className="w-5 h-5" />
            {tier.label}
          </a>
        ))}
      </div>
    )
  }

  return (
    <section className="py-20 bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="section-heading">Ready to get started?</h2>
          <p className="section-subheading mt-4">
            Choose your path -- from a quick overview to a full pilot deployment.
          </p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {CTA_TIERS.map((tier, index) => (
            <a
              key={tier.id}
              href={tier.href}
              className="group flex flex-col items-center text-center p-6 bg-white rounded-xl shadow-md hover:shadow-xl transition-all duration-300 hover:-translate-y-1 border border-gray-100"
            >
              <div className="w-14 h-14 rounded-full bg-primary-50 flex items-center justify-center mb-4 group-hover:bg-primary-100 transition-colors">
                <tier.icon className="w-7 h-7 text-primary" />
              </div>
              <div className="text-xs font-medium text-gray-400 mb-1 uppercase tracking-wider">Step {index + 1}</div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">{tier.label}</h3>
              <p className="text-sm text-gray-600 mb-4">{tier.description}</p>
              <span className="inline-flex items-center gap-1 text-sm font-medium text-primary group-hover:gap-2 transition-all">
                Get started <ArrowRight className="w-4 h-4" />
              </span>
            </a>
          ))}
        </div>
      </div>
    </section>
  )
}
