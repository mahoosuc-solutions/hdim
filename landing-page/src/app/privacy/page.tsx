import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';
import { EMAIL_INFO } from '@/lib/constants';

export const metadata = {
  title: 'Privacy Policy | HDIM',
  description: 'HDIM privacy policy — how we handle data, HIPAA compliance, and your rights.',
};

export default function PrivacyPage() {
  return (
    <>
      <Header />
      <main id="main-content" className="min-h-screen bg-white pt-24 pb-16">
        <div className="container-lg">
          <div className="max-w-3xl mx-auto">
            <Link href="/" className="inline-flex items-center text-blue-600 hover:underline mb-8">
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
              Home
            </Link>

            <h1 className="text-4xl font-bold text-gray-900 mb-4">Privacy Policy</h1>
            <p className="text-sm text-gray-500 mb-12">Last updated: March 2026</p>

            <div className="prose prose-gray max-w-none space-y-8">
              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Overview</h2>
                <p className="text-gray-700 leading-relaxed">
                  HealthData-in-Motion (&quot;HDIM&quot;), operated by Mahoosuc Solutions LLC, is committed to
                  protecting the privacy and security of your information. This policy describes how we collect,
                  use, and safeguard data when you visit our website or use our platform.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">HIPAA Compliance</h2>
                <p className="text-gray-700 leading-relaxed">
                  HDIM is designed to be HIPAA-compliant. Protected Health Information (PHI) processed by our
                  platform is handled in accordance with HIPAA Privacy and Security Rules. We execute Business
                  Associate Agreements (BAAs) with all customers who process PHI through our platform.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Data We Collect</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  <strong>Website visitors:</strong> We may collect standard web analytics data (page views,
                  referral sources, device type) to improve our website. We do not collect PHI through our
                  marketing website.
                </p>
                <p className="text-gray-700 leading-relaxed">
                  <strong>Platform customers:</strong> Clinical data processed by HDIM remains within your
                  infrastructure. HDIM queries your FHIR servers directly and does not centralize or replicate
                  your clinical data.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Data Retention</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  <strong>Platform data:</strong> PHI cache entries are retained for a maximum of 5 minutes
                  (per HIPAA compliance requirements). Quality measure results, care gap reports, and audit
                  logs are retained within your infrastructure according to your organization&apos;s retention
                  policies. HDIM does not independently retain copies of your clinical data.
                </p>
                <p className="text-gray-700 leading-relaxed mb-4">
                  <strong>Account data:</strong> Customer account information (organization name, contact
                  details, contract terms) is retained for the duration of the service agreement plus 3 years
                  for compliance and audit purposes.
                </p>
                <p className="text-gray-700 leading-relaxed">
                  <strong>Website analytics:</strong> Web analytics data is retained for up to 26 months and
                  is not linked to individual identities.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Cookies &amp; Tracking</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  Our marketing website uses privacy-respecting, cookieless analytics (Plausible). We do
                  not use tracking cookies, advertising cookies, or sell data to third parties.
                </p>
                <ul className="list-disc pl-6 space-y-2 text-gray-700">
                  <li><strong>Essential cookies:</strong> Required for site functionality (session management, security). Cannot be disabled.</li>
                  <li><strong>Optional cookies:</strong> If we introduce optional analytics or marketing tools in the future, they will only activate after you grant consent via our cookie banner.</li>
                </ul>
                <p className="text-gray-700 leading-relaxed mt-4">
                  You can manage your cookie preferences at any time via the Cookie Settings link in our
                  footer. The HDIM platform itself does not use cookies — authentication is handled via JWT
                  tokens within your infrastructure.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Subprocessors</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  HDIM deploys on your infrastructure. For customers using our hosted services, we may use the
                  following categories of subprocessors:
                </p>
                <ul className="list-disc pl-6 space-y-2 text-gray-700">
                  <li><strong>Cloud infrastructure:</strong> Compute and storage providers (customer-selected: AWS, Azure, or GCP)</li>
                  <li><strong>Monitoring:</strong> Application performance and uptime monitoring (no PHI transmitted)</li>
                  <li><strong>Support:</strong> Customer support ticketing (no PHI included in support tickets)</li>
                </ul>
                <p className="text-gray-700 leading-relaxed mt-4">
                  A current list of specific subprocessors is available upon request and is included in your
                  Business Associate Agreement.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Data Security</h2>
                <p className="text-gray-700 leading-relaxed">
                  We employ TLS 1.3 encryption for all data in transit, AES-256 encryption for data at rest,
                  multi-tenant isolation at the database level with row-level security, role-based access
                  control with five privilege tiers, and comprehensive audit logging for 100% of data access
                  events per HIPAA &sect;164.312(b).
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Breach Notification</h2>
                <p className="text-gray-700 leading-relaxed">
                  In the event of a confirmed security breach involving PHI, Mahoosuc Solutions LLC will notify
                  affected customers within 72 hours of discovery, consistent with HIPAA Breach Notification
                  Rule requirements (&sect;164.404). Notification will include the nature of the breach,
                  types of data involved, steps taken to mitigate harm, and recommended protective actions.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Your Rights</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  You have the right to:
                </p>
                <ul className="list-disc pl-6 space-y-2 text-gray-700">
                  <li><strong>Access:</strong> Request a copy of personal data we hold about you or your organization</li>
                  <li><strong>Correction:</strong> Request correction of inaccurate personal data</li>
                  <li><strong>Deletion:</strong> Request deletion of your account data (subject to legal retention requirements)</li>
                  <li><strong>Portability:</strong> Receive your data in a structured, machine-readable format</li>
                  <li><strong>Objection:</strong> Object to processing of your personal data for marketing purposes</li>
                </ul>
                <p className="text-gray-700 leading-relaxed mt-4">
                  For PHI-related rights (access, amendment, accounting of disclosures), these are governed
                  by your organization&apos;s HIPAA policies, as HDIM acts as a Business Associate.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Changes to This Policy</h2>
                <p className="text-gray-700 leading-relaxed">
                  We may update this policy from time to time. Material changes will be communicated via
                  email to platform customers and posted on this page with an updated revision date.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Contact</h2>
                <p className="text-gray-700 leading-relaxed">
                  For privacy-related inquiries, data subject requests, or to obtain our current subprocessor
                  list, contact us at{' '}
                  <a href={`mailto:${EMAIL_INFO}?subject=Privacy Inquiry`} className="text-blue-600 hover:underline">
                    {EMAIL_INFO}
                  </a>.
                </p>
              </section>
            </div>
          </div>
        </div>
      </main>
    </>
  );
}
