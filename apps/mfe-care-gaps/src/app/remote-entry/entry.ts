import { Component } from '@angular/core';
import { CareGapsComponent } from '../components/care-gaps/care-gaps.component';

@Component({
  imports: [CareGapsComponent],
  selector: 'app-mfe-care-gaps-entry',
  template: `<app-care-gaps></app-care-gaps>`,
})
export class RemoteEntry {}
