'use client'

import { useState } from 'react'
import { ChevronDown, Building2, Users, Heart, Hospital } from 'lucide-react'

interface Segment {
  id: string
  label: string
  selectorText: string
  icon: React.ElementType
  headline: string
  subheadline: string
  stat: string
  statLabel: string
  cta: string
  ctaLink: string
}

const segments: Segment[] = [
  {
    id: 'health-plans',
    label: 'Health Plans',
    selectorText: 'a health plan',
    icon: Building2,
    headline: 'Improve Star Ratings',
    subheadline: 'Turn Star Ratings from a compliance headache into a competitive advantage. A 1-star improvement for 175K members means $192M in annual CMS bonus payments.',
    stat: '$192M',
    statLabel: 'Potential Star Rating Revenue',
    cta: 'Calculate Your Star Rating Impact',
    ctaLink: '/research#calculator',
  },
  {
    id: 'acos',
    label: 'ACOs',
    selectorText: 'an ACO',
    icon: Users,
    headline: 'Capture Shared Savings',
    subheadline: 'Close 35% more care gaps without adding staff. Move from reactive quarterly reporting to proactive daily interventions that capture shared savings before they slip away.',
    stat: '+$1.07M',
    statLabel: 'Annual Shared Savings Increase',
    cta: 'Model Your Savings Potential',
    ctaLink: '/research#calculator',
  },
  {
    id: 'fqhcs',
    label: 'FQHCs',
    selectorText: 'an FQHC',
    icon: Heart,
    headline: 'Protect Your 330 Grant',
    subheadline: 'Enterprise-grade quality automation at community health center budgets. Achieve UDS reporting compliance while identifying your highest-risk patients.',
    stat: '$500/mo',
    statLabel: 'Starting Price for 10K Patients',
    cta: 'See FQHC Pricing',
    ctaLink: '/pricing#professional',
  },
  {
    id: 'health-systems',
    label: 'Health Systems',
    selectorText: 'a health system',
    icon: Hospital,
    headline: 'Unify Quality Across EHRs',
    subheadline: 'One platform for Epic, Cerner, Meditech, and any FHIR-compliant system. Eliminate the integration nightmare with native FHIR R4 support.',
    stat: '70-95%',
    statLabel: 'Lower TCO vs. Competitors',
    cta: 'Request Integration Assessment',
    ctaLink: '/schedule',
  },
]

export function SegmentSelector() {
  const [activeSegment, setActiveSegment] = useState<Segment>(segments[0])
  const [isOpen, setIsOpen] = useState(false)

  return (
    <div className="mt-8">
      {/* Segment Dropdown */}
      <div className="relative inline-block w-full max-w-md">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="w-full flex items-center justify-between px-4 py-3 bg-white/10 backdrop-blur-sm border border-white/20 rounded-lg text-white hover:bg-white/20 transition-colors"
          aria-expanded={isOpen}
          aria-haspopup="listbox"
        >
          <div className="flex items-center gap-3">
            <activeSegment.icon className="w-5 h-5" />
            <span className="font-medium">I work at {activeSegment.selectorText}</span>
          </div>
          <ChevronDown className={`w-5 h-5 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
        </button>

        {/* Dropdown Options */}
        {isOpen && (
          <div
            className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-xl border border-gray-200 overflow-hidden z-50"
            role="listbox"
          >
            {segments.map((segment) => (
              <button
                key={segment.id}
                onClick={() => {
                  setActiveSegment(segment)
                  setIsOpen(false)
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors ${
                  activeSegment.id === segment.id ? 'bg-primary/5 text-primary' : 'text-gray-700'
                }`}
                role="option"
                aria-selected={activeSegment.id === segment.id}
              >
                <segment.icon className="w-5 h-5" />
                <span className="font-medium">{segment.label}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Segment-Specific Content */}
      <div className="mt-6 p-6 bg-white/10 backdrop-blur-sm rounded-xl border border-white/20">
        <div className="flex items-center gap-2 mb-3">
          <activeSegment.icon className="w-6 h-6 text-accent" />
          <h3 className="text-xl font-semibold text-white">{activeSegment.headline}</h3>
        </div>
        <p className="text-white/80 mb-4">{activeSegment.subheadline}</p>

        <div className="flex items-center justify-between">
          <div>
            <div className="text-3xl font-bold text-accent">{activeSegment.stat}</div>
            <div className="text-sm text-white/60">{activeSegment.statLabel}</div>
          </div>
          <a
            href={activeSegment.ctaLink}
            className="inline-flex items-center px-4 py-2 bg-white text-primary rounded-lg font-medium hover:bg-white/90 transition-colors text-sm"
          >
            {activeSegment.cta}
          </a>
        </div>
      </div>

      {/* Quick Links to Other Segments */}
      <div className="mt-4 flex flex-wrap gap-2 justify-center lg:justify-start">
        {segments.filter(s => s.id !== activeSegment.id).map((segment) => (
          <button
            key={segment.id}
            onClick={() => setActiveSegment(segment)}
            className="text-xs text-white/60 hover:text-white/90 transition-colors underline"
          >
            For {segment.label}
          </button>
        ))}
      </div>
    </div>
  )
}
