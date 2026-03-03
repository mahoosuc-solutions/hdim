import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';

export const metadata = {
  title: 'Terms of Service | HDIM',
  description: 'HDIM terms of service — usage terms, licensing, and service agreements.',
};

export default function TermsPage() {
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

            <h1 className="text-4xl font-bold text-gray-900 mb-4">Terms of Service</h1>
            <p className="text-sm text-gray-500 mb-12">Last updated: March 2026</p>

            <div className="prose prose-gray max-w-none space-y-8">
              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Agreement</h2>
                <p className="text-gray-700 leading-relaxed">
                  These Terms of Service (&quot;Terms&quot;) govern your use of the HDIM website and marketing
                  materials operated by Mahoosuc Solutions LLC. By accessing this website, you agree to these
                  Terms.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Platform Services</h2>
                <p className="text-gray-700 leading-relaxed">
                  Use of the HDIM platform for clinical quality measurement, care gap detection, and related
                  healthcare analytics is governed by a separate Master Service Agreement (MSA) and Business
                  Associate Agreement (BAA) executed between Mahoosuc Solutions LLC and your organization.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Intellectual Property</h2>
                <p className="text-gray-700 leading-relaxed">
                  All content on this website — including text, graphics, architecture diagrams, and software
                  descriptions — is the property of Mahoosuc Solutions LLC and is protected by applicable
                  intellectual property laws.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Disclaimer</h2>
                <p className="text-gray-700 leading-relaxed">
                  ROI projections, case study scenarios, and performance metrics presented on this website are
                  illustrative and based on modeled outcomes. Actual results will vary based on your
                  organization&apos;s data quality, patient population, and implementation approach.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Limitation of Liability</h2>
                <p className="text-gray-700 leading-relaxed">
                  Mahoosuc Solutions LLC provides this website and its content on an &quot;as-is&quot; basis.
                  We make no warranties regarding the accuracy or completeness of website content.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Contact</h2>
                <p className="text-gray-700 leading-relaxed">
                  For questions about these Terms, contact us at{' '}
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
