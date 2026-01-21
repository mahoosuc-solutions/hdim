import { Component } from '@angular/core';
import { QualityMeasuresComponent } from '../components/quality-measures/quality-measures.component';

@Component({
  imports: [QualityMeasuresComponent],
  selector: 'app-mfe-quality-entry',
  template: `<app-quality-measures></app-quality-measures>`,
})
export class RemoteEntry {}
