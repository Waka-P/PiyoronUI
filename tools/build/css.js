// トークンを CSS 変数にする

import { flatten } from '../lib/tokens.js';
import { generateRoles, isRef, refName } from '../generate-colors/rules.js';
import { toHex } from '../generate-colors/color.js';

// camelCase を kebab-case にする
function kebab(s) {
  return s.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();
}

// 数値を短く書く
function num(n) {
  return String(Math.round(n * 10000) / 10000);
}

// 割合を % にする
function pct(n) {
  return `${num(n * 100)}%`;
}

// 変数名を作る
function varName(prefix, path) {
  return `--${prefix}-${path.map(kebab).join('-')}`;
}

// スタイル用の色ルールを CSS の式にする(mix と alpha だけ使える)
export function colorToCss(expr, prefix) {
  if (typeof expr === 'string') return isRef(expr) ? `var(--${prefix}-color-${kebab(refName(expr))})` : expr.toLowerCase();
  if (expr.oklch || expr.over || expr.contrast) throw new Error(`スタイルの色に oklch / over / contrast は使えない: ${JSON.stringify(expr)}`);
  let css;
  if (expr.from != null) css = colorToCss(expr.from, prefix);
  else if (expr.mix) css = `color-mix(in srgb, ${colorToCss(expr.mix[0], prefix)} ${pct(expr.mix[2])}, ${colorToCss(expr.mix[1], prefix)})`;
  else throw new Error(`色のルールに from / mix がない: ${JSON.stringify(expr)}`);
  if (expr.alpha != null) css = `color-mix(in srgb, ${css} ${pct(expr.alpha)}, transparent)`;
  return css;
}

// 影を CSS にする
function shadowToCss(layers, prefix) {
  return layers.map((s) => `${s.inset ? 'inset ' : ''}${num(s.x)}px ${num(s.y)}px ${num(s.blur)}px ${num(s.spread)}px ${colorToCss(s.color, prefix)}`).join(', ');
}

// 線形グラデーションを CSS にする
function gradientToCss(g, prefix) {
  return `linear-gradient(${num(g.angle)}deg, ${g.stops.map((s) => `${colorToCss(s.color, prefix)} ${pct(s.at)}`).join(', ')})`;
}

// 色付きの枠を、tone ごとの conic-gradient にする
function toneEdgeToCss(stops, tone, prefix, alpha = 1) {
  const t = `var(--${prefix}-color-${tone})`;
  const s = `var(--${prefix}-color-surface)`;
  // 1色(alpha が 1 未満なら透かす)
  const color = (p) => {
    const c = `color-mix(in srgb, ${t} ${pct(p.amount)}, ${s})`;
    return alpha < 1 ? `color-mix(in srgb, ${c} ${pct(alpha)}, transparent)` : c;
  };
  return `conic-gradient(from 0deg, ${stops.map((p) => `${color(p)} ${num(p.angle)}deg`).join(', ')})`;
}

// 色以外の値を CSS にする
function valueToCss(token, prefix) {
  const v = token.$value;
  switch (token.$type) {
    case 'dimension': return `${num(v)}px`;
    case 'duration': return `${num(v)}ms`;
    case 'number': return num(v);
    case 'fontSize': return `${num(v)}px`;
    case 'fontWeight': return String(v);
    case 'cubicBezier': return `cubic-bezier(${v.map(num).join(', ')})`;
    case 'fontFamily': return v.map((f) => (/^(sans-serif|serif|monospace|system-ui)$/.test(f) ? f : `"${f}"`)).join(', ');
    case 'color': return colorToCss(v, prefix);
    case 'shadow': return shadowToCss(v, prefix);
    case 'gradient': return gradientToCss(v, prefix);
    default: throw new Error(`CSS にできない型: ${token.$type}`);
  }
}

// 宣言の並びをブロックにする
function block(comment, selector, decls) {
  return `/* ${comment} */\n${selector} {\n${decls.map(([k, v]) => `  ${k}: ${v};`).join('\n')}\n}\n`;
}

