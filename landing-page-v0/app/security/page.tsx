import { Metadata } from 'next'
import {
  Shield,
  CheckCircle2,
  Lock,
  Eye,
  Users,
  FileCheck,
  Server,
} from 'lucide-react'
import { LandingPageClient } from '../components/LandingPageClient'
import { ProgressiveCTA } from '../components/ProgressiveCTA'
import { SiteFooter } from '../components/SiteFooter'

export const metadata: Metadata = {
  title: 'HDIM Security - HIPAA Engineered, CVE Remediated, Enterprise Ready',
  description: 'Security posture for the HDIM healthcare quality platform: HIPAA compliance, CVE remediation, ZAP scanning, multi-tenant isolation, and audit controls.',
}

const SECURITY_TABLE = [
  { area: 'HIPAA Compliance', status: 'Engineered', icon: Shield, description: 'Built into architecture, not bolted on after the fact.' },
  { area: 'CVE Remediation', status: 'Active', icon: FileCheck, description: 'Pre-NVD CVE packets with burn-down tracking and evidence manifests.' },
  { area: 'ZAP Scanning', status: 'Every PR', icon: Eye, description: 'OWASP ZAP scans run on every pull request before merge.' },
  { area: 'Audit Controls', status: '100% Coverage', icon: Lock, description: 'Every API call logged with resource type, action, user, and tenant.' },
  { area: 'Multi-Tenant Isolation', status: 'Database-Level', icon: Users, description: 'Tenant data isolated at the database query level. No cross-tenant access possible.' },
  { area: 'Infrastructure', status: 'Hardened', icon: Server, description: '16-class operations orchestration with header security and rate limiting.' },
]

export default function SecurityPage() {
  return (
    <LandingPageClient>
      {/* Hero */}
      <section id="main-content" className="relative bg-gradient-to-br from-primary via-primary-600 to-primary-800 pt-32 pb-20">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 right-0 w-96 h-96 bg-accent rounded-full filter blur-3xl" />
        </div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="w-16 h-16 rounded-2xl bg-white/10 flex items-center justify-center mx-auto mb-6">
            <Shield className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-4xl md:text-5xl font-bold text-white leading-tight">
            Security & Compliance
          </h1>
          <p className="mt-6 text-lg text-white/80 max-w-2xl mx-auto">
            HIPAA engineered from day one. Security evidence provided, not promised.
          </p>
        </div>
      </section>

      {/* Security Summary Table */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="section-heading">Security posture</h2>
            <p className="section-subheading mt-4">
              Enterprise-grade security across every layer of the platform.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {SECURITY_TABLE.map((item) => (
              <div key={item.area} className="p-6 bg-white rounded-xl border border-gray-200 shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-lg bg-green-50 flex items-center justify-center flex-shrink-0">
                    <item.icon className="w-5 h-5 text-green-600" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="text-base font-semibold text-gray-900">{item.area}</h3>
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-green-50 text-green-700">
                        {item.status}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600">{item.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* HIPAA Details */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
            <div>
              <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">HIPAA compliance details</h2>
              <p className="text-gray-600 mb-6">
                HDIM was designed from the ground up to meet HIPAA technical safeguard requirements.
                Compliance is architectural, not a checklist applied after development.
              </p>
              <ul className="space-y-4">
                {[
                  { rule: '164.312(a)(2)(iii) - Automatic Logoff', detail: '15-minute idle timeout with audit logging. Session timeout differentiates automatic vs. explicit logout.' },
                  { rule: '164.312(b) - Audit Controls', detail: 'HTTP Audit Interceptor provides 100% API call coverage. Every access logged with user, resource, action, and duration.' },
                  { rule: '164.312(a)(1) - Access Control', detail: 'Role-based access (SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER) with JWT authentication and gateway-trust architecture.' },
                  { rule: '164.312(e)(1) - Transmission Security', detail: 'TLS encryption on all data in transit. No-cache headers on all PHI responses.' },
                ].map((item) => (
                  <li key={item.rule} className="flex items-start gap-3">
                    <CheckCircle2 className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" />
                    <div>
                      <div className="text-sm font-semibold text-gray-900">{item.rule}</div>
                      <div className="text-sm text-gray-600">{item.detail}</div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">Multi-tenant isolation</h2>
              <p className="text-gray-600 mb-6">
                Every database query is filtered by tenant ID. There is no code path that can return
                data from one tenant to another.
              </p>
              <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
                {[
                  { label: 'Database layer', detail: 'All queries include WHERE tenant_id = :tenantId. Enforced via Spring Data specifications.' },
                  { label: 'API layer', detail: 'X-Tenant-ID header required on every request. Validated at the gateway before reaching services.' },
                  { label: 'Cache layer', detail: 'Cache keys include tenant ID. TTL limited to 5 minutes for PHI data.' },
                  { label: 'Audit layer', detail: 'Tenant ID recorded on every audit event. Cross-tenant access attempts are logged and blocked.' },
                ].map((item) => (
                  <div key={item.label} className="flex items-start gap-3">
                    <Lock className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
                    <div>
                      <div className="text-sm font-semibold text-gray-900">{item.label}</div>
                      <div className="text-sm text-gray-600">{item.detail}</div>
                    </div>
                  </div>
                ))}
              </div>

              <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6 mt-12">Security scanning</h2>
              <p className="text-gray-600 mb-6">
                Automated security scanning is integrated into the development workflow.
              </p>
              <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
                {[
                  { label: 'OWASP ZAP', detail: 'Dynamic application security testing on every pull request.' },
                  { label: 'CVE monitoring', detail: 'Pre-NVD CVE packets with immutable evidence manifests and burn-down tracking.' },
                  { label: 'Dependency scanning', detail: 'Automated vulnerability detection across all dependencies.' },
                  { label: 'ESLint security rules', detail: 'Console.log banned in frontend to prevent PHI exposure in browser DevTools.' },
                ].map((item) => (
                  <div key={item.label} className="flex items-start gap-3">
                    <Eye className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
                    <div>
                      <div className="text-sm font-semibold text-gray-900">{item.label}</div>
                      <div className="text-sm text-gray-600">{item.detail}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <ProgressiveCTA variant="compact" />

      {/* Spacer for compact CTA */}
      <div className="py-12 bg-white" />

      <SiteFooter />
    </LandingPageClient>
  )
}
