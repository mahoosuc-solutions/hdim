import nx from '@nx/eslint-plugin';
import baseConfig from '../../eslint.config.mjs';

export default [
  ...baseConfig,
  ...nx.configs['flat/angular'],
  ...nx.configs['flat/angular-template'],
  {
    files: ['**/*.ts'],
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      // HIPAA Compliance: Prevent console statements in production code
      // PHI MUST NOT be logged to browser console - use LoggerService instead
      // LoggerService automatically filters PHI and provides proper audit trails
      'no-console': 'error',
    },
  },
  {
    files: ['**/*.spec.ts', '**/*.test.ts'],
    rules: {
      // Allow console in test files for debugging
      'no-console': 'off',
    },
  },
  {
    files: ['**/logger.service.ts', '**/server.ts'],
    rules: {
      // Allow console in LoggerService (its purpose is to wrap console)
      // and server.ts (Node.js server-side logging)
      'no-console': 'off',
    },
  },
  {
    files: ['**/*.html'],
    // Override or add rules here
    rules: {},
  },
];
