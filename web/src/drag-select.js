// 長押ししたまま指やマウスを動かして、選ぶ項目を移す仕組み(タブ、ひょうたん、メニューなどで共有する)
// 長押しで始め、指の下の項目を onPreview に渡し、離した所の項目で onCommit を呼ぶ。入れ物から離れて離すと onCancel

import { cssNumber } from './motion.js';

// 指が入れ物からこれ以上離れたら、指の下の項目をなしにする(px)
const TOLERANCE = 24;

// 長押しになる前に、これ以上動いたらやめる(px)
const SLOP = 8;

// 位置 (x, y) から四角までの距離の2乗(中なら 0)
function distance(rect, x, y) {
  const dx = Math.max(rect.left - x, 0, x - rect.right);
  const dy = Math.max(rect.top - y, 0, y - rect.bottom);
  return dx * dx + dy * dy;
}

// container の中で、長押ししたまま動かして選べるようにする。止めるときは返した関数を呼ぶ
// items は項目の要素の並びを返す関数。onPreview(番号か null)は動かしている間、onCommit(番号)は離したとき、onCancel は取り消したとき
export function dragSelect(container, { items, onPreview, onCommit, onCancel }) {
  const abort = new AbortController();
  container.setAttribute('data-piyo-drag-select', '');
  const signal = abort.signal;
  let timer = 0;
  let start = null;
  let active = false;
  let pointer = null;
  let current = null;

  // 位置にいちばん近い、選べる項目の番号(入れ物から離れすぎていれば null)
  const hit = (x, y) => {
    const box = container.getBoundingClientRect();
    if (x < box.left - TOLERANCE || x > box.right + TOLERANCE || y < box.top - TOLERANCE || y > box.bottom + TOLERANCE) return null;
    let best = null;
    let bestDistance = Infinity;
    items().forEach((el, i) => {
      if (el.disabled || el.getAttribute('aria-disabled') === 'true') return;
      const d = distance(el.getBoundingClientRect(), x, y);
      if (d < bestDistance) {
        best = i;
        bestDistance = d;
      }
    });
    return best;
  };

  // 指の下の項目を変える
  const preview = (x, y) => {
    const next = hit(x, y);
    if (next === current) return;
    current = next;
    onPreview?.(next);
  };

  // 長押しの待ちをやめる
  const reset = () => {
    clearTimeout(timer);
    start = null;
    pointer = null;
  };

  // 動かしている状態を終える(commit なら指の下の項目で決める)
  const finish = (commit) => {
    const target = current;
    active = false;
    current = null;
    container.removeAttribute('data-piyo-dragging');
    if (commit && target !== null) onCommit?.(target);
    else onCancel?.();
    // 離した直後に来るクリックを捨てる(押した項目が選ばれ直さないように。来なければすぐ外す)
    const swallow = (e) => {
      e.preventDefault();
      e.stopPropagation();
    };
    container.addEventListener('click', swallow, { capture: true, once: true });
    setTimeout(() => container.removeEventListener('click', swallow, true), 0);
    reset();
  };

  container.addEventListener('pointerdown', (e) => {
    if (e.button !== 0 || active) return;
    start = { x: e.clientX, y: e.clientY };
    pointer = e.pointerId;
    const delay = cssNumber('--piyo-press-long-press-delay', container) || 400;
    timer = setTimeout(() => {
      active = true;
      container.setAttribute('data-piyo-dragging', '');
      try {
        container.setPointerCapture(pointer);
      } catch {
        // 指がもう離れているときは、とらえられなくてもよい
      }
      navigator.vibrate?.(10);
      preview(start.x, start.y);
    }, delay);
  }, { signal });

  container.addEventListener('pointermove', (e) => {
    if (e.pointerId !== pointer) return;
    if (active) preview(e.clientX, e.clientY);
    else if (start && Math.hypot(e.clientX - start.x, e.clientY - start.y) > SLOP) reset();
  }, { signal });

  container.addEventListener('pointerup', (e) => {
    if (e.pointerId !== pointer) return;
    if (active) {
      preview(e.clientX, e.clientY);
      finish(true);
    } else {
      reset();
    }
  }, { signal });

  container.addEventListener('pointercancel', (e) => {
    if (e.pointerId !== pointer) return;
    if (active) finish(false);
    else reset();
  }, { signal });

  // 動かしている間は、画面のスクロールと、長押しのメニューを止める
  container.addEventListener('touchmove', (e) => {
    if (active) e.preventDefault();
  }, { signal, passive: false });
  container.addEventListener('contextmenu', (e) => {
    if (active || start) e.preventDefault();
  }, { signal });

  // 止める
  return () => {
    clearTimeout(timer);
    abort.abort();
    container.removeAttribute('data-piyo-dragging');
    container.removeAttribute('data-piyo-drag-select');
  };
}
