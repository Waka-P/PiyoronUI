// 色の計算(sRGB、OKLCH、混色、コントラスト)
// Kotlin 側の PiyoColorMath と同じ手順で計算する

// 0〜1 に収める
export function clamp01(v) {
  return Math.min(1, Math.max(0, v));
}

// #rrggbb / #rrggbbaa を { r, g, b, a }(0〜1)にする
export function parseHex(hex) {
  const h = hex.replace('#', '');
  if (!/^([0-9a-f]{6}|[0-9a-f]{8})$/i.test(h)) throw new Error(`色コードが不正: ${hex}`);
  // 2桁を 0〜1 にする
  const n = (i) => parseInt(h.slice(i, i + 2), 16) / 255;
  return { r: n(0), g: n(2), b: n(4), a: h.length === 8 ? n(6) : 1 };
}

// 0〜1 を 0〜255 の整数にする
function toByte(v) {
  return Math.round(clamp01(v) * 255);
}

// { r, g, b, a } を #rrggbb(不透明)か #rrggbbaa にする
export function toHex(c) {
  // 0〜1 を2桁にする
  const hex = (v) => toByte(v).toString(16).padStart(2, '0');
  const base = `#${hex(c.r)}${hex(c.g)}${hex(c.b)}`;
  return toByte(c.a) === 255 ? base : base + hex(c.a);
}

// 8bit に丸めた色にそろえる
export function quantize(c) {
  return { r: toByte(c.r) / 255, g: toByte(c.g) / 255, b: toByte(c.b) / 255, a: toByte(c.a) / 255 };
}

// sRGB の値を線形にする
function toLinear(v) {
  return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
}

// 線形の値を sRGB にする
function fromLinear(v) {
  return v <= 0.0031308 ? v * 12.92 : 1.055 * v ** (1 / 2.4) - 0.055;
}

// sRGB を OKLab にする
function rgbToOklab(c) {
  const r = toLinear(c.r), g = toLinear(c.g), b = toLinear(c.b);
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return {
    L: 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
    a: 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
    b: 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
  };
}

// OKLab を線形 RGB にする(範囲外もそのまま返す)
function oklabToLinear(L, a, b) {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return {
    r: 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    g: -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    b: -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  };
}

// sRGB を OKLCH { l, c, h(度) } にする
export function toOklch(c) {
  const lab = rgbToOklab(c);
  const chroma = Math.hypot(lab.a, lab.b);
  let h = (Math.atan2(lab.b, lab.a) * 180) / Math.PI;
  if (h < 0) h += 360;
  return { l: lab.L, c: chroma, h };
}

// OKLCH が sRGB の範囲に収まるか
function inGamut(l, c, h) {
  const rad = (h * Math.PI) / 180;
  const lin = oklabToLinear(l, c * Math.cos(rad), c * Math.sin(rad));
  const e = 1e-6;
  return [lin.r, lin.g, lin.b].every((v) => v >= -e && v <= 1 + e);
}

// OKLCH を sRGB にする。範囲外は彩度を下げて収める
export function fromOklch(l, c, h, a = 1) {
  const L = clamp01(l);
  let chroma = Math.max(0, c);
  if (!inGamut(L, chroma, h)) {
    let lo = 0, hi = chroma;
    for (let i = 0; i < 24; i++) {
      const mid = (lo + hi) / 2;
      if (inGamut(L, mid, h)) lo = mid; else hi = mid;
    }
    chroma = lo;
  }
  const rad = (h * Math.PI) / 180;
  const lin = oklabToLinear(L, chroma * Math.cos(rad), chroma * Math.sin(rad));
  return { r: clamp01(fromLinear(lin.r)), g: clamp01(fromLinear(lin.g)), b: clamp01(fromLinear(lin.b)), a };
}

// CSS の color-mix(in srgb, a p, b) と同じ混色
export function mix(a, b, p) {
  const alpha = a.a * p + b.a * (1 - p);
  if (alpha === 0) return { r: 0, g: 0, b: 0, a: 0 };
  // 成分を1つ混ぜる
  const ch = (k) => (a[k] * a.a * p + b[k] * b.a * (1 - p)) / alpha;
  return { r: ch('r'), g: ch('g'), b: ch('b'), a: alpha };
}

// 上の色を下の色に重ねる(source-over)
export function over(top, bottom) {
  const alpha = top.a + bottom.a * (1 - top.a);
  if (alpha === 0) return { r: 0, g: 0, b: 0, a: 0 };
  // 成分を1つ重ねる
  const ch = (k) => (top[k] * top.a + bottom[k] * bottom.a * (1 - top.a)) / alpha;
  return { r: ch('r'), g: ch('g'), b: ch('b'), a: alpha };
}

// 相対輝度(WCAG)
export function luminance(c) {
  return 0.2126 * toLinear(c.r) + 0.7152 * toLinear(c.g) + 0.0722 * toLinear(c.b);
}

// コントラスト比(WCAG)
export function contrast(a, b) {
  const la = luminance(a), lb = luminance(b);
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}

// 背景に対して比が足りるまで OKLCH の明度を動かす
export function ensureContrast(fg, bg, ratio) {
  if (contrast(fg, bg) >= ratio) return fg;
  const { l, c, h } = toOklch(fg);
  const darker = luminance(bg) > 0.18;
  let lo = darker ? 0 : l, hi = darker ? l : 1;
  for (let i = 0; i < 24; i++) {
    const mid = (lo + hi) / 2;
    const ok = contrast(quantize(fromOklch(mid, c, h, fg.a)), bg) >= ratio;
    if (darker) { if (ok) lo = mid; else hi = mid; } else { if (ok) hi = mid; else lo = mid; }
  }
  return quantize(fromOklch(darker ? lo : hi, c, h, fg.a));
}
