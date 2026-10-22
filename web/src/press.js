// 押したときの動きのため、部品の幅と高さを CSS 変数(--piyo-w、--piyo-h)に入れる
// 拡大率は CSS が「1 + 量 ÷ 幅」で求める。CSS では部品が自分の大きさを知れないので、ここで測る
// マウスを乗せたときと押したときに測る(あとから足した部品にも効く)。読み込むと自動で始まる

// 動きを付ける部品
const SELECTOR = '.piyo-btn, .piyo-chip, .piyo-pressable';

// 出来事の起きた部品の大きさを測って入れる(拡大・縮小を含まない元の大きさ)
function measure(event) {
  const el = event.target instanceof Element ? event.target.closest(SELECTOR) : null;
  if (!el) return;
  el.style.setProperty('--piyo-w', `${el.offsetWidth}px`);
  el.style.setProperty('--piyo-h', `${el.offsetHeight}px`);
}

// 始めたかどうか(場所ごと)
const started = new WeakSet();

// 測るのを始める(root の中の部品が対象。止めるときは返した関数を呼ぶ)
export function startPiyoPress(root = document) {
  if (started.has(root)) return () => {};
  started.add(root);
  root.addEventListener('pointerover', measure, true);
  root.addEventListener('pointerdown', measure, true);
  // 止める
  return () => {
    started.delete(root);
    root.removeEventListener('pointerover', measure, true);
    root.removeEventListener('pointerdown', measure, true);
  };
}

if (typeof document !== 'undefined') startPiyoPress();
