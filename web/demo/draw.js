// カタログの共通の道具(アイコンの形、帯と背景の描き方)

// 線のアイコンの形(24x24。Android のサンプルと同じ)
export const ICONS = {
  plus: 'M12 5v14M5 12h14',
  check: 'M5 12.5l5 5L19 7',
  trash: 'M4 7h16M9 7V4.5h6V7M6.5 7l1 12.5h9l1-12.5',
  star: 'M12 4l2.4 5.2 5.6.6-4.2 3.8 1.2 5.6-5-2.8-5 2.8 1.2-5.6L4 9.8l5.6-.6z',
  home: 'M4 10.5L12 4l8 6.5V20h-5v-6H9v6H4z',
  search: 'M17 10.5a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0zM15.2 15.2L20 20',
  gear: 'M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1',
  copy: 'M8 8h12v12H8zM16 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3',
  download: 'M12 3v12M7 10l5 5 5-5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4',
  more: 'M12 5.5v.01M12 12v.01M12 18.5v.01',
  sparkle: 'M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z',
};

// 帯の左右の、つまみがはみ出して見える幅
export const BAR_OVERHANG = 16;

// CSS 変数の色を読む(element の場所での値。テーマを付けた要素の中なら、その色)
export function cssColor(name, element = document.documentElement) {
  return getComputedStyle(element).getPropertyValue(name).trim();
}

// canvas を今の大きさと画面の細かさに合わせ、2D の描画先を返す(大きさが 0 なら null)
export function prepare(canvas) {
  const w = canvas.clientWidth;
  const h = canvas.clientHeight;
  if (!w) return null;
  canvas.width = Math.round(w * devicePixelRatio);
  canvas.height = Math.round(h * devicePixelRatio);
  const ctx = canvas.getContext('2d');
  ctx.scale(devicePixelRatio, devicePixelRatio);
  return { ctx, w, h };
}

// 帯の canvas の上で押した位置を、0〜1 の割合にする
export function barFraction(canvas, clientX) {
  const r = canvas.getBoundingClientRect();
  return Math.min(1, Math.max(0, (clientX - r.left - BAR_OVERHANG) / (r.width - BAR_OVERHANG * 2)));
}

// 色相を選ぶ帯を描く(seedFromHue は色相から色を作る関数)
export function drawHueBar(canvas, hue, seedFromHue) {
  const c = prepare(canvas);
  if (!c) return;
  const { ctx, h } = c;
  const w = c.w - BAR_OVERHANG * 2;
  ctx.translate(BAR_OVERHANG, 0);
  const g = ctx.createLinearGradient(0, 0, w, 0);
  for (let i = 0; i <= 12; i++) g.addColorStop(i / 12, seedFromHue((i * 30) % 360));
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.roundRect(0, 0, w, h, h / 2);
  ctx.fill();
  ctx.strokeStyle = '#fff';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.arc((hue / 360) * w, h / 2, h / 2 - 2, 0, Math.PI * 2);
  ctx.stroke();
}

// -1〜1 の値を選ぶ帯を描く
export function drawBiasSlider(canvas, value) {
  const c = prepare(canvas);
  if (!c) return;
  const { ctx, h } = c;
  const w = c.w - BAR_OVERHANG * 2;
  ctx.translate(BAR_OVERHANG, 0);
  const y = h / 2;
  const x = ((value + 1) / 2) * w;
  ctx.lineCap = 'round';
  ctx.lineWidth = 6;
  ctx.strokeStyle = cssColor('--piyo-color-surface-muted', canvas);
  ctx.beginPath();
  ctx.moveTo(0, y);
  ctx.lineTo(w, y);
  ctx.stroke();
  ctx.strokeStyle = cssColor('--piyo-color-primary', canvas);
  ctx.beginPath();
  ctx.moveTo(0, y);
  ctx.lineTo(x, y);
  ctx.stroke();
  ctx.fillStyle = cssColor('--piyo-color-surface', canvas);
  ctx.beginPath();
  ctx.arc(x, y, 12, 0, Math.PI * 2);
  ctx.fill();
  ctx.lineWidth = 2;
  ctx.strokeStyle = cssColor('--piyo-color-primary', canvas);
  ctx.stroke();
}

// パネルの後ろの背景(格子と、ぼんやりした丸)を描く
export function drawBackdrop(canvas) {
  const c = prepare(canvas);
  if (!c) return;
  const { ctx, w, h } = c;
  ctx.fillStyle = cssColor('--piyo-color-background', canvas);
  ctx.fillRect(0, 0, w, h);
  ctx.strokeStyle = cssColor('--piyo-color-surface-muted', canvas);
  ctx.lineWidth = 1;
  for (let x = 0; x < w; x += 36) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  }
  for (let y = 0; y < h; y += 36) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }
  const primary = cssColor('--piyo-color-primary', canvas);
  // 主な色の丸を描く
  const circle = (cx, cy, r, alpha) => {
    ctx.globalAlpha = alpha;
    ctx.fillStyle = primary;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = 1;
  };
  circle(w * 0.8, h * 0.25, w * 0.28, 0.18);
  circle(w * 0.3, h * 0.85, w * 0.35, 0.12);
}
