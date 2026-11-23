// カードの角の丸みを、横幅で決める(度合い x 基準の横幅 x (横幅 / 基準の横幅)^growth。高さの cap 倍までで、Android の piyoCardShape と同じ)
// CSS では部品が自分の大きさを知れないので、ここで測って --piyo-card-radius と枠のマスク(--piyo-card-edge-mask)を入れる
// 読み込むと自動で始まる。あとから足したカードや、大きさが変わったカードにも効く

// 対象のカード
const SELECTOR = '.piyo-card';

// 丸み度合いから角の半径(px)を求める(度合いは 0 から 0.5、growth は 0 から 1 に収め、半径は短い辺の半分まで)
export function cardRadius(width, height, roundness, growth = 0.3, base = 240, cap = 0.3) {
  const r = Math.min(Math.max(Number(roundness) || 0, 0), 0.5);
  const g = Math.min(Math.max(Number(growth), 0), 1);
  const b = Number(base) > 0 ? Number(base) : 240;
  if (width <= 0 || height <= 0) return 0;
  const byWidth = r * b * Math.pow(width / b, g);
  const radius = Math.min(byWidth, height * cap, Math.min(width, height) / 2);
  return Math.round(radius * 10) / 10;
}

// 半径 r の形の内側に、太さ width の帯を残すマスク(tools/build/css.js の edgeMask と同じ作り)
export function cardEdgeMask(r, width) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%"><rect width="100%" height="100%" ry="${r}" fill="none" stroke="#000" stroke-width="${width * 2}"/></svg>`;
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
}

// 1枚のカードの丸みを入れ直す
function apply(el, width, height) {
  if (!width || !height) return;
  const style = getComputedStyle(el);
  const roundness = parseFloat(style.getPropertyValue('--piyo-card-roundness'));
  if (Number.isNaN(roundness)) return;
  const growth = parseFloat(style.getPropertyValue('--piyo-card-roundness-growth'));
  const baseText = style.getPropertyValue('--piyo-card-roundness-base').trim();
  const base = baseText.endsWith('px') ? parseFloat(baseText) : NaN;
  const cap = parseFloat(style.getPropertyValue('--piyo-card-roundness-height-cap'));
  const r = cardRadius(width, height, roundness, Number.isNaN(growth) ? 0.3 : growth, Number.isNaN(base) || base <= 0 ? 240 : base, Number.isNaN(cap) ? 0.3 : cap);
  const edge = parseFloat(style.getPropertyValue('--piyo-glass-edge-width')) || 1;
  if (el.style.getPropertyValue('--piyo-card-radius') === `${r}px`) return;
  el.style.setProperty('--piyo-card-radius', `${r}px`);
  el.style.setProperty('--piyo-card-edge-mask', cardEdgeMask(r, edge));
}

// 始めたかどうか(場所ごと)
const started = new WeakMap();

// 測るのを始める(root の中のカードが対象。止めるときは返した関数を呼ぶ)
export function startPiyoCards(root = document) {
  if (started.has(root) || typeof ResizeObserver === 'undefined') return () => {};
  const resize = new ResizeObserver((entries) => {
    entries.forEach((e) => {
      const box = e.borderBoxSize?.[0];
      apply(e.target, box ? box.inlineSize : e.target.offsetWidth, box ? box.blockSize : e.target.offsetHeight);
    });
  });
  // root の中のカードを見張りに加える
  const watch = (node) => {
    if (!(node instanceof Element)) return;
    if (node.matches(SELECTOR)) resize.observe(node);
    node.querySelectorAll(SELECTOR).forEach((c) => resize.observe(c));
  };
  const added = new MutationObserver((records) => records.forEach((r) => r.addedNodes.forEach(watch)));
  const base = root === document ? document.documentElement : root;
  watch(base);
  added.observe(base, { childList: true, subtree: true });
  started.set(root, true);
  // 止める
  return () => {
    started.delete(root);
    added.disconnect();
    resize.disconnect();
  };
}

if (typeof document !== 'undefined') {
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => startPiyoCards(), { once: true });
  else startPiyoCards();
}
