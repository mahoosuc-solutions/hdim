export interface ROIInputs {
  organizationType: 'solo-practice' | 'health-system' | 'aco' | 'payer' | 'other';
  patientPopulation: number;
  currentFTE: number;
  expectedQualityImprovement: number; // percentage points (0-10)
  deploymentModel: 'pilot' | 'growth' | 'enterprise' | 'hybrid';
  numberOfEHRs: number;
}

export interface ROIResults {
  laborSavings: number;
  qualityBonus: number;
  memberEngagementBenefit: number;
  implementationCost: number;
  yearlyMonthlyCost: number;
  totalCost: number;
  netROI: number;
  roiPercent: number;
  paybackMonths: number;
}

export function calculateROI(inputs: ROIInputs): ROIResults {
  // Base costs
  const fteCost = 100000; // Average healthcare staff cost
  const laborSavingsPercent = inputs.numberOfEHRs > 1 ? 0.5 : 0.3; // More EHRs = more savings
  const laborSavings = inputs.currentFTE * fteCost * laborSavingsPercent;

  // Quality bonus calculation (varies by organization type)
  let qualityBonus = 0;
  const baseQualityBonus = inputs.patientPopulation < 50000 ? 50000 : 500000;

  switch (inputs.organizationType) {
    case 'solo-practice':
      qualityBonus = inputs.expectedQualityImprovement * 25000;
      break;
    case 'health-system':
      qualityBonus = inputs.expectedQualityImprovement * inputs.patientPopulation * 2.5;
      break;
    case 'aco':
      qualityBonus = inputs.expectedQualityImprovement * inputs.patientPopulation * 5;
      break;
    case 'payer':
      qualityBonus = inputs.expectedQualityImprovement * inputs.patientPopulation * 15;
      break;
    default:
      qualityBonus = baseQualityBonus * inputs.expectedQualityImprovement;
  }

  // Member engagement benefit (for payers)
  const memberEngagementBenefit =
    inputs.organizationType === 'payer' ? inputs.patientPopulation * 2 : 0;

  // Implementation cost
  let implementationCost = 0;
  switch (inputs.deploymentModel) {
    case 'pilot':
      implementationCost = 5000;
      break;
    case 'growth':
      implementationCost = 30000 + inputs.numberOfEHRs * 5000;
      break;
    case 'enterprise':
      implementationCost = 80000 + inputs.numberOfEHRs * 10000;
      break;
    case 'hybrid':
      implementationCost = 120000 + inputs.numberOfEHRs * 15000;
      break;
  }

  // Monthly cost
  let monthlyCost = 0;
  switch (inputs.deploymentModel) {
    case 'pilot':
      monthlyCost = 500;
      break;
    case 'growth':
      monthlyCost = 2500;
      break;
    case 'enterprise':
      monthlyCost = 7500; // midpoint
      break;
    case 'hybrid':
      monthlyCost = 12500; // midpoint
      break;
  }

  const yearlyMonthlyCost = monthlyCost * 12;
  const totalCost = implementationCost + yearlyMonthlyCost;

  const totalBenefits = laborSavings + qualityBonus + memberEngagementBenefit;
  const netROI = totalBenefits - totalCost;
  const roiPercent = (netROI / totalCost) * 100;
  const paybackMonths = totalCost > 0 ? Math.round((totalCost / (totalBenefits / 12)) * 12) / 12 : 0;

  return {
    laborSavings,
    qualityBonus,
    memberEngagementBenefit,
    implementationCost,
    yearlyMonthlyCost,
    totalCost,
    netROI,
    roiPercent,
    paybackMonths: Math.max(paybackMonths, 0.1), // Minimum 0.1 months
  };
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatPercent(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value / 100);
}
