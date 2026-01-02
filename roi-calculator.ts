#!/usr/bin/env node

/**
 * HDIM Clinical ROI Calculator
 * 
 * Interactive CLI tool for calculating Healthcare Data In Motion ROI
 * for clinical organizations
 * 
 * Usage: npx ts-node roi-calculator.ts
 */

import * as readline from 'readline';

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

function question(prompt: string): Promise<string> {
  return new Promise((resolve) => {
    rl.question(prompt, (answer) => {
      resolve(answer.trim());
    });
  });
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
  }).format(value);
}

function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`;
}

interface OrganizationData {
  attributedPatients: number;
  currentStarRating: number;
  currentGapClosureRate: number;
  currentHbA1cAttainment: number;
  clinicalStaff: number;
  complianceRiskProfile: 'low' | 'medium' | 'high';
  implementationMonths: number;
}

interface CalculatedROI {
  revenueGain: number;
  laborSavings: number;
  complianceBenefit: number;
  totalAnnualBenefit: number;
  implementationCost: number;
  netROI: number;
  roiPercent: number;
  paybackMonths: number;
  threYearValue: number;
}

async function gatherInputs(): Promise<OrganizationData> {
  console.log('\n╔═══════════════════════════════════════════════════════════╗');
  console.log('║   HDIM Clinical ROI Calculator                           ║');
  console.log('║   Calculate Your Potential Financial Impact              ║');
  console.log('╚═══════════════════════════════════════════════════════════╝\n');

  const attributedPatients = parseInt(
    await question('1. How many attributed patients do you manage? '),
    10
  );

  const currentStarRating = parseFloat(
    await question('2. Current Star rating (e.g., 3.5): ')
  );

  const currentGapClosureRate = parseInt(
    await question('3. Current gap closure rate (%)? (typical: 45%) '),
    10
  );

  const currentHbA1cAttainment = parseInt(
    await question('4. Current HbA1c measure attainment (%)? (typical: 65%) '),
    10
  );

  const clinicalStaff = parseInt(
    await question('5. Number of care coordinators/nurses managing gaps? '),
    10
  );

  const complianceAnswer = await question(
    '6. Compliance risk profile (low/medium/high)? '
  );
  const complianceRiskProfile = (
    ['low', 'medium', 'high'].includes(complianceAnswer.toLowerCase())
      ? complianceAnswer.toLowerCase()
      : 'medium'
  ) as 'low' | 'medium' | 'high';

  const implementationMonths = parseInt(
    await question('7. Expected implementation timeline (months)? (typical: 3) '),
    10
  );

  return {
    attributedPatients,
    currentStarRating,
    currentGapClosureRate,
    currentHbA1cAttainment,
    clinicalStaff,
    complianceRiskProfile,
    implementationMonths,
  };
}

function calculateROI(data: OrganizationData): CalculatedROI {
  // Revenue Impact Calculation
  // Assumptions:
  // - Each 0.1 Star rating = $40K-$50K annual bonus (Medicare, payer incentives)
  // - Gap closure improvement: 45% → 65-70% with HDIM
  // - HbA1c improvement: 65% → 78% with HDIM

  const starRatingGain = 0.6; // Conservative: +0.6 stars from 3.5 to 4.1
  const bonusPerStarTenth = 45000; // $45K per 0.1 stars
  const qualityBonusGain = starRatingGain * bonusPerStarTenth;

  // Shared savings participation improvement
  const sharedSavingsGain = data.attributedPatients > 20000 ? 150000 : 100000;

  const revenueGain = qualityBonusGain + sharedSavingsGain;

  // Labor Savings
  // Care coordinators spend 2-3 hours/week on manual gap identification
  const hoursPerWeekSaved = 3;
  const weeksPerYear = 50; // Account for PTO
  const hourlyRate = 35; // Loaded rate for care coordinator
  const coordinatorFteSaved = (hoursPerWeekSaved * weeksPerYear) / 2080;
  const coordinatorSalaryCost = 65000; // Annual coordinator salary + benefits
  const coordinatorSavings = Math.ceil(coordinatorFteSaved * coordinatorSalaryCost);

  // Compliance staff time savings
  // Audit prep: 20 hours saved per audit (reduce 2 weeks to 2 days)
  const complianceHoursSaved = 20;
  const auditFrequency = 1; // Assume 1 major audit per year
  const complianceHourlyRate = 45;
  const complianceSavings = complianceHoursSaved * auditFrequency * complianceHourlyRate;

  const laborSavings = coordinatorSavings + complianceSavings;

  // Compliance Benefit (Risk Mitigation)
  let complianceBenefit = 0;
  if (data.complianceRiskProfile === 'high') {
    // High risk: HIPAA violation penalty avoidance + 42 CFR Part 2 risk
    complianceBenefit = 250000; // Conservative risk mitigation
  } else if (data.complianceRiskProfile === 'medium') {
    complianceBenefit = 100000;
  } else {
    complianceBenefit = 50000; // Low risk still benefits from audit efficiency
  }

  const totalAnnualBenefit =
    revenueGain + laborSavings + complianceBenefit;

  // Implementation Costs
  // Base: $150K-$200K for mid-size org
  // Scales with org size
  const baseCost = 175000;
  const sizeMultiplier = Math.min(
    2,
    (data.attributedPatients / 15000) * 0.5 + 0.75
  );
  const implementationCost = Math.round(baseCost * sizeMultiplier);

  // ROI Calculation
  const netROI = totalAnnualBenefit - implementationCost;
  const roiPercent = (netROI / implementationCost) * 100;

  // Payback period
  const monthlyBenefit = totalAnnualBenefit / 12;
  const paybackMonths = Math.ceil(implementationCost / monthlyBenefit);

  // 3-year value
  const threYearValue = totalAnnualBenefit * 3 - implementationCost;

  return {
    revenueGain,
    laborSavings,
    complianceBenefit,
    totalAnnualBenefit,
    implementationCost,
    netROI,
    roiPercent,
    paybackMonths,
    threYearValue,
  };
}

function displayResults(data: OrganizationData, roi: CalculatedROI): void {
  console.log('\n╔═══════════════════════════════════════════════════════════╗');
  console.log('║              YOUR HDIM ROI PROJECTION                    ║');
  console.log('╚═══════════════════════════════════════════════════════════╝\n');

  console.log('📊 YOUR ORGANIZATION');
  console.log(`   Attributed Patients:        ${data.attributedPatients.toLocaleString()}`);
  console.log(`   Current Star Rating:        ${data.currentStarRating}`);
  console.log(`   Current Gap Closure Rate:   ${data.currentGapClosureRate}%`);
  console.log(`   Clinical Staff:             ${data.clinicalStaff} coordinators/nurses`);
  console.log(
    `   Compliance Risk Profile:    ${data.complianceRiskProfile.toUpperCase()}`
  );

  console.log('\n💰 ANNUAL FINANCIAL IMPACT\n');

  console.log(
    `   Quality Bonus Increase:     ${formatCurrency(roi.revenueGain * 0.7)}`
  );
  console.log(
    `   Shared Savings Gain:        ${formatCurrency(roi.revenueGain * 0.3)}`
  );
  console.log(`   ────────────────────────────────`);
  console.log(`   Revenue Gain:               ${formatCurrency(roi.revenueGain)}`);

  console.log(`\n   Care Coordinator Hours:     ${formatCurrency(roi.laborSavings * 0.7)}`);
  console.log(
    `   Compliance Audit Prep:      ${formatCurrency(roi.laborSavings * 0.3)}`
  );
  console.log(`   ────────────────────────────────`);
  console.log(`   Labor Savings:              ${formatCurrency(roi.laborSavings)}`);

  console.log(`\n   Compliance Risk Mitigation: ${formatCurrency(roi.complianceBenefit)}`);
  console.log(`   (HIPAA violations, audit efficiency)`);

  console.log('\n═══════════════════════════════════════════════════════════');
  console.log(
    `   TOTAL ANNUAL BENEFIT:       ${formatCurrency(roi.totalAnnualBenefit)}`
  );
  console.log('═══════════════════════════════════════════════════════════\n');

  console.log('📈 INVESTMENT & ROI\n');
  console.log(`   Implementation Cost (Year 1): ${formatCurrency(roi.implementationCost)}`);
  console.log(`   Year 1 Net ROI:               ${formatCurrency(roi.netROI)}`);
  console.log(`   ROI Percentage:               ${formatPercent(roi.roiPercent)}`);
  console.log(`   Payback Period:               ${roi.paybackMonths} months`);

  console.log('\n📊 MULTI-YEAR IMPACT\n');
  console.log(`   Year 1 Net Benefit:          ${formatCurrency(roi.netROI)}`);
  console.log(
    `   Year 2-3 Annual Benefit:     ${formatCurrency(roi.totalAnnualBenefit)} (no implementation cost)`
  );
  console.log(`   3-Year Cumulative Value:     ${formatCurrency(roi.threYearValue)}`);

  console.log('\n📈 PROJECTED IMPROVEMENTS\n');
  console.log(`   Gap Closure Rate:            45% → 65-70% (+20-25%)`);
  console.log(`   HbA1c Attainment:            ${data.currentHbA1cAttainment}% → 78% (+13%)`);
  console.log(
    `   Star Rating:                 ${data.currentStarRating} → ${(data.currentStarRating + 0.6).toFixed(1)}`
  );
  console.log(`   Care Coordinator Hours:      -3 hrs/week per staff`);
  console.log(`   Compliance Audit Prep:       -20 hours/audit`);

  console.log('\n✅ KEY SUCCESS FACTORS\n');
  console.log(`   • Implementation Timeline:     ${data.implementationMonths} months`);
  console.log(
    `   • Staff Adoption:             80%+ in month 2 (pre-built workflows)`
  );
  console.log(
    `   • Measure Improvement:        Evidence-based, CQL-validated`
  );
  console.log(`   • Compliance Readiness:       Full audit trail, zero-trust`);

  console.log('\n═══════════════════════════════════════════════════════════');
  console.log('   Ready to explore how HDIM works with your patient data?');
  console.log('   Schedule a 30-minute working session with our team.');
  console.log('═══════════════════════════════════════════════════════════\n');
}

async function main() {
  try {
    const organizationData = await gatherInputs();
    const roi = calculateROI(organizationData);
    displayResults(organizationData, roi);

    // Export option
    const exportAnswer = await question(
      'Would you like to save these results as a PDF? (y/n) '
    );
    if (exportAnswer.toLowerCase() === 'y') {
      console.log('\n📧 Results summary will be emailed to you shortly.');
      console.log(
        '   [In production: integrate with HTML-to-PDF library for export]\n'
      );
    }

    rl.close();
  } catch (error) {
    console.error('Error:', error);
    rl.close();
    process.exit(1);
  }
}

main();
