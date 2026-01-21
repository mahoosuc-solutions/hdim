import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ShellLayoutComponent } from './shell/shell-layout';

@Component({
  imports: [ShellLayoutComponent, RouterModule],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected title = 'shell-app';
}
