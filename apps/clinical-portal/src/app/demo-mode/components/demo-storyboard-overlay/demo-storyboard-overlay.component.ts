import { Component, OnDestroy, OnInit, Renderer2, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { DemoModeService } from '../../services/demo-mode.service';
import { DemoNarrationService } from '../../../services/demo-narration.service';

@Component({
  selector: 'app-demo-storyboard-overlay',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    @if (activeStep) {
      <div class="storyboard-overlay">
        <div class="storyboard-card">
          <div class="storyboard-header">
            <span class="storyboard-label">Storyboard</span>
            <div class="storyboard-connection" [class.connected]="connected">
              <mat-icon>{{ connected ? 'wifi' : 'wifi_off' }}</mat-icon>
              {{ connected ? 'Live' : 'Offline' }}
            </div>
          </div>
          <h3>{{ activeStep.title }}</h3>
          <p>{{ activeStep.narration }}</p>
        </div>
      </div>
    }
  `,
  styles: [`
    .storyboard-overlay {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 10001;
    }

    .storyboard-card {
      width: 360px;
      padding: 16px 18px;
      border-radius: 16px;
      background: rgba(15, 23, 42, 0.92);
      color: #f8fafc;
      border: 1px solid rgba(148, 163, 184, 0.25);
      box-shadow: 0 18px 40px rgba(15, 23, 42, 0.45);
      backdrop-filter: blur(12px);
    }

    .storyboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.12em;
      color: #94a3b8;
      margin-bottom: 10px;
    }

    .storyboard-connection {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #f87171;
    }

    .storyboard-connection.connected {
      color: #34d399;
    }

    h3 {
      margin: 0 0 8px;
      font-size: 18px;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.5;
      color: #e2e8f0;
    }
  `],
})
export class DemoStoryboardOverlayComponent implements OnInit, OnDestroy {
  activeStep: ReturnType<DemoModeService['activeStoryboardStep']> | null = null;
  connected = false;

  private highlightedElements: HTMLElement[] = [];
  private popupElements: HTMLElement[] = [];

  constructor(
    private demoModeService: DemoModeService,
    private narrationService: DemoNarrationService,
    private renderer: Renderer2
  ) {}

  ngOnInit(): void {
    this.activeStep = this.demoModeService.activeStoryboardStep();
    this.connected = this.demoModeService.storyboardConnected();
    effect(() => {
      const step = this.demoModeService.activeStoryboardStep();
      this.activeStep = step;
      this.connected = this.demoModeService.storyboardConnected();
      const selectors = step?.highlightSelectors ?? step?.popups?.map((popup) => popup.selector) ?? [];
      this.updateHighlights(selectors);
      this.renderPopups(step?.popups ?? []);
      if (step?.id) {
        this.narrationService.getNarration(step.id).subscribe((text) => {
          if (text) {
            this.narrationService.speak(text);
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.clearHighlights();
    this.clearPopups();
  }

  private updateHighlights(selectors: string[]): void {
    this.clearHighlights();
    selectors.forEach((selector) => {
      document.querySelectorAll(selector).forEach((element) => {
        const el = element as HTMLElement;
        this.renderer.addClass(el, 'demo-storyboard-highlight');
        this.highlightedElements.push(el);
      });
    });
  }

  private clearHighlights(): void {
    this.highlightedElements.forEach((element) => {
      this.renderer.removeClass(element, 'demo-storyboard-highlight');
    });
    this.highlightedElements = [];
  }

  private renderPopups(popups: Array<{ selector: string; content: string; position: string }>): void {
    this.clearPopups();
    popups.forEach((popup) => {
      const target = document.querySelector(popup.selector) as HTMLElement | null;
      if (!target) {
        return;
      }
      const popupEl = this.renderer.createElement('div');
      this.renderer.addClass(popupEl, 'demo-storyboard-popup');
      this.renderer.addClass(popupEl, `popup-${popup.position}`);
      const text = this.renderer.createText(popup.content);
      this.renderer.appendChild(popupEl, text);
      this.positionPopup(popupEl, target, popup.position);
      this.renderer.appendChild(document.body, popupEl);
      this.popupElements.push(popupEl);
    });
  }

  private positionPopup(popupEl: HTMLElement, target: HTMLElement, position: string): void {
    const hostRect = target.getBoundingClientRect();
    this.renderer.setStyle(popupEl, 'position', 'fixed');
    this.renderer.setStyle(popupEl, 'z-index', '10002');
    this.renderer.setStyle(popupEl, 'max-width', '320px');

    switch (position) {
      case 'bottom':
        this.renderer.setStyle(popupEl, 'top', `${hostRect.bottom + 10}px`);
        this.renderer.setStyle(popupEl, 'left', `${hostRect.left + hostRect.width / 2}px`);
        this.renderer.setStyle(popupEl, 'transform', 'translateX(-50%)');
        break;
      case 'left':
        this.renderer.setStyle(popupEl, 'top', `${hostRect.top + hostRect.height / 2}px`);
        this.renderer.setStyle(popupEl, 'right', `${window.innerWidth - hostRect.left + 12}px`);
        this.renderer.setStyle(popupEl, 'transform', 'translateY(-50%)');
        break;
      case 'right':
        this.renderer.setStyle(popupEl, 'top', `${hostRect.top + hostRect.height / 2}px`);
        this.renderer.setStyle(popupEl, 'left', `${hostRect.right + 12}px`);
        this.renderer.setStyle(popupEl, 'transform', 'translateY(-50%)');
        break;
      case 'top':
      default:
        this.renderer.setStyle(popupEl, 'bottom', `${window.innerHeight - hostRect.top + 10}px`);
        this.renderer.setStyle(popupEl, 'left', `${hostRect.left + hostRect.width / 2}px`);
        this.renderer.setStyle(popupEl, 'transform', 'translateX(-50%)');
        break;
    }
  }

  private clearPopups(): void {
    this.popupElements.forEach((popupEl) => {
      if (popupEl.parentNode) {
        popupEl.parentNode.removeChild(popupEl);
      }
    });
    this.popupElements = [];
  }
}
