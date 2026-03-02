import { Metadata } from 'next'
import Image from 'next/image'
import Link from 'next/link'
import {
  HeartPulse,
  BarChart3,
  Shield,
  ArrowRight,
  Building2,
  Hospital,
  Users,
  DollarSign,
  Wrench,
  Database,
  Monitor,
  Settings,
  Zap,
} from 'lucide-react'
import { CyclingText } from './components/CyclingText'
import { LandingPageClient } from './components/LandingPageClient'
import { JourneyWizard } from './components/JourneyWizard'
import { MetricsBar } from './components/MetricsBar'
import { WhatsNew } from './components/WhatsNew'
import { ProgressiveCTA } from './components/ProgressiveCTA'
import { SiteFooter } from './components/SiteFooter'
import ScreenshotGallery from './components/ScreenshotLightbox'
import { CAPABILITIES, SEGMENTS } from '../lib/constants'

export const metadata: Metadata = {
  title: 'HDIM - FHIR-Native Healthcare Quality Platform | Care Gaps, HEDIS, Star Ratings',
  description: 'Real-time HEDIS quality measurement, care gap detection, and Star Rating optimization. Built on FHIR R4 with CQL-native execution for health plans, ACOs, and health systems.',
}

const SEGMENT_CARDS = [
  { key: 'healthPlans', icon: Building2, color: 'from-primary to-primary-600' },
  { key: 'healthSystems', icon: Hospital, color: 'from-accent-600 to-accent' },
  { key: 'acos', icon: Users, color: 'from-primary-700 to-accent-600' },
] as const

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

export default function LandingPage() {
  return (
    <LandingPageClient>
      {/* Hero Section */}
      <section id="main-content" className="relative min-h-[85vh] flex items-center bg-gradient-to-br from-primary via-primary-600 to-primary-800 overflow-hidden">
        <div className="absolute inset-0">
          <Image
            src="/images/hero/hero-02.png"
            alt="Healthcare data visualization"
            fill
            className="object-cover opacity-20"
            priority
          />
        </div>
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 left-0 w-96 h-96 bg-accent rounded-full filter blur-3xl" />
          <div className="absolute bottom-0 right-0 w-96 h-96 bg-white rounded-full filter blur-3xl" />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-20 text-center">
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight text-balance max-w-4xl mx-auto">
            Healthcare quality measurement.{' '}
            <span className="block mt-2">Reimagined for <CyclingText /></span>
          </h1>
          <p className="mt-6 text-lg md:text-xl text-white/80 max-w-3xl mx-auto">
            The FHIR-native platform for HEDIS evaluation, care gap detection, and clinical quality improvement.
            59 microservices. 80+ measures. 90-day deployment.
          </p>
          <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/schedule"
              className="btn-primary text-lg px-8 py-4"
            >
              Schedule Demo
            </Link>
            <Link
              href="/platform"
              className="btn-secondary bg-white/10 text-white border-white/30 hover:bg-white/20 text-lg px-8 py-4"
            >
              Explore Platform
            </Link>
          </div>
        </div>
      </section>

      {/* Segment Selector */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="section-heading">Built for your organization</h2>
            <p className="section-subheading mt-4">
              Role-specific solutions for every stakeholder in healthcare quality.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {SEGMENT_CARDS.map(({ key, icon: Icon, color }) => {
              const segment = SEGMENTS[key]
              return (
                <Link
                  key={key}
                  href={`/${segment.slug}`}
                  className="group relative overflow-hidden rounded-2xl shadow-lg hover:shadow-2xl transition-all duration-300 hover:-translate-y-2"
                >
                  <div className={`bg-gradient-to-br ${color} p-8 min-h-[280px] flex flex-col justify-between`}>
                    <div>
                      <div className="w-14 h-14 rounded-xl bg-white/15 flex items-center justify-center mb-6">
                        <Icon className="w-7 h-7 text-white" />
                      </div>
                      <h3 className="text-2xl font-bold text-white mb-2">{segment.label}</h3>
                      <p className="text-white/90 text-lg font-medium mb-3">{segment.tagline}</p>
                      <p className="text-white/70 text-sm">{segment.description}</p>
                    </div>
                    <div className="flex items-center gap-2 mt-6 text-white/80 group-hover:text-white group-hover:gap-3 transition-all text-sm font-medium">
                      See how it works <ArrowRight className="w-4 h-4" />
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        </div>
      </section>

      {/* Journey Wizard - "Not sure where to start?" */}
      <JourneyWizard />

      {/* Metrics Bar */}
      <MetricsBar />

      {/* What's New */}
      <WhatsNew />

      {/* Capabilities Overview */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="section-heading">Platform capabilities</h2>
            <p className="section-subheading mt-4">
              A complete quality measurement and clinical operations platform.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {CAPABILITIES.map((cap) => {
              const Icon = CAPABILITY_ICONS[cap.id] || Zap
              return (
                <Link
                  key={cap.id}
                  href="/platform"
                  className="group p-6 bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1"
                >
                  <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0">
                      <Icon className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="text-base font-semibold text-gray-900">{cap.title}</h3>
                        {cap.isNew && (
                          <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-full bg-accent-50 text-accent-600">
                            {cap.badge}
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-600">{cap.description}</p>
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
          <div className="text-center mt-10">
            <Link
              href="/platform"
              className="inline-flex items-center gap-2 text-primary font-semibold hover:gap-3 transition-all"
            >
              View all platform details <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </section>

      {/* See It In Action */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <p className="text-sm font-semibold tracking-widest uppercase text-primary mb-3">
              Live Portal
            </p>
            <h2 className="section-heading">See it in action</h2>
            <p className="section-subheading mt-4">
              Every screen below is from our live Clinical Portal — not mockups, not prototypes.
            </p>
          </div>

          <ScreenshotGallery
            heroScreenshot={{
              src: "/images/dashboard-modern/01-dashboard-overview-clean.jpg",
              alt: "HDIM Clinical Portal Dashboard with KPIs, care gap alerts, and quick actions",
              width: 1915,
              height: 939,
              title: "Provider Dashboard",
              description: "Role-based clinical dashboard with real-time KPIs, results awaiting review, and high-priority care gaps",
              badge: "LIVE",
            }}
            rows={[
              {
                columns: 3,
                screenshots: [
                  {
                    src: "/images/dashboard-modern/05-quality-measures-library.jpg",
                    alt: "HEDIS Quality Measures Library with benchmark scores",
                    width: 1918,
                    height: 920,
                    title: "Quality Measures Library",
                    description: "HEDIS measures with national benchmarks and CMS star ratings",
                  },
                  {
                    src: "/images/dashboard-modern/08-care-gap-management.jpg",
                    alt: "High Priority Care Gaps worklist",
                    width: 1918,
                    height: 917,
                    title: "Care Gap Management",
                    description: "Priority-based care gap worklist with one-click actions",
                  },
                  {
                    src: "/images/dashboard-modern/06-evaluation-results.jpg",
                    alt: "Evaluation Results with compliance scoring",
                    width: 1916,
                    height: 919,
                    title: "Evaluation Results",
                    description: "Real-time compliance scoring with outcome distribution",
                  },
                ],
              },
            ]}
          />

          <div className="text-center mt-10">
            <Link
              href="/features"
              className="inline-flex items-center gap-2 text-primary font-semibold hover:gap-3 transition-all"
            >
              See all screenshots <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </section>

      {/* Progressive CTA */}
      <ProgressiveCTA variant="full" />

      <SiteFooter />
    </LandingPageClient>
  )
}
