import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

// Configure Jest test environment
Object.defineProperty(window, 'CSS', { value: null });
Object.defineProperty(window, 'getComputedStyle', {
  value: () => ({
    display: 'none',
    appearance: ['-webkit-appearance']
  })
});

Object.defineProperty(document, 'doctype', {
  value: '<!DOCTYPE html>'
});

Object.defineProperty(document, 'URL', {
  writable: true,
  value: 'http://localhost'
});

// Jasmine compatibility shim for Jest
// Some tests were written for Jasmine and use jasmine.createSpyObj
// This provides a Jest-based implementation
if (!global.jasmine) {
  global.jasmine = {
    createSpyObj: (baseName: string, methods: string[] | Record<string, any>) => {
      const methodArray = Array.isArray(methods) ? methods : Object.keys(methods);
      const spyObj: any = {};
      methodArray.forEach((method: string) => {
        spyObj[method] = jest.fn();
      });
      return spyObj;
    },
    SpyObj: {} as any,
  } as any;
}

setupZoneTestEnv({
  errorOnUnknownElements: true,
  errorOnUnknownProperties: true,
});
