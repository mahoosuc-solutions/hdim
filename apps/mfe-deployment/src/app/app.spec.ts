import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { DeploymentConsoleComponent } from './deployment-console.component';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App, DeploymentConsoleComponent],
    }).compileComponents();
  });

  it('should render deployment console header', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain(
      'Deployment & Seeding Console',
    );
  });
});
