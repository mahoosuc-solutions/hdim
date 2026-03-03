import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';

const VENDORS = [
  {
    slug: 'epic',
    name: 'Epic Systems',
    segment: 'Large health systems & academic medical centers',
    description: 'Epic exposes clinical data through FHIR R4, but raw resources scattered across encounters, conditions, and observations do not tell a provider which patients have open care gaps. HDIM deploys alongside Epic and synthesizes that data into actionable quality intelligence — in real time, within your network.',
    color: 'from-red-500 to-red-700',
    choke: 'RS384 JWT backend auth, App Orchard registration',
  },
  {
    slug: 'oracle-health',
    name: 'Oracle Health (Cerner)',
    segment: 'Enterprise hospitals & federal/DoD systems',
    description: "Millennium stores rich clinical data, but quality measurement typically requires extracting it into a separate analytics warehouse. HDIM fronts the Millennium FHIR server directly — evaluating HEDIS measures against live data without building a parallel data pipeline.",
    color: 'from-orange-500 to-red-600',
    choke: 'Standard OAuth2, Cerner Code Console',
  },
  {
    slug: 'intersystems',
    name: 'InterSystems HealthShare',
    segment: 'Health information exchanges (HIEs)',
    description: "HIEs aggregate data from thousands of providers but lack the processing layer to turn that data into quality scores. HDIM plugs into IRIS for Health and evaluates CQL measures across entire exchange populations — proven at 16M+ patients with Healthix.",
    color: 'from-purple-500 to-indigo-700',
    choke: 'OAuth2 or API key, HIE-scale data volumes',
  },
  {
    slug: 'meditech',
    name: 'Meditech',
    segment: 'Community hospitals & rural health systems',
    description: "Community hospitals face the same quality reporting burden as large systems with a fraction of the IT staff. HDIM connects to Meditech Expanse via FHIR R4 and automates HEDIS evaluation that would otherwise require manual chart abstraction.",
    color: 'from-teal-500 to-cyan-700',
    choke: 'OAuth2, Expanse FHIR endpoint',
  },
  {
    slug: 'athenahealth',
    name: 'Athenahealth',
    segment: 'Ambulatory practices & community health centers',
    description: "Athenahealth is cloud-native with a standardized FHIR API across all customers — no per-site configuration. HDIM integrates once and deploys to any athena practice, making it the fastest path to quality measurement for ambulatory networks.",
    color: 'from-green-500 to-emerald-700',
    choke: 'Simple OAuth2, rate-limited API',
  },
  {
    slug: 'allscripts',
    name: 'Allscripts / Veradigm',
    segment: 'Mixed ambulatory & acute care environments',
    description: "Organizations running both Sunrise (inpatient) and TouchWorks (ambulatory) face the challenge of unified quality reporting across two platforms. HDIM normalizes data from both through a single FHIR R4 interface.",
    color: 'from-blue-500 to-blue-700',
    choke: 'OAuth2, dual-platform resource mapping',
  },
  {
    slug: 'eclinicalworks',
    name: 'eClinicalWorks',
    segment: 'Large ambulatory networks & FQHCs',
    description: "eClinicalWorks serves large multi-site ambulatory practices where quality measurement must scale across hundreds of locations. HDIM provides per-site tenant isolation while aggregating network-wide quality scores.",
    color: 'from-indigo-500 to-violet-700',
    choke: 'OAuth2, cloud-to-cloud integration',
  },
  {
    slug: 'nextgen',
    name: 'NextGen Healthcare',
    segment: 'Specialty & ambulatory practices',
    description: "NextGen practices often have specialty-specific quality measures (cardiology, behavioral health, pediatrics) that generic tools do not address. HDIM evaluates custom CQL measures alongside standard HEDIS, with optional Mirth Connect for complex routing.",
    color: 'from-sky-500 to-blue-700',
    choke: 'OAuth2, optional Mirth Connect',
  },
];

