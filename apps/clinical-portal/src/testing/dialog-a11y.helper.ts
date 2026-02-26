import { ComponentFixture } from '@angular/core/testing';

/**
 * Asserts that an alert region exists and includes expected message text.
 */
export function expectAlertBanner<T>(
  fixture: ComponentFixture<T>,
  expectedMessage: string
): void {
  const alert = fixture.nativeElement.querySelector('[role="alert"]');
  expect(alert).toBeTruthy();
  expect(alert.textContent).toContain(expectedMessage);
}

/**
 * Asserts that expected inline validation text is rendered.
 */
export function expectInlineValidationText<T>(
  fixture: ComponentFixture<T>,
  expectedMessage: string
): void {
  expect(fixture.nativeElement.textContent).toContain(expectedMessage);
}
