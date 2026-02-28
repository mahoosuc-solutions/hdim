import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { CmoOnboardingService, CmoOnboardingKpiCard } from '../../services/cmo-onboarding.service';

@Component({
  selector: 'app-cmo-onboarding',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatChipsModule, MatButtonModule],
  templateUrl: './cmo-onboarding.component.html',
  styleUrl: './cmo-onboarding.component.scss',
})
export class CmoOnboardingComponent implements OnInit {
  kpis: CmoOnboardingKpiCard[] = [];
  topActions: string[] = [];
  governanceSignals: string[] = [];
  loading = true;

  constructor(private readonly cmoOnboardingService: CmoOnboardingService) {}

  ngOnInit(): void {
    this.cmoOnboardingService.getDashboardSummary().subscribe((summary) => {
      this.kpis = summary.kpis;
      this.topActions = summary.topActions;
      this.governanceSignals = summary.governanceSignals;
      this.loading = false;
    });
  }
}
