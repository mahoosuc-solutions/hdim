import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

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
  ],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss',
})
export class NavigationComponent {
  navigationItems: NavigationItem[] = [
    {
      label: 'Dashboard',
      icon: 'dashboard',
      route: '/',
      description: 'Overview and statistics',
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
      label: 'Visualizations',
      icon: '3d_rotation',
      route: '/visualization',
      description: '3D quality analytics visualizations',
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
}
