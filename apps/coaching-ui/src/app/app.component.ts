import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoachingPanelComponent } from './coaching-panel/coaching-panel.component';

/**
 * Root application component for the Coaching UI.
 * This is a minimal container that displays the coaching panel component
 * which handles all WebSocket communication and message display.
 */
@Component({
  selector: 'hdim-root',
  standalone: true,
  imports: [CommonModule, CoachingPanelComponent],
  template: '<app-coaching-panel></app-coaching-panel>',
  styles: [`
    :host {
      display: block;
      width: 100%;
      height: 100%;
      margin: 0;
      padding: 0;
    }
  `],
})
export class AppComponent {
  title = 'HDIM Coaching UI';
}
