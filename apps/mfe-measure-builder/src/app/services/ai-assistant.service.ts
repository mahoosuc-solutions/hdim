import { Injectable } from '@angular/core';

export interface UserInteraction {
  component: string;
  action: string;
  duration?: number;
  success: boolean;
  errorMessage?: string;
  context?: Record<string, any>;
}

/**
 * Minimal AI Assistant stub for MFE.
 * Full implementation lives in clinical-portal; this keeps tracking optional.
 */
@Injectable({
  providedIn: 'root',
})
export class AIAssistantService {
  trackInteraction(_interaction: UserInteraction): void {
    // no-op in MFE
  }
}
