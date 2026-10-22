// 変わる形の面を描く(外の影、塗り、線状の影、枠)
// 色は部品のスタイルの CSS 変数(--piyo-s-fill、--piyo-s-edge、--piyo-s-ring-* など)を使う
// 形は SVG のマスクで切り抜く。いくつかの形を渡すと合わせた形として描き、重なった所の継ぎ目には線を出さない
// 線の位置は Android 版と同じ(枠は形の内側、線状の影は輪郭の真ん中)

import { shapeToPath } from './geometry.js';

// 層を部品より外に広げる幅(線状の影や枠が外へ出ても切れないように)
const MARGIN = 8;

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

// 線を引くパス
function stroke(d, width) {
  return `<path d="${d}" fill="none" stroke="#fff" stroke-width="${width}"/>`;
}

// 形ごとの線を、ほかの形の内側では消して描く SVG(合わせた形の継ぎ目に線を出さない)
// inside なら形の内側の帯(太さ line)、そうでなければ輪郭の真ん中の帯。穴の縁にも引く
// 消すのは、ほかの形を縁から線の太さだけ縮めた範囲。辺がちょうど重なる所(合わせた形の外周)の線は残る
function lineMask(paths, holes, width, height, line, inside) {
  // マスクの定義
  const mask = (id, content) => `<mask id="${id}" maskUnits="userSpaceOnUse" x="0" y="0" width="${width}" height="${height}">${content}</mask>`;
  const whole = '<rect width="100%" height="100%" fill="#fff"/>';
  const band = inside ? line * 2 : line;
  const defs = [];
  const body = [];
  // 形ごとの「縁の近く」を除くマスク(縮めた内側だけを残す)
  paths.forEach((d, j) => defs.push(mask(`n${j}`, `${whole}<path d="${d}" fill="none" stroke="#000" stroke-width="${line * 2}"/>`)));
  if (inside) defs.push(mask('mu', inner(paths, holes)));
  paths.forEach((d, i) => {
    const erase = paths.map((o, j) => (j === i ? '' : `<g mask="url(#n${j})"><path d="${o}" fill="#000"/></g>`)).join('');
    defs.push(mask(`m${i}`, whole + erase));
    body.push(`<g mask="url(#m${i})">${stroke(d, band)}</g>`);
  });
  const lines = inside ? `<g mask="url(#mu)">${body.join('')}</g>` : body.join('');
  let holeLines = '';
  if (holes.length) {
    defs.push(mask('mh', inner(paths, holes)));
    holeLines = `<g mask="url(#mh)">${holes.map((d) => stroke(d, band)).join('')}</g>`;
  }
  return svgUrl(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}"><defs>${defs.join('')}</defs>${lines}${holeLines}</svg>`);
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

// 影のもとの形のマスク(輪郭の上の画素から黒が漏れないよう、縁を少し削る)
function shadowSourceMask(paths, holes, width, height) {
  const trim = paths.map((d) => `<path d="${d}" fill="none" stroke="#000" stroke-width="3"/>`).join('');
  return masked(width, height, inner(paths, holes) + trim, WHOLE);
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

// 部品の面を、渡した形で描く。ringOnTop なら線状の影を枠の上に重ねる
// 外の影(--piyo-s-shadow-filter)と後ろのぼかし(--piyo-s-backdrop)は、変数があるときだけ効く
export class PaintedSurface {
  // 部品の要素の下に層を用意する
  constructor(host, { ringOnTop = false, margin = MARGIN } = {}) {
    this.host = host;
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
    // 線状の影の層を作る(マスクで切った線を、外の層でずらしてぼかす)
    const ring = () => layer(layer(this.root, {
      filter: 'blur(var(--piyo-s-ring-blur))',
      transform: 'translate(var(--piyo-s-ring-x), var(--piyo-s-ring-y))',
    }), { background: 'var(--piyo-s-ring-color)' });
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
    setMask(this.shadowShape, shadowSourceMask(paths, holePaths, w, h));
    setMask(this.shadow, outsideMask(paths, holePaths, w, h));
    setMask(this.edge, lineMask(paths, holePaths, w, h, edgeWidth, true));
    setMask(this.ring, lineMask(paths, holePaths, w, h, ringWidth, false));
  }

  // 描くのをやめ、CSS で描く面に戻す
  destroy() {
    this.root.remove();
    this.host.classList.remove('piyo-painted');
  }
}