// 色の役割を宣言の並びにする
function paletteDecls(semantic, input, prefix) {
  const { roles, edgeStops } = generateRoles(semantic, input);
  const decls = Object.entries(roles).map(([k, c]) => [varName(prefix, ['color', k]), toHex(c)]);
  edgeStops.slice(0, -1).forEach((s, i) => decls.push([varName(prefix, ['color', 'edge', String(i)]), toHex(s.color)]));
  return decls;
}

// Glass の宣言の並びを作る
function glassDecls(glass, semantic, prefix, radius = {}) {
  const decls = [];
  for (const [path, token] of flatten(glass)) {
    if (token.$type === 'toneStops') {
      for (const tone of ['primary', 'danger']) decls.push([varName(prefix, [...path, tone]), toneEdgeToCss(token.$value, tone, prefix)]);
    } else {
      decls.push([varName(prefix, path), valueToCss(token, prefix)]);
    }
  }
  const stops = semantic.color.edgeStops.$value.stops;
  const edge = [...stops.map((s, i) => `var(--${prefix}-color-edge-${i}) ${num(s.angle)}deg`), `var(--${prefix}-color-edge-0) 360deg`];
  decls.push([varName(prefix, ['glass', 'edge']), `conic-gradient(from 0deg, ${edge.join(', ')})`]);
  // 枠のマスク(角の丸みごと)。形の内側の帯を SVG の線で作るので、太さがどの向きでもそろう
  const width = glass.glass.edge?.width?.$value ?? 1;
  for (const [name, token] of Object.entries(radius)) {
    const r = token.$value >= 999 ? '50%' : String(token.$value);
    decls.push([varName(prefix, ['glass', 'edge', 'mask', name]), edgeMask(r, width)]);
  }
  // タブのつまみの枠(選んだ色の枠を blend だけ透かして、通常の枠に重ねる)
  const blend = glass.glass.tabs?.thumbEdgeBlend?.$value;
  if (blend !== undefined) {
    const tone = toneEdgeToCss(glass.glass.tone.edge.$value, 'primary', prefix, blend);
    decls.push([varName(prefix, ['glass', 'tabs', 'thumbEdge']), `${tone}, var(--${prefix}-glass-edge)`]);
  }
  return decls;
}

// 形の内側に太さ width の帯を残すマスク(SVG の線を枠の外まで太く引き、外側の半分は画像の外で切れる)
// r は角の丸み(px の数か '50%')。rx を省くと ry と同じになるので、'50%' なら高さの半分の丸み(ピルと円)
function edgeMask(r, width) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="100%" height="100%"><rect width="100%" height="100%" ry="${r}" fill="none" stroke="#000" stroke-width="${width * 2}"/></svg>`;
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
}

// CSS ファイルの中身を作る
export function buildCss(t) {
  const prefix = t.config.cssPrefix;
  const attr = t.config.dataAttribute;
  const coreDecls = [...flatten(t.primitives), ...flatten(t.effects)].map(([p, tok]) => [varName(prefix, p), valueToCss(tok, prefix)]);
  const [first, ...rest] = Object.entries(t.presets);
  const parts = [
    `/* ${t.config.name} トークン(自動生成。手で書き換えない。元は tokens/) */\n`,
    block('共通', ':root', coreDecls),
    block(`色: ${first[0]}(既定)`, `:root, [data-${attr}-palette="${first[0]}"]`, paletteDecls(t.semantic, first[1], prefix)),
    ...rest.map(([name, p]) => block(`色: ${name}`, `[data-${attr}-palette="${name}"]`, paletteDecls(t.semantic, p, prefix))),
    block('Glass', `:root, [data-${attr}-style="glass"], [data-${attr}-palette]`, glassDecls(t.styles.glass, t.semantic, prefix, t.primitives.radius)),
    ...Object.entries(t.presets).filter(([, p]) => p.toneUsesEdge).map(([name]) =>
      block(`${name}: 色付きの枠も枠の色の並びにする`, `[data-${attr}-palette="${name}"]`, [
        [`--${prefix}-glass-tone-edge-primary`, `var(--${prefix}-glass-edge)`],
        [`--${prefix}-glass-tabs-thumb-edge`, `var(--${prefix}-glass-edge)`],
      ])),
  ];
  return parts.join('\n');
}
