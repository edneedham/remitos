#!/usr/bin/env node
// Generate the `@theme { … }` block in website/src/app/globals.css from the
// shared design tokens in design-tokens/tokens.json.
//
// Run via: `pnpm --filter enpunto-website gen:tokens`
// CI verifies the generated section is in sync with tokens.json.

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..', '..');
const tokensPath = resolve(repoRoot, 'design-tokens', 'tokens.json');
const cssPath = resolve(repoRoot, 'website', 'src', 'app', 'globals.css');

const BEGIN_MARK = '/* >>> DESIGN_TOKENS_BEGIN (generated — do not edit) */';
const END_MARK = '/* <<< DESIGN_TOKENS_END */';

const tokens = JSON.parse(readFileSync(tokensPath, 'utf8'));

function renderThemeBlock(t) {
  const lines = [];
  lines.push('@theme {');
  lines.push('  --color-background: var(--background);');
  lines.push('  --color-foreground: var(--foreground);');
  lines.push('  --font-sans: var(--font-inter);');
  lines.push('  --font-mono: var(--font-geist-mono);');
  lines.push('');
  lines.push('  /* Brand */');
  lines.push(`  --color-brand: ${t.color.brand.primary};`);
  lines.push(`  --color-brand-hover: ${t.color.brand.primaryHover};`);
  lines.push(`  --color-brand-pressed: ${t.color.brand.primaryPressed};`);
  lines.push(`  --color-brand-subtle: ${t.color.brand.primarySubtle};`);
  lines.push(`  --color-brand-surface: ${t.color.brand.primarySurface};`);
  lines.push(`  --color-brand-red: ${t.color.brand.red};`);
  lines.push(`  --color-brand-red-hover: ${t.color.brand.redHover};`);
  lines.push(`  --color-brand-red-subtle: ${t.color.brand.redSubtle};`);
  lines.push('');
  lines.push('  /* Semantic */');
  lines.push(`  --color-success: ${t.color.semantic.success};`);
  lines.push(`  --color-success-subtle: ${t.color.semantic.successSubtle};`);
  lines.push(`  --color-success-on: ${t.color.semantic.successOn};`);
  lines.push(`  --color-warning: ${t.color.semantic.warning};`);
  lines.push(`  --color-warning-subtle: ${t.color.semantic.warningSubtle};`);
  lines.push(`  --color-warning-on: ${t.color.semantic.warningOn};`);
  lines.push(`  --color-error: ${t.color.semantic.error};`);
  lines.push(`  --color-error-subtle: ${t.color.semantic.errorSubtle};`);
  lines.push(`  --color-error-on: ${t.color.semantic.errorOn};`);
  lines.push(`  --color-info: ${t.color.semantic.info};`);
  lines.push(`  --color-info-subtle: ${t.color.semantic.infoSubtle};`);
  lines.push(`  --color-info-on: ${t.color.semantic.infoOn};`);
  lines.push('');
  lines.push('  /* Neutrals */');
  lines.push(`  --color-surface: ${t.color.surface.background};`);
  lines.push(`  --color-surface-muted: ${t.color.surface.muted};`);
  lines.push(`  --color-text: ${t.color.surface.text};`);
  lines.push(`  --color-text-muted: ${t.color.surface.textMuted};`);
  lines.push(`  --color-border: ${t.color.surface.border};`);
  lines.push('');
  lines.push('  /* Buttons */');
  lines.push(`  --color-button-disabled-bg: ${t.color.button.disabledBackground};`);
  lines.push(`  --color-button-disabled-content: ${t.color.button.disabledContent};`);
  lines.push('');
  lines.push('  /* Radii (px) */');
  for (const [name, value] of Object.entries(t.radius)) {
    lines.push(`  --radius-${name}: ${value}px;`);
  }
  lines.push('');
  lines.push(
    '  /* Tailwind palette overrides so existing `bg-blue-600` / `text-red-600`',
  );
  lines.push(
    '     etc. callsites adopt the brand palette without needing per-file edits. */',
  );
  lines.push(`  --color-blue-50: ${t.color.brand.primarySurface};`);
  lines.push(`  --color-blue-100: ${t.color.brand.primarySubtle};`);
  lines.push(`  --color-blue-500: ${t.color.brand.primary};`);
  lines.push(`  --color-blue-600: ${t.color.brand.primary};`);
  lines.push(`  --color-blue-700: ${t.color.brand.primaryHover};`);
  lines.push(`  --color-blue-800: ${t.color.brand.primaryPressed};`);
  lines.push(`  --color-red-50: ${t.color.brand.redSubtle};`);
  lines.push(`  --color-red-100: ${t.color.brand.redSubtle};`);
  lines.push(`  --color-red-500: ${t.color.brand.red};`);
  lines.push(`  --color-red-600: ${t.color.brand.red};`);
  lines.push(`  --color-red-700: ${t.color.brand.redHover};`);
  lines.push('}');
  return lines.join('\n');
}

const generated = renderThemeBlock(tokens);

const existing = readFileSync(cssPath, 'utf8');

const begin = existing.indexOf(BEGIN_MARK);
const end = existing.indexOf(END_MARK);

let next;
if (begin === -1 || end === -1) {
  // First run: replace the original hand-written @theme block (if present) with
  // the marker-bracketed generated one. Falls back to prepending at top of file
  // if no @theme block was found.
  const themeRe = /@theme\s*\{[\s\S]*?\n\}\n?/;
  const replacement = `${BEGIN_MARK}\n${generated}\n${END_MARK}\n`;
  if (themeRe.test(existing)) {
    next = existing.replace(themeRe, replacement);
  } else {
    next = `${replacement}\n${existing}`;
  }
} else {
  const before = existing.slice(0, begin);
  const after = existing.slice(end + END_MARK.length);
  next = `${before}${BEGIN_MARK}\n${generated}\n${END_MARK}${after}`;
}

const check = process.argv.includes('--check');
if (check) {
  if (next !== existing) {
    process.stderr.write(
      `gen-tokens: ${cssPath} is out of sync with tokens.json.\n` +
        `Run \`pnpm --filter enpunto-website gen:tokens\` and commit the result.\n`,
    );
    process.exit(1);
  }
  process.stdout.write('gen-tokens: globals.css in sync with tokens.json\n');
  process.exit(0);
}

writeFileSync(cssPath, next);
process.stdout.write(`gen-tokens: wrote ${cssPath}\n`);
