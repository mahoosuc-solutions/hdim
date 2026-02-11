import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSliderModule } from '@angular/material/slider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { SliderConfig } from '../../models/measure-builder.model';

/**
 * Measure Config Slider Component
 *
 * Stub component to allow compilation.
 * TODO: Implement full slider configuration functionality
 */
@Component({
  selector: 'app-measure-config-slider',
  standalone: true,
  imports: [
    CommonModule,
    MatSliderModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FormsModule,
  ],
  template: `
    <div class="measure-config-slider">
      <h3>{{ config?.label }}</h3>
      <p>{{ config?.description }}</p>
      <!-- TODO: Implement slider UI based on config type -->
      <p class="stub-notice">Component under construction</p>
    </div>
  `,
  styles: [`
    .measure-config-slider {
      padding: 16px;
      border: 1px solid #ddd;
      border-radius: 4px;
      margin: 8px 0;
    }
    .stub-notice {
      color: #666;
      font-style: italic;
      font-size: 0.9em;
    }
  `]
})
export class MeasureConfigSliderComponent {
  @Input() config?: SliderConfig;
  @Output() valueChange = new EventEmitter<any>();
  @Output() cqlUpdate = new EventEmitter<string>();
}
