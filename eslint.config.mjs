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
];
