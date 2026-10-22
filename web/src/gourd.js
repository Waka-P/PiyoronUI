// ひょうたん型のタブと、パネルとつないだ部品(Android 版の GourdTabRail.kt を移したもの)

import {
  GenieFrame, GourdProfile, bridgeShape, filletShape, filletShapeUp, gourdShape, gourdShapeOriented, mapShape, moveShape, shapeToPath,
} from './geometry.js';
import { animate, cssNumber, cssValue, easingFromCss, lerp, part, reduceMotion, smooth, smoothKeyframes, smoothMin } from './motion.js';
import { PaintedSurface } from './surface.js';

// ひょうたんの寸法と動きの値(トークンの CSS 変数から読む)
function gourdTokens(element) {
  // 1つ読む
  const n = (name) => cssNumber(`--piyo-gourd-${name}`, element);
  return {
    button: n('button-size'),
    gap: n('gap'),
    padding: n('padding'),
    fillet: n('fillet'),
    panelLeft: n('panel-left'),
    switchDuration: n('switch-duration'),
    bounceDuration: n('bounce-duration'),
    easing: easingFromCss(cssValue('--piyo-gourd-switch-easing', element)),
    trail: n('trail'),
    waist: n('waist'),
    anticipation: n('anticipation'),
    overshoot: n('overshoot'),
    fitInset: n('fit-inset'),
    arriveAt: n('arrive-at'),
    radius: cssNumber('--piyo-radius-lg', element),
    genieDuration: cssNumber('--piyo-genie-duration', element),
    genieEasing: easingFromCss(cssValue('--piyo-genie-easing', element)),
    neck: cssNumber('--piyo-genie-neck', element),
  };
}

// ひょうたんの幅(丸ボタン + 余白)
function railWidth(t) {
  return t.button + t.padding * 2;
}

// ひょうたんの長さ(丸の数 n)
function railLength(t, n) {
  return t.button * n + t.gap * Math.max(0, n - 1) + t.padding * 2;
}

// i 番目の丸ボタンの上端(小数も使う)
function buttonTop(t, i) {
  return t.padding + (t.button + t.gap) * i;
}

// ---------- 印の動き ----------

// くびれに合わせ始めと終わりの時間の幅
const FIT_RAMP = 0.12;

// 伸び縮みしたときの幅の変わりやすさ(長さの比の何乗で幅を変えるか)
const STRETCH_POWER = 0.25;

// 弾みありの、前の端と後ろの端の時間の区切り
const BOUNCE_FRONT = [0, 0.2, 0.55, 0.75, 1];
const BOUNCE_BACK = [0, 0.2, 0.4, 0.72, 0.88, 1];

// 前の端の進み(0〜1)。初動はゆっくり
function frontAt(t, k) {
  return k.easing(part(t, 0, 1 - k.trail));
}

// 後ろの端の進み(0〜1)。前より遅れて同じ動きをする
function backAt(t, k) {
  return k.easing(part(t, k.trail, 1));
}

// 弾みなしで隣へ移るときの、前と後ろの差の最大(移動距離に対する割合)
function peakLead(k) {
  let max = 0;
  for (let i = 0; i <= 200; i++) max = Math.max(max, frontAt(i / 200, k) - backAt(i / 200, k));
  return max;
}

