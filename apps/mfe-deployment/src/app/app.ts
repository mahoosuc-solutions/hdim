import { Component } from '@angular/core';
import { DeploymentConsoleComponent } from './deployment-console.component';

@Component({
  imports: [DeploymentConsoleComponent],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected title = 'mfe-deployment';
}
