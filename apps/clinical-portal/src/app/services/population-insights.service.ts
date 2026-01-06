import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { map, catchError, delay } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';

/**
 * Risk factor contributing to an insight
 */
export interface RiskFactor {
  factor: string;
  contribution: number;
  trend?: 'improving' | 'worsening' | 'stable';
}

/**
 * Suggested action for an insight
 */
export interface SuggestedAction {
  id: string;
  type: 'BATCH_OUTREACH' | 'SCHEDULE_VISITS' | 'ORDER_LABS' | 'MEDICATION_REVIEW' | 'REFER_SPECIALIST' | 'PATIENT_EDUCATION';
  label: string;
  description: string;
  patientCount: number;
  estimatedImpact: number;
}

/**
 * A single population health insight
 */
export interface PopulationInsight {
  id: string;
  type: 'CARE_GAP_CLUSTER' | 'PERFORMANCE_TREND' | 'AT_RISK_POPULATION' | 'INTERVENTION_OPPORTUNITY' | 'QUALITY_ALERT' | 'PREDICTED_GAP';
  title: string;
  description: string;
  impact: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  category: string;
  affectedPatients: number;
  affectedPatientIds?: string[];
  metrics: {
    currentValue?: number;
    previousValue?: number;
    changePercent?: number;
    targetValue?: number;
    potentialImprovement?: number;
  };
  riskFactors?: RiskFactor[];
  suggestedActions: SuggestedAction[];
  createdAt: string;
  expiresAt?: string;
  status: 'ACTIVE' | 'DISMISSED' | 'ACTED_UPON' | 'EXPIRED';
  dismissedReason?: string;
  dismissedAt?: string;
  dismissedBy?: string;
}

/**
 * Population health summary statistics
 */
export interface PopulationSummary {
  totalPatients: number;
  riskDistribution: {
    critical: number;
    high: number;
    medium: number;
    low: number;
  };
  careGapDistribution: {
    category: string;
    count: number;
    percentage: number;
  }[];
  qualityScores: {
    measureId: string;
    measureName: string;
    score: number;
    target: number;
    trend: 'improving' | 'worsening' | 'stable';
  }[];
  trendsOverTime: {
    period: string;
    avgRiskScore: number;
    careGapCount: number;
    complianceRate: number;
  }[];
}

/**
 * Insights response from API
 */
export interface InsightsResponse {
  providerId: string;
  generatedAt: string;
  insights: PopulationInsight[];
  summary: {
    critical: number;
    high: number;
    medium: number;
    low: number;
    dismissed: number;
  };
}

/**
 * Predicted care gap
 */
export interface PredictedCareGap {
  id: string;
  patientId: string;
  patientName: string;
  patientMRN: string;
  measureId: string;
  measureName: string;
  currentStatus: 'COMPLIANT' | 'NON_COMPLIANT' | 'EXCLUDED';
  predictedGapDate: string;
  daysUntilGap: number;
  riskScore: number;
  confidence: number;
  riskFactors: RiskFactor[];
  recommendedIntervention: string;
  lastContactDate?: string;
}

@Injectable({
  providedIn: 'root',
})
export class PopulationInsightsService {
  private dismissedInsightsSubject = new BehaviorSubject<Set<string>>(new Set());

  constructor(private http: HttpClient) {
    // Load dismissed insights from localStorage
    const dismissed = localStorage.getItem('dismissedInsights');
    if (dismissed) {
      this.dismissedInsightsSubject.next(new Set(JSON.parse(dismissed)));
    }
  }

  /**
   * Get population health insights for the provider
   */
  getInsights(
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID,
    filters?: {
      impact?: ('CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW')[];
      type?: string[];
      includeDisissed?: boolean;
    }
  ): Observable<InsightsResponse> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/insights`;
    const headers = new HttpHeaders({ 'X-Tenant-ID': tenantId });

    return this.http.get<InsightsResponse>(url, { headers }).pipe(
      map((response) => this.applyFilters(response, filters)),
      catchError(() => of(this.generateMockInsights()).pipe(
        delay(500),
        map((response) => this.applyFilters(response, filters))
      ))
    );
  }

  /**
   * Get population health summary
   */
  getPopulationSummary(
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<PopulationSummary> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/insights/population-summary`;
    const headers = new HttpHeaders({ 'X-Tenant-ID': tenantId });

