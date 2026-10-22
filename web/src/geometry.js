// 形の計算(ひょうたん、継ぎ目、フィレット、ジニー、メニューの置き場所)
// Android 版の GourdGeometry.kt と Genie.kt を移したもの
// 形は「始点と3次ベジェの並び」の配列で持つ: [{ start: [x, y], segs: [[c1x, c1y, c2x, c2y, x, y], ...] }]

import { keyframes, lerp } from './motion.js';

// 90度の円弧を3次ベジェで近似する係数
const K = 0.5523;

// 円弧の近似で、制御点を端から離す割合の残り
const ARC = 1 - K;

// 丸からくびれへ移る角度
const NECK_ANGLE = (35 * Math.PI) / 180;

// くびれの曲線の膨らみ(半径に対する割合)
const NECK_BULGE = 8 / 30;

// 直線を3次ベジェの区間にする
function straight(p, q) {
  return [p[0] + (q[0] - p[0]) / 3, p[1] + (q[1] - p[1]) / 3, p[0] + ((q[0] - p[0]) * 2) / 3, p[1] + ((q[1] - p[1]) * 2) / 3, q[0], q[1]];
}

// 区間の並び([始点, 制御点1, 制御点2, 終点])を、閉じた形にする
function shapeOf(segments) {
  return { start: segments[0][0], segs: segments.map(([, c1, c2, p3]) => [...c1, ...c2, ...p3]) };
}

// ひょうたんの右半分の区間(幅 w、高さ h、丸の数 n)
function gourdRight(w, h, n) {
  const r = w / 2;
  const cx = r;
  const kr = K * r;
  const k = (4 / 3) * Math.tan(NECK_ANGLE / 4) * r;
  const t = NECK_BULGE * r;
  const sinA = Math.sin(NECK_ANGLE);
  const cosA = Math.cos(NECK_ANGLE);
  const centers = n <= 1 ? [r] : Array.from({ length: n }, (_, i) => r + (i * (h - 2 * r)) / (n - 1));
  const first = centers[0];
  const last = centers[centers.length - 1];
  const segs = [[[cx, 0], [cx + kr, 0], [cx + r, first - kr], [cx + r, first]]];
  for (let i = 0; i + 1 < centers.length; i++) {
    const c1 = centers[i];
    const c2 = centers[i + 1];
    const p1 = [cx + r * cosA, c1 + r * sinA];
    const p2 = [cx + r * cosA, c2 - r * sinA];
    segs.push(
      [[cx + r, c1], [cx + r, c1 + k], [p1[0] + k * sinA, p1[1] - k * cosA], p1],
      [p1, [p1[0] - t * sinA, p1[1] + t * cosA], [p2[0] - t * sinA, p2[1] - t * cosA], p2],
      [p2, [p2[0] + k * sinA, p2[1] + k * cosA], [cx + r, c2 - k], [cx + r, c2]],
    );
  }
  segs.push([[cx + r, last], [cx + r, last + kr], [cx + kr, h], [cx, h]]);
  return segs;
}

// 右半分を左右反転して左半分にする
function mirrorLeft(right, cx) {
  // 中心線で反転した点
  const m = ([x, y]) => [2 * cx - x, y];
  return right.slice().reverse().map(([p0, c1, c2, p3]) => [m(p3), m(c2), m(c1), m(p0)]);
}

// ひょうたんの輪郭(縦向き。幅 w、高さ h、丸の数 n)
export function gourdShape(w, h, n) {
  const right = gourdRight(w, h, n);
  return [shapeOf([...right, ...mirrorLeft(right, w / 2)])];
}

// ひょうたんの輪郭(縦か横)。横なら丸が横に並ぶ(幅 w、高さ h)
export function gourdShapeOriented(w, h, n, horizontal) {
  return horizontal ? mapShape(gourdShape(h, w, n), (x, y) => [y, x]) : gourdShape(w, h, n);
}

