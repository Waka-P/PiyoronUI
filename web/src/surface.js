// 変わる形の面を描く(塗り、線状の影、枠)
// 色は部品のスタイルの CSS 変数(--piyo-s-fill、--piyo-s-edge、--piyo-s-ring-* など)を使う
// 形は SVG のマスクで切り抜く。いくつかの形を渡すと、合わせた形の外周に線を描く

import { shapeToPath } from './geometry.js';

// 層を部品より外に広げる幅(線状の影や枠が外へ出ても切れないように)
const MARGIN = 24;

// SVG を CSS のマスクに使える文字列にする
function svgUrl(svg) {
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
}

// 形の並びを、塗りのパスの並びにする(重なりは合わさる)
function fills(paths, color) {
  return paths.map((d) => `<path d="${d}" fill="${color}"/>`).join('');
}

// 形の内側(穴は除く)を白、外側を黒にした塗り
function inner(paths, holes) {
  return fills(paths, '#fff') + fills(holes, '#000');
}

// 形の外側(穴を含む)を白、内側を黒にした塗り
function outer(paths, holes) {
  return `<rect width="100%" height="100%" fill="#fff"/>${fills(paths, '#000')}${fills(holes, '#fff')}`;
}

// 中身を、マスクを通して描く SVG
function masked(width, height, keep, body) {
  return svgUrl(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">`
      + `<defs><mask id="m" maskUnits="userSpaceOnUse" x="0" y="0" width="${width}" height="${height}">${keep}</mask></defs>`
      + `<g mask="url(#m)">${body}</g></svg>`,
  );
}

// 線のマスク。inside なら形の内側の帯、そうでなければ合わせた形の外側の帯(穴の縁にも線を引く)
function lineMask(paths, holes, width, height, line, inside) {
  const strokes = [...paths, ...holes].map((d) => `<path d="${d}" fill="none" stroke="#fff" stroke-width="${line * 2}"/>`).join('');
  return masked(width, height, inside ? inner(paths, holes) : outer(paths, holes), strokes);
}

// 全面の白(SVG のマスクを通して、白黒を透明度に変える)
const WHOLE = '<rect width="100%" height="100%" fill="#fff"/>';

// 塗りのマスク(形を塗り、穴は抜く)
function fillMask(paths, holes, width, height) {
  return masked(width, height, inner(paths, holes), WHOLE);
}

// 外側だけのマスク(影を形の外にだけ出す)
function outsideMask(paths, holes, width, height) {
  return masked(width, height, outer(paths, holes), WHOLE);
}

// 層にマスクをかける
function setMask(el, url) {
  el.style.maskImage = url;
  el.style.webkitMaskImage = url;
}

// 層を1枚作る
function layer(parent, style) {
  const el = document.createElement('div');
  Object.assign(el.style, { position: 'absolute', inset: '0', pointerEvents: 'none', maskSize: '100% 100%', webkitMaskSize: '100% 100%', ...style });
  parent.append(el);
  return el;
}

// CSS の長さを px の数にする(読めなければ fallback)
function px(element, name, fallback) {
  const v = parseFloat(getComputedStyle(element).getPropertyValue(name));
  return Number.isNaN(v) ? fallback : v;
}

// 部品の面を、渡した形で描く。inside なら枠を形の内側に描く(形が1つのとき)。ringOnTop なら線状の影を枠の上に重ねる
// 外の影(--piyo-s-shadow-filter)と後ろのぼかし(--piyo-s-backdrop)は、変数があるときだけ効く
export class PaintedSurface {
  // 部品の要素の下に層を用意する
  constructor(host, { inside = true, ringOnTop = false, margin = MARGIN } = {}) {
    this.host = host;
    this.inside = inside;
    this.margin = margin;
    host.classList.add('piyo-painted');
    this.root = document.createElement('div');
    this.root.className = 'piyo-paint';
    Object.assign(this.root.style, { position: 'absolute', inset: `-${margin}px`, zIndex: '-1', pointerEvents: 'none' });
    this.shadow = layer(this.root, { filter: 'var(--piyo-s-shadow-filter, none)' });
    this.shadowShape = layer(this.shadow, { background: '#000' });
    this.drop = layer(this.root, { filter: 'var(--piyo-s-drop, none)' });
    this.fill = layer(this.drop, {
      background: 'var(--piyo-s-fill)',
      backdropFilter: 'var(--piyo-s-backdrop, none)',
      webkitBackdropFilter: 'var(--piyo-s-backdrop, none)',
    });
    this.inset = layer(this.drop, { background: 'var(--piyo-s-inset, none)' });
    // 線状の影の層を作る
    const ring = () => layer(this.root, {
      background: 'var(--piyo-s-ring-color)',
      filter: 'blur(var(--piyo-s-ring-blur))',
      transform: 'translate(var(--piyo-s-ring-x), var(--piyo-s-ring-y))',
    });
    if (!ringOnTop) this.ring = ring();
    this.edge = layer(this.root, { background: 'var(--piyo-s-edge)', opacity: 'var(--piyo-s-edge-opacity, 1)' });
    if (ringOnTop) this.ring = ring();
    host.prepend(this.root);
  }

  // 形を描く。shapes は形の並び(部品の左上から見た px)、width と height は部品の大きさ、holes はくり抜く形の並び
  draw(shapes, width, height, holes = []) {
    const m = this.margin;
    const w = width + m * 2;
    const h = height + m * 2;
    // 形をずらしてパスにする
    const toPath = (s) => shapeToPath(s.map(({ start, segs }) => ({
      start: [start[0] + m, start[1] + m],
      segs: segs.map((g) => g.map((v) => v + m)),
    })));
    const paths = shapes.map(toPath);
    const holePaths = holes.map(toPath);
    const edgeWidth = px(this.host, '--piyo-glass-edge-width', 1);
    const ringWidth = px(this.host, '--piyo-s-ring-width', 1);
    const fill = fillMask(paths, holePaths, w, h);
    setMask(this.fill, fill);
    setMask(this.inset, fill);
    setMask(this.shadowShape, fill);
    setMask(this.shadow, outsideMask(paths, holePaths, w, h));
    setMask(this.edge, lineMask(paths, holePaths, w, h, edgeWidth, this.inside));
    setMask(this.ring, lineMask(paths, holePaths, w, h, ringWidth, false));
  }

  // 描くのをやめ、CSS で描く面に戻す
  destroy() {
    this.root.remove();
    this.host.classList.remove('piyo-painted');
  }
}
