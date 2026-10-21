// 色のルールの評価
// ルールの書き方は tokens/schema.md を参照

import { parseHex, toHex, quantize, toOklch, fromOklch, mix, over, ensureContrast } from './color.js';

// "{color.primary}" を "primary" にする
export function refName(ref) {
  const m = /^\{color\.(\w+)\}$/.exec(ref);
  if (!m) throw new Error(`色の参照が不正: ${ref}`);
  return m[1];
}

// 参照か
export function isRef(v) {
  return typeof v === 'string' && v.startsWith('{');
}

// 色相のずらし幅の倍率(中心の近くは 1、離れるほど min に近づく)
export function spreadFactor(hue, p) {
  if (!p) return 1;
  const d = Math.abs(((hue - p.center + 540) % 360) - 180);
  const t = Math.min(1, Math.max(0, (d - p.plateau) / p.width));
  return p.min + ((1 - p.min) * (1 + Math.cos(Math.PI * t))) / 2;
}

// 明度・彩度・色相の1つに操作をかける。factor は spread のときの倍率
function applyOp(v, op, factor = 1) {
  if (!op) return v;
  let out = v;
  if (op.set != null) out = op.set;
  if (op.add != null) out = out + op.add * (op.spread ? factor : 1);
  if (op.scale != null) out = out * op.scale;
  if (op.max != null) out = Math.min(out, op.max);
  return out;
}

// 色のルールを1つ評価する。resolve は役割名から色を返す。spread は色相のずらし幅の設定
export function evalColor(expr, resolve, spread = null) {
  if (typeof expr === 'string') return isRef(expr) ? resolve(refName(expr)) : parseHex(expr);
  let c;
  if (expr.from != null) c = evalColor(expr.from, resolve, spread);
  else if (expr.mix) c = mix(evalColor(expr.mix[0], resolve, spread), evalColor(expr.mix[1], resolve, spread), expr.mix[2]);
  else if (expr.over) c = over(evalColor(expr.over[0], resolve, spread), evalColor(expr.over[1], resolve, spread));
  else throw new Error(`色のルールに from / mix / over がない: ${JSON.stringify(expr)}`);
  if (expr.oklch) {
    const o = toOklch(c);
    const f = spreadFactor(o.h, spread);
    c = fromOklch(applyOp(o.l, expr.oklch.l), applyOp(o.c, expr.oklch.c), applyOp(o.h, expr.oklch.h, f), c.a);
  }
  if (expr.alpha != null) c = { ...c, a: c.a * expr.alpha };
  if (expr.contrast) {
    const layers = expr.contrast.against.map((e) => evalColor(e, resolve, spread));
    const bg = layers.reduceRight((below, top) => over(top, below));
    c = ensureContrast(quantize(c), quantize(bg), expr.contrast.min);
  }
  return c;
}

// シードから枠の色の並びを作る
export function evalEdgeStops(def, resolve, spread = null) {
  const base = toOklch(evalColor(def.from, resolve));
  const chroma = Math.min(base.c, def.chromaCap);
  const f = def.spread ? spreadFactor(base.h, spread) : 1;
  const stops = def.stops.map((s) => ({ angle: s.angle, color: quantize(fromOklch(s.l, s.c * chroma, base.h + s.h * f, 1)) }));
  return [...stops, { angle: 360, color: stops[0].color }];
}

// 手で決めた枠の色の並び(最後に最初の色で一周を閉じる)
export function handEdgeStops(edge) {
  const stops = edge.map((s) => ({ angle: s.angle, color: parseHex(s.color) }));
  return [...stops, { angle: 360, color: stops[0].color }];
}

// semantic.json と入力から全roleを作る
export function generateRoles(semantic, { seed, overrides = {}, edge = null, toneUsesEdge = false } = {}) {
  const defs = semantic.color;
  const spread = semantic.hueSpread?.$value ?? null;
  const cache = new Map();
  const visiting = new Set();
  const given = { ...overrides };
  if (seed) given.seed = seed;

  // 役割を1つ解決する(結果は8bitに丸める)
  function resolve(name) {
    if (cache.has(name)) return cache.get(name);
    if (!defs[name] || defs[name].$type !== 'color') throw new Error(`色の役割がない: ${name}`);
    if (visiting.has(name)) throw new Error(`色の参照が循環している: ${name}`);
    visiting.add(name);
    const value = given[name] != null ? parseHex(given[name]) : evalColor(defs[name].$value, resolve, spread);
    const c = quantize(value);
    visiting.delete(name);
    cache.set(name, c);
    return c;
  }

  const roles = {};
  for (const [name, def] of Object.entries(defs)) {
    if (def.$type === 'color') roles[name] = resolve(name);
  }
  const edgeStops = edge ? handEdgeStops(edge) : evalEdgeStops(defs.edgeStops.$value, resolve, spread);
  return { roles, edgeStops, toneEdge: toneUsesEdge ? edgeStops : null, resolve, spread };
}

// 結果を色コードの表にする
export function rolesToHex({ roles, edgeStops }) {
  const out = {};
  for (const [k, v] of Object.entries(roles)) out[k] = toHex(v);
  out.edgeStops = edgeStops.map((s) => ({ angle: s.angle, color: toHex(s.color) }));
  return out;
}
