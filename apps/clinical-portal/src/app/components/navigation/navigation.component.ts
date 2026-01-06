import { Component, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OfflineIndicatorComponent } from '../offline-indicator/offline-indicator.component';
import { HelpPanelComponent } from '../help/help-panel.component';
import { WhatsNewBannerComponent } from '../help/whats-new-banner.component';
import { HelpService } from '../../services/help.service';

export interface NavigationItem {
  label: string;
  icon: string;
  route: string;
  description: string;
}

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    OfflineIndicatorComponent,
    HelpPanelComponent,
    WhatsNewBannerComponent,
  ],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss',
})
export class NavigationComponent {
  @ViewChild(HelpPanelComponent) helpPanel!: HelpPanelComponent;

  private readonly helpService = inject(HelpService);

  navigationItems: NavigationItem[] = [
    {
      label: 'Dashboard',
      icon: 'dashboard',
      route: '/',
      description: 'Overview and statistics',
    },
    {
      label: 'Population Insights',
      icon: 'insights',
      route: '/insights',
      description: 'AI-powered population health insights',
    },
    {
      label: 'Patients',
      icon: 'people',
      route: '/patients',
      description: 'Patient management',
    },
    {
      label: 'Evaluations',
      icon: 'assessment',
      route: '/evaluations',
      description: 'Submit quality measure evaluations',
    },
    {
      label: 'Results',
      icon: 'bar_chart',
      route: '/results',
      description: 'View evaluation results',
    },
    {
      label: 'Reports',
      icon: 'description',
      route: '/reports',
      description: 'Generate quality reports',
    },
    {
      label: 'Report Builder',
      icon: 'build',
      route: '/report-builder',
      description: 'Create custom report templates',
    },
    {
      label: 'Pre-Visit Planning',
      icon: 'event_note',
      route: '/pre-visit',
      description: 'Prepare for upcoming patient visits',
    },
    {
      label: 'Visualizations',
      icon: '3d_rotation',
      route: '/visualization',
      description: '3D quality analytics visualizations',
    },
    {
      label: 'Agent Builder',
      icon: 'smart_toy',
      route: '/agent-builder',
      description: 'Configure AI agents',
    },
  ];

  sidenavOpened = true;

  constructor(private router: Router) {}

  isActive(route: string): boolean {
    return this.router.url === route || (route === '/' && this.router.url === '/');
  }

  toggleSidenav(): void {
    this.sidenavOpened = !this.sidenavOpened;
  }

  /**
   * Issue #24: Toggle help panel visibility
   */
  toggleHelp(): void {
    this.helpService.toggleHelpPanel();
  }

  /**
   * Get help panel observable for template binding
   */
  get showHelpPanel$() {
    return this.helpService.showHelpPanel$;
  }
}
