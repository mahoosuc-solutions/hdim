import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';
import type { IntegrationPageData, IntegrationSection } from '@/lib/data/intersystems-integration';

function renderInlineMarkdown(text: string): React.ReactNode[] {
  const parts: React.ReactNode[] = [];
  const regex = /\*\*(.+?)\*\*/g;
  let lastIndex = 0;
  let match;
  let key = 0;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    parts.push(<strong key={key++}>{match[1]}</strong>);
    lastIndex = regex.lastIndex;
  }
  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }
  return parts;
}

function renderTextBlock(block: string, blockIndex: number): React.ReactNode {
  const lines = block.split('\n');
  const bulletLines = lines.filter((l) => l.startsWith('• '));

  if (bulletLines.length > 0 && bulletLines.length === lines.length) {
    return (
      <ul key={blockIndex} className="list-disc pl-6 space-y-2 mb-4">
        {lines.map((line, i) => (
          <li key={i} className="text-gray-700 leading-relaxed">
            {renderInlineMarkdown(line.replace(/^• /, ''))}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <p key={blockIndex} className="text-gray-700 leading-relaxed mb-4">
      {renderInlineMarkdown(block)}
    </p>
  );
}

function SectionRenderer({ section }: { section: IntegrationSection }) {
  switch (section.type) {
    case 'text':
      return (
        <div className="prose prose-lg max-w-none">
          {section.content.split('\n\n').map((block, i) => renderTextBlock(block, i))}
        </div>
      );

    case 'steps':
      return (
        <div>
          <p className="text-gray-700 mb-6">{section.content}</p>
          <ol className="space-y-4">
            {section.items?.map((step, i) => {
              const [title, ...rest] = step.split(' — ');
              const description = rest.join(' — ');
              return (
                <li key={i} className="flex gap-4">
                  <span className="flex-shrink-0 w-8 h-8 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold text-sm">
                    {i + 1}
                  </span>
                  <div>
                    <span className="font-semibold text-gray-900">{title}</span>
                    {description && (
                      <span className="text-gray-600"> — {description}</span>
                    )}
                  </div>
                </li>
              );
            })}
          </ol>
        </div>
      );

    case 'table':
      return (
        <div>
          <p className="text-gray-700 mb-6">{section.content}</p>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-gray-50">
                  <th className="text-left py-3 px-4 font-semibold text-gray-900 border-b">Protocol</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-900 border-b">Description</th>
                </tr>
              </thead>
              <tbody>
                {section.tableData?.map((row, i) => (
                  <tr key={i} className="border-b hover:bg-gray-50">
                    <td className="py-3 px-4 font-medium text-gray-900">{row.label}</td>
                    <td className="py-3 px-4 text-gray-700">{row.value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      );

    case 'code':
      return (
        <div>
          <p className="text-gray-700 mb-4">{section.content}</p>
          <pre className="bg-gray-900 text-green-400 rounded-lg p-6 overflow-x-auto text-sm leading-relaxed">
            <code>{section.codeSnippet}</code>
          </pre>
        </div>
      );

    case 'diagram':
      return (
        <div>
          <p className="text-gray-700 mb-4">{section.content}</p>
          <pre className="bg-gray-50 border border-gray-200 rounded-lg p-6 overflow-x-auto text-sm font-mono text-gray-800 leading-relaxed">
            <code>{section.codeSnippet}</code>
          </pre>
        </div>
      );

    default:
      return null;
  }
}

export default function IntegrationPageLayout({ data }: { data: IntegrationPageData }) {
  return (
    <>
      <Header />
      <main>
        {/* Hero */}
        <section className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-20">
          <div className="container-lg">
            <div className="max-w-4xl">
              <Link
                href="/integrations"
                className="inline-flex items-center text-blue-200 hover:text-white mb-6 transition"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
                All Integrations
              </Link>
              <h1 className="text-4xl md:text-5xl font-bold mb-4">
                HDIM + {data.ehrName}
              </h1>
              <p className="text-xl text-blue-100 mb-2">{data.tagline}</p>
              <p className="text-lg text-blue-200 max-w-3xl">{data.heroDescription}</p>
            </div>
          </div>
        </section>

        {/* Data Sovereignty */}
        <section className="bg-gray-900 text-white py-12">
          <div className="container-lg">
            <div className="max-w-4xl mx-auto">
              <div className="grid md:grid-cols-2 gap-8 items-center">
                <div>
                  <h2 className="text-xl font-bold mb-4">
                    Deploys on Your Infrastructure
                  </h2>
                  <p className="text-gray-300 text-sm leading-relaxed mb-4">
                    HDIM is a Java/Spring Boot platform that runs on your servers — RHEL, Ubuntu,
                    or any cloud VPC. It fronts your CDR and FHIR server, processing clinical data
                    into quality intelligence without transmitting PHI outside your network boundary.
                  </p>
                  <p className="text-gray-300 text-sm leading-relaxed">
                    This is not a SaaS integration that extracts your data. HDIM is clinical
                    infrastructure that deploys alongside your EHR and makes data accessible
                    that providers cannot reach today.
                  </p>
                </div>
                <div className="bg-gray-800 rounded-lg p-6 font-mono text-xs leading-relaxed">
                  <div className="text-gray-500 mb-2"># Your infrastructure</div>
                  <div className="text-green-400">{'┌─── Your Network Boundary ───────────┐'}</div>
                  <div className="text-green-400">{'│                                     │'}</div>
                  <div className="text-gray-300">{'│  [CDR / FHIR Server]                │'}</div>
                  <div className="text-gray-300">{'│        ↓ FHIR R4 (private network)  │'}</div>
                  <div className="text-blue-400">{'│  [HDIM Platform]                    │'}</div>
                  <div className="text-blue-400">{'│    ├─ CQL Engine (quality measures)  │'}</div>
                  <div className="text-blue-400">{'│    ├─ Care Gap Detection             │'}</div>
                  <div className="text-blue-400">{'│    ├─ Risk Stratification            │'}</div>
                  <div className="text-blue-400">{'│    └─ Quality Reporting              │'}</div>
                  <div className="text-gray-300">{'│        ↓                             │'}</div>
                  <div className="text-gray-300">{'│  [Clinical Workflows / Dashboards]   │'}</div>
                  <div className="text-green-400">{'│                                     │'}</div>
                  <div className="text-green-400">{'└─────────────────────────────────────┘'}</div>
                  <div className="text-gray-500 mt-2"># PHI never crosses this boundary</div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Sections */}
        {data.sections.map((section) => (
          <section
            key={section.id}
            id={section.id}
            className="section bg-white even:bg-gray-50"
          >
            <div className="container-lg">
              <div className="max-w-4xl mx-auto">
                <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">
                  {section.title}
                </h2>
                <SectionRenderer section={section} />
              </div>
            </div>
          </section>
        ))}

        {/* FHIR Resources */}
        <section className="section bg-white">
          <div className="container-lg">
            <div className="max-w-4xl mx-auto">
              <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">
                Supported FHIR R4 Resources
              </h2>
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {data.fhirResources.map((resource) => (
                  <div key={resource.name} className="card p-4">
                    <h3 className="font-semibold text-blue-600 mb-1">{resource.name}</h3>
                    <p className="text-sm text-gray-600">{resource.description}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Deployment Models */}
        <section className="section bg-gray-50">
          <div className="container-lg">
            <div className="max-w-4xl mx-auto">
              <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">
                Deployment Options
              </h2>
              <div className="grid md:grid-cols-3 gap-6">
                {data.deploymentModels.map((model) => (
                  <div key={model.name} className="card p-6">
                    <h3 className="text-lg font-bold text-gray-900 mb-2">{model.name}</h3>
                    <p className="text-gray-600 text-sm">{model.description}</p>
                    {model.link && (
                      <Link
                        href={model.link}
                        className="inline-flex items-center text-blue-600 text-sm font-medium mt-3 hover:underline"
                      >
                        Learn more
                        <svg className="w-3 h-3 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </Link>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="section bg-gradient-to-r from-blue-600 to-indigo-600 text-white">
          <div className="container-lg text-center">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">
              Ready to Connect HDIM with {data.ehrName}?
            </h2>
            <p className="text-xl text-blue-100 mb-8 max-w-2xl mx-auto">
              Schedule a personalized demo to see HDIM working with your {data.ehrName} environment.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <a
                href="https://calendar.app.google/zKDs6ZdXW7V61c7i7"
                target="_blank"
                rel="noopener noreferrer"
                className="px-8 py-4 bg-white text-blue-600 font-bold rounded-lg hover:bg-blue-50 transition-all inline-block"
              >
                Schedule Demo
              </a>
              <Link
                href="/integrations"
                className="px-8 py-4 border-2 border-white text-white font-bold rounded-lg hover:bg-blue-700 transition-all"
              >
                View All Integrations
              </Link>
            </div>
          </div>
        </section>
      </main>
    </>
  );
}
