import baseConfig from '../../eslint.config.mjs';
import playwright from 'eslint-plugin-playwright';

export default [
  playwright.configs['flat/recommended'],
  ...baseConfig,
  {
    files: ['**/*.ts'],
    rules: {
      '@typescript-eslint/no-unused-vars': 'off',
    },
  },
];
