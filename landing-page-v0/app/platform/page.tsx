import { Metadata } from 'next'
import {
  HeartPulse,
  BarChart3,
  Shield,
  DollarSign,
  Wrench,
  Database,
  Monitor,
  Settings,
  Users,
  Zap,
} from 'lucide-react'
import { LandingPageClient } from '../components/LandingPageClient'
import { MetricsBar } from '../components/MetricsBar'
import { ProgressiveCTA } from '../components/ProgressiveCTA'
import { SiteFooter } from '../components/SiteFooter'
import ScreenshotGallery from '../components/ScreenshotLightbox'
import { CAPABILITIES } from '../../lib/constants'

export const metadata: Metadata = {
  title: 'HDIM Platform - Complete Healthcare Quality Measurement Capabilities',
  description: 'All platform capabilities: HEDIS measures, care gap detection, FHIR R4 integration, revenue cycle, custom measure builder, and enterprise security.',
}

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

const GROUPS = [
  {
    title: 'Core Clinical',
    description: 'The foundation of quality measurement and care gap management.',
    ids: ['quality-engine', 'care-gaps', 'fhir'],
  },
  {
    title: 'New Capabilities',
    description: 'Recently shipped features expanding platform scope.',
    ids: ['revenue-cycle', 'measure-builder', 'cmo-onboarding', 'operations'],
  },
  {
    title: 'Infrastructure',
    description: 'Enterprise-grade operations, security, and clinical workspace.',
    ids: ['clinical-portal', 'security'],
  },
]

export default function PlatformPage() {
  return (
    <LandingPageClient>
      {/* Hero */}
      <section id="main-content" className="relative bg-gradient-to-br from-primary via-primary-600 to-primary-800 pt-32 pb-20">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 left-0 w-96 h-96 bg-accent rounded-full filter blur-3xl" />
        </div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h1 className="text-4xl md:text-5xl font-bold text-white leading-tight">
            Platform Capabilities
          </h1>
          <p className="mt-6 text-lg text-white/80 max-w-2xl mx-auto">
            A complete quality measurement and clinical operations platform.
            9 capability areas, 59 microservices, 62 API endpoints.
          </p>
        </div>
      </section>

      <MetricsBar />

      {/* Capability Groups */}
      {GROUPS.map((group) => {
        const groupCaps = CAPABILITIES.filter((c) => group.ids.includes(c.id))
        return (
          <section key={group.title} className="py-16 bg-white even:bg-gray-50">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
              <div className="mb-10">
                <h2 className="text-2xl md:text-3xl font-bold text-gray-900">{group.title}</h2>
                <p className="text-gray-600 mt-2">{group.description}</p>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {groupCaps.map((cap) => {
                  const Icon = CAPABILITY_ICONS[cap.id] || Zap
                  return (
                    <div
                      key={cap.id}
                      className="p-8 bg-white rounded-2xl border border-gray-200 shadow-sm hover:shadow-lg transition-shadow duration-300"
                    >
                      <div className="flex items-start gap-4 mb-4">
                        <div className="w-12 h-12 rounded-xl bg-primary-50 flex items-center justify-center flex-shrink-0">
                          <Icon className="w-6 h-6 text-primary" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="text-xl font-bold text-gray-900">{cap.title}</h3>
                            {cap.isNew && (
                              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-accent-50 text-accent-600">
                                {cap.badge}
                              </span>
                            )}
                          </div>
                          <p className="text-gray-600 mt-1">{cap.description}</p>
                        </div>
                      </div>
                      <p className="text-sm text-gray-500 leading-relaxed pl-16">{cap.detail}</p>
                    </div>
                  )
                })}
              </div>
            </div>
          </section>
        )
      })}

      {/* Live Platform Screenshots */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <p className="text-sm font-semibold tracking-widest uppercase text-primary mb-3">
              Live Portal
            </p>
            <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-4">
              Live Platform Screenshots
            </h2>
            <p className="text-lg text-gray-600 max-w-3xl mx-auto">
              Every screen from our production Clinical Portal — not mockups.
            </p>
          </div>

          <ScreenshotGallery
            heroScreenshot={{
              src: "/images/dashboard-modern/01-dashboard-overview-clean.jpg",
              alt: "HDIM Clinical Portal Dashboard",
              width: 1915,
              height: 939,
              title: "Clinical Dashboard",
              description: "Role-based dashboard with real-time KPIs, care gap alerts, and quick actions",
              badge: "LIVE",
            }}
            rows={[
              {
                columns: 3,
                screenshots: [
                  {
                    src: "/images/dashboard-modern/07-patient-management.png",
                    alt: "Patient Management view",
                    width: 1918,
                    height: 920,
                    title: "Patient Management",
                    description: "Comprehensive patient lookup and health record access",
                  },
                  {
                    src: "/images/dashboard-modern/05-quality-measures-library.jpg",
                    alt: "Quality Measures Library",
                    width: 1918,
                    height: 920,
                    title: "Quality Measures Library",
                    description: "HEDIS measures with national benchmarks and star ratings",
                  },
                  {
                    src: "/images/dashboard-modern/10-care-gap-closure.jpg",
                    alt: "Care Gap Closure workflow",
                    width: 1915,
                    height: 921,
                    title: "Gap Closure Workflow",
                    description: "One-click closure with audit documentation",
                  },
                ],
              },
            ]}
          />
        </div>
      </section>

      <ProgressiveCTA variant="full" />

      <SiteFooter />
    </LandingPageClient>
  )
}