// 時間 t(0〜1)での印の形(レール上の上端、長さ、幅、くびれに合わせる強さ)
function blobAt(k, motion, t, bouncy, lead) {
  const s = k.button;
  const step = k.button + k.gap;
  const from = buttonTop(k, motion.from);
  const to = buttonTop(k, motion.to);
  if (!motion.moving || from === to) return { top: to, length: s, width: s, fit: 0 };
  const dist = Math.abs(to - from);
  let front;
  let back;
  if (bouncy) {
    const a = k.anticipation;
    const o = k.overshoot;
    // 遠くへ移るときも伸び縮みは隣へ移るときと同じにし、残りの距離は前の端と同じ割合で進める
    const near = Math.min(dist, step);
    const extra = (dist - near) * smoothKeyframes(t, BOUNCE_FRONT, [0, 0, 1, 1, 1]);
    front = smoothKeyframes(t, BOUNCE_FRONT, [0, -a, dist + o, dist - o * 0.3, dist]);
    back = smoothKeyframes(t, BOUNCE_BACK, [0, -a * 0.25, -a * 0.25, near + o * 0.8, near - o * 0.2, near]) + extra;
  } else {
    front = dist * frontAt(t, k);
    back = dist * backAt(t, k);
    if (dist > step) {
      const cap = step * lead;
      back = front - smoothMin(front - back, cap, cap * 0.2);
    }
  }
  const top = to > from ? from + back : from - front;
  const length = s + front - back;
  const width = Math.min(Math.max(s * Math.pow(s / length, STRETCH_POWER), s * k.waist), s * 1.08);
  const fit = smooth(t / FIT_RAMP) * smooth((1 - t) / FIT_RAMP);
  return { top, length, width, fit };
}

// 輪郭を何点で表すか(片側)
const BLOB_SAMPLES = 40;

// 伸びた印の形(縦向きの箱の中。両端は丸く、くびれのところはひょうたんの内側に合わせて細る)
function blobShape(blob, profile, inset, soft) {
  const half = blob.width / 2;
  const len = blob.length;
  const cap = Math.min(half, len / 2);
  const right = [];
  for (let i = 0; i <= BLOB_SAMPLES; i++) {
    const y = (len / 2) * (1 - Math.cos((Math.PI * i) / BLOB_SAMPLES));
    const e = Math.min(y, len - y);
    const q = (cap - e) / cap;
    const round = e < cap ? half * Math.sqrt(Math.max(0, 1 - q * q)) : half;
    const room = profile.halfWidth(blob.top + y) - inset;
    const hw = Math.max(0, lerp(round, smoothMin(round, room, soft), blob.fit));
    right.push([half + hw, y]);
  }
  const left = right.slice(1, -1).reverse().map(([x, y]) => [blob.width - x, y]);
  const points = [...right, ...left];
  // 2点の真ん中
  const mid = (p, q) => [(p[0] + q[0]) / 2, (p[1] + q[1]) / 2];
  const start = mid(points[points.length - 1], points[0]);
  let prev = start;
  const segs = points.map((p, i) => {
    const next = mid(p, points[(i + 1) % points.length]);
    const seg = [prev[0] + ((p[0] - prev[0]) * 2) / 3, prev[1] + ((p[1] - prev[1]) * 2) / 3, next[0] + ((p[0] - next[0]) * 2) / 3, next[1] + ((p[1] - next[1]) * 2) / 3, next[0], next[1]];
    prev = next;
    return seg;
  });
  return [{ start, segs }];
}

// 選んだ項目の印の動きと形(ひょうたん単体とパネル付きで共有する)
class RailIndicator {
  // 印の要素を作る
  constructor(rail, buttons, { selected = 0, bouncy = false, horizontal = false } = {}) {
    this.rail = rail;
    this.buttons = buttons;
    this.bouncy = bouncy;
    this.horizontal = horizontal;
    this.k = gourdTokens(rail);
    this.motion = { from: selected, to: selected, t: 1, moving: false };
    this.lead = peakLead(this.k);
    this.profile = new GourdProfile(railWidth(this.k), railLength(this.k, buttons.length), buttons.length);
    this.element = document.createElement('div');
    this.element.className = 'piyo-gourd-indicator';
    rail.prepend(this.element);
    this.surface = new PaintedSurface(this.element, { ringOnTop: true });
    this.render();
  }

  // 選んだ項目を変える(印が伸びて移る)
  select(index) {
    if (index === this.motion.to) return;
    const m = this.motion;
    const fraction = m.moving ? this.k.easing(m.t) : 1;
    m.from = lerp(m.from, m.to, fraction);
    m.to = index;
    this.stop?.();
    if (reduceMotion(this.rail)) {
      m.t = 1;
      m.moving = false;
      this.render();
      return;
    }
    m.moving = true;
    const duration = this.bouncy ? this.k.bounceDuration : this.k.switchDuration;
    this.stop = animate(duration, (t) => {
      m.t = t;
      this.render();
    }, () => {
      m.moving = false;
      this.render();
    });
  }

