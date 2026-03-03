import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';
import { CALENDAR_URL, EMAIL_INFO } from '@/lib/constants';

export const metadata = {
  title: 'About | HDIM',
  description:
    'HDIM is built by Mahoosuc Solutions LLC — a healthcare technology company focused on real-time quality measurement and FHIR interoperability.',
};

export default function AboutPage() {
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

            <h1 className="text-4xl font-bold text-gray-900 mb-6">About HDIM</h1>

            <div className="space-y-8 text-gray-700 leading-relaxed">
              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-3">Our Mission</h2>
                <p>
                  HDIM (HealthData-in-Motion) exists to close the gap between clinical data and quality outcomes.
                  Health plans, ACOs, and health systems sit on rich FHIR data — but turning that data into
                  actionable quality insights still takes weeks of manual work, fragile batch processes, and
                  expensive consulting engagements.
                </p>
                <p className="mt-4">
                  We believe quality measurement should be real-time, automated, and accessible to every
                  organization — not just those with seven-figure analytics budgets.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-3">What We Build</h2>
                <p>
                  HDIM is an enterprise healthcare interoperability platform that connects directly to your
                  FHIR R4 servers — Epic, Cerner, Athena, InterSystems, and others — to evaluate HEDIS quality
                  measures, detect care gaps, and generate quality reports in real time.
                </p>
                <ul className="list-disc pl-6 mt-4 space-y-2">
                  <li>52 HEDIS measures included, unlimited custom measures via CQL</li>
                  <li>Data stays in your FHIR server — no centralization required</li>
                  <li>Multi-tenant isolation with complete data separation</li>
                  <li>HIPAA-compliant by design: TLS encryption, audit logging, BAA included</li>
                </ul>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-3">The Company</h2>
                <p>
                  HDIM is built by Mahoosuc Solutions LLC, based in Maine. We are a healthcare technology
                  company focused on interoperability, clinical quality, and value-based care enablement.
                </p>
              </section>

              <section>
                <h2 className="text-2xl font-bold text-gray-900 mb-3">Get in Touch</h2>
                <p>
                  Interested in learning how HDIM can impact your quality program?{' '}
                  <a
                    href={CALENDAR_URL}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline font-medium"
                  >
                    Schedule a demo
                  </a>{' '}
                  or reach us at{' '}
                  <a
                    href={`mailto:${EMAIL_INFO}?subject=General Inquiry`}
                    className="text-blue-600 hover:underline font-medium"
                  >
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
