import { Component } from '@angular/core';
import { DeploymentConsoleComponent } from '../deployment-console.component';

@Component({
  imports: [DeploymentConsoleComponent],
  selector: 'app-mfe-deployment-entry',
  template: `<app-deployment-console></app-deployment-console>`,
})
export class RemoteEntry {}
