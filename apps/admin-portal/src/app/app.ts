import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AdminLayoutComponent } from './shared/components/layout/admin-layout.component';

@Component({
  imports: [RouterModule, AdminLayoutComponent],
  selector: 'app-root',
  template: `<app-admin-layout></app-admin-layout>`,
  styles: [
    `
      :host {
        display: block;
        height: 100vh;
      }
    `,
  ],
})
export class App {
  protected title = 'admin-portal';
}