export default function IntegrationsIndexPage() {
  return (
    <>
      <Header />
      <main>
        {/* Hero */}
        <section className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-20">
          <div className="container-lg">
            <div className="max-w-4xl">
              <Link
                href="/"
                className="inline-flex items-center text-blue-200 hover:text-white mb-6 transition"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
                Home
              </Link>
              <h1 className="text-4xl md:text-5xl font-bold mb-4">
                Clinical Infrastructure That Fronts Your EHR
              </h1>
              <p className="text-xl text-blue-100 max-w-3xl">
                Your CDR and FHIR servers hold the clinical data. HDIM deploys on your infrastructure —
                on-prem or in your cloud — and transforms that data into quality intelligence, care gap
                detection, and risk stratification that providers can act on at the point of care.
              </p>
            </div>
          </div>
        </section>

        {/* Architecture Positioning */}
        <section className="bg-white border-b py-12">
          <div className="container-lg">
            <div className="max-w-4xl mx-auto">
              <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">
                How HDIM Deploys
              </h2>
              <div className="grid md:grid-cols-3 gap-8">
                <div className="text-center">
                  <div className="w-14 h-14 mx-auto mb-4 rounded-full bg-blue-100 flex items-center justify-center">
                    <svg className="w-7 h-7 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M12 5l7 7-7 7" />
                    </svg>
                  </div>
                  <h3 className="font-bold text-gray-900 mb-2">Fronts Your CDR</h3>
                  <p className="text-sm text-gray-600">
                    HDIM sits between your clinical data repository and your clinical workflows.
                    It consumes FHIR resources, evaluates CQL measures, and surfaces results — all
                    within your trust boundary.
                  </p>
                </div>
                <div className="text-center">
                  <div className="w-14 h-14 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
                    <svg className="w-7 h-7 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </div>
                  <h3 className="font-bold text-gray-900 mb-2">Data Never Leaves</h3>
                  <p className="text-sm text-gray-600">
                    Java services run on your RHEL servers or in your VPC. PHI stays within your
                    infrastructure. No data is transmitted to external systems. Full HIPAA audit trail
                    on every access.
                  </p>
                </div>
                <div className="text-center">
                  <div className="w-14 h-14 mx-auto mb-4 rounded-full bg-purple-100 flex items-center justify-center">
                    <svg className="w-7 h-7 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                  </div>
                  <h3 className="font-bold text-gray-900 mb-2">Unlocks Hidden Data</h3>
                  <p className="text-sm text-gray-600">
                    The clinical data to close care gaps already exists in your CDR — encounters,
                    conditions, observations. HDIM processes it into quality scores and care gap
                    alerts that providers can act on today.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Stats Bar */}
        <section className="bg-gray-50 border-b py-8">
          <div className="container-lg">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-center">
              <div>
                <div className="text-3xl font-bold text-blue-600">8</div>
                <div className="text-sm text-gray-600">EHR Platforms</div>
              </div>
              <div>
                <div className="text-3xl font-bold text-blue-600">On-Prem</div>
                <div className="text-sm text-gray-600">Deploys on Your Infrastructure</div>
              </div>
              <div>
                <div className="text-3xl font-bold text-blue-600">FHIR R4</div>
                <div className="text-sm text-gray-600">Standard Protocol</div>
              </div>
              <div>
                <div className="text-3xl font-bold text-blue-600">52+</div>
                <div className="text-sm text-gray-600">HEDIS Measures</div>
              </div>
            </div>
          </div>
        </section>

        {/* Vendor Grid */}
        <section className="section bg-white">
          <div className="container-lg">
            <h2 className="text-2xl font-bold text-gray-900 mb-2 text-center">
              Select Your EHR Platform
            </h2>
            <p className="text-gray-600 text-center mb-10 max-w-2xl mx-auto">
              Each integration guide covers authentication, architecture, configuration, and
              deployment — written for the engineers who will implement it.
            </p>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
              {VENDORS.map((vendor) => (
                <Link
                  key={vendor.slug}
                  href={`/integrations/${vendor.slug}`}
                  className="card p-6 hover:shadow-xl transition-all group"
                >
                  <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${vendor.color} mb-4`} />
                  <h3 className="text-xl font-bold text-gray-900 group-hover:text-blue-600 transition mb-1">
                    {vendor.name}
                  </h3>
                  <p className="text-sm text-blue-600 font-medium mb-2">{vendor.segment}</p>
                  <p className="text-gray-600 text-sm mb-3">{vendor.description}</p>
                  <p className="text-xs text-gray-400 mb-4">Auth: {vendor.choke}</p>
                  <span className="inline-flex items-center text-blue-600 font-medium text-sm group-hover:underline">
                    View Integration Guide
                    <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </span>
                </Link>
              ))}
            </div>
          </div>
        </section>

        {/* Generic FHIR */}
        <section className="section bg-gray-50">
          <div className="container-lg">
            <div className="max-w-3xl mx-auto text-center">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">Any FHIR R4 Server</h2>
              <p className="text-gray-600 mb-6">
                HDIM is built on HAPI FHIR 7.x and speaks standard FHIR R4. If your CDR or EHR
                exposes a FHIR R4 endpoint — regardless of vendor — HDIM can front it. The Java
                services deploy on your infrastructure and connect over your private network.
              </p>
              <a
                href="mailto:contact@healthdatainmotion.com?subject=Custom FHIR Integration"
                className="btn-primary inline-block"
              >
                Contact Us for Custom Integration
              </a>
            </div>
          </div>
        </section>
      </main>
    </>
  );
}
