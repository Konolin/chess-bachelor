module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  ignorePatterns: ['projects/**/*'],
  rules: {
    'no-console': 0,
  },
  overrides: [
    {
      files: ['*.ts'],
      parserOptions: {
        project: ['tsconfig.json'],
        createDefaultProgram: true,
      },
      extends: [
        'plugin:@angular-eslint/recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:@typescript-eslint/recommended-requiring-type-checking',
        'plugin:@angular-eslint/template/process-inline-templates',
      ],
      rules: {
        '@typescript-eslint/no-explicit-any': 'error',
        '@typescript-eslint/explicit-function-return-type': 'warn',
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
        '@typescript-eslint/unbound-method': [
          'error',
          {
            ignoreStatic: true,
          },
        ],
        '@typescript-eslint/no-unused-vars': [
          'error',
          {
            ignoreRestSiblings: true,
          },
        ],
        '@typescript-eslint/no-unsafe-enum-comparison': 'off',
      },
    },
    {
      files: ['*.html'],
      extends: ['plugin:@angular-eslint/template/recommended'],
    },
  ],
};
