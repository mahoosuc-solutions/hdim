import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';

export const metadata = {
  title: 'Privacy Policy | HDIM',
  description: 'HDIM privacy policy — how we handle data, HIPAA compliance, and your rights.',
};

export default function PrivacyPage() {
  return (
    <>
      <Header />
      <main className="min-h-screen bg-white pt-24 pb-16">
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
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Data Security</h2>
                <p className="text-gray-700 leading-relaxed">
                  We employ TLS 1.3 encryption for all data in transit, multi-tenant isolation at the database
                  level, role-based access control, and comprehensive audit logging for all data access events.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Contact</h2>
                <p className="text-gray-700 leading-relaxed">
                  For privacy-related inquiries, contact us at{' '}
                  <a href="mailto:info@mahoosuc.solutions" className="text-blue-600 hover:underline">
                    info@mahoosuc.solutions
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
