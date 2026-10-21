// 色生成とトークン出力のテスト

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { loadTokens, root } from '../lib/tokens.js';
import { generateRoles, rolesToHex } from '../generate-colors/rules.js';
import { parseHex, toHex, toOklch, fromOklch, contrast, over, quantize } from '../generate-colors/color.js';
import { buildCss, colorToCss } from '../build/css.js';
import { buildKotlin } from '../build/kotlin.js';
import { fixtureCases } from '../build/cases.js';

const t = loadTokens();

// OKLab 上の色の差
function deltaE(a, b) {
  const p = toOklch(parseHex(a));
  const q = toOklch(parseHex(b));
  // 度をラジアンにする
  const rad = (h) => (h * Math.PI) / 180;
  return Math.hypot(p.l - q.l, p.c * Math.cos(rad(p.h)) - q.c * Math.cos(rad(q.h)), p.c * Math.sin(rad(p.h)) - q.c * Math.sin(rad(q.h)));
}

// 既定のシードで、参照実装の枠色とほぼ同じになる
test('既定シードの枠色が参照実装と近い', () => {
  const reference = ['#84a8ea', '#93b3ec', '#7fc4f0', '#8fbdf0', '#7fd8d0', '#a3e2e2', '#78cfc4', '#7fa6e4', '#5f8fdc', '#7396e2', '#8cb6ee', '#9ad6f0', '#7fa8ea', '#84a8ea'];
  const { edgeStops } = rolesToHex(generateRoles(t.semantic));
  assert.equal(edgeStops.length, reference.length);
  edgeStops.forEach((s, i) => assert.ok(deltaE(s.color, reference[i]) < 0.01, `${s.angle}deg: ${s.color} / ${reference[i]}`));
});

// 既定のシードで、参照実装の色コードを再現する
test('既定シードの役割が参照実装と同じ', () => {
  const r = rolesToHex(generateRoles(t.semantic));
  const reference = {
    primary: '#2293c7', primaryStrong: '#1d7bbf', surface: '#ffffff', surfaceMuted: '#f6f9fa', background: '#f1f5f7',
    contentDefault: '#1d2433', danger: '#e5484d', dangerStrong: '#c9363b', ok: '#1e9e6a', warn: '#e8920c',
    fillTop: '#ffffff9e', innerGlow: '#2293c724', innerHighlight: '#ffffffb3',
  };
  for (const [k, v] of Object.entries(reference)) assert.equal(r[k], v, k);
});

// どのシードでも文字のコントラストが足りる
test('文字のコントラストが 4.5:1 以上', () => {
  for (let h = 0; h < 360; h += 10) {
    for (const [l, c] of [[0.6, 0.15], [0.85, 0.2], [0.4, 0.05], [0.95, 0.05]]) {
      const seed = toHex(fromOklch(l, c, h));
      const { roles } = generateRoles(t.semantic, { seed });
      const fill = quantize(over(roles.fillBottom, roles.surface));
      const selected = quantize(over(roles.fillSelectedBottom, roles.surface));
      assert.ok(contrast(roles.contentDefault, fill) >= 4.5, `${seed} contentDefault`);
      assert.ok(contrast(roles.contentMuted, fill) >= 4.5, `${seed} contentMuted`);
      assert.ok(contrast(roles.contentSelected, selected) >= 4.5, `${seed} contentSelected`);
    }
  }
});

// 上書きした色が使われ、そこから作る色にも効く
test('danger の上書きが dangerStrong にも効く', () => {
  const base = rolesToHex(generateRoles(t.semantic));
  const r = rolesToHex(generateRoles(t.semantic, { seed: '#2293C7', overrides: { danger: '#B00020' } }));
  assert.equal(r.danger, '#b00020');
  assert.notEqual(r.dangerStrong, base.dangerStrong);
  assert.equal(r.primary, base.primary);
});

// プリセットが全部作れて、上書きした色もコントラストが足りる
test('プリセットが全部作れる', () => {
  for (const [name, p] of Object.entries(t.presets)) {
    const generated = generateRoles(t.semantic, p);
    const { roles } = generated;
    const r = rolesToHex(generated);
    assert.equal(r.seed, p.seed.toLowerCase(), name);
    assert.equal(r.edgeStops.length, 14, name);
    const fill = quantize(over(roles.fillBottom, roles.surface));
    const selected = quantize(over(roles.fillSelectedBottom, roles.surface));
    assert.ok(contrast(roles.contentDefault, fill) >= 4.5, `${name} contentDefault`);
    assert.ok(contrast(roles.contentMuted, fill) >= 4.5, `${name} contentMuted`);
    assert.ok(contrast(roles.contentSelected, selected) >= 4.5, `${name} contentSelected`);
  }
});

// スタイルの色は CSS で表せる操作だけ使う
test('スタイルの色に oklch は使えない', () => {
  assert.throws(() => colorToCss({ from: '{color.primary}', oklch: { l: { add: 0.1 } } }, 'piyo'));
  assert.equal(colorToCss({ from: '{color.primary}', alpha: 0.85 }, 'piyo'), 'color-mix(in srgb, var(--piyo-color-primary) 85%, transparent)');
});

// 書き出したファイルが、今のトークンと合っている
test('生成済みのファイルが最新', () => {
  const files = { 'web/tokens.css': buildCss(t), ...buildKotlin(t, fixtureCases(t)) };
  for (const [rel, text] of Object.entries(files)) {
    const onDisk = readFileSync(join(root, rel), 'utf8').replace(/\r\n/g, '\n');
    assert.equal(onDisk, text, `${rel} が古い。npm run tokens で作り直す`);
  }
});
