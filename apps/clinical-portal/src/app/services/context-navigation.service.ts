import { Injectable } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';

/**
 * Context-Aware Navigation Service
 * Issue #155: Enable Quick Links with Context-Aware Routing
 *
 * Provides context-preserving navigation for dashboard quick links,
 * ensuring deep-linking support and state preservation across navigation.
 */
@Injectable({
  providedIn: 'root'
})
export class ContextNavigationService {

  private returnStack: string[] = [];

  constructor(private router: Router) {}

  /**
   * Navigate to patient detail with care gap context
   */
  navigateToPatientCareGap(
    patientId: string,
    careGapId: string,
    measureId?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    this.router.navigate(['/patients', patientId], {
      queryParams: {
        tab: 'care-gaps',
        careGapId: careGapId,
        highlight: 'true',
        measureId: measureId,
        returnUrl: this.getCurrentReturnUrl()
      }
    });
  }

  /**
   * Navigate to patient detail with action to close care gap
   */
  navigateToAddressGap(
    patientId: string,
    careGapId: string,
    measureId?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    this.router.navigate(['/patients', patientId], {
      queryParams: {
        action: 'close-gap',
        careGapId: careGapId,
        measureId: measureId,
        returnUrl: this.getCurrentReturnUrl()
      }
    });
  }

  /**
   * Navigate to patient detail with result context
   */
  navigateToPatientResult(
    patientId: string,
    resultId: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    this.router.navigate(['/patients', patientId], {
      queryParams: {
        tab: 'results',
        resultId: resultId,
        highlight: 'true',
        returnUrl: this.getCurrentReturnUrl()
      }
    });
  }

  /**
   * Navigate to results page filtered by measure and provider
   */
  navigateToMeasureResults(
    measureId: string,
    providerId?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    const queryParams: any = {
      measureId: measureId,
      returnUrl: this.getCurrentReturnUrl()
    };

    if (providerId) {
      queryParams.providerId = providerId;
    }

    this.router.navigate(['/results'], { queryParams });
  }

  /**
   * Navigate to pre-visit planning for a specific patient
   */
  navigateToPreVisit(
    patientId: string,
    appointmentDate?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    const queryParams: any = {
      patientId: patientId,
      returnUrl: this.getCurrentReturnUrl()
    };

    if (appointmentDate) {
      queryParams.date = appointmentDate;
    }

    this.router.navigate(['/pre-visit'], { queryParams });
  }

  /**
   * Navigate to care gaps page filtered by patient
   */
  navigateToCareGaps(
    patientId?: string,
    measureId?: string,
    urgency?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    const queryParams: any = {
      returnUrl: this.getCurrentReturnUrl()
    };

    if (patientId) queryParams.patientId = patientId;
    if (measureId) queryParams.measureId = measureId;
    if (urgency) queryParams.urgency = urgency;

    this.router.navigate(['/care-gaps'], { queryParams });
  }

  /**
   * Navigate to risk stratification filtered view
   */
  navigateToRiskStratification(
    riskLevel?: string,
    condition?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    const queryParams: any = {
      returnUrl: this.getCurrentReturnUrl()
    };

    if (riskLevel) queryParams.riskLevel = riskLevel;
    if (condition) queryParams.condition = condition;

    this.router.navigate(['/risk-stratification'], { queryParams });
  }

  /**
   * Navigate to evaluations page with context
   */
  navigateToEvaluations(
    measureId?: string,
    patientId?: string,
    returnUrl?: string
  ): void {
    this.pushReturnUrl(returnUrl || this.router.url);

    const queryParams: any = {
      returnUrl: this.getCurrentReturnUrl()
    };

    if (measureId) queryParams.measureId = measureId;
    if (patientId) queryParams.patientId = patientId;

    this.router.navigate(['/evaluations'], { queryParams });
  }

  /**
   * Navigate back using stored return URL or fallback
   */
  navigateBack(fallbackUrl = '/dashboard'): void {
    const returnUrl = this.popReturnUrl();
    if (returnUrl) {
      this.router.navigateByUrl(returnUrl);
    } else {
      this.router.navigate([fallbackUrl]);
    }
  }

  /**
   * Get current return URL from stack
   */
  getCurrentReturnUrl(): string | null {
    return this.returnStack.length > 0
      ? this.returnStack[this.returnStack.length - 1]
      : null;
  }

  /**
   * Push return URL to stack
   */
  private pushReturnUrl(url: string): void {
    // Avoid duplicates
    if (this.returnStack[this.returnStack.length - 1] !== url) {
      this.returnStack.push(url);
    }
    // Limit stack size
    if (this.returnStack.length > 10) {
      this.returnStack.shift();
    }
  }

  /**
   * Pop return URL from stack
   */
  private popReturnUrl(): string | null {
    return this.returnStack.pop() || null;
  }

  /**
   * Clear return stack
   */
  clearReturnStack(): void {
    this.returnStack = [];
  }

  /**
   * Build navigation extras with context preservation
   */
  buildNavigationExtras(
    queryParams: Record<string, any>,
    preserveParams: string[] = []
  ): NavigationExtras {
    // Get current query params to preserve
    const currentParams = this.router.parseUrl(this.router.url).queryParams;
    const preserved: Record<string, any> = {};

    preserveParams.forEach(param => {
      if (currentParams[param]) {
        preserved[param] = currentParams[param];
      }
    });

    return {
      queryParams: { ...preserved, ...queryParams },
      queryParamsHandling: 'merge'
    };
  }
}

/**
 * Interface for navigation context
 */
export interface NavigationContext {
  careGapId?: string;
  resultId?: string;
  measureId?: string;
  patientId?: string;
  providerId?: string;
  tab?: string;
  action?: string;
  highlight?: boolean;
  returnUrl?: string;
  date?: string;
  urgency?: string;
  riskLevel?: string;
  condition?: string;
}

/**
 * Parse query params into NavigationContext
 */
export function parseNavigationContext(params: Record<string, any>): NavigationContext {
  return {
    careGapId: params['careGapId'],
    resultId: params['resultId'],
    measureId: params['measureId'],
    patientId: params['patientId'],
    providerId: params['providerId'],
    tab: params['tab'],
    action: params['action'],
    highlight: params['highlight'] === 'true',
    returnUrl: params['returnUrl'],
    date: params['date'],
    urgency: params['urgency'],
    riskLevel: params['riskLevel'],
    condition: params['condition']
  };
}