  // 今の時間の印を描き、着いた丸ボタンの色を変える
  render() {
    const k = this.k;
    const m = this.motion;
    const blob = blobAt(k, m, m.t, this.bouncy, this.lead);
    const cross = (railWidth(k) - blob.width) / 2;
    const s = this.element.style;
    if (this.horizontal) {
      Object.assign(s, { left: `${blob.top}px`, top: `${cross}px`, width: `${blob.length}px`, height: `${blob.width}px` });
    } else {
      Object.assign(s, { left: `${cross}px`, top: `${blob.top}px`, width: `${blob.width}px`, height: `${blob.length}px` });
    }
    let shape = blobShape(blob, this.profile, k.fitInset, k.fitInset * 1.5);
    if (this.horizontal) shape = mapShape(shape, (x, y) => [y, x]);
    this.surface.draw([shape], this.horizontal ? blob.length : blob.width, this.horizontal ? blob.width : blob.length);
    const arrived = !m.moving || m.t >= k.arriveAt ? m.to : -1;
    this.buttons.forEach((b, i) => b.dataset.arrived = String(i === arrived));
  }

  // 後片付け
  destroy() {
    this.stop?.();
    this.surface.destroy();
    this.element.remove();
  }
}

// ---------- ひょうたん単体 ----------

// ひょうたん型のタブ。element は .piyo-gourd(中に .piyo-gourd-button を並べる)。選ぶと onSelect(番号)を呼ぶ
export class GourdTabRail {
  // 形を描き、印を置き、押したときの処理をつなぐ
  constructor(element, { selected = 0, onSelect, bouncy = false } = {}) {
    this.element = element;
    this.horizontal = element.dataset.orientation === 'horizontal';
    this.buttons = [...element.querySelectorAll('.piyo-gourd-button')];
    this.onSelect = onSelect;
    element.setAttribute('role', 'tablist');
    if (this.horizontal) element.setAttribute('aria-orientation', 'horizontal');
    else element.setAttribute('aria-orientation', 'vertical');
    this.surface = new PaintedSurface(element);
    this.indicator = new RailIndicator(element, this.buttons, { selected, bouncy, horizontal: this.horizontal });
    this.abort = new AbortController();
    this.buttons.forEach((b, i) => {
      b.setAttribute('role', 'tab');
      b.addEventListener('click', () => (this.onSelect ? this.onSelect(i) : this.select(i)), { signal: this.abort.signal });
    });
    this.select(selected);
    this.draw();
  }

  // 土台のひょうたんを描く
  draw() {
    const k = this.indicator.k;
    const w = railWidth(k);
    const h = railLength(k, this.buttons.length);
    const width = this.horizontal ? h : w;
    const height = this.horizontal ? w : h;
    this.surface.draw([gourdShapeOriented(width, height, this.buttons.length, this.horizontal)], width, height);
  }

  // 選んだ項目を変える
  select(index) {
    this.buttons.forEach((b, i) => b.setAttribute('aria-selected', String(i === index)));
    this.indicator.select(index);
  }

  // 後片付け
  destroy() {
    this.abort.abort();
    this.surface.destroy();
    this.indicator.destroy();
  }
}

// ---------- ひょうたんとパネル ----------

// 継ぎ目の動きの区切り(参照実装の page-rail.js と同じ)
const RETRACT_FROM = 0.4;
const ROUND_UNTIL = 0.65;
const EXTEND_UNTIL = 0.6;
const STRAIGHT_FROM = 0.35;
const STRAIGHT_UNTIL = 0.9;

// 継ぎ目とフィレットを、隣の形に食い込ませる幅(線の太さの2倍より大きく)
const JOIN_OVERLAP = 3;

