// 形と置き場所の計算のテスト(Android 版の MenuPositionTest と同じ値)

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { GourdProfile, menuPosition } from '../src/geometry.js';
import { PiyoMenuAlignment as A } from '../src/menu.js';

const area = { width: 400, height: 800 };
const size = { width: 150, height: 120 };

// 四角を作る
function rect(left, top, right, bottom) {
  return { left, top, right, bottom };
}

// 既定の重なりと余白で置き場所を求める
function place(anchor, options = {}) {
  return menuPosition(anchor, options.size ?? size, area, 10, 8, options.side, options.align);
}

// ボタンの下に、ボタンの中心がメニューの右の角に来るよう少し重ねて置く
test('belowRightCorner', () => {
  assert.deepEqual(place(rect(300, 100, 338, 138)), { x: 169, y: 128 });
});

// 下に入らなければ、ボタンの上に少し重ねて置く
test('flipsAbove', () => {
  assert.deepEqual(place(rect(300, 700, 338, 738)), { x: 169, y: 590 });
});

// ボタンの中心をメニューの左の角、真ん中に置ける
test('alignsByBias', () => {
  assert.equal(place(rect(100, 100, 138, 138), { align: A.startCorner }).x, 119);
  assert.equal(place(rect(100, 100, 138, 138), { align: A.center }).x, 44);
});

// 右にはみ出すときは、画面の端から margin 空けて止める
test('clampsToRightMargin', () => {
  assert.equal(place(rect(330, 100, 368, 138), { align: A.startCorner }).x, 242);
});

// 左にはみ出すときは、画面の端から margin 空けて止める
test('clampsToLeftMargin', () => {
  assert.equal(place(rect(10, 100, 48, 138)).x, 8);
});

// 上にも下にも入らなければ、下に置いて画面の中に収める
test('clampsInsideWhenNoRoom', () => {
  assert.equal(place(rect(300, 100, 338, 138), { size: { width: 150, height: 780 } }).y, 12);
});

// 端どうしをそろえる
test('alignsEdges', () => {
  assert.equal(place(rect(100, 100, 138, 138), { align: A.startEdge }).x, 100);
  assert.equal(place(rect(100, 100, 138, 138), { align: A.endEdge }).x, 8);
});

// 縦に並んだボタンの右に出し、上の角にボタンの中心が来る
test('rightSideOfVerticalButtons', () => {
  assert.deepEqual(place(rect(20, 300, 58, 338), { side: 'right', align: A.startCorner }), { x: 48, y: 319 });
});

// 右に入らなければ左に出す
test('rightFlipsToLeft', () => {
  assert.deepEqual(place(rect(340, 300, 378, 338), { side: 'right', align: A.startCorner }), { x: 200, y: 319 });
});

// ひょうたんの幅は、丸の中心で最大、端で 0 になる
test('gourdProfile', () => {
  const p = new GourdProfile(48, 48 * 3 + 8, 3);
  assert.ok(Math.abs(p.halfWidth(24) - 24) < 0.5);
  assert.equal(p.halfWidth(0), 0);
  assert.ok(p.halfWidth(52) < 20);
});
