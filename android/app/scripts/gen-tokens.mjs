#!/usr/bin/env node
// Bootstrap generator for Tokens.kt — kept in lockstep with the Gradle
// `genTokens` task defined in android/app/build.gradle.kts. Either entry point
// produces the same file from design-tokens/tokens.json.
//
// In normal development the Gradle task runs automatically as part of
// `preBuild`; this Node script lets contributors who don't have the Android
// toolchain regenerate the file as well (`node android/app/scripts/gen-tokens.mjs`).

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..', '..', '..');
const tokensPath = resolve(repoRoot, 'design-tokens', 'tokens.json');
const outPath = resolve(
  repoRoot,
  'android',
  'app',
  'src',
  'main',
  'java',
  'com',
  'remitos',
  'app',
  'ui',
  'theme',
  'Tokens.kt',
);

const tokens = JSON.parse(readFileSync(tokensPath, 'utf8'));

function hex(s) {
  const raw = s.replace(/^#/, '').toUpperCase();
  const withAlpha = raw.length === 6 ? `FF${raw}` : raw;
  return `0x${withAlpha}`;
}

function render(t) {
  const lines = [];
  lines.push('// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT.');
  lines.push('// Run `./gradlew :app:genTokens` after editing tokens.json.');
  lines.push('@file:Suppress("unused", "MayBeConstant")');
  lines.push('');
  lines.push('package com.remitos.app.ui.theme');
  lines.push('');
  lines.push('import androidx.compose.ui.graphics.Color');
  lines.push('import androidx.compose.ui.text.font.FontWeight');
  lines.push('import androidx.compose.ui.unit.dp');
  lines.push('import androidx.compose.ui.unit.sp');
  lines.push('import androidx.compose.ui.unit.TextUnit');
  lines.push('import androidx.compose.ui.unit.Dp');
  lines.push('');
  lines.push('object Tokens {');
  lines.push('    object Color {');
  lines.push('        // Brand');
  lines.push(`        val brandPrimary = Color(${hex(t.color.brand.primary)})`);
  lines.push(`        val brandPrimaryHover = Color(${hex(t.color.brand.primaryHover)})`);
  lines.push(`        val brandPrimaryPressed = Color(${hex(t.color.brand.primaryPressed)})`);
  lines.push(`        val brandPrimarySubtle = Color(${hex(t.color.brand.primarySubtle)})`);
  lines.push(`        val brandPrimarySurface = Color(${hex(t.color.brand.primarySurface)})`);
  lines.push(`        val brandRed = Color(${hex(t.color.brand.red)})`);
  lines.push(`        val brandRedHover = Color(${hex(t.color.brand.redHover)})`);
  lines.push(`        val brandRedSubtle = Color(${hex(t.color.brand.redSubtle)})`);
  lines.push('');
  lines.push('        // Neutrals');
  for (const [k, v] of Object.entries(t.color.neutral)) {
    lines.push(`        val ${k} = Color(${hex(v)})`);
  }
  lines.push('');
  lines.push('        // Semantic');
  lines.push(`        val success = Color(${hex(t.color.semantic.success)})`);
  lines.push(`        val successSubtle = Color(${hex(t.color.semantic.successSubtle)})`);
  lines.push(`        val successOn = Color(${hex(t.color.semantic.successOn)})`);
  lines.push(`        val warning = Color(${hex(t.color.semantic.warning)})`);
  lines.push(`        val warningSubtle = Color(${hex(t.color.semantic.warningSubtle)})`);
  lines.push(`        val warningOn = Color(${hex(t.color.semantic.warningOn)})`);
  lines.push(`        val error = Color(${hex(t.color.semantic.error)})`);
  lines.push(`        val errorSubtle = Color(${hex(t.color.semantic.errorSubtle)})`);
  lines.push(`        val errorOn = Color(${hex(t.color.semantic.errorOn)})`);
  lines.push(`        val info = Color(${hex(t.color.semantic.info)})`);
  lines.push(`        val infoSubtle = Color(${hex(t.color.semantic.infoSubtle)})`);
  lines.push(`        val infoOn = Color(${hex(t.color.semantic.infoOn)})`);
  lines.push('');
  lines.push('        // Surface');
  lines.push(`        val surfaceBackground = Color(${hex(t.color.surface.background)})`);
  lines.push(`        val surfaceMuted = Color(${hex(t.color.surface.muted)})`);
  lines.push(`        val surfaceBorder = Color(${hex(t.color.surface.border)})`);
  lines.push(`        val surfaceText = Color(${hex(t.color.surface.text)})`);
  lines.push(`        val surfaceTextMuted = Color(${hex(t.color.surface.textMuted)})`);
  lines.push('');
  lines.push('        // Buttons');
  lines.push(`        val buttonDisabledBackground = Color(${hex(t.color.button.disabledBackground)})`);
  lines.push(`        val buttonDisabledContent = Color(${hex(t.color.button.disabledContent)})`);
  lines.push('    }');
  lines.push('');
  lines.push('    object Radius {');
  for (const [k, v] of Object.entries(t.radius)) {
    lines.push(`        val ${k}: Dp = ${v}.dp`);
  }
  lines.push('    }');
  lines.push('');
  lines.push('    object Spacing {');
  for (const [k, v] of Object.entries(t.spacing)) {
    lines.push(`        val ${k}: Dp = ${v}.dp`);
  }
  lines.push('    }');
  lines.push('');
  lines.push('    object Type {');
  lines.push(`        const val family: String = "${t.type.family}"`);
  lines.push(
    '        data class Style(val size: TextUnit, val lineHeight: TextUnit, val weight: FontWeight, val letterSpacing: TextUnit)',
  );
  for (const [name, props] of Object.entries(t.type.scale)) {
    lines.push(
      `        val ${name} = Style(${props.size}.sp, ${props.lineHeight}.sp, FontWeight(${props.weight}), ${props.letterSpacing}.sp)`,
    );
  }
  lines.push('    }');
  lines.push('}');
  return lines.join('\n') + '\n';
}

writeFileSync(outPath, render(tokens));
process.stdout.write(`gen-tokens(android): wrote ${outPath}\n`);
