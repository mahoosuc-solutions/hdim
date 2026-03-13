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
    files: ['**/*.html'],
    // Override or add rules here
    rules: {
      '@angular-eslint/template/prefer-control-flow': 'warn',
      '@angular-eslint/template/click-events-have-key-events': 'warn',
      '@angular-eslint/template/interactive-supports-focus': 'warn',
      '@angular-eslint/template/label-has-associated-control': 'warn',
      '@angular-eslint/template/no-negated-async': 'warn',
      '@angular-eslint/template/role-has-required-aria': 'warn',
    },
  },
  {
    // Allow console in main.ts (bootstrap error handler before LoggerService initialization)
    files: ['**/main.ts'],
    rules: {
      'no-console': ['error', { allow: ['error'] }],
    },
  },
  {
    // Allow console in test files
    files: ['**/*.spec.ts', '**/*.test.ts'],
    rules: {
      'no-console': 'off',
    },
  },
  {
    files: ['**/*.a11y.spec.ts'],
    rules: {
      '@typescript-eslint/no-non-null-asserted-optional-chain': 'warn',
    },
  },
  {
    files: ['**/logger.service.ts'],
    rules: {
      'no-console': ['error', { allow: ['debug', 'log', 'warn', 'error'] }],
    },
  },
  {
    files: ['**/*.ts'],
    rules: {
      '@angular-eslint/prefer-inject': 'warn',
      '@angular-eslint/directive-selector': 'warn',
      '@angular-eslint/no-output-native': 'warn',
      '@angular-eslint/no-empty-lifecycle-method': 'warn',
      '@typescript-eslint/no-empty-function': 'warn',
      '@typescript-eslint/no-empty-object-type': 'warn',
      '@typescript-eslint/adjacent-overload-signatures': 'warn',
      '@typescript-eslint/no-inferrable-types': 'warn',
      '@typescript-eslint/no-namespace': 'warn',
      '@typescript-eslint/no-this-alias': 'warn',
      'no-case-declarations': 'warn',
      'no-prototype-builtins': 'warn',
    },
  },
];
