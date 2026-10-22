// 変わる形の面を描く(外の影、塗り、線状の影、枠)
// 色は部品のスタイルの CSS 変数(--piyo-s-fill、--piyo-s-edge、--piyo-s-ring-* など)を使う
// 形はページの中の SVG のマスクで切り抜く(毎コマ書き換えるのはマスクの中のパスだけ。画像を作り直さない)
// いくつかの形を渡すと合わせた形として描き、重なった所の継ぎ目には線を出さない
// 線の位置は Android 版と同じ(枠は形の内側、線状の影は輪郭の真ん中)

import { shapeToPath } from './geometry.js';

// 層を部品より外に広げる幅(線状の影や枠が外へ出ても切れないように)
const MARGIN = 8;

// マスクの名前の通し番号(ページの中で名前が重ならないように)
let nextId = 0;

// 形の並びを、塗りのパスの並びにする(重なりは合わさる)
function fills(paths, color) {
  return paths.map((d) => `<path d="${d}" fill="${color}"/>`).join('');
}

// 形の内側(穴は除く)を白、外側を黒にした塗り
function inner(paths, holes) {
  return fills(paths, '#fff') + fills(holes, '#000');
}

// 全面の白(マスクを入れる svg は大きさ 0 なので、% ではなく数で大きさを書く)
function whole(width, height) {
  return `<rect width="${width}" height="${height}" fill="#fff"/>`;
}

// 形の外側(穴を含む)を白、内側を黒にした塗り
function outer(paths, holes, width, height) {
  return `${whole(width, height)}${fills(paths, '#000')}${fills(holes, '#fff')}`;
}

// 線を引くパス
function stroke(d, width) {
  return `<path d="${d}" fill="none" stroke="#fff" stroke-width="${width}"/>`;
}

// マスクの定義(id は名前、content は中身。白い所が見える)
function maskDef(id, width, height, content) {
  return `<mask id="${id}" maskUnits="userSpaceOnUse" x="0" y="0" width="${width}" height="${height}">${content}</mask>`;
}

// 形ごとの線を、ほかの形の内側では消して描くマスクの定義(合わせた形の継ぎ目に線を出さない)
// inside なら形の内側の帯(太さ line)、そうでなければ輪郭の真ん中の帯。穴の縁にも引く
// 消すのは、ほかの形を縁から線の太さだけ縮めた範囲。辺がちょうど重なる所(合わせた形の外周)の線は残る
// 形が1つで穴がなければ、入れ子のマスクを使わない(印など、毎コマ動く形を軽くする)
function lineMask(id, paths, holes, width, height, line, inside) {
  const band = inside ? line * 2 : line;
  const all = whole(width, height);
  if (paths.length === 1 && !holes.length) {
    if (!inside) return maskDef(id, width, height, stroke(paths[0], band));
    return maskDef(`${id}-u`, width, height, inner(paths, holes)) + maskDef(id, width, height, `<g mask="url(#${id}-u)">${stroke(paths[0], band)}</g>`);
  }
  const defs = [];
  const body = [];
  // 形ごとの「縁の近く」を除くマスク(縮めた内側だけを残す)
  paths.forEach((d, j) => defs.push(maskDef(`${id}-n${j}`, width, height, `${all}<path d="${d}" fill="none" stroke="#000" stroke-width="${line * 2}"/>`)));
  if (inside) defs.push(maskDef(`${id}-u`, width, height, inner(paths, holes)));
  paths.forEach((d, i) => {
    const erase = paths.map((o, j) => (j === i ? '' : `<g mask="url(#${id}-n${j})"><path d="${o}" fill="#000"/></g>`)).join('');
    defs.push(maskDef(`${id}-m${i}`, width, height, all + erase));
    body.push(`<g mask="url(#${id}-m${i})">${stroke(d, band)}</g>`);
  });
  const lines = inside ? `<g mask="url(#${id}-u)">${body.join('')}</g>` : body.join('');
  let holeLines = '';
  if (holes.length) {
    defs.push(maskDef(`${id}-h`, width, height, inner(paths, holes)));
    holeLines = `<g mask="url(#${id}-h)">${holes.map((d) => stroke(d, band)).join('')}</g>`;
  }
  return defs.join('') + maskDef(id, width, height, lines + holeLines);
}

// 層を1枚作る
function layer(parent, style) {
  const el = document.createElement('div');
  Object.assign(el.style, { position: 'absolute', inset: '0', pointerEvents: 'none', ...style });
  parent.append(el);
  return el;
}

