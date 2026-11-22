// カードの角の丸みを、大きさに合わせて決める(度合い x 基準の長さ x (短い辺 / 基準の長さ)^growth。Android の piyoCardShape と同じ)
// CSS では部品が自分の大きさを知れないので、ここで測って --piyo-card-radius と枠のマスク(--piyo-card-edge-mask)を入れる
// 読み込むと自動で始まる。あとから足したカードや、大きさが変わったカードにも効く

// 対象のカード
const SELECTOR = '.piyo-card';

// 丸み度合いから角の半径(px)を求める(度合いは 0 から 0.5、growth は 0 から 1 に収め、半径は短い辺の半分まで)
export function cardRadius(width, height, roundness, growth = 0.3, base = 150) {
  const r = Math.min(Math.max(Number(roundness) || 0, 0), 0.5);
  const g = Math.min(Math.max(Number(growth), 0), 1);
  const side = Math.min(width, height);
  if (side <= 0) return 0;
  const radius = Math.min(side / 2, r * base * Math.pow(side / base, g));
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
  const base = parseFloat(style.getPropertyValue('--piyo-card-roundness-base'));
  const r = cardRadius(width, height, roundness, Number.isNaN(growth) ? 0.3 : growth, Number.isNaN(base) ? 150 : base);
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