    return this.http.get<PopulationSummary>(url, { headers }).pipe(
      catchError(() => of(this.generateMockSummary()).pipe(delay(300)))
    );
  }

  /**
   * Get predicted care gaps
   */
  getPredictedCareGaps(
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID,
    limit: number = 20
  ): Observable<PredictedCareGap[]> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/insights/predicted-gaps`;
    const headers = new HttpHeaders({ 'X-Tenant-ID': tenantId });

    return this.http.get<PredictedCareGap[]>(url, { headers }).pipe(
      catchError(() => of(this.generateMockPredictedGaps(limit)).pipe(delay(400)))
    );
  }

  /**
   * Dismiss an insight
   */
  dismissInsight(insightId: string, reason: string): Observable<void> {
    const dismissed = this.dismissedInsightsSubject.value;
    dismissed.add(insightId);
    this.dismissedInsightsSubject.next(dismissed);
    localStorage.setItem('dismissedInsights', JSON.stringify(Array.from(dismissed)));
    return of(undefined);
  }

  /**
   * Restore a dismissed insight
   */
  restoreInsight(insightId: string): Observable<void> {
    const dismissed = this.dismissedInsightsSubject.value;
    dismissed.delete(insightId);
    this.dismissedInsightsSubject.next(dismissed);
    localStorage.setItem('dismissedInsights', JSON.stringify(Array.from(dismissed)));
    return of(undefined);
  }

  /**
   * Execute an action from an insight
   */
  executeAction(insightId: string, actionId: string): Observable<{ success: boolean; message: string }> {
    // In real implementation, this would call the appropriate backend service
    return of({ success: true, message: 'Action queued successfully' }).pipe(delay(500));
  }

  /**
   * Apply filters to insights response
   */
  private applyFilters(
    response: InsightsResponse,
    filters?: {
      impact?: ('CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW')[];
      type?: string[];
      includeDisissed?: boolean;
    }
  ): InsightsResponse {
    let insights = response.insights;
    const dismissed = this.dismissedInsightsSubject.value;

    // Filter out dismissed unless explicitly requested
    if (!filters?.includeDisissed) {
      insights = insights.filter((i) => !dismissed.has(i.id));
    }

    // Filter by impact
    if (filters?.impact && filters.impact.length > 0) {
      insights = insights.filter((i) => filters.impact!.includes(i.impact));
    }

    // Filter by type
    if (filters?.type && filters.type.length > 0) {
      insights = insights.filter((i) => filters.type!.includes(i.type));
    }

    // Update counts
    const summary = {
      critical: insights.filter((i) => i.impact === 'CRITICAL').length,
      high: insights.filter((i) => i.impact === 'HIGH').length,
      medium: insights.filter((i) => i.impact === 'MEDIUM').length,
      low: insights.filter((i) => i.impact === 'LOW').length,
      dismissed: dismissed.size,
    };

    return { ...response, insights, summary };
  }

  /**
   * Generate mock insights for demo/fallback
   */
  private generateMockInsights(): InsightsResponse {
    const now = new Date();
    return {
      providerId: 'demo-provider',
      generatedAt: now.toISOString(),
      insights: [
        {
          id: 'insight-1',
          type: 'CARE_GAP_CLUSTER',
          title: 'Colorectal Cancer Screening Gap',
          description: '23 patients aged 50-75 have not completed colorectal cancer screening in the recommended timeframe. This represents a significant opportunity to improve preventive care compliance.',
          impact: 'HIGH',
          category: 'Preventive Care',
          affectedPatients: 23,
          metrics: {
            currentValue: 65,
            targetValue: 80,
            potentialImprovement: 15,
          },
          suggestedActions: [
            {
              id: 'action-1-1',
              type: 'BATCH_OUTREACH',
              label: 'Send Screening Reminders',
              description: 'Send personalized screening reminders to all 23 patients',
              patientCount: 23,
              estimatedImpact: 12,
            },
            {
              id: 'action-1-2',
              type: 'SCHEDULE_VISITS',
              label: 'Schedule Screening Visits',
              description: 'Proactively schedule colonoscopy appointments',
              patientCount: 23,
              estimatedImpact: 18,
            },
          ],
          createdAt: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'insight-2',
          type: 'PERFORMANCE_TREND',
          title: 'Diabetes A1c Control Declining',
          description: 'Blood sugar control (A1c < 8%) has declined 8% over the past month. 15 patients have moved from controlled to uncontrolled status.',
          impact: 'CRITICAL',
          category: 'Chronic Disease Management',
          affectedPatients: 15,
          metrics: {
            currentValue: 72,
            previousValue: 80,
            changePercent: -8,
            targetValue: 85,
          },
          riskFactors: [
            { factor: 'Holiday season medication non-adherence', contribution: 45, trend: 'worsening' },
            { factor: 'Reduced clinic visit frequency', contribution: 30, trend: 'stable' },
            { factor: 'New patients with poor baseline control', contribution: 25, trend: 'stable' },
          ],
          suggestedActions: [
            {
              id: 'action-2-1',
              type: 'MEDICATION_REVIEW',
              label: 'Review Medications',
              description: 'Schedule medication review appointments for uncontrolled patients',
              patientCount: 15,
              estimatedImpact: 20,
            },
            {
              id: 'action-2-2',
              type: 'PATIENT_EDUCATION',
              label: 'Send Education Materials',
              description: 'Send diabetes self-management education resources',
              patientCount: 15,
              estimatedImpact: 8,
            },
          ],
          createdAt: new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000).toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'insight-3',
          type: 'AT_RISK_POPULATION',
          title: 'Rising High-Risk Patient Count',
          description: '12 patients have moved to high-risk status in the past 30 days. Common factors include multiple chronic conditions and recent ED visits.',
          impact: 'HIGH',
          category: 'Risk Management',
          affectedPatients: 12,
          metrics: {
            currentValue: 35,
            previousValue: 23,
            changePercent: 52,
          },
          riskFactors: [
            { factor: 'Multiple ED visits in 30 days', contribution: 40, trend: 'worsening' },
            { factor: 'New chronic condition diagnosis', contribution: 35, trend: 'stable' },
            { factor: 'Medication non-adherence indicators', contribution: 25, trend: 'worsening' },
          ],
          suggestedActions: [
            {
              id: 'action-3-1',
              type: 'SCHEDULE_VISITS',
              label: 'Schedule Care Coordination',
              description: 'Schedule comprehensive care coordination visits',
              patientCount: 12,
              estimatedImpact: 25,
            },
          ],
          createdAt: now.toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'insight-4',
          type: 'INTERVENTION_OPPORTUNITY',
          title: 'Flu Vaccination Batch Opportunity',
          description: '45 patients are due for flu vaccination and have appointments scheduled in the next 2 weeks. This is an ideal opportunity for batch vaccination.',
          impact: 'MEDIUM',
          category: 'Preventive Care',
          affectedPatients: 45,
          metrics: {
            currentValue: 58,
            targetValue: 75,
            potentialImprovement: 17,
          },
          suggestedActions: [
            {
              id: 'action-4-1',
              type: 'BATCH_OUTREACH',
              label: 'Pre-Visit Notification',
              description: 'Notify patients to expect flu shot during upcoming visit',
              patientCount: 45,
              estimatedImpact: 15,
            },
          ],
          createdAt: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000).toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'insight-5',
          type: 'QUALITY_ALERT',
          title: 'Hypertension Control Below Target',
          description: 'Blood pressure control rate (< 140/90) is at 68%, below the 75% target. Focus on patients with recent elevated readings.',
          impact: 'MEDIUM',
          category: 'Chronic Disease Management',
          affectedPatients: 28,
          metrics: {
            currentValue: 68,
            targetValue: 75,
            potentialImprovement: 7,
          },
          suggestedActions: [
            {
              id: 'action-5-1',
              type: 'ORDER_LABS',
              label: 'Order Follow-up BPs',
              description: 'Order home BP monitoring or clinic rechecks',
              patientCount: 28,
              estimatedImpact: 10,
            },
            {
              id: 'action-5-2',
              type: 'MEDICATION_REVIEW',
              label: 'Adjust Medications',
              description: 'Review and adjust antihypertensive medications',
              patientCount: 28,
              estimatedImpact: 15,
            },
          ],
          createdAt: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000).toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'insight-6',
          type: 'PREDICTED_GAP',
          title: 'Predicted Mammography Gaps',
          description: '8 patients are predicted to miss their mammography screening deadline based on historical patterns and appointment behavior.',
          impact: 'MEDIUM',
          category: 'Predictive Analytics',
          affectedPatients: 8,
          metrics: {
            potentialImprovement: 5,
          },
          riskFactors: [
            { factor: 'Missed last 2 scheduled appointments', contribution: 50 },
            { factor: 'No recent preventive care visits', contribution: 30 },
            { factor: 'Similar patient behavior patterns', contribution: 20 },
          ],
          suggestedActions: [
            {
              id: 'action-6-1',
              type: 'BATCH_OUTREACH',
              label: 'Proactive Outreach',
              description: 'Contact patients before they miss the screening window',
              patientCount: 8,
              estimatedImpact: 6,
            },
          ],
          createdAt: now.toISOString(),
          status: 'ACTIVE',
        },
        {
          id: 'insight-7',
          type: 'CARE_GAP_CLUSTER',
          title: 'Depression Screening Overdue',
          description: '18 patients with chronic conditions have not completed PHQ-9 depression screening in the past year.',
          impact: 'LOW',
          category: 'Behavioral Health',
          affectedPatients: 18,
          metrics: {
            currentValue: 72,
            targetValue: 85,
            potentialImprovement: 13,
          },
          suggestedActions: [
            {
              id: 'action-7-1',
              type: 'PATIENT_EDUCATION',
              label: 'Send Screening Link',
              description: 'Send PHQ-9 questionnaire via patient portal',
              patientCount: 18,
              estimatedImpact: 10,
            },
          ],
          createdAt: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString(),
          status: 'ACTIVE',
        },
      ],
      summary: {
        critical: 1,
        high: 2,
        medium: 3,
        low: 1,
        dismissed: 0,
      },
    };
  }

  /**
   * Generate mock population summary
   */
  private generateMockSummary(): PopulationSummary {
    return {
      totalPatients: 156,
      riskDistribution: {
        critical: 8,
        high: 27,
        medium: 58,
        low: 63,
      },
      careGapDistribution: [
        { category: 'Preventive Screenings', count: 45, percentage: 28.8 },
        { category: 'Diabetes Management', count: 32, percentage: 20.5 },
        { category: 'Hypertension Control', count: 28, percentage: 17.9 },
        { category: 'Immunizations', count: 25, percentage: 16.0 },
        { category: 'Behavioral Health', count: 18, percentage: 11.5 },
        { category: 'Other', count: 8, percentage: 5.1 },
      ],
      qualityScores: [
        { measureId: 'CMS122v11', measureName: 'Diabetes A1c Control', score: 72, target: 85, trend: 'worsening' },
        { measureId: 'CMS165v11', measureName: 'Blood Pressure Control', score: 68, target: 75, trend: 'stable' },
        { measureId: 'CMS130v11', measureName: 'Colorectal Cancer Screening', score: 65, target: 80, trend: 'improving' },
        { measureId: 'CMS125v11', measureName: 'Breast Cancer Screening', score: 78, target: 75, trend: 'improving' },
        { measureId: 'CMS147v12', measureName: 'Flu Immunization', score: 58, target: 75, trend: 'stable' },
      ],
      trendsOverTime: [
        { period: 'Jul', avgRiskScore: 42, careGapCount: 165, complianceRate: 68 },
        { period: 'Aug', avgRiskScore: 44, careGapCount: 158, complianceRate: 69 },
        { period: 'Sep', avgRiskScore: 43, careGapCount: 162, complianceRate: 68 },
        { period: 'Oct', avgRiskScore: 45, careGapCount: 155, complianceRate: 70 },
        { period: 'Nov', avgRiskScore: 48, careGapCount: 160, complianceRate: 69 },
        { period: 'Dec', avgRiskScore: 50, careGapCount: 156, complianceRate: 68 },
      ],
    };
  }

  /**
   * Generate mock predicted care gaps
   */
  private generateMockPredictedGaps(limit: number): PredictedCareGap[] {
    const patients = [
      { id: 'p1', name: 'Jackson, Robert', mrn: 'MRN-301' },
      { id: 'p2', name: 'Garcia, Maria', mrn: 'MRN-302' },
      { id: 'p3', name: 'Chen, David', mrn: 'MRN-303' },
      { id: 'p4', name: 'Williams, Sarah', mrn: 'MRN-304' },
      { id: 'p5', name: 'Brown, Michael', mrn: 'MRN-305' },
      { id: 'p6', name: 'Davis, Jennifer', mrn: 'MRN-306' },
      { id: 'p7', name: 'Miller, James', mrn: 'MRN-307' },
      { id: 'p8', name: 'Wilson, Emily', mrn: 'MRN-308' },
    ];

    const measures = [
      { id: 'CMS122v11', name: 'Diabetes A1c Control' },
      { id: 'CMS165v11', name: 'Blood Pressure Control' },
      { id: 'CMS130v11', name: 'Colorectal Cancer Screening' },
      { id: 'CMS147v12', name: 'Flu Immunization' },
    ];

    const now = new Date();

    return patients.slice(0, limit).map((patient, i) => {
      const measure = measures[i % measures.length];
      const daysUntilGap = 15 + Math.floor(Math.random() * 45);
      const predictedDate = new Date(now.getTime() + daysUntilGap * 24 * 60 * 60 * 1000);

      return {
        id: `predicted-${i}`,
        patientId: patient.id,
        patientName: patient.name,
        patientMRN: patient.mrn,
        measureId: measure.id,
        measureName: measure.name,
        currentStatus: 'COMPLIANT' as const,
        predictedGapDate: predictedDate.toISOString(),
        daysUntilGap,
        riskScore: 60 + Math.floor(Math.random() * 30),
        confidence: 0.7 + Math.random() * 0.25,
        riskFactors: [
          { factor: 'Missed last scheduled appointment', contribution: 40 + Math.floor(Math.random() * 20) },
          { factor: 'Historical pattern of delayed care', contribution: 20 + Math.floor(Math.random() * 20) },
          { factor: 'Similar patient cohort behavior', contribution: 10 + Math.floor(Math.random() * 15) },
        ],
        recommendedIntervention: `Schedule proactive ${measure.name.toLowerCase()} follow-up`,
        lastContactDate: new Date(now.getTime() - (30 + Math.floor(Math.random() * 60)) * 24 * 60 * 60 * 1000).toISOString(),
      };
    });
  }
}
