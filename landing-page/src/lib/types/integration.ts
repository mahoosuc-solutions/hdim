export interface IntegrationSection {
  id: string;
  title: string;
  content: string;
  type: 'text' | 'steps' | 'table' | 'code' | 'diagram';
  items?: string[];
  tableData?: { label: string; value: string }[];
  codeSnippet?: string;
  codeLanguage?: string;
}

export interface IntegrationPageData {
  ehrName: string;
  tagline: string;
  heroDescription: string;
  sections: IntegrationSection[];
  fhirResources: { name: string; description: string }[];
  deploymentModels: { name: string; description: string; link?: string }[];
}