// これより小さい継ぎ目とフィレットは描かない(幅のない形の輪郭が線として残らないように)
const MIN_PART = 0.5;

// 開閉の時間 t(0〜1)での継ぎ目の状態(伸び 0〜1、角の丸み 0〜1)
function seamAt(t, closing) {
  return closing
    ? { extend: 1 - smooth((t - RETRACT_FROM) / (1 - RETRACT_FROM)), round: smooth(t / ROUND_UNTIL) }
    : { extend: smooth(t / EXTEND_UNTIL), round: 1 - smooth((t - STRAIGHT_FROM) / (STRAIGHT_UNTIL - STRAIGHT_FROM)) };
}

// ひょうたんとパネルの端の間の余りに合わせた、パネルの角とフィレットの縦の丸み
function cornerFit(gap, r, f) {
  if (gap <= 0.5) return [r, 0];
  if (gap >= r + f) return [r, f];
  const k = gap / (r + f);
  return [r * k, f * k];
}

// 左に縦のひょうたんを置いた向き(基準の向き)から、実際の向きへの写し方
function sideMap(side, width, height) {
  const transposed = side === 'top' || side === 'bottom';
  const size = transposed ? { width: height, height: width } : { width, height };
  // 基準の向きの点を、実際の向きへ
  const map = {
    left: (x, y) => [x, y],
    right: (x, y) => [width - x, y],
    top: (x, y) => [y, x],
    bottom: (x, y) => [y, height - x],
  }[side];
  // 基準の向きの四角を、実際の向きへ
  const rect = (l, t, r, b) => {
    const p = map(l, t);
    const q = map(r, b);
    return { left: Math.min(p[0], q[0]), top: Math.min(p[1], q[1]), right: Math.max(p[0], q[0]), bottom: Math.max(p[1], q[1]) };
  };
  return { transposed, size, map, rect };
}

// ひょうたん型のタブとパネルをつないだ部品。表示中のタブをもう一度押すと、パネルがジニー効果で開閉する。閉じているときは、どのタブを押しても開く
// element は .piyo-gourd-panel。中に .piyo-gourd(丸ボタン)と .piyo-gourd-panel-content(中身)を置く
// side はひょうたんを置く側(left、right、top、bottom)、align は辺のどこに寄せるか(-1〜1)
export class GourdRailPanel {
  // 部品を組み立てる
  constructor(element, { selected = 0, open = true, side = 'left', align = -1, bouncy = false, onSelect, onOpenChange } = {}) {
    this.element = element;
    this.side = side;
    this.align = align;
    this.selected = selected;
    this.open = open;
    this.onSelect = onSelect;
    this.onOpenChange = onOpenChange;
    this.rail = element.querySelector('.piyo-gourd');
    this.rail.dataset.orientation = side === 'top' || side === 'bottom' ? 'horizontal' : 'vertical';
    this.buttons = [...this.rail.querySelectorAll('.piyo-gourd-button')];
    this.content = element.querySelector('.piyo-gourd-panel-content');
    this.body = this.content.parentElement.classList.contains('piyo-gourd-panel-body') ? this.content.parentElement : null;
    if (!this.body) {
      this.body = document.createElement('div');
      this.body.className = 'piyo-gourd-panel-body';
      this.content.replaceWith(this.body);
      this.body.append(this.content);
    }
    this.surfaceElement = element.querySelector(':scope > .piyo-gourd-panel-surface');
    this.ownSurfaceElement = !this.surfaceElement;
    if (this.ownSurfaceElement) {
      this.surfaceElement = document.createElement('div');
      this.surfaceElement.className = 'piyo-gourd-panel-surface';
      element.prepend(this.surfaceElement);
    }
    this.surface = new PaintedSurface(this.surfaceElement);
    this.k = gourdTokens(element);
    this.time = open ? 0 : 1;
    this.closing = !open;
    this.rail.setAttribute('role', 'tablist');
    this.indicator = new RailIndicator(this.rail, this.buttons, { selected, bouncy, horizontal: this.rail.dataset.orientation === 'horizontal' });
    this.abort = new AbortController();
    this.buttons.forEach((b, i) => {
      b.setAttribute('role', 'tab');
      b.addEventListener('click', () => this.press(i), { signal: this.abort.signal });
    });
    this.observer = new ResizeObserver(() => this.layout());
    this.observer.observe(element);
    this.markButtons();
    this.layout();
  }