// 層に、ページの中のマスクをかける
function useMask(el, id) {
  el.style.maskImage = `url(#${id})`;
  el.style.webkitMaskImage = `url(#${id})`;
}

// CSS の長さを px の数にする(読めなければ fallback)
function px(style, name, fallback) {
  const v = parseFloat(style.getPropertyValue(name));
  return Number.isNaN(v) ? fallback : v;
}

// 部品の面を、渡した形で描く。ringOnTop なら線状の影を枠の上に重ねる
// 外の影(--piyo-s-shadow-filter)は、作ったときに変数があれば描く。後ろのぼかし(--piyo-s-backdrop)は変数があるときだけ効く
export class PaintedSurface {
  // 部品の要素の下に層を用意する
  constructor(host, { ringOnTop = false, margin = MARGIN } = {}) {
    this.host = host;
    this.margin = margin;
    this.id = `piyo-paint-${++nextId}`;
    host.classList.add('piyo-painted');
    this.root = document.createElement('div');
    this.root.className = 'piyo-paint';
    Object.assign(this.root.style, { position: 'absolute', inset: `-${margin}px`, zIndex: '-1', pointerEvents: 'none' });
    this.svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    this.svg.setAttribute('width', '0');
    this.svg.setAttribute('height', '0');
    this.svg.setAttribute('aria-hidden', 'true');
    this.svg.style.position = 'absolute';
    this.root.append(this.svg);
    const style = getComputedStyle(host);
    // 外の影はメニューなど一部だけ。ないときは層もマスクも作らない
    if (style.getPropertyValue('--piyo-s-shadow-filter').trim()) {
      this.shadow = layer(this.root, { filter: 'var(--piyo-s-shadow-filter)' });
      this.shadowShape = layer(this.shadow, { background: '#000' });
      useMask(this.shadow, `${this.id}-out`);
      useMask(this.shadowShape, `${this.id}-src`);
    }
    this.drop = layer(this.root, { filter: 'var(--piyo-s-drop, none)' });
    this.fill = layer(this.drop, {
      background: 'var(--piyo-s-fill)',
      backdropFilter: 'var(--piyo-s-backdrop, none)',
      webkitBackdropFilter: 'var(--piyo-s-backdrop, none)',
    });
    this.inset = layer(this.drop, { background: 'var(--piyo-s-inset, none)' });
    useMask(this.fill, `${this.id}-fill`);
    useMask(this.inset, `${this.id}-fill`);
    // 線状の影の層を作る(マスクで切った線を、外の層でずらしてぼかす)
    const ring = () => layer(layer(this.root, {
      filter: 'blur(var(--piyo-s-ring-blur))',
      transform: 'translate(var(--piyo-s-ring-x), var(--piyo-s-ring-y))',
    }), { background: 'var(--piyo-s-ring-color)' });
    if (!ringOnTop) this.ring = ring();
    this.edge = layer(this.root, { background: 'var(--piyo-s-edge)', opacity: 'var(--piyo-s-edge-opacity, 1)' });
    if (ringOnTop) this.ring = ring();
    useMask(this.edge, `${this.id}-edge`);
    useMask(this.ring, `${this.id}-ring`);
    // 線の太さは最初に読んでおく(毎コマ読むとスタイルの計算が走る)
    this.edgeWidth = px(style, '--piyo-glass-edge-width', 1);
    this.ringWidth = px(style, '--piyo-s-ring-width', 1);
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
    const id = this.id;
    const defs = [
      maskDef(`${id}-fill`, w, h, inner(paths, holePaths)),
      lineMask(`${id}-edge`, paths, holePaths, w, h, this.edgeWidth, true),
      lineMask(`${id}-ring`, paths, holePaths, w, h, this.ringWidth, false),
    ];
    if (this.shadow) {
      const trim = paths.map((d) => `<path d="${d}" fill="none" stroke="#000" stroke-width="3"/>`).join('');
      defs.push(maskDef(`${id}-out`, w, h, outer(paths, holePaths, w, h)));
      defs.push(maskDef(`${id}-src`, w, h, inner(paths, holePaths) + trim));
    }
    this.svg.innerHTML = `<defs>${defs.join('')}</defs>`;
  }

  // 描くのをやめ、CSS で描く面に戻す
  destroy() {
    this.root.remove();
    this.host.classList.remove('piyo-painted');
  }
}
