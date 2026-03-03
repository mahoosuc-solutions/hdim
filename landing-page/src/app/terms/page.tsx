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
                <h2 className="text-2xl font-bold text-gray-900 mb-4">1. Agreement</h2>
                <p className="text-gray-700 leading-relaxed">
                  These Terms of Service (&quot;Terms&quot;) govern your use of the HDIM website and marketing
                  materials operated by Mahoosuc Solutions LLC (&quot;Company,&quot; &quot;we,&quot; &quot;us&quot;).
                  By accessing this website, you agree to these Terms. If you do not agree, do not use this website.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">2. Platform Services</h2>
                <p className="text-gray-700 leading-relaxed">
                  Use of the HDIM platform for clinical quality measurement, care gap detection, and related
                  healthcare analytics is governed by a separate Master Service Agreement (MSA) and Business
                  Associate Agreement (BAA) executed between Mahoosuc Solutions LLC and your organization.
                  These website Terms do not govern platform usage.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">3. Acceptable Use</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  You agree not to:
                </p>
                <ul className="list-disc pl-6 space-y-2 text-gray-700">
                  <li>Use this website for any unlawful purpose or in violation of any applicable regulations</li>
                  <li>Attempt to gain unauthorized access to any systems or networks connected to this website</li>
                  <li>Reproduce, distribute, or create derivative works from website content without written permission</li>
                  <li>Use automated tools to scrape, crawl, or extract data from this website beyond standard search engine indexing</li>
                  <li>Transmit any malicious code, viruses, or harmful content through this website</li>
                </ul>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">4. Intellectual Property</h2>
                <p className="text-gray-700 leading-relaxed">
                  All content on this website — including text, graphics, architecture diagrams, software
                  descriptions, trade names, and logos — is the property of Mahoosuc Solutions LLC and is
                  protected by applicable intellectual property laws. &quot;HDIM&quot; and
                  &quot;HealthData-in-Motion&quot; are trademarks of Mahoosuc Solutions LLC.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">5. Disclaimer of Warranties</h2>
                <p className="text-gray-700 leading-relaxed mb-4">
                  This website and its content are provided on an &quot;as-is&quot; and &quot;as-available&quot;
                  basis. Mahoosuc Solutions LLC makes no warranties, express or implied, regarding:
                </p>
                <ul className="list-disc pl-6 space-y-2 text-gray-700">
                  <li>The accuracy, completeness, or reliability of website content</li>
                  <li>Uninterrupted or error-free operation of this website</li>
                  <li>The results of using information presented on this website</li>
                </ul>
                <p className="text-gray-700 leading-relaxed mt-4">
                  ROI projections, case study scenarios, and performance metrics presented on this website are
                  illustrative and based on modeled outcomes. Actual results will vary based on your
                  organization&apos;s data quality, patient population, EHR configuration, and implementation
                  approach. These projections do not constitute a guarantee of performance.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">6. Limitation of Liability</h2>
                <p className="text-gray-700 leading-relaxed">
                  To the maximum extent permitted by applicable law, Mahoosuc Solutions LLC shall not be liable
                  for any indirect, incidental, special, consequential, or punitive damages arising from your
                  use of this website, including but not limited to loss of revenue, data, or business
                  opportunity, regardless of the cause of action or theory of liability. Our total aggregate
                  liability for any claims arising from use of this website shall not exceed one hundred dollars
                  ($100). Liability related to HDIM platform services is governed by the applicable MSA.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">7. Indemnification</h2>
                <p className="text-gray-700 leading-relaxed">
                  You agree to indemnify, defend, and hold harmless Mahoosuc Solutions LLC, its officers,
                  directors, employees, and agents from any claims, damages, losses, or expenses (including
                  reasonable attorneys&apos; fees) arising from your use of this website or violation of these
                  Terms.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">8. Service Level Commitments</h2>
                <p className="text-gray-700 leading-relaxed">
                  Service level agreements (SLAs) for the HDIM platform — including uptime guarantees,
                  response times, and support availability — are defined in the applicable MSA and are not
                  covered by these website Terms. Website availability is provided on a best-effort basis.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">9. Third-Party Links</h2>
                <p className="text-gray-700 leading-relaxed">
                  This website may contain links to third-party websites or services. We are not responsible
                  for the content, privacy practices, or availability of third-party sites. Links do not imply
                  endorsement.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">10. Governing Law &amp; Jurisdiction</h2>
                <p className="text-gray-700 leading-relaxed">
                  These Terms are governed by and construed in accordance with the laws of the State of Maine,
                  United States, without regard to its conflict of law provisions. Any disputes arising from
                  these Terms shall be resolved exclusively in the state or federal courts located in
                  Cumberland County, Maine.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">11. Changes to These Terms</h2>
                <p className="text-gray-700 leading-relaxed">
                  We reserve the right to modify these Terms at any time. Material changes will be posted on
                  this page with an updated revision date. Continued use of the website after changes
                  constitutes acceptance of the revised Terms.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">12. Severability</h2>
                <p className="text-gray-700 leading-relaxed">
                  If any provision of these Terms is found to be unenforceable or invalid, that provision will
                  be limited or eliminated to the minimum extent necessary, and the remaining provisions will
                  remain in full force and effect.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-4">13. Contact</h2>
                <p className="text-gray-700 leading-relaxed">
                  For questions about these Terms, contact us at{' '}
                  <a href="mailto:info@mahoosuc.solutions?subject=Terms Inquiry" className="text-blue-600 hover:underline">
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