  // 丸ボタンを押したとき(表示中なら開閉、ほかなら切り替えて、閉じていれば開く)
  press(i) {
    if (i === this.selected) {
      this.setOpen(!this.open);
      return;
    }
    if (this.onSelect) this.onSelect(i);
    else this.select(i);
    if (!this.open) this.setOpen(true);
  }

  // 選んだ項目を変える
  select(index) {
    this.selected = index;
    this.indicator.select(index);
    this.markButtons();
    this.layout();
  }

  // パネルを開閉する(ジニー効果)
  setOpen(open) {
    if (open === this.open) return;
    this.open = open;
    this.onOpenChange?.(open);
    this.closing = !open;
    this.markButtons();
    this.stop?.();
    const goal = open ? 0 : 1;
    if (reduceMotion(this.element)) {
      this.time = goal;
      this.layout();
      return;
    }
    const start = this.time;
    this.body.hidden = false;
    this.stop = animate(this.k.genieDuration * Math.abs(goal - start), (t) => {
      this.time = lerp(start, goal, t);
      this.layout();
    });
  }

  // 丸ボタンの読み上げ(選んだもの、開く・閉じる)
  markButtons() {
    this.buttons.forEach((b, i) => {
      b.setAttribute('aria-selected', String(i === this.selected));
      b.setAttribute('aria-expanded', String(this.open && i === this.selected));
    });
  }

  // 配置を求める(実際の向きでの px)
  placement() {
    const k = this.k;
    const width = this.element.clientWidth;
    const height = this.element.clientHeight;
    const m = sideMap(this.side, width, height);
    const len = railLength(k, this.buttons.length);
    const top = Math.max(0, m.size.height - len) * (Math.min(1, Math.max(-1, this.align)) + 1) / 2;
    const pl = k.panelLeft;
    const pw = m.size.width - pl;
    return { width, height, m, len, top, pl, pw };
  }

