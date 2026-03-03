import { MetadataRoute } from 'next'

export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = 'https://healthdatainmotion.com'

  const ehrVendors = [
    'epic',
    'oracle-health',
    'meditech',
    'athenahealth',
    'allscripts',
    'eclinicalworks',
    'nextgen',
    'intersystems',
  ]

  return [
    { url: baseUrl, lastModified: new Date(), changeFrequency: 'weekly', priority: 1.0 },
    { url: `${baseUrl}/pricing`, lastModified: new Date(), changeFrequency: 'monthly', priority: 0.9 },
    { url: `${baseUrl}/about`, lastModified: new Date(), changeFrequency: 'monthly', priority: 0.8 },
    { url: `${baseUrl}/integrations`, lastModified: new Date(), changeFrequency: 'monthly', priority: 0.8 },
    ...ehrVendors.map((vendor) => ({
      url: `${baseUrl}/integrations/${vendor}`,
      lastModified: new Date(),
      changeFrequency: 'monthly' as const,
      priority: 0.7,
    })),
    { url: `${baseUrl}/privacy`, lastModified: new Date(), changeFrequency: 'yearly', priority: 0.3 },
    { url: `${baseUrl}/terms`, lastModified: new Date(), changeFrequency: 'yearly', priority: 0.3 },
  ]
}
