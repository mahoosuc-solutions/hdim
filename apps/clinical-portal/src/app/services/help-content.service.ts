import { Injectable } from '@angular/core';
import { HelpSection } from '../shared/components/help-panel/help-panel.component';

/**
 * Help Content Service
 *
 * Centralizes all help content for the application
 * Provides context-specific help for different pages/features
 */
@Injectable({
  providedIn: 'root'
})
export class HelpContentService {

  /**
   * Get help content for Measure Builder page
   */
  getMeasureBuilderHelp(): HelpSection[] {
    return [
      {
        title: 'What are Custom Quality Measures?',
        type: 'info',
        content: `
          <p>Custom quality measures allow you to define your own clinical quality metrics beyond standard HEDIS and CMS measures.</p>
          <p><strong>Use cases:</strong></p>
          <ul>
            <li>Specialty-specific quality tracking (oncology, cardiology, etc.)</li>
            <li>Practice-specific performance goals</li>
            <li>Patient safety indicators</li>
            <li>Value-based care programs</li>
          </ul>
        `,
        link: {
          text: 'View Custom Measures Examples',
          url: '/docs/custom-measures'
        }
      },
      {
        title: 'Creating Your First Measure',
        type: 'tip',
        content: `
          <p><strong>Step 1:</strong> Click "Create New Measure" button</p>
          <p><strong>Step 2:</strong> Fill in basic information:</p>
          <ul>
            <li><strong>Name:</strong> Descriptive name (e.g., "CDC-A1C - Diabetes HbA1c Control")</li>
            <li><strong>Category:</strong> Select or create a category (Diabetes, Hypertension, etc.)</li>
            <li><strong>Description:</strong> Explain what the measure tracks</li>
            <li><strong>Year:</strong> Measurement year (typically current year)</li>
          </ul>
          <p><strong>Step 3:</strong> (Optional) Add CQL logic for automated calculations</p>
          <p><strong>Step 4:</strong> Save as Draft or Publish</p>
        `
      },
      {
        title: 'Understanding Measure Status',
        type: 'info',
        content: `
          <p><strong>DRAFT:</strong> Measure is being created/edited. Not used in evaluations.</p>
          <p><strong>PUBLISHED:</strong> Measure is active and available for use in quality evaluations.</p>
          <p><strong>ARCHIVED:</strong> Measure is no longer active but kept for historical records.</p>
          <p>💡 <em>Tip: Start with DRAFT status while testing your measure logic</em></p>
        `
      },
      {
        title: 'What is CQL?',
        type: 'info',
        content: `
          <p><strong>Clinical Quality Language (CQL)</strong> is a standard for expressing clinical logic used in quality measures.</p>
          <p><strong>Key benefits:</strong></p>
          <ul>
            <li>Automated patient eligibility determination</li>
            <li>Precise clinical criteria definition</li>
            <li>Standardized across healthcare systems</li>
          </ul>
          <p><strong>Example CQL:</strong></p>
          <pre style="background: #f3f4f6; padding: 12px; border-radius: 4px; font-size: 12px;">
define "Has Diabetes":
  exists([Condition: "Diabetes Mellitus"])

define "HbA1c Less Than 8":
  exists([Observation: "HbA1c"] O
    where O.value < 8.0%)
          </pre>
        `,
        link: {
          text: 'Learn CQL Basics',
          url: 'https://cql.hl7.org/index.html'
        }
      },
      {
        title: 'Working with Test Data',
        type: 'tip',
        content: `
          <p>We've created comprehensive test patients for you:</p>
          <ul>
            <li><strong>Patient 179 (Thomas Anderson):</strong> Diabetes + Hypertension</li>
            <li><strong>Patient 180 (Sofia Martinez):</strong> Pregnant</li>
            <li><strong>Patient 181 (Emily Chen):</strong> Pediatric Asthma</li>
            <li><strong>Patient 182 (Robert Johnson):</strong> Elderly with CKD</li>
            <li><strong>Patient 183 (Sarah Williams):</strong> Mental Health</li>
          </ul>
          <p>Use these patients to test your custom measures before deploying to production.</p>
        `,
        link: {
          text: 'View Test Data Details',
          url: '/docs/test-data'
        }
      },
      {
        title: 'Common Issues & Solutions',
        type: 'warning',
        content: `
          <p><strong>Issue:</strong> CQL validation error</p>
          <p><strong>Solution:</strong> Check syntax, ensure code systems are valid, verify value set bindings</p>
          <hr style="margin: 12px 0; border: none; border-top: 1px solid #e5e7eb;">
          <p><strong>Issue:</strong> No patients match measure criteria</p>
          <p><strong>Solution:</strong> Review Initial Population logic, check FHIR resource availability</p>
          <hr style="margin: 12px 0; border: none; border-top: 1px solid #e5e7eb;">
          <p><strong>Issue:</strong> Measure takes too long to evaluate</p>
          <p><strong>Solution:</strong> Optimize CQL queries, add date range filters, use indexes</p>
        `
      }
    ];
  }

