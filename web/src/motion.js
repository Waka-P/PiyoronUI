// 動きの計算(イージング、区切りのつなぎ方、なめらかな変化)

// 2つの値の間
export function lerp(from, to, t) {
  return from + (to - from) * t;
}

// 0〜1 に収めた割合
export function part(t, from, to) {
  return Math.min(1, Math.max(0, (t - from) / (to - from)));
}

// なめらかに 0 から 1 へ(smoothstep)
export function smooth(x) {
  const c = Math.min(1, Math.max(0, x));
  return c * c * (3 - 2 * c);
}

// なめらかな小さいほう(差が k より小さいところを丸める)
export function smoothMin(a, b, k) {
  const h = Math.min(1, Math.max(0, 0.5 + (0.5 * (b - a)) / k));
  return lerp(b, a, h) - k * h * (1 - h);
}

// 区切りの位置ごとの値を、進行度に合わせて直線でつなぐ
export function keyframes(t, offsets, values) {
  if (t <= offsets[0]) return values[0];
  for (let i = 1; i < offsets.length; i++) {
    if (t <= offsets[i]) return lerp(values[i - 1], values[i], (t - offsets[i - 1]) / (offsets[i] - offsets[i - 1]));
  }
  return values[values.length - 1];
}

// 区切りの値を、区切りの間はなめらかにつなぐ
export function smoothKeyframes(t, offsets, values) {
  if (t <= offsets[0]) return values[0];
  for (let i = 1; i < offsets.length; i++) {
    if (t <= offsets[i]) return lerp(values[i - 1], values[i], smooth(part(t, offsets[i - 1], offsets[i])));
  }
  return values[values.length - 1];
}

// CSS の cubic-bezier と同じイージングを作る(x から y を求める)
export function cubicBezier(x1, y1, x2, y2) {
  // 曲線の t での x
  const bx = (t) => 3 * (1 - t) * (1 - t) * t * x1 + 3 * (1 - t) * t * t * x2 + t * t * t;
  // 曲線の t での y
  const by = (t) => 3 * (1 - t) * (1 - t) * t * y1 + 3 * (1 - t) * t * t * y2 + t * t * t;
  // x になる t を二分法で求めて、その y を返す
  return (x) => {
    if (x <= 0) return 0;
    if (x >= 1) return 1;
    let lo = 0;
    let hi = 1;
    for (let i = 0; i < 24; i++) {
      const mid = (lo + hi) / 2;
      if (bx(mid) < x) lo = mid;
      else hi = mid;
    }
    return by((lo + hi) / 2);
  };
}

// CSS 変数の cubic-bezier(...) からイージングを作る
export function easingFromCss(value) {
  const m = /cubic-bezier\(([^)]+)\)/.exec(value);
  if (!m) return (x) => x;
  const [a, b, c, d] = m[1].split(',').map(Number);
  return cubicBezier(a, b, c, d);
}

// CSS 変数を数値で読む(px と ms は数字だけ)
export function cssNumber(name, element = document.documentElement) {
  return parseFloat(getComputedStyle(element).getPropertyValue(name)) || 0;
}

// CSS 変数をそのまま読む
export function cssValue(name, element = document.documentElement) {
  return getComputedStyle(element).getPropertyValue(name).trim();
}

// 動きを減らす設定か(端末の設定、または data-piyoron-reduce-motion)
export function reduceMotion(element = document.documentElement) {
  if (element.closest?.('[data-piyoron-reduce-motion="true"]')) return true;
  return matchMedia('(prefers-reduced-motion: reduce)').matches;
}

// 0〜1 の進み具合を、時間をかけて動かす(毎コマ onFrame を呼ぶ。止めるときは返した関数を呼ぶ)
export function animate(duration, onFrame, onEnd) {
  if (duration <= 0) {
    onFrame(1);
    onEnd?.();
    return () => {};
  }
  const start = performance.now();
  let id = 0;
  let stopped = false;
  // 1コマ進める
  const step = (now) => {
    if (stopped) return;
    const t = Math.min(1, (now - start) / duration);
    onFrame(t);
    if (t < 1) id = requestAnimationFrame(step);
    else onEnd?.();
  };
  id = requestAnimationFrame(step);
  // 止める
  return () => {
    stopped = true;
    cancelAnimationFrame(id);
  };
}
