import type { Metadata } from 'next'
import Link from 'next/link'
import {
  ArrowRight,
  CheckCircle2,
  Clock,
  HeartPulse,
  Hospital,
  Shield,
  Stethoscope,
  UserRound,
} from 'lucide-react'
import PortalNav from '../../../components/PortalNav'

export const metadata: Metadata = {
  title: 'Care Transitions Pilot | HDIM',
  description:
    'A structured 90-day care transitions pilot for Medicare Advantage plans, ACOs, and health systems focused on post-discharge engagement, comprehension, and secure care-team escalation.',
  alternates: {
    canonical: '/solutions/transitions-of-care',
  },
  openGraph: {
    title: 'Care Transitions Pilot | HDIM',
    description:
      'Structured, secure pilot model for improving post-discharge engagement and care-team response workflows.',
    url: '/solutions/transitions-of-care',
  },
  twitter: {
    title: 'Care Transitions Pilot | HDIM',
    description:
      'Structured, secure pilot model for improving post-discharge engagement and care-team response workflows.',
  },
}

const pilotTimeline = [
  {
    window: 'Week 1',
    title: 'Architecture and security workshop',
    detail:
      'Confirm deployment model, data boundaries, escalation paths, and go-live gates with clinical, operations, and security stakeholders.',
  },
  {
    window: 'Weeks 2-3',
    title: 'Integration and readiness validation',
    detail:
      'Validate data flow, role-based access, auditability, and pilot workflow readiness across required teams.',
  },
  {
    window: 'Weeks 4-10',
    title: 'Pilot operation and optimization',
    detail:
      'Run live workflows for discharged members with weekly KPI review, issue resolution, and rapid operational tuning.',
  },
  {
    window: 'Weeks 11-13',
    title: 'Outcomes synthesis and scale plan',
    detail:
      'Publish pilot readout, compare outcomes against baseline, and define scale-up recommendation with deployment roadmap.',
  },
]