  /**
   * Get help content for Patients page
   */
  getPatientsHelp(): HelpSection[] {
    return [
      {
        title: 'Master Patient Index (MPI)',
        type: 'info',
        content: `
          <p>The MPI identifies and links duplicate patient records to maintain data quality.</p>
          <p><strong>Features:</strong></p>
          <ul>
            <li><strong>Auto-Detection:</strong> Click "Detect Duplicates" to find likely matches</li>
            <li><strong>Master Records:</strong> Primary patient record (green badge)</li>
            <li><strong>Duplicate Records:</strong> Linked duplicates (orange badge)</li>
            <li><strong>Matching Algorithm:</strong> Uses name, DOB, MRN, and gender (85% threshold)</li>
          </ul>
        `
      },
      {
        title: 'Detecting Duplicate Patients',
        type: 'tip',
        content: `
          <p><strong>Step 1:</strong> Click the purple "Detect Duplicates" button at the top</p>
          <p><strong>Step 2:</strong> Wait for the algorithm to analyze patient records</p>
          <p><strong>Step 3:</strong> Review results:</p>
          <ul>
            <li>Green rows = Master records</li>
            <li>Orange rows = Linked duplicates</li>
          </ul>
          <p><strong>Step 4:</strong> Use "Show Master Records Only" filter to view consolidated list</p>
          <p>💡 <em>Tip: The algorithm scores matches 0-100%. Scores ≥85% auto-link.</em></p>
        `
      },
      {
        title: 'Understanding MRN (Medical Record Number)',
        type: 'info',
        content: `
          <p><strong>MRN</strong> is a unique identifier assigned to each patient by your healthcare organization.</p>
          <p><strong>Best practices:</strong></p>
          <ul>
            <li>Always verify MRN matches before linking duplicates</li>
            <li>MRN format varies by organization (e.g., MRN-12345)</li>
            <li>MRN should never change for a patient</li>
          </ul>
          <p><strong>In test data:</strong> MRNs start with "TEST-" prefix (e.g., TEST-1001)</p>
        `
      },
      {
        title: 'Patient Detail View',
        type: 'tip',
        content: `
          <p>Click any patient row to view comprehensive details:</p>
          <ul>
            <li><strong>Demographics:</strong> Name, DOB, gender, contact info</li>
            <li><strong>Clinical Summary:</strong> Conditions, medications, allergies</li>
            <li><strong>Quality Measures:</strong> Current compliance status</li>
            <li><strong>Care Gaps:</strong> Missing screenings or interventions</li>
            <li><strong>MPI Status:</strong> Master/duplicate linkage information</li>
          </ul>
        `
      }
    ];
  }

  /**
   * Get help content for Evaluations page
   */
  getEvaluationsHelp(): HelpSection[] {
    return [
      {
        title: 'Running Quality Measure Evaluations',
        type: 'info',
        content: `
          <p>Evaluations calculate quality measure compliance for individual patients or populations.</p>
          <p><strong>Evaluation types:</strong></p>
          <ul>
            <li><strong>Single Patient:</strong> Evaluate one patient against selected measures</li>
            <li><strong>Batch:</strong> Evaluate multiple patients simultaneously</li>
            <li><strong>Population:</strong> Evaluate entire patient panel</li>
          </ul>
        `
      },
      {
        title: 'Understanding Evaluation Results',
        type: 'info',
        content: `
          <p><strong>Initial Population:</strong> Patients who meet age/demographic criteria</p>
          <p><strong>Denominator:</strong> Patients eligible for the measure</p>
          <p><strong>Numerator:</strong> Patients who meet the quality criteria</p>
          <p><strong>Exclusions:</strong> Patients excluded due to specific conditions</p>
          <p><strong>Compliance Rate:</strong> (Numerator / Denominator) × 100%</p>
          <hr style="margin: 12px 0;">
          <p><strong>Example:</strong></p>
          <p>Initial Pop: 100 patients<br>
          Denominator: 80 eligible<br>
          Numerator: 72 compliant<br>
          <strong>Compliance: 90%</strong> (72/80)</p>
        `
      },
      {
        title: 'Real-Time vs Batch Processing',
        type: 'tip',
        content: `
          <p><strong>Real-Time (WebSocket):</strong></p>
          <ul>
            <li>See evaluation progress live</li>
            <li>Best for small batches (&lt;100 patients)</li>
            <li>Immediate feedback</li>
          </ul>
          <p><strong>Batch Processing:</strong></p>
          <ul>
            <li>Processes in background</li>
            <li>Handles large populations (1000+ patients)</li>
            <li>Results available when complete</li>
          </ul>
          <p>💡 <em>Use batch mode for monthly/quarterly reporting</em></p>
        `
      }
    ];
  }

