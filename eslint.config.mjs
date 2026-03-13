import nx from '@nx/eslint-plugin';

export default [
  ...nx.configs['flat/base'],
  ...nx.configs['flat/typescript'],
  ...nx.configs['flat/javascript'],
  {
    ignores: ['**/dist'],
  },
  {
    files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
    rules: {
      '@nx/enforce-module-boundaries': [
        'error',
        {
          enforceBuildableLibDependency: true,
          allow: ['^.*/eslint(\\.base)?\\.config\\.[cm]?[jt]s$'],
          depConstraints: [
            {
              sourceTag: '*',
              onlyDependOnLibsWithTags: ['*'],
            },
          ],
        },
      ],
    },
  },
  {
    files: [
      '**/*.ts',
      '**/*.tsx',
      '**/*.cts',
      '**/*.mts',
      '**/*.js',
      '**/*.jsx',
      '**/*.cjs',
      '**/*.mjs',
    ],
    // Override or add rules here
    rules: {
      // SECURITY: Prevent console statements in production code
      // Use LoggerService instead for proper log level control and PHI filtering
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      // Existing Angular surfaces have not been migrated to inject()-style DI yet.
      '@angular-eslint/prefer-inject': 'warn',
      '@angular-eslint/directive-selector': 'warn',
      '@angular-eslint/component-selector': 'warn',
      '@angular-eslint/no-output-native': 'warn',
      // These remain valuable, but they are not release blockers for the current codebase.
      '@typescript-eslint/adjacent-overload-signatures': 'warn',
      '@typescript-eslint/no-empty-function': 'warn',
      '@typescript-eslint/no-empty-object-type': 'warn',
      '@typescript-eslint/no-inferrable-types': 'warn',
      '@typescript-eslint/no-namespace': 'warn',
      '@typescript-eslint/no-this-alias': 'warn',
      'no-case-declarations': 'warn',
      'no-prototype-builtins': 'warn',
    },
  },
  {
    files: ['**/*.html'],
    rules: {
      '@angular-eslint/template/prefer-control-flow': 'warn',
      '@angular-eslint/template/click-events-have-key-events': 'warn',
      '@angular-eslint/template/interactive-supports-focus': 'warn',
      '@angular-eslint/template/label-has-associated-control': 'warn',
      '@angular-eslint/template/no-negated-async': 'warn',
      '@angular-eslint/template/role-has-required-aria': 'warn',
    },
  },
  // Allow console in test files and logger service
  {
    files: [
      '**/*.spec.ts',
      '**/*.test.ts',
      '**/e2e/**/*.ts',
      '**/*-e2e/**/*.ts',
      '**/logger.service.ts',
      '**/api.service.ts', // Temporary - uses conditional logging
    ],
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
];
