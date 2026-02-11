import { Component } from '@angular/core';
import { MeasureBuilderComponent } from './measure-builder/measure-builder.component';

@Component({
  imports: [MeasureBuilderComponent],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected title = 'mfe-measure-builder';
}
