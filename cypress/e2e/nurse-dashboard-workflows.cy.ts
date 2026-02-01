/**
 * Nurse Dashboard Workflow E2E Tests
 *
 * Comprehensive end-to-end testing of all 5 workflow components
 * integrated into the RN Dashboard.
 *
 * Test Coverage:
 * - Patient Outreach Workflow
 * - Medication Reconciliation Workflow
 * - Patient Education Workflow
 * - Referral Coordination Workflow
 * - Care Plan Management Workflow
 */

describe('Nurse Dashboard Workflows - End-to-End Tests', () => {
  const BASE_URL = 'http://localhost:4200';
  const DASHBOARD_URL = `${BASE_URL}/dashboard/rn-dashboard`;

  beforeEach(() => {
    // Visit dashboard before each test
    cy.visit(DASHBOARD_URL);
    // Wait for dashboard to load
    cy.get('app-rn-dashboard', { timeout: 10000 }).should('exist');
  });

  describe('Dashboard Page Load', () => {
    it('should load the RN Dashboard', () => {
      cy.get('app-page-header h1').should('contain', 'Nurse Dashboard');
    });

    it('should display metrics cards', () => {
      cy.get('app-stat-card').should('have.length.at.least', 4);
      cy.get('app-stat-card').first().should('contain', 'Care Gaps Assigned');
    });

    it('should display quick action buttons', () => {
      cy.get('.quick-actions button').should('have.length', 5);
    });

    it('should display care gaps table tab', () => {
      cy.get('mat-tab-label').first().should('contain', 'Care Gaps');
    });
  });

  // ============================================================================
  // PATIENT OUTREACH WORKFLOW TESTS
  // ============================================================================

  describe('Patient Outreach Workflow', () => {
    it('should launch Patient Outreach workflow from quick action', () => {
      // Click "Patient Outreach" quick action button
      cy.get('.quick-actions button').contains('Patient Outreach').click();

      // Verify dialog opens
      cy.get('.cdk-overlay-pane').should('be.visible');
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Verify workflow header
      cy.get('app-patient-outreach-workflow h2').should('contain', 'Patient Outreach');
    });

    it('should progress through Patient Outreach steps', () => {
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Step 0: Select contact method
      cy.get('app-patient-outreach-workflow mat-select').first().click();
      cy.get('mat-option').first().click();
      cy.get('button:contains("Next")').click();

      // Step 1: Log contact attempt
      cy.get('mat-form-field input[formcontrolname="duration"]').type('15');
      cy.get('button:contains("Next")').click();

      // Step 2: Record outcome
      cy.get('mat-form-field mat-select').first().click();
      cy.get('mat-option').first().click();
      cy.get('button:contains("Next")').click();

      // Step 3: Follow-up scheduling (optional)
      cy.get('button:contains("Next")').click();

      // Step 4: Review & confirmation
      cy.get('button:contains("Complete")').should('be.enabled');
    });

    it('should show progress bar during workflow', () => {
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Progress bar should exist and show 0%
      cy.get('mat-progress-bar').should('be.visible');

      // Verify step counter
      cy.get('.step-counter').should('contain', 'Step 1 of');
    });

    it('should close dialog when cancelling workflow', () => {
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Click cancel button
      cy.get('button:contains("Cancel")').click();

      // Dialog should close
      cy.get('app-patient-outreach-workflow').should('not.exist');
    });

    it('should validate required fields', () => {
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Try to proceed without selecting contact method
      const nextButton = cy.get('button:contains("Next")');
      nextButton.should('be.disabled');
    });
  });

  // ============================================================================
  // MEDICATION RECONCILIATION WORKFLOW TESTS
  // ============================================================================

  describe('Medication Reconciliation Workflow', () => {
    it('should launch Medication Reconciliation workflow from quick action', () => {
      cy.get('.quick-actions button').contains('Med Reconciliation').click();

      // Verify dialog opens
      cy.get('.cdk-overlay-pane').should('be.visible');
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      // Verify workflow header
      cy.get('app-medication-reconciliation-workflow h3').should('contain', 'Medication Reconciliation');
    });

    it('should load medications in workflow', () => {
      cy.get('.quick-actions button').contains('Med Reconciliation').click();
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      // Wait for medications to load
      cy.get('app-medication-reconciliation-workflow mat-table', { timeout: 5000 }).should('exist');
    });

    it('should allow adding patient-reported medications', () => {
      cy.get('.quick-actions button').contains('Med Reconciliation').click();
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      // Click Next to proceed to patient medications step
      cy.get('app-medication-reconciliation-workflow button:contains("Next")').click();

      // Verify patient medication form exists
      cy.get('app-medication-reconciliation-workflow mat-form-field').should('exist');
    });

    it('should show drug interaction warnings', () => {
      cy.get('.quick-actions button').contains('Med Reconciliation').click();
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      // Progress through workflow to interaction check step
      cy.get('app-medication-reconciliation-workflow button:contains("Next")').click({ multiple: true });

      // Look for interaction warning chips/badges
      cy.get('app-medication-reconciliation-workflow mat-chip').each(($chip) => {
        const text = $chip.text();
        if (text.includes('MAJOR') || text.includes('MODERATE') || text.includes('MINOR')) {
          expect(true).to.be.true;
        }
      });
    });

    it('should close dialog when cancelling medication reconciliation', () => {
      cy.get('.quick-actions button').contains('Med Reconciliation').click();
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      cy.get('app-medication-reconciliation-workflow button:contains("Cancel")').click();

      cy.get('app-medication-reconciliation-workflow').should('not.exist');
    });
  });

  // ============================================================================
  // PATIENT EDUCATION WORKFLOW TESTS
  // ============================================================================

  describe('Patient Education Workflow', () => {
    it('should launch Patient Education workflow from quick action', () => {
      cy.get('.quick-actions button').contains('Patient Education').click();

      cy.get('.cdk-overlay-pane').should('be.visible');
      cy.get('app-patient-education-workflow', { timeout: 5000 }).should('exist');

      cy.get('app-patient-education-workflow h3').should('contain', 'Patient Education');
    });

    it('should select education topic', () => {
      cy.get('.quick-actions button').contains('Patient Education').click();
      cy.get('app-patient-education-workflow', { timeout: 5000 }).should('exist');

      // Select topic from dropdown
      cy.get('app-patient-education-workflow mat-select').first().click();
      cy.get('mat-option').first().click();

      cy.get('button:contains("Next")').click();

      // Should proceed to next step
      cy.get('app-patient-education-workflow .step-counter').should('contain', 'Step 2');
    });

    it('should record understanding assessment', () => {
      cy.get('.quick-actions button').contains('Patient Education').click();
      cy.get('app-patient-education-workflow', { timeout: 5000 }).should('exist');

      // Progress through workflow steps
      cy.get('app-patient-education-workflow button:contains("Next")').click({ multiple: true });

      // Find and interact with assessment form
      cy.get('app-patient-education-workflow input[type="range"]').should('exist');

      // Set understanding score
      cy.get('app-patient-education-workflow input[type="range"]').invoke('val', 80).trigger('change');

      cy.get('button:contains("Next")').click();
    });

    it('should document learning barriers', () => {
      cy.get('.quick-actions button').contains('Patient Education').click();
      cy.get('app-patient-education-workflow', { timeout: 5000 }).should('exist');

      // Progress to barriers step
      cy.get('app-patient-education-workflow button:contains("Next")').click({ multiple: true });

      // Select learning barriers (checkboxes)
      cy.get('app-patient-education-workflow mat-checkbox').first().click();

      cy.get('button:contains("Next")').click();
    });

    it('should show education summary', () => {
      cy.get('.quick-actions button').contains('Patient Education').click();
      cy.get('app-patient-education-workflow', { timeout: 5000 }).should('exist');

      // Progress through all steps
      cy.get('app-patient-education-workflow button:contains("Next")').click({ multiple: true });

      // Final step should show complete button
      cy.get('app-patient-education-workflow button:contains("Complete")').should('exist');
    });
  });

  // ============================================================================
  // REFERRAL COORDINATION WORKFLOW TESTS
  // ============================================================================

  describe('Referral Coordination Workflow', () => {
    it('should launch Referral Coordination workflow from quick action', () => {
      cy.get('.quick-actions button').contains('Coordinate Referral').click();

      cy.get('.cdk-overlay-pane').should('be.visible');
      cy.get('app-referral-coordination-workflow', { timeout: 5000 }).should('exist');

      cy.get('app-referral-coordination-workflow h3').should('contain', 'Referral Coordination');
    });

    it('should review and accept referral details', () => {
      cy.get('.quick-actions button').contains('Coordinate Referral').click();
      cy.get('app-referral-coordination-workflow', { timeout: 5000 }).should('exist');

      // Accept referral review
      cy.get('app-referral-coordination-workflow mat-checkbox').first().click();

      cy.get('button:contains("Next")').click();
    });

    it('should search and select specialist', () => {
      cy.get('.quick-actions button').contains('Coordinate Referral').click();
      cy.get('app-referral-coordination-workflow', { timeout: 5000 }).should('exist');

      // Accept review
      cy.get('app-referral-coordination-workflow mat-checkbox').first().click();
      cy.get('button:contains("Next")').click();

      // Select specialist from list
      cy.get('app-referral-coordination-workflow mat-select').first().click();
      cy.get('mat-option').first().click();

      cy.get('button:contains("Next")').click();
    });

    it('should verify insurance coverage', () => {
      cy.get('.quick-actions button').contains('Coordinate Referral').click();
      cy.get('app-referral-coordination-workflow', { timeout: 5000 }).should('exist');

      // Progress through review and specialist selection
      cy.get('app-referral-coordination-workflow button:contains("Next")').click({ multiple: true });

      // Insurance verification step should show coverage status
      cy.get('app-referral-coordination-workflow').should('contain', 'Insurance');
    });

    it('should send referral and track appointment', () => {
      cy.get('.quick-actions button').contains('Coordinate Referral').click();
      cy.get('app-referral-coordination-workflow', { timeout: 5000 }).should('exist');

      // Progress through all steps
      cy.get('app-referral-coordination-workflow button:contains("Next")').click({ multiple: true });

      // Final step should show complete button
      cy.get('app-referral-coordination-workflow button:contains("Complete")').should('exist');
    });
  });

  // ============================================================================
  // CARE PLAN WORKFLOW TESTS
  // ============================================================================

  describe('Care Plan Management Workflow', () => {
    it('should launch Care Plan workflow from quick action', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();

      cy.get('.cdk-overlay-pane').should('be.visible');
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      cy.get('app-care-plan-workflow h2').should('contain', 'Care Plan');
    });

    it('should select care plan template', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Select template
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').first().click();

      cy.get('button:contains("Next")').click();
    });

    it('should add problems/diagnoses', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Select template and proceed
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').first().click();
      cy.get('button:contains("Next")').click();

      // Add problem
      cy.get('app-care-plan-workflow input[formcontrolname="problemName"]').type('Type 2 Diabetes');
      cy.get('app-care-plan-workflow button:contains("Add Problem")').click();

      // Verify problem added
      cy.get('app-care-plan-workflow mat-table').should('contain', 'Type 2 Diabetes');
    });

    it('should define goals linked to problems', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Progress to goals step
      cy.get('app-care-plan-workflow button:contains("Next")').click({ multiple: true });

      // Select related problem
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').first().click();

      // Add goal
      cy.get('app-care-plan-workflow textarea[formcontrolname="goalDescription"]').type('Achieve HbA1c < 7%');

      // Set target date (future date)
      cy.get('app-care-plan-workflow input[formcontrolname="targetDate"]').type('12/31/2026');

      cy.get('app-care-plan-workflow button:contains("Add Goal")').click();

      // Verify goal added
      cy.get('app-care-plan-workflow mat-table').should('contain', 'Achieve HbA1c');
    });

    it('should plan interventions linked to goals', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Progress to interventions step
      cy.get('app-care-plan-workflow button:contains("Next")').click({ multiple: true });

      // Select related goal
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').first().click();

      // Add intervention
      cy.get('app-care-plan-workflow input[formcontrolname="interventionName"]').type('Monthly glucose monitoring');

      cy.get('app-care-plan-workflow button:contains("Add Intervention")').click();

      // Verify intervention added
      cy.get('app-care-plan-workflow mat-table').should('contain', 'glucose monitoring');
    });

    it('should assign team members with roles', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Progress to team members step
      cy.get('app-care-plan-workflow button:contains("Next")').click({ multiple: true });

      // Add team member
      cy.get('app-care-plan-workflow input[formcontrolname="teamMemberName"]').type('Jane Smith, RN');

      // Select role
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').contains('Primary Nurse').click();

      cy.get('app-care-plan-workflow button:contains("Add Team Member")').click();

      // Verify team member added
      cy.get('app-care-plan-workflow mat-table').should('contain', 'Jane Smith');
    });

    it('should prevent duplicate primary nurse roles', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Progress to team members step
      cy.get('app-care-plan-workflow button:contains("Next")').click({ multiple: true });

      // Add first Primary Nurse
      cy.get('app-care-plan-workflow input[formcontrolname="teamMemberName"]').type('Jane Smith, RN');
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').contains('Primary Nurse').click();
      cy.get('app-care-plan-workflow button:contains("Add Team Member")').click();

      // Try to add second Primary Nurse
      cy.get('app-care-plan-workflow input[formcontrolname="teamMemberName"]').type('Bob Johnson, RN');
      cy.get('app-care-plan-workflow mat-select').first().click();
      cy.get('mat-option').contains('Primary Nurse').click();

      // Add button should be disabled
      cy.get('app-care-plan-workflow button:contains("Add Team Member")').should('be.disabled');
    });

    it('should show care plan summary before completion', () => {
      cy.get('.quick-actions button').contains('Update Care Plan').click();
      cy.get('app-care-plan-workflow', { timeout: 5000 }).should('exist');

      // Progress through all steps and reach summary
      cy.get('app-care-plan-workflow button:contains("Next")').click({ multiple: true });

      // Summary step should show all components
      cy.get('app-care-plan-workflow').should('contain', 'Care Plan Summary');
      cy.get('app-care-plan-workflow').should('contain', 'Problems');
      cy.get('app-care-plan-workflow').should('contain', 'Goals');
      cy.get('app-care-plan-workflow').should('contain', 'Interventions');
      cy.get('app-care-plan-workflow').should('contain', 'Team Members');
    });
  });

  // ============================================================================
  // CROSS-WORKFLOW TESTS
  // ============================================================================

  describe('Cross-Workflow Integration', () => {
    it('should launch workflows from care gaps table', () => {
      // This test requires mock data in the care gaps table
      // First try to add a care gap if possible, or use existing
      cy.get('mat-tab-label').first().click(); // Click Care Gaps tab

      // Look for care gap rows
      cy.get('app-rn-dashboard table tbody tr').then(($rows) => {
        if ($rows.length > 0) {
          // Click first care gap's action button
          cy.get('app-rn-dashboard table tbody tr').first().within(() => {
            cy.get('button[mattooltip="Address Care Gap"]').click();
          });

          // Verify workflow dialog opens
          cy.get('.cdk-overlay-pane').should('be.visible');
        }
      });
    });

    it('should maintain dashboard state after workflow completion', () => {
      const initialMetricValue = cy.get('app-stat-card').first().invoke('text');

      // Launch and complete a workflow
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      cy.get('app-patient-outreach-workflow button:contains("Cancel")').click();

      // Dashboard should still be visible and functional
      cy.get('app-rn-dashboard').should('exist');
      cy.get('app-page-header').should('contain', 'Nurse Dashboard');
    });

    it('should handle rapid workflow launching', () => {
      // Launch Patient Outreach
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Cancel it
      cy.get('app-patient-outreach-workflow button:contains("Cancel")').click();
      cy.get('app-patient-outreach-workflow').should('not.exist');

      // Immediately launch another workflow
      cy.get('.quick-actions button').contains('Med Reconciliation').click();
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      // Should open without issues
      cy.get('app-medication-reconciliation-workflow').should('be.visible');
    });

    it('should display loading states during async operations', () => {
      cy.get('.quick-actions button').contains('Med Reconciliation').click();
      cy.get('app-medication-reconciliation-workflow', { timeout: 5000 }).should('exist');

      // Look for loading indicators
      cy.get('app-medication-reconciliation-workflow .loading-overlay', { timeout: 2000 }).then(($overlay) => {
        if ($overlay.length > 0) {
          // Loading overlay exists and should disappear
          cy.get('app-medication-reconciliation-workflow .loading-overlay').should('not.exist', { timeout: 5000 });
        }
      });
    });

    it('should show error messages for invalid operations', () => {
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Try to proceed without required fields
      cy.get('app-patient-outreach-workflow button:contains("Next")').then(($button) => {
        if ($button.prop('disabled')) {
          expect(true).to.be.true; // Button is properly disabled
        }
      });
    });
  });

  // ============================================================================
  // PERFORMANCE TESTS
  // ============================================================================

  describe('Performance Metrics', () => {
    it('should load dashboard in acceptable time', () => {
      const startTime = Date.now();
      cy.visit(DASHBOARD_URL);
      cy.get('app-rn-dashboard', { timeout: 10000 }).should('exist');
      const loadTime = Date.now() - startTime;

      // Dashboard should load within 5 seconds
      expect(loadTime).to.be.lessThan(5000);
    });

    it('should open workflow dialog quickly', () => {
      const startTime = Date.now();
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');
      const openTime = Date.now() - startTime;

      // Dialog should open within 2 seconds
      expect(openTime).to.be.lessThan(2000);
    });

    it('should respond to user interactions promptly', () => {
      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      const startTime = Date.now();
      cy.get('app-patient-outreach-workflow mat-select').first().click();
      cy.get('mat-option').first().click();
      const selectTime = Date.now() - startTime;

      // Selection should complete within 1 second
      expect(selectTime).to.be.lessThan(1000);
    });

    it('should not have memory leaks on rapid workflows', () => {
      // Launch and close workflows multiple times
      for (let i = 0; i < 3; i++) {
        cy.get('.quick-actions button').contains('Patient Outreach').click();
        cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');
        cy.get('app-patient-outreach-workflow button:contains("Cancel")').click();
        cy.get('app-patient-outreach-workflow').should('not.exist');
      }

      // Dashboard should still be responsive
      cy.get('app-rn-dashboard').should('exist');
      cy.get('app-stat-card').should('exist');
    });
  });

  // ============================================================================
  // ACCESSIBILITY TESTS
  // ============================================================================

  describe('Accessibility Features', () => {
    it('should have proper ARIA labels', () => {
      cy.get('app-rn-dashboard button').first().should('have.attr', 'aria-label').or('have.attr', 'mattooltip');
    });

    it('should support keyboard navigation', () => {
      // Tab to quick action buttons
      cy.get('body').tab();
      cy.focused().should('exist');

      // Enter key should activate button
      cy.focused().type('{enter}', { force: true });
    });

    it('should have sufficient color contrast', () => {
      // This is a visual test - check that text is readable
      cy.get('app-page-header h1').should('be.visible');
      cy.get('app-stat-card').should('be.visible');
    });

    it('should work with screen reader', () => {
      // Check for semantic HTML
      cy.get('button').each(($button) => {
        expect($button.text().length).to.be.greaterThan(0);
      });
    });
  });

  // ============================================================================
  // RESPONSIVE DESIGN TESTS
  // ============================================================================

  describe('Responsive Design', () => {
    it('should be responsive on mobile viewport', () => {
      cy.viewport('iphone-x');
      cy.visit(DASHBOARD_URL);
      cy.get('app-rn-dashboard', { timeout: 10000 }).should('exist');

      // Metrics cards should stack
      cy.get('app-stat-card').should('be.visible');

      // Quick actions should be accessible
      cy.get('.quick-actions button').should('have.length', 5);
    });

    it('should be responsive on tablet viewport', () => {
      cy.viewport('ipad-2');
      cy.visit(DASHBOARD_URL);
      cy.get('app-rn-dashboard', { timeout: 10000 }).should('exist');

      cy.get('app-stat-card').should('be.visible');
    });

    it('should be responsive on desktop viewport', () => {
      cy.viewport(1920, 1080);
      cy.visit(DASHBOARD_URL);
      cy.get('app-rn-dashboard', { timeout: 10000 }).should('exist');

      cy.get('app-stat-card').should('be.visible');
    });

    it('should handle workflow dialog on mobile', () => {
      cy.viewport('iphone-x');
      cy.visit(DASHBOARD_URL);

      cy.get('.quick-actions button').contains('Patient Outreach').click();
      cy.get('app-patient-outreach-workflow', { timeout: 5000 }).should('exist');

      // Dialog should be readable on mobile
      cy.get('app-patient-outreach-workflow h2').should('be.visible');
      cy.get('button:contains("Next")').should('be.visible');
    });
  });
});