  /**
   * Get help content for Reports page
   */
  getReportsHelp(): HelpSection[] {
    return [
      {
        title: 'Generating Quality Reports',
        type: 'info',
        content: `
          <p>Reports provide detailed analysis of quality measure performance.</p>
          <p><strong>Report types:</strong></p>
          <ul>
            <li><strong>Patient Report:</strong> Individual patient's quality metrics</li>
            <li><strong>Population Report:</strong> Aggregate statistics for patient panel</li>
            <li><strong>Trend Report:</strong> Performance over time</li>
            <li><strong>Care Gap Report:</strong> Patients needing interventions</li>
          </ul>
        `
      },
      {
        title: 'Exporting Reports',
        type: 'tip',
        content: `
          <p>Export reports in multiple formats:</p>
          <ul>
            <li><strong>CSV:</strong> For Excel, data analysis</li>
            <li><strong>Excel (XLSX):</strong> Formatted spreadsheets with charts</li>
            <li><strong>PDF:</strong> Professional reports for sharing</li>
            <li><strong>JSON:</strong> For API integration, custom processing</li>
          </ul>
          <p>💡 <em>Use CSV for importing into other systems</em></p>
        `
      },
      {
        title: 'Saved Reports',
        type: 'info',
        content: `
          <p>Save reports for future reference and tracking:</p>
          <ul>
            <li>Name your reports descriptively (e.g., "Q4 2025 Diabetes Report")</li>
            <li>Add notes about context or findings</li>
            <li>Reports automatically track creation date and author</li>
            <li>Access saved reports from the Reports list</li>
          </ul>
        `
      }
    ];
  }

  /**
   * Get help content for Dashboard
   */
  getDashboardHelp(): HelpSection[] {
    return [
      {
        title: 'Dashboard Overview',
        type: 'info',
        content: `
          <p>The dashboard provides at-a-glance insights into your quality measure performance.</p>
          <p><strong>Key metrics:</strong></p>
          <ul>
            <li><strong>Overall Compliance:</strong> Average across all measures</li>
            <li><strong>Total Evaluations:</strong> Number of quality assessments completed</li>
            <li><strong>Care Gaps:</strong> Patients needing attention</li>
            <li><strong>Trend Charts:</strong> Performance over time</li>
          </ul>
        `
      },
      {
        title: 'Understanding the Metrics',
        type: 'info',
        content: `
          <p><strong>Compliance Rate:</strong> Percentage of eligible patients meeting quality criteria</p>
          <p><strong>Color Coding:</strong></p>
          <ul>
            <li>🟢 <strong>Green (≥90%):</strong> Excellent performance</li>
            <li>🟡 <strong>Yellow (70-89%):</strong> Needs improvement</li>
            <li>🔴 <strong>Red (&lt;70%):</strong> Requires immediate attention</li>
          </ul>
        `
      },
      {
        title: 'Using Dashboard Filters',
        type: 'tip',
        content: `
          <p>Customize your dashboard view:</p>
          <ul>
            <li><strong>Date Range:</strong> View specific time periods</li>
            <li><strong>Measure Category:</strong> Filter by Diabetes, Hypertension, etc.</li>
            <li><strong>Provider:</strong> View individual provider performance</li>
            <li><strong>Location:</strong> Compare clinic sites</li>
          </ul>
          <p>💡 <em>Save favorite filter combinations for quick access</em></p>
        `
      }
    ];
  }

  /**
   * Get general quick links
   */
  getQuickLinks() {
    return [
      { text: 'CUSTOM_MEASURES_EXAMPLES.md', url: '/docs/custom-measures' },
      { text: 'COMPREHENSIVE_FHIR_TEST_DATA.md', url: '/docs/test-data' },
      { text: 'CQL Language Reference', url: 'https://cql.hl7.org/' },
      { text: 'HEDIS Measures Documentation', url: 'https://www.ncqa.org/hedis/' },
      { text: 'FHIR R4 Specification', url: 'http://hl7.org/fhir/R4/' }
    ];
  }
}
