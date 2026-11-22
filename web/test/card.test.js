// カードの角の丸みの計算のテスト(Android 版の piyoCardShape と同じ決め方)

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cardRadius, cardEdgeMask } from '../src/card.js';

// 基準のカード(240x150、度合い 0.1333)は角丸 20
test('baseCard', () => {
  assert.equal(cardRadius(240, 150, 0.1333), 20);
});

// 短い辺で決まる(向きを変えても同じ)
test('shorterSide', () => {
  assert.equal(cardRadius(150, 600, 0.2), cardRadius(600, 150, 0.2));
});

// 既定では、半径はゆるやかに増える(小さいカードは割合として丸い)
test('growsGently', () => {
  assert.equal(cardRadius(300, 60, 0.1333), 15.2);
  assert.equal(cardRadius(400, 250, 0.1333), 23.3);
});

// growth が 1 なら比例、0 なら固定
test('growthEnds', () => {
  assert.equal(cardRadius(600, 200, 0.2, 1), 40);
  assert.equal(cardRadius(600, 200, 0.2, 0), 30);
});

// 度合いは 0 から 0.5 に収める
test('clamped', () => {
  assert.equal(cardRadius(100, 80, 0.9), 40);
  assert.equal(cardRadius(100, 80, -1), 0);
});

// 枠のマスクは半径と太さを持つ
test('edgeMask', () => {
  const mask = decodeURIComponent(cardEdgeMask(20, 1));
  assert.ok(mask.includes('ry="20"'));
  assert.ok(mask.includes('stroke-width="2"'));
});
