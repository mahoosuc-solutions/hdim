import Link from 'next/link'
import {
  HeartPulse,
  BarChart3,
  Shield,
  ArrowRight,
  DollarSign,
  Wrench,
  Database,
  Monitor,
  Settings,
  Users,
  Zap,
  Search,
  FileCheck,
  Handshake,
  Rocket,
  CheckCircle2,
} from 'lucide-react'
import { LandingPageClient } from './LandingPageClient'
import { ProgressiveCTA } from './ProgressiveCTA'
import { SiteFooter } from './SiteFooter'
import { CAPABILITIES } from '../../lib/constants'
import type { Segment } from '../../lib/constants'

const CAPABILITY_ICONS: Record<string, React.ElementType> = {
  'quality-engine': BarChart3,
  'care-gaps': HeartPulse,
  'revenue-cycle': DollarSign,
  'fhir': Database,
  'measure-builder': Wrench,
  'cmo-onboarding': Users,
  'clinical-portal': Monitor,
  'operations': Settings,
  'security': Shield,
}

const JOURNEY_STEPS = [
  { icon: Search, title: 'Discovery', subtitle: '30-minute call', description: 'We learn your quality program, data landscape, and goals.' },
  { icon: FileCheck, title: 'Fit Assessment', subtitle: '48 hours', description: 'Technical review of integration points and measure coverage.' },
  { icon: Rocket, title: '90-Day Pilot', subtitle: 'Live deployment', description: 'Production deployment with dedicated success team and weekly check-ins.' },
  { icon: Handshake, title: 'Partnership', subtitle: 'Ongoing', description: 'Full platform access, priority support, and custom measure development.' },
]

interface SegmentPageProps {
  segment: Segment
}

export function SegmentPage({ segment }: SegmentPageProps) {
  const segmentCapabilities = CAPABILITIES.filter((c) =>
    segment.capabilities.includes(c.id)
  )

  return (
    <LandingPageClient>
      {/* Hero */}
      <section id="main-content" className="relative bg-gradient-to-br from-primary via-primary-600 to-primary-800 pt-32 pb-20">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 left-0 w-96 h-96 bg-accent rounded-full filter blur-3xl" />
          <div className="absolute bottom-0 right-0 w-96 h-96 bg-white rounded-full filter blur-3xl" />
        </div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-white/10 rounded-full text-white/80 text-sm mb-6">
            For {segment.label}
          </div>
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight text-balance max-w-4xl mx-auto">
            {segment.tagline}
          </h1>
          <p className="mt-6 text-lg text-white/80 max-w-2xl mx-auto">
            {segment.description}
          </p>
          <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/schedule" className="btn-primary text-lg px-8 py-4">
              Schedule Demo
            </Link>
            <Link href="/platform" className="btn-secondary bg-white/10 text-white border-white/30 hover:bg-white/20 text-lg px-8 py-4">
              See Platform
            </Link>
          </div>
        </div>
      </section>

      {/* Persona Cards */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="section-heading">Built for every role on your team</h2>
            <p className="section-subheading mt-4">
              From clinical leadership to IT -- solutions for the problems that matter most.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {segment.roles.map((role) => (
              <div key={role.title} className="rounded-2xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-lg transition-shadow duration-300">
                <div className="p-6">
                  <h3 className="text-lg font-bold text-gray-900 mb-4">{role.title}</h3>
                  <div className="space-y-4">
                    <div className="p-4 bg-red-50 rounded-lg border border-red-100">
                      <div className="text-xs font-semibold text-red-600 uppercase tracking-wider mb-1">The Problem</div>
                      <p className="text-sm text-red-900">{role.pain}</p>
                    </div>
                    <div className="p-4 bg-green-50 rounded-lg border border-green-100">
                      <div className="text-xs font-semibold text-green-600 uppercase tracking-wider mb-1">The Solution</div>
                      <p className="text-sm text-green-900">{role.solution}</p>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Capabilities */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="section-heading">Capabilities for {segment.label.toLowerCase()}</h2>
            <p className="section-subheading mt-4">
              The platform features most relevant to your organization.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {segmentCapabilities.map((cap) => {
              const Icon = CAPABILITY_ICONS[cap.id] || Zap
              return (
                <div
                  key={cap.id}
                  className="p-6 bg-white rounded-xl border border-gray-200 shadow-sm"
                >
                  <div className="flex items-start gap-3 mb-3">
                    <div className="w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0">
                      <Icon className="w-5 h-5 text-primary" />
                    </div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-semibold text-gray-900">{cap.title}</h3>
                      {cap.isNew && (
                        <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-full bg-accent-50 text-accent-600">
                          {cap.badge}
                        </span>
                      )}
                    </div>
                  </div>
                  <p className="text-sm text-gray-600 mb-2">{cap.description}</p>
                  <p className="text-sm text-gray-500">{cap.detail}</p>
                </div>
              )
            })}
          </div>
        </div>
      </section>

      {/* ROI Section */}
      <section className="py-20 bg-gradient-to-br from-primary to-primary-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-white">Return on Investment</h2>
            <p className="text-lg text-white/80 mt-4 max-w-2xl mx-auto">
              Concrete financial impact for {segment.label.toLowerCase()}.
            </p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {[
              { label: 'Impact', value: segment.roi.headline },
              { label: 'Savings', value: segment.roi.savings },
              { label: 'Payback', value: segment.roi.payback },
              { label: 'Result', value: segment.roi.metric },
            ].map((item) => (
              <div key={item.label} className="text-center p-6 bg-white/10 rounded-xl backdrop-blur-sm border border-white/20">
                <div className="text-xs font-semibold text-white/60 uppercase tracking-wider mb-2">{item.label}</div>
                <div className="text-xl md:text-2xl font-bold text-white">{item.value}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Journey */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="section-heading">How we work together</h2>
            <p className="section-subheading mt-4">
              From first call to production in 90 days.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {JOURNEY_STEPS.map((step, index) => (
              <div key={step.title} className="relative text-center">
                {index < JOURNEY_STEPS.length - 1 && (
                  <div className="hidden md:block absolute top-8 left-[60%] w-[80%] h-px bg-gray-200" />
                )}
                <div className="relative z-10 w-16 h-16 rounded-full bg-primary-50 flex items-center justify-center mx-auto mb-4">
                  <step.icon className="w-7 h-7 text-primary" />
                </div>
                <div className="text-xs font-semibold text-primary uppercase tracking-wider mb-1">{step.subtitle}</div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">{step.title}</h3>
                <p className="text-sm text-gray-600">{step.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <ProgressiveCTA variant="full" />

      <SiteFooter />
    </LandingPageClient>
  )
}
