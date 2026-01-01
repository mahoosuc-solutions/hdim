import React from 'react';
import Header from '@/components/layout/Header';
import HeroSection from '@/components/hero/HeroSection';
import ROICalculator from '@/components/roi/ROICalculator';
import CaseStudyCard from '@/components/social-proof/CaseStudyCard';
import ScenarioCard from '@/components/scenarios/ScenarioCard';
import { MESSAGING, CASE_STUDIES, CUSTOMIZATION_LEVELS, PRICING_TIERS, FAQ } from '@/lib/constants';

export default function Home() {
  return (
    <>
      <Header />
      <main>
        {/* Hero Section */}
        <HeroSection />

        {/* Problem Statement */}
        <section className="section bg-white">
          <div className="container-lg">
            <div className="max-w-3xl mx-auto text-center">
              <h2 className="section-title">{MESSAGING.PROBLEM_HEADLINE}</h2>
              <p className="section-subtitle text-lg">
                {MESSAGING.PROBLEM_DESCRIPTION}
              </p>
              <div className="grid md:grid-cols-3 gap-6 mt-12">
                <div className="card p-6">
                  <div className="text-3xl font-bold text-red-600 mb-2">400 hrs</div>
                  <p className="text-gray-600 text-sm">Annual manual quality measure tracking</p>
                </div>
                <div className="card p-6">
                  <div className="text-3xl font-bold text-red-600 mb-2">63%</div>
                  <p className="text-gray-600 text-sm">Payers still use spreadsheets for Star ratings</p>
                </div>
                <div className="card p-6">
                  <div className="text-3xl font-bold text-red-600 mb-2">3-5 days</div>
                  <p className="text-gray-600 text-sm">Gap identification to action for multi-EHR systems</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Solution Overview */}
        <section id="solution" className="section bg-gradient-to-b from-blue-50 to-white">
          <div className="container-lg">
            <div className="max-w-4xl mx-auto">
              <h2 className="section-title text-center mb-12">
                {MESSAGING.SOLUTION_HEADLINE}
              </h2>

              <div className="grid md:grid-cols-2 gap-12 items-center">
                {/* Left: Architecture Diagram */}
                <div className="card p-8 bg-gradient-to-br from-blue-100 to-indigo-100">
                  <div className="space-y-4">
                    <div className="bg-white rounded-lg p-4 text-center font-mono text-sm">
                      <div className="text-gray-600">Your FHIR Servers</div>
                      <div className="text-gray-900 font-bold">Epic, Cerner, Athena...</div>
                    </div>
                    <div className="flex justify-center">
                      <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                      </svg>
                    </div>
                    <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg p-4 text-center font-mono text-sm">
                      <div className="font-bold">HDIM Gateway</div>
                      <div className="text-blue-100 text-xs mt-1">Real-time orchestration</div>
                    </div>
                    <div className="flex justify-center">
                      <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                      </svg>
                    </div>
                    <div className="bg-white rounded-lg p-4 text-center font-mono text-sm">
                      <div className="text-gray-600">Your Dashboard</div>
                      <div className="text-green-600 font-bold">Real-time Insights</div>
                    </div>
                  </div>
                </div>

                {/* Right: Features */}
                <div>
                  <h3 className="text-2xl font-bold text-gray-900 mb-6">
                    {MESSAGING.SOLUTION_DESCRIPTION}
                  </h3>
                  <ul className="space-y-4">
                    {[
                      'Real-time measure calculation (no batch delays)',
                      'Data stays in your FHIR server (no centralization)',
                      'Direct queries (encrypted, HIPAA-compliant)',
                      '52 HEDIS measures included',
                      'Multi-tenant isolation (complete separation)',
                      'Unlimited custom measures via CQL',
                    ].map((feature, idx) => (
                      <li key={idx} className="flex items-start gap-3">
                        <svg
                          className="w-6 h-6 text-green-500 flex-shrink-0"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path
                            fillRule="evenodd"
                            d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                            clipRule="evenodd"
                          />
                        </svg>
                        <span className="text-gray-700 font-medium">{feature}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Deployment Models */}
        <section id="deployment" className="section bg-white">
          <div className="container-lg">
            <div className="text-center mb-12">
              <h2 className="section-title">{MESSAGING.DEPLOYMENT_HEADLINE}</h2>
              <p className="section-subtitle">
                {MESSAGING.DEPLOYMENT_DESCRIPTION}
              </p>
            </div>

            <div className="grid md:grid-cols-4 gap-6">
              {[
                { name: 'Pilot', price: '$500/mo', patients: '<50K', timeline: '2-3 weeks' },
                { name: 'Growth', price: '$2.5K/mo', patients: '50K-500K', timeline: '4-8 weeks', highlighted: true },
                { name: 'Enterprise', price: '$5-15K/mo', patients: '>500K', timeline: '8-12 weeks' },
                { name: 'Hybrid', price: '$10-20K/mo', patients: '>500K', timeline: '6-10 weeks' },
              ].map((model, idx) => (
                <div key={idx} className={`card p-6 ${model.highlighted ? 'ring-2 ring-blue-600 shadow-xl' : ''}`}>
                  <h3 className="text-xl font-bold text-gray-900 mb-2">{model.name}</h3>
                  <p className="text-3xl font-bold text-blue-600 mb-6">{model.price}</p>
                  <div className="space-y-3">
                    <div>
                      <p className="text-sm text-gray-500">Patient Volume</p>
                      <p className="font-semibold text-gray-900">{model.patients}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">Deployment Time</p>
                      <p className="font-semibold text-gray-900">{model.timeline}</p>
                    </div>
                  </div>
                  <button className={model.highlighted ? 'btn-primary w-full mt-6' : 'btn-secondary w-full mt-6'}>
                    Learn More
                  </button>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Scenarios */}
        <section id="scenarios" className="section bg-gradient-to-b from-blue-50 to-white">
          <div className="container-lg">
            <div className="text-center mb-12">
              <h2 className="section-title">Customer Scenarios</h2>
              <p className="section-subtitle">
                See how organizations like yours are using HDIM to drive clinical value and financial impact
              </p>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
              <ScenarioCard scenarioId="solo-practice" />
              <ScenarioCard scenarioId="regional-health-system" />
              <ScenarioCard scenarioId="aco-network" />
              <ScenarioCard scenarioId="payer" />
            </div>
          </div>
        </section>

        {/* Customization Roadmap */}
        <section className="section bg-white">
          <div className="container-lg">
            <div className="text-center mb-12">
              <h2 className="section-title">{MESSAGING.CUSTOMIZATION_HEADLINE}</h2>
              <p className="section-subtitle">
                {MESSAGING.CUSTOMIZATION_DESCRIPTION}
              </p>
            </div>

            <div className="grid md:grid-cols-5 gap-4">
              {CUSTOMIZATION_LEVELS.map((level) => (
                <div key={level.level} className="card p-6 hover:shadow-xl transition-all">
                  <div className="flex items-center justify-center w-10 h-10 rounded-full bg-blue-600 text-white font-bold mb-4">
                    {level.level}
                  </div>
                  <h3 className="text-lg font-bold text-gray-900 mb-2">{level.name}</h3>
                  <p className="text-sm text-gray-600 mb-4">{level.description}</p>
                  <div className="space-y-3 text-sm">
                    <div>
                      <p className="text-gray-500">Timeline</p>
                      <p className="font-semibold text-gray-900">{level.timeline}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">Cost</p>
                      <p className="font-semibold text-gray-900">{level.cost}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ROI Calculator */}
        <ROICalculator />

        {/* Case Studies */}
        <section className="section bg-white">
          <div className="container-lg">
            <div className="text-center mb-12">
              <h2 className="section-title">Customer Success Stories</h2>
              <p className="section-subtitle">
                See real-world impact across different organization types and EHRs
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
              {CASE_STUDIES.map((study) => (
                <CaseStudyCard
                  key={study.id}
                  organization={study.organization}
                  metric={study.metric}
                  challenge={study.challenge}
                  solution={study.solution}
                  results={study.results}
                  quote={study.quote}
                  quoteAuthor={study.quoteAuthor}
                />
              ))}
            </div>
          </div>
        </section>

        {/* Pricing */}
        <section id="pricing" className="section bg-gradient-to-b from-blue-50 to-white">
          <div className="container-lg">
            <div className="text-center mb-12">
              <h2 className="section-title">{MESSAGING.PRICING_HEADLINE}</h2>
              <p className="section-subtitle">
                {MESSAGING.PRICING_DESCRIPTION}
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
              {PRICING_TIERS.map((tier, idx) => (
                <div
                  key={idx}
                  className={`card p-8 ${
                    tier.highlighted ? 'ring-2 ring-blue-600 shadow-xl scale-105' : ''
                  }`}
                >
                  <h3 className="text-2xl font-bold text-gray-900 mb-2">{tier.name}</h3>
                  <p className="text-gray-600 mb-6">{tier.description}</p>
                  <div className="mb-8">
                    <p className="text-4xl font-bold text-blue-600">
                      {tier.price ? `$${tier.price}` : 'Custom'}
                    </p>
                    <p className="text-gray-600">{tier.period}</p>
                  </div>
                  <ul className="space-y-3 mb-8">
                    {tier.features.map((feature, fidx) => (
                      <li key={fidx} className="flex items-start gap-2 text-sm text-gray-700">
                        <svg
                          className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5"
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path
                            fillRule="evenodd"
                            d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                            clipRule="evenodd"
                          />
                        </svg>
                        {feature}
                      </li>
                    ))}
                  </ul>
                  <button className={tier.highlighted ? 'btn-primary w-full' : 'btn-secondary w-full'}>
                    {tier.cta}
                  </button>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* FAQ */}
        <section className="section bg-white">
          <div className="container-lg">
            <div className="text-center mb-12">
              <h2 className="section-title">Frequently Asked Questions</h2>
            </div>

            <div className="max-w-3xl mx-auto">
              {FAQ.map((item, idx) => (
                <details key={idx} className="card p-6 mb-4">
                  <summary className="cursor-pointer font-semibold text-gray-900 flex justify-between items-center">
                    {item.question}
                    <span className="ml-2">+</span>
                  </summary>
                  <p className="mt-4 text-gray-600 leading-relaxed">{item.answer}</p>
                </details>
              ))}
            </div>
          </div>
        </section>

        {/* Final CTA */}
        <section className="section bg-gradient-to-r from-blue-600 to-indigo-600 text-white">
          <div className="container-lg text-center">
            <h2 className="text-4xl font-bold mb-4">Ready to Transform Your Quality Program?</h2>
            <p className="text-xl text-blue-100 mb-8 max-w-2xl mx-auto">
              Schedule a 30-minute personalized demo to see how HDIM can impact your specific organization.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button className="px-8 py-4 bg-white text-blue-600 font-bold rounded-lg hover:bg-blue-50 transition-all">
                Schedule Demo
              </button>
              <button className="px-8 py-4 border-2 border-white text-white font-bold rounded-lg hover:bg-blue-700 transition-all">
                Contact Sales
              </button>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12">
        <div className="container-lg">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-6 h-6 bg-blue-600 rounded"></div>
                <span className="text-white font-bold">HDIM</span>
              </div>
              <p className="text-sm">Healthcare Quality Measurement Platform</p>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Product</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white">Features</a></li>
                <li><a href="#" className="hover:text-white">Pricing</a></li>
                <li><a href="#" className="hover:text-white">Security</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Company</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white">About</a></li>
                <li><a href="#" className="hover:text-white">Blog</a></li>
                <li><a href="#" className="hover:text-white">Contact</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Legal</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white">Privacy</a></li>
                <li><a href="#" className="hover:text-white">Terms</a></li>
                <li><a href="#" className="hover:text-white">Security</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-800 pt-8 text-center text-sm">
            <p>&copy; 2024 HDIM. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </>
  );
}