export default function CareTransitionsPilotPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <PortalNav />

      <main className="pt-24 pb-16">
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="rounded-2xl bg-gradient-to-br from-[#0D4F8B] to-[#0A3D6E] text-white p-8 md:p-12">
            <p className="text-teal-300 text-sm font-semibold uppercase tracking-widest mb-4">
              Solutions
            </p>
            <h1 className="text-4xl md:text-5xl font-bold leading-tight mb-4">
              Care Transitions Pilot
            </h1>
            <p className="text-lg text-blue-100 max-w-3xl">
              A 90-day pilot model for Medicare Advantage plans, ACOs, and
              health systems to improve post-discharge engagement,
              comprehension, and secure escalation to care teams.
            </p>
            <div className="mt-8 flex flex-col sm:flex-row gap-4">
              <Link
                href="/schedule"
                className="inline-flex items-center justify-center gap-2 bg-white text-[#0D4F8B] px-6 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors"
              >
                Schedule Architecture Workshop
                <ArrowRight className="w-4 h-4" />
              </Link>
              <a
                href="#deployment-models"
                className="inline-flex items-center justify-center gap-2 border border-white/40 text-white px-6 py-3 rounded-lg font-semibold hover:bg-white/10 transition-colors"
              >
                View Deployment Models
              </a>
            </div>
          </div>
        </section>

        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-10">
          <div className="grid lg:grid-cols-2 gap-8">
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">
                Core Problem
              </h2>
              <p className="text-gray-700 leading-relaxed mb-5">
                Patients frequently leave acute care settings unsure about
                medications, follow-up actions, and warning signs. This creates
                avoidable engagement failures, missed follow-up, and
                transitions-of-care risk.
              </p>
              <h3 className="text-lg font-semibold text-gray-900 mb-3">
                Pilot Objective
              </h3>
              <p className="text-gray-700 leading-relaxed">
                Demonstrate measurable improvement in post-discharge patient
                engagement and comprehension through secure AI-assisted guidance
                and care-team escalation workflows.
              </p>
            </div>

            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">
                Phase 1 Scope
              </h2>
              <div className="space-y-3 mb-6">
                {[
                  'Patient-facing plain-language summaries and contextual guidance.',
                  'Secure escalation workflow to care teams for follow-up.',
                  'Weekly KPI governance with gate-based progression.',
                ].map((item, idx) => (
                  <div key={idx} className="flex items-start gap-3">
                    <CheckCircle2 className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                    <p className="text-gray-700">{item}</p>
                  </div>
                ))}
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Out of Scope in Phase 1
              </h3>
              <p className="text-gray-700">
                Enterprise-wide rollout and production bidirectional EHR
                write-back. These are addressed only after pilot success gates
                are met.
              </p>
            </div>
          </div>
        </section>

        <section
          id="deployment-models"
          className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-10"
        >
          <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">
              Deployment Models
            </h2>
            <div className="grid md:grid-cols-3 gap-6">
              <div className="rounded-xl border border-gray-200 p-5">
                <div className="flex items-center gap-2 mb-3">
                  <Hospital className="w-5 h-5 text-[#0D4F8B]" />
                  <h3 className="font-semibold text-gray-900">Vendor-Hosted</h3>
                </div>
                <p className="text-sm text-gray-700">
                  Fastest launch model with lower internal infrastructure
                  overhead and managed application operations.
                </p>
              </div>
              <div className="rounded-xl border border-gray-200 p-5">
                <div className="flex items-center gap-2 mb-3">
                  <Stethoscope className="w-5 h-5 text-[#0D4F8B]" />
                  <h3 className="font-semibold text-gray-900">
                    Customer-Hosted Cloud
                  </h3>
                </div>
                <p className="text-sm text-gray-700">
                  Customer cloud-account ownership with managed operations and
                  control over account-level governance.
                </p>
              </div>
              <div className="rounded-xl border border-gray-200 p-5">
                <div className="flex items-center gap-2 mb-3">
                  <Shield className="w-5 h-5 text-[#0D4F8B]" />
                  <h3 className="font-semibold text-gray-900">
                    On-Prem / Private Data Center
                  </h3>
                </div>
                <p className="text-sm text-gray-700">
                  For strict locality and network controls. Air-gapped variant
                  is available for highly restricted environments.
                </p>
              </div>
            </div>
            <div className="mt-6 rounded-lg bg-blue-50 border border-blue-100 p-4 text-sm text-blue-900">
              <strong>Commercial note:</strong> pricing is finalized after the
              Week-1 architecture workshop based on deployment model, data
              sources, and operating requirements.
            </div>
          </div>
        </section>

        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-10">
          <div className="grid lg:grid-cols-2 gap-8">
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">
                Security Baseline
              </h2>
              <div className="space-y-4">
                {[
                  'Minimum-necessary PHI handling aligned to role and workflow.',
                  'Request-scoped AI context with zero-retention processing pattern.',
                  'Role-based access controls and tenant-aware isolation.',
                  'Auditability across access, prompts, responses, and escalations.',
                ].map((item, idx) => (
                  <div key={idx} className="flex items-start gap-3">
                    <Shield className="w-5 h-5 text-[#0D4F8B] mt-0.5 flex-shrink-0" />
                    <p className="text-gray-700">{item}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">
                Pilot KPI Set
              </h2>
              <div className="space-y-4">
                {[
                  '7-day post-discharge engagement rate.',
                  'Patient comprehension confirmation rate.',
                  'First-response time to patient escalation.',
                  'Escalation closure time and ownership clarity.',
                ].map((item, idx) => (
                  <div key={idx} className="flex items-start gap-3">
                    <UserRound className="w-5 h-5 text-[#0D4F8B] mt-0.5 flex-shrink-0" />
                    <p className="text-gray-700">{item}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-10">
          <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">
              Pilot Delivery Timeline
            </h2>
            <div className="grid md:grid-cols-2 gap-6">
              {pilotTimeline.map((stage, idx) => (
                <div key={idx} className="rounded-xl border border-gray-200 p-5">
                  <div className="inline-flex items-center gap-2 text-[#0D4F8B] text-sm font-semibold mb-2">
                    <Clock className="w-4 h-4" />
                    {stage.window}
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-2">
                    {stage.title}
                  </h3>
                  <p className="text-sm text-gray-700">{stage.detail}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-10">
          <div className="rounded-2xl bg-gray-900 text-white p-8 md:p-10 text-center">
            <div className="flex items-center justify-center gap-2 mb-4">
              <HeartPulse className="w-5 h-5 text-teal-300" />
              <p className="text-teal-300 text-sm font-semibold uppercase tracking-widest">
                Next Step
              </p>
            </div>
            <h2 className="text-3xl font-bold mb-4">
              Start with the Week-1 Architecture Workshop
            </h2>
            <p className="text-gray-300 max-w-2xl mx-auto mb-8">
              We align clinical workflow, security controls, and deployment
              model first, then lock pilot gates and implementation timeline.
            </p>
            <Link
              href="/schedule"
              className="inline-flex items-center gap-2 bg-white text-gray-900 px-8 py-3 rounded-lg font-semibold hover:bg-gray-100 transition-colors"
            >
              Schedule Workshop
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </section>
      </main>
    </div>
  )
}