// ひょうたんの、高さごとの中心から縁までの幅(右半分を上から下へたどった表)
export class GourdProfile {
  // 表を作る
  constructor(w, h, n) {
    const cx = w / 2;
    const ys = [];
    const xs = [];
    for (const [p0, c1, c2, p3] of gourdRight(w, h, n)) {
      for (let i = 0; i < 16; i++) {
        const t = i / 16;
        const u = 1 - t;
        ys.push(u * u * u * p0[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t * t * t * p3[1]);
        xs.push(u * u * u * p0[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t * t * t * p3[0] - cx);
      }
    }
    ys.push(h);
    xs.push(0);
    this.ys = ys;
    this.xs = xs;
  }

  // 高さ y での中心から縁までの幅
  halfWidth(y) {
    const { ys, xs } = this;
    if (y <= ys[0] || y >= ys[ys.length - 1]) return 0;
    let lo = 0;
    let hi = ys.length - 1;
    while (hi - lo > 1) {
      const mid = (lo + hi) >> 1;
      if (ys[mid] <= y) lo = mid;
      else hi = mid;
    }
    const span = ys[hi] - ys[lo];
    return span <= 0 ? Math.max(xs[lo], xs[hi]) : xs[lo] + ((xs[hi] - xs[lo]) * (y - ys[lo])) / span;
  }
}

// 継ぎ目(ひょうたんの中心から x まで伸びる帯。上端 top から下端 h まで。rc は右側の角の丸み)
export function bridgeShape(w, h, x, rc, top = 0) {
  const cx = w / 2;
  const r = Math.max(0, Math.min(rc, x - cx, (h - top) / 2));
  const kr = K * r;
  return [shapeOf([
    [[cx, top], ...pairs(straight([cx, top], [x - r, top]))],
    [[x - r, top], [x - r + kr, top], [x, top + r - kr], [x, top + r]],
    [[x, top + r], ...pairs(straight([x, top + r], [x, h - r]))],
    [[x, h - r], [x, h - r + kr], [x - r + kr, h], [x - r, h]],
    [[x - r, h], ...pairs(straight([x - r, h], [cx, h]))],
  ])];
}

// 6つの数を、点3つにする
function pairs(seg) {
  return [[seg[0], seg[1]], [seg[2], seg[3]], [seg[4], seg[5]]];
}

// フィレット(ひょうたんの下端 h とパネルの左端 pl の間をなめらかにつなぐ角。rf は横、ry は縦の大きさ)
export function filletShape(h, pl, rf, ry) {
  return [shapeOf([
    [[pl, h], ...pairs(straight([pl, h], [pl, h + ry]))],
    [[pl, h + ry], [pl, h + ry - K * ry], [pl - rf + K * rf, h], [pl - rf, h]],
  ])];
}

// 上向きのフィレット(ひょうたんの上端 y とパネルの左端の間)
export function filletShapeUp(y, pl, rf, ry) {
  return [shapeOf([
    [[pl, y], ...pairs(straight([pl, y], [pl, y - ry]))],
    [[pl, y - ry], [pl, y - ry + K * ry], [pl - rf + K * rf, y], [pl - rf, y]],
  ])];
}

// 角丸の四角
export function roundRectShape(x, y, w, h, r) {
  const k = r * ARC;
  return [shapeOf([
    [[x + r, y], ...pairs(straight([x + r, y], [x + w - r, y]))],
    [[x + w - r, y], [x + w - k, y], [x + w, y + k], [x + w, y + r]],
    [[x + w, y + r], ...pairs(straight([x + w, y + r], [x + w, y + h - r]))],
    [[x + w, y + h - r], [x + w, y + h - k], [x + w - k, y + h], [x + w - r, y + h]],
    [[x + w - r, y + h], ...pairs(straight([x + w - r, y + h], [x + r, y + h]))],
    [[x + r, y + h], [x + k, y + h], [x, y + h - k], [x, y + h - r]],
    [[x, y + h - r], ...pairs(straight([x, y + h - r], [x, y + r]))],
    [[x, y + r], [x, y + k], [x + k, y], [x + r, y]],
  ])];
}

// 楕円(四角 left、top、right、bottom に収まる)
export function ovalShape(left, top, right, bottom) {
  const cx = (left + right) / 2;
  const cy = (top + bottom) / 2;
  const kx = ((right - left) / 2) * K;
  const ky = ((bottom - top) / 2) * K;
  return [shapeOf([
    [[cx, top], [cx + kx, top], [right, cy - ky], [right, cy]],
    [[right, cy], [right, cy + ky], [cx + kx, bottom], [cx, bottom]],
    [[cx, bottom], [cx - kx, bottom], [left, cy + ky], [left, cy]],
    [[left, cy], [left, cy - ky], [cx - kx, top], [cx, top]],
  ])];
}

// 形の点を全部写す(反転や縦横の入れ替え、移動)
export function mapShape(shape, fn) {
  return shape.map(({ start, segs }) => ({
    start: fn(start[0], start[1]),
    segs: segs.map((s) => [...fn(s[0], s[1]), ...fn(s[2], s[3]), ...fn(s[4], s[5])]),
  }));
}

// 形をずらす
export function moveShape(shape, dx, dy) {
  return mapShape(shape, (x, y) => [x + dx, y + dy]);
}

// 形を SVG のパスの文字列にする
export function shapeToPath(shape) {
  // 小数2桁にする
  const f = (n) => Math.round(n * 100) / 100;
  return shape.map(({ start, segs }) => `M${f(start[0])} ${f(start[1])}` + segs.map((s) => `C${s.map(f).join(' ')}`).join('') + 'Z').join('');
}

// ---------- ジニー ----------

// 区切りの位置(開いた形、途中、閉じた形)
const OFFSETS = [0, 0.5, 1];

// 長さ len の範囲から v がはみ出している量
function outside(v, len) {
  return v < 0 ? -v : v > len ? v - len : 0;
}

// パネルの輪郭(参照実装の outline)。左端が行き先の側。a〜d は左上・右上・右下・左下の高さ
// r1 は行き先側の角の横の丸み、r1aY と r1dY はその上と下の角の縦の丸み、r2 は反対側の角の丸み。map は点の変換
function genieOutline(w, a, b, c, d, r1, r1aY, r1dY, r2, mirror, map) {
  const m = w / 2;
  // 反転と変換をかけた点
  const pt = (x, y) => map(mirror ? w - x : x, y);
  // 3つの点を1区間にする
  const seg = (x1, y1, x2, y2, x3, y3) => [...pt(x1, y1), ...pt(x2, y2), ...pt(x3, y3)];
  const side = [...pt(w, b + r2), ...pt(w, c - r2)];
  return [{
    start: pt(0, a + r1aY),
    segs: [
      seg(0, a + r1aY * ARC, r1 * ARC, a, r1, a),
      seg(m, a, m, b, w - r2, b),
      seg(w - r2 * ARC, b, w, b + r2 * ARC, w, b + r2),
      [side[0] + (side[2] - side[0]) / 3, side[1] + (side[3] - side[1]) / 3, side[0] + ((side[2] - side[0]) * 2) / 3, side[1] + ((side[3] - side[1]) * 2) / 3, side[2], side[3]],
      seg(w, c - r2 * ARC, w - r2 * ARC, c, w - r2, c),
      seg(m, c, m, d, r1, d),
      seg(r1 * ARC, d, 0, d - r1dY * ARC, 0, d - r1dY),
    ],
  }];
}

// 進行度 p での不透明度(終わり際に消える)
export function genieAlpha(p) {
  return keyframes(p, [0, 0.8, 1], [1, 1, 0]);
}

// 進行度 p(0 で開いた形、1 で閉じた形)でのジニーの変形。大きさ、行き先、首、角の丸みは px
// 行き先が上下にあるときは縦に縮める(縦横を入れ替えて同じ計算をする)。nearStart と nearEnd は行き先側の角の縦の丸み
export class GenieFrame {
  // 変形を求める
  constructor(p, width, height, target, neck, radius, nearStart = radius, nearEnd = radius) {
    this.vertical = outside(target[1], height) > outside(target[0], width);
    const w = this.vertical ? height : width;
    const h = this.vertical ? width : height;
    const tMain = this.vertical ? target[1] : target[0];
    const tCross = this.vertical ? target[0] : target[1];
    this.w = w;
    this.mirror = tMain > w / 2;
    this.origin = this.mirror ? w : 0;
    this.shift = keyframes(p, OFFSETS, [0, 0.45, 1]) * (this.mirror ? tMain - w : tMain);
    this.scale = keyframes(p, OFFSETS, [1, 0.55, 0.02]);
    const top = tCross - neck;
    const bottom = tCross + neck;
    this.a = keyframes(p, OFFSETS, [0, lerp(0, top, 0.9), top]);
    this.b = keyframes(p, OFFSETS, [0, lerp(0, top, 0.45), top]);
    this.c = keyframes(p, OFFSETS, [h, lerp(h, bottom, 0.45), bottom]);
    this.d = keyframes(p, OFFSETS, [h, lerp(h, bottom, 0.9), bottom]);
    this.r1 = keyframes(p, OFFSETS, [radius, 0, 0]);
    this.r1aY = keyframes(p, OFFSETS, [nearStart, 0, 0]);
    this.r1dY = keyframes(p, OFFSETS, [nearEnd, 0, 0]);
    this.r2 = keyframes(p, OFFSETS, [radius, radius / 2, 0]);
  }

  // 計算した向きの点を、要素の向きに戻す
  out(x, y) {
    return this.vertical ? [y, x] : [x, y];
  }

  // 縮めて動かしたあとの、縮める向きの位置
  placeX(x) {
    return this.origin + (x - this.origin) * this.scale + this.shift;
  }

  // 縮める前の形(中身を切り抜く用。縮みと移動は transform でかける)
  clipShape() {
    return genieOutline(this.w, this.a, this.b, this.c, this.d, this.r1, this.r1aY, this.r1dY, this.r2, this.mirror, (x, y) => this.out(x, y));
  }

  // 縮めて動かしたあとの形(面を描く用。線の太さは変わらない)
  placedShape() {
    return genieOutline(this.w, this.a, this.b, this.c, this.d, this.r1, this.r1aY, this.r1dY, this.r2, this.mirror, (x, y) => this.out(this.placeX(x), y));
  }

  // 中身にかける transform と、その中心
  transform() {
    const pivot = this.mirror ? '100%' : '0';
    return this.vertical
      ? { transform: `translateY(${this.shift}px) scaleY(${this.scale})`, origin: `50% ${pivot}` }
      : { transform: `translateX(${this.shift}px) scaleX(${this.scale})`, origin: `${pivot} 50%` };
  }
}

// ---------- メニューの置き場所 ----------

// ボタンに沿った向きの位置。角はボタンの中心がメニューの角に、端はメニューとボタンの端どうしをそろえる
function alongPosition(start, end, length, limit, margin, align) {
  const t = (Math.min(1, Math.max(-1, align.bias)) + 1) / 2;
  const center = (start + end) / 2;
  const wanted = align.edges ? lerp(start, end - length, t) : lerp(center, center - length, t);
  return Math.min(Math.max(wanted, margin), Math.max(margin, limit - margin - length));
}

// メニューの置き場所(左上)。ボタンの side の側に overlap だけ重ねて置き、入らなければ反対側。画面の端から margin は空ける
// anchor はボタンの四角({ left, top, right, bottom })、size はメニューの大きさ、area は置ける範囲の大きさ
export function menuPosition(anchor, size, area, overlap, margin, side = 'bottom', align = { bias: 1, edges: false }) {
  const vertical = side === 'bottom' || side === 'top';
  const cross = vertical
    ? alongPosition(anchor.left, anchor.right, size.width, area.width, margin, align)
    : alongPosition(anchor.top, anchor.bottom, size.height, area.height, margin, align);
  const length = vertical ? size.height : size.width;
  const limit = vertical ? area.height : area.width;
  const near = vertical ? anchor.top : anchor.left;
  const far = vertical ? anchor.bottom : anchor.right;
  const after = far - overlap;
  const before = near + overlap - length;
  const fitsAfter = after + length <= limit - margin;
  const fitsBefore = before >= margin;
  const preferAfter = side === 'bottom' || side === 'right';
  const raw = preferAfter ? (!fitsAfter && fitsBefore ? before : after) : (!fitsBefore && fitsAfter ? after : before);
  const main = Math.min(Math.max(raw, margin), Math.max(margin, limit - margin - length));
  return vertical ? { x: cross, y: main } : { x: main, y: cross };
}
