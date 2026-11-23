// カードの角の丸みの計算のテスト(Android 版の piyoCardShape と同じ決め方)

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cardRadius, cardEdgeMask } from '../src/card.js';

// 基準のカード(240x150、度合い 0.0833)は角丸 20
test('baseCard', () => {
  assert.equal(cardRadius(240, 150, 0.0833), 20);
});

// 横幅で決まり、高さが変わっても変わらない
test('widthDecides', () => {
  assert.equal(cardRadius(380, 120, 0.0833), cardRadius(380, 400, 0.0833));
});

// 背の低いカードでも横幅で決まり、開いて背が伸びても同じ(短い辺の半分まで)
test('shortCardKeepsRadius', () => {
  assert.equal(cardRadius(380, 60, 0.0833), cardRadius(380, 300, 0.0833));
  assert.equal(cardRadius(380, 20, 0.0833), 10);
});

// growth が 1 なら比例、0 なら固定
test('growthEnds', () => {
  assert.equal(cardRadius(400, 300, 0.1, 1), 40);
  assert.equal(cardRadius(400, 300, 0.1, 0), 24);
});

// 枠のマスクは半径と太さを持つ
test('edgeMask', () => {
  const mask = decodeURIComponent(cardEdgeMask(20, 1));
  assert.ok(mask.includes('ry="20"'));
  assert.ok(mask.includes('stroke-width="2"'));
});

// 基準の横幅が 0 以下なら、既定の 240 で求める(角が壊れない)
test('badBase', () => {
  assert.equal(cardRadius(400, 250, 0.0833, 0.3, 0), cardRadius(400, 250, 0.0833));
  assert.equal(cardRadius(400, 250, 0.0833, 0.3, -5), cardRadius(400, 250, 0.0833));
});
