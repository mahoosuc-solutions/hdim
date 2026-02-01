import { Component } from '@angular/core';
import { ReportsDashboardComponent } from '../components/reports-dashboard/reports-dashboard.component';

@Component({
  imports: [ReportsDashboardComponent],
  selector: 'app-mfe-reports-entry',
  template: `<app-reports-dashboard></app-reports-dashboard>`,
})
export class RemoteEntry {}
