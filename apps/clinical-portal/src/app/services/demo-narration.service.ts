import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, of, catchError } from 'rxjs';

interface NarrationStep {
  text: string;
}

interface NarrationPayload {
  steps?: Record<string, NarrationStep>;
}

@Injectable({
  providedIn: 'root',
})
export class DemoNarrationService {
  private readonly narrationUrl = '/demo/narration.json';
  private narrationCache: Record<string, string> = {};
  private narrationEnabled$ = new BehaviorSubject<boolean>(true);

  constructor(private http: HttpClient) {}

  isEnabled(): Observable<boolean> {
    return this.narrationEnabled$.asObservable();
  }

  setEnabled(enabled: boolean): void {
    this.narrationEnabled$.next(enabled);
  }

  loadNarration(): Observable<Record<string, string>> {
    if (Object.keys(this.narrationCache).length > 0) {
      return of(this.narrationCache);
    }
    return this.http.get<NarrationPayload>(this.narrationUrl).pipe(
      map((payload) => {
        this.narrationCache = {};
        Object.entries(payload.steps ?? {}).forEach(([key, value]) => {
          this.narrationCache[key] = value.text;
        });
        return this.narrationCache;
      }),
      catchError(() => of(this.narrationCache))
    );
  }

  getNarration(stepId: string): Observable<string | null> {
    return this.loadNarration().pipe(
      map((steps) => steps[stepId] ?? null)
    );
  }

  speak(text: string): void {
    if (!this.narrationEnabled$.value || !('speechSynthesis' in window)) {
      return;
    }
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1;
    utterance.pitch = 1;
    utterance.volume = 0.9;
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }
}
