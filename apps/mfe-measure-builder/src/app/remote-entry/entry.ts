import { Component } from '@angular/core';
import { MeasureBuilderComponent } from '../measure-builder/measure-builder.component';

@Component({
  imports: [MeasureBuilderComponent],
  selector: 'app-mfe-measure-builder-entry',
  template: `<app-measure-builder></app-measure-builder>`,
})
export class RemoteEntry {}