  // 配置と形を、今の時間に合わせて描く
  layout() {
    const k = this.k;
    const { width, height, m, len, top, pl, pw } = this.placement();
    const w = railWidth(k);
    const rail = m.rect(0, top, w, top + len);
    Object.assign(this.rail.style, { left: `${rail.left}px`, top: `${rail.top}px` });
    const panel = m.rect(pl, 0, pl + pw, m.size.height);
    Object.assign(this.body.style, { left: `${panel.left}px`, top: `${panel.top}px`, width: `${panel.right - panel.left}px`, height: `${panel.bottom - panel.top}px` });
    const t = this.time;
    const p = t >= 1 ? null : k.genieEasing(t);
    const seam = seamAt(this.closing ? t : 1 - t, this.closing);
    const r = k.radius;
    const f = k.fillet;
    const cx = w / 2;
    const ch = m.size.height;
    const [rTop, fTop] = cornerFit(top, r, f);
    const [rBottom, fBottom] = cornerFit(ch - top - len, r, f);
    const target = [cx - pl, top + buttonTop(k, this.selected) + k.button / 2];
    const frame = p === null ? null : new GenieFrame(p, pw, ch, target, k.neck, r, rTop, rBottom);
    // 継ぎ目は、パネルの行き先側の辺に合わせ、パネルがある間は先をパネルの角の丸みの終わりまで届かせる
    const bridgeTop = Math.min(len, Math.max(0, (frame ? frame.a : 0) - top));
    const bridgeBottom = Math.min(len, Math.max(0, (frame ? frame.d : ch) - top));
    const bridgeEnd = cx + (pl + r - cx) * seam.extend;
    const reach = frame ? pl + frame.placeX(0) + frame.r1 : null;
    const joinEnd = reach === null ? bridgeEnd : Math.max(bridgeEnd, reach);
    const joinRound = reach === null ? cx * seam.round : Math.min(cx * seam.round, joinEnd - reach);
    // 継ぎ目はパネルに少し食い込ませる(継ぎ目とパネルの境の線を、パネルの内側として確実に消すため。外形は変わらない)
    const overlap = reach === null ? 0 : JOIN_OVERLAP;
    const parts = [gourdShape(w, len, this.buttons.length)];
    if (bridgeBottom - bridgeTop > MIN_PART && joinEnd - cx > MIN_PART) parts.push(bridgeShape(w, bridgeBottom, joinEnd + overlap, joinRound, bridgeTop));
    if (frame) {
      // フィレットは、今のパネルの行き先側の辺に付け、辺の残りと継ぎ目の重なりに合わせて縮める
      const edge = pl + frame.placeX(0);
      const kf = smooth((bridgeEnd - edge) / f);
      const rx = Math.max(0, Math.min(f, edge - cx)) * kf;
      const below = Math.max(0, Math.min(fBottom, frame.d - frame.r1dY - (top + len))) * kf;
      const above = Math.max(0, Math.min(fTop, top - (frame.a + frame.r1aY))) * kf;
      if (rx > MIN_PART && below > MIN_PART) parts.push(filletShape(len, edge, rx, below, JOIN_OVERLAP));
      if (rx > MIN_PART && above > MIN_PART) parts.push(filletShapeUp(0, edge, rx, above, JOIN_OVERLAP));
    }
    const shapes = parts.map((s) => mapShape(moveShape(s, 0, top), m.map));
    if (frame) shapes.push(mapShape(moveShape(frame.placedShape(), pl, 0), m.map));
    this.surface.draw(shapes, width, height);
    this.drawContent(frame, m, panel, rail, rTop, rBottom);
  }

  // パネルの中身を、ジニーの形で切り抜いて縮め、ひょうたんの上には描かない(角の丸みは面と同じ rTop、rBottom)
  drawContent(frame, m, panel, rail, rTop, rBottom) {
    const k = this.k;
    const pw = panel.right - panel.left;
    const ph = panel.bottom - panel.top;
    this.body.hidden = this.time >= 1;
    const style = this.content.style;
    if (!frame || this.time <= 0) {
      Object.assign(style, { clipPath: '', transform: '', transformOrigin: '', opacity: '' });
    } else {
      // 実際の向きで、パネルから見たジニーの変形
      const target = [rail.left + railWidth(k) / 2 - panel.left, 0];
      const center = m.map(railWidth(k) / 2, this.placement().top + buttonTop(k, this.selected) + k.button / 2);
      target[0] = center[0] - panel.left;
      target[1] = center[1] - panel.top;
      const real = new GenieFrame(k.genieEasing(this.time), pw, ph, target, k.neck, k.radius, rTop, rBottom);
      const { transform, origin } = real.transform();
      Object.assign(style, {
        clipPath: `path("${shapeToPath(real.clipShape())}")`,
        transform,
        transformOrigin: origin,
        opacity: String(k.genieEasing(this.time) > 0.8 ? 1 - (k.genieEasing(this.time) - 0.8) / 0.2 : 1),
      });
    }
    // ひょうたんの形をくり抜く(パネルから見た位置)
    const hole = moveShape(
      gourdShapeOriented(rail.right - rail.left, rail.bottom - rail.top, this.buttons.length, this.rail.dataset.orientation === 'horizontal'),
      rail.left - panel.left,
      rail.top - panel.top,
    );
    this.body.style.clipPath = `path(evenodd, "M-9999 -9999H9999V9999H-9999Z${shapeToPath(hole)}")`;
  }

  // 後片付け
  destroy() {
    this.stop?.();
    this.abort.abort();
    this.observer.disconnect();
    this.surface.destroy();
    this.indicator.destroy();
    if (this.ownSurfaceElement) this.surfaceElement.remove();
  }
}
