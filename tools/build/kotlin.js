// トークンを Kotlin のコードにする

import { flatten } from '../lib/tokens.js';
import { generateRoles, evalColor, isRef, refName } from '../generate-colors/rules.js';
import { toHex, mix } from '../generate-colors/color.js';

const HEADER = '// 自動生成。手で書き換えない。元は tokens/(npm run tokens で作り直す)';

// 先頭を大文字にする
function pascal(s) {
  return s[0].toUpperCase() + s.slice(1);
}

// 道筋を camelCase の1語にする
function camel(path) {
  return path.map((p, i) => (i === 0 ? p : pascal(p))).join('');
}

// Double の書き方にする
function dbl(n) {
  const s = String(n);
  return /[.e]/.test(s) ? s : `${s}.0`;
}

// Float の書き方にする
function flt(n) {
  return `${n}f`;
}

// Dp の書き方にする
function dp(n) {
  return `${n}.dp`;
}

// 色コードを ARGB の Long にする
function argbLiteral(hex) {
  const h = hex.replace('#', '').toUpperCase();
  const [rgb, a] = h.length === 8 ? [h.slice(0, 6), h.slice(6)] : [h, 'FF'];
  return `0x${a}${rgb}`;
}

// ルールが参照する役割を集める
function collectRefs(expr, out = new Set()) {
  if (typeof expr === 'string') { if (isRef(expr)) out.add(refName(expr)); return out; }
  if (Array.isArray(expr)) { expr.forEach((e) => collectRefs(e, out)); return out; }
  if (expr && typeof expr === 'object') Object.values(expr).forEach((v) => collectRefs(v, out));
  return out;
}

// 色のルールを Kotlin の式にする
function colorExpr(expr, ident) {
  if (typeof expr === 'string') return isRef(expr) ? ident(refName(expr)) : `Rgba.argb(${argbLiteral(expr)})`;
  let k;
  if (expr.from != null) k = colorExpr(expr.from, ident);
  else if (expr.mix) k = `mix(${colorExpr(expr.mix[0], ident)}, ${colorExpr(expr.mix[1], ident)}, ${dbl(expr.mix[2])})`;
  else if (expr.over) k = `over(${colorExpr(expr.over[0], ident)}, ${colorExpr(expr.over[1], ident)})`;
  if (expr.oklch) {
    const ops = ['l', 'c', 'h'].filter((ch) => expr.oklch[ch])
      .map((ch) => `${ch} = ChannelOp(${Object.entries(expr.oklch[ch]).map(([op, v]) => `${op} = ${typeof v === 'boolean' ? v : dbl(v)}`).join(', ')})`);
    const spread = ['l', 'c', 'h'].some((ch) => expr.oklch[ch]?.spread) ? ', spread = hueSpread' : '';
    k = `${k}.adjustOklch(${ops.join(', ')}${spread})`;
  }
  if (expr.alpha != null) k = `${k}.alpha(${dbl(expr.alpha)})`;
  if (expr.contrast) {
    const layers = expr.contrast.against.map((e) => colorExpr(e, ident));
    const bg = layers.reduceRight((below, top) => `over(${top}, ${below})`);
    k = `ensureContrast(${k}.quantize(), ${bg}.quantize(), ${dbl(expr.contrast.min)})`;
  }
  return k;
}

// 色以外の値を Kotlin の式にする
function valueExpr(token) {
  const v = token.$value;
  switch (token.$type) {
    case 'dimension': return dp(v);
    case 'duration': return String(v);
    case 'number': return flt(v);
    case 'fontSize': return `${v}.sp`;
    case 'fontWeight': return `FontWeight(${v})`;
    case 'cubicBezier': return `CubicBezierEasing(${v.map(flt).join(', ')})`;
    case 'fontFamily': return `listOf(${v.map((f) => JSON.stringify(f)).join(', ')})`;
    default: throw new Error(`Kotlin の定数にできない型: ${token.$type}`);
  }
}

// 入れ子の object を書く
function objectTree(entries, indent = '    ') {
  const groups = new Map();
  const lines = [];
  for (const [path, token] of entries) {
    if (path.length === 1) {
      if (token.$description) lines.push(`${indent}// ${token.$description}`);
      lines.push(`${indent}val ${pascal(path[0])} = ${valueExpr(token)}`);
    } else {
      if (!groups.has(path[0])) groups.set(path[0], []);
      groups.get(path[0]).push([path.slice(1), token]);
    }
  }
  for (const [name, sub] of groups) {
    lines.push('', `${indent}object ${pascal(name)} {`, ...objectTree(sub, indent + '    '), `${indent}}`);
  }
  return lines;
}

// 色に関わる型か
function isColorish(type) {
  return ['color', 'shadow', 'gradient', 'toneStops'].includes(type);
}

// 共通トークンとGlassの定数のファイルを作る
function tokensFile(t, pkg) {
  const core = [...flatten(t.primitives), ...flatten(t.effects)];
  const glass = flatten(t.styles.glass.glass).filter(([, tok]) => !isColorish(tok.$type));
  return [
    HEADER,
    `package ${pkg}`,
    '',
    'import androidx.compose.animation.core.CubicBezierEasing',
    'import androidx.compose.ui.text.font.FontWeight',
    'import androidx.compose.ui.unit.dp',
    'import androidx.compose.ui.unit.sp',
    '',
    '// 全スタイル共通の値(寸法は dp、文字は sp、時間はミリ秒)',
    'object PiyoTokens {',
    ...objectTree(core).slice(1),
    '}',
    '',
    '// Glass スタイルの、色によらない値',
    'object GlassTokens {',
    ...objectTree(glass).slice(1),
    '}',
    '',
  ].join('\n');
}

// role を参照順に並べる
function roleOrder(defs) {
  const order = [];
  const seen = new Set();
  // 1つの role と、その参照先を並べる
  function visit(name) {
    if (seen.has(name)) return;
    seen.add(name);
    collectRefs(defs[name].$value).forEach(visit);
    order.push(name);
  }
  Object.keys(defs).filter((k) => defs[k].$type === 'color').forEach(visit);
  return order;
}

// 色の役割と生成関数のファイルを作る
function rolesFile(t, pkg) {
  const defs = t.semantic.color;
  const names = Object.keys(defs).filter((k) => defs[k].$type === 'color');
  // role 名を Kotlin の変数名にする
  const local = (n) => (n === 'seed' ? 'seedRole' : n);
  const edge = defs.edgeStops.$value;
  const body = roleOrder(defs).map((n) => {
    const expr = n === 'seed' ? 'Rgba.of(seed)' : colorExpr(defs[n].$value, local);
    return `    val ${local(n)} = o["${n}"] ?: ${expr}.quantize()`;
  });
  const stops = edge.stops.map((s) => `        EdgeStop(${dbl(s.angle)}, ${dbl(s.l)}, ${dbl(s.c)}, ${dbl(s.h)}),`);
  return [
    HEADER,
    `package ${pkg}`,
    '',
    'import androidx.compose.ui.graphics.Color',
    'import piyoron.core.color.ChannelOp',
    'import piyoron.core.color.EdgeStop',
    'import piyoron.core.color.HueSpread',
    'import piyoron.core.color.Rgba',
    'import piyoron.core.color.edgeStops',
    'import piyoron.core.color.ensureContrast',
    'import piyoron.core.color.mix',
    'import piyoron.core.color.over',
    '',
    `// ${t.semantic.hueSpread.$description}`,
    `private val hueSpread = HueSpread(${['center', 'plateau', 'width', 'min'].map((k) => dbl(t.semantic.hueSpread.$value[k])).join(', ')})`,
    '',
    '// 色の役割(全スタイル共通の語彙)',
    'data class PiyoColorRoles(',
    ...names.flatMap((n) => [...(defs[n].$description ? [`    // ${defs[n].$description}`] : []), `    val ${n}: Color,`]),
    `    // ${defs.edgeStops.$description}。位置は 0〜1(CSS の conic-gradient と同じく上が 0)`,
    '    val edgeStops: List<Pair<Float, Color>>,',
    '    // 色付きの枠(選んだもの、主な操作)にも枠の色の並びを使うとき、その並び',
    '    val toneEdge: List<Pair<Float, Color>>? = null,',
    ')',
    '',
    '// シード色から全roleを作る。danger / ok / warn は任意で上書きできる',
    'fun piyoColorRoles(seed: Color, danger: Color? = null, ok: Color? = null, warn: Color? = null): PiyoColorRoles =',
    '    generateRoles(seed, listOfNotNull(danger?.let { "danger" to it }, ok?.let { "ok" to it }, warn?.let { "warn" to it }).toMap())',
    '',
    '// シード色と、role 名ごとの上書き、手で決めた枠の色から全roleを作る',
    'internal fun generateRoles(',
    '    seed: Color,',
    '    overrides: Map<String, Color> = emptyMap(),',
    '    edge: List<Pair<Float, Color>>? = null,',
    '    toneUsesEdge: Boolean = false,',
    '): PiyoColorRoles {',
    '    val o = overrides.mapValues { Rgba.of(it.value) }',
    ...body,
    `    val generated = edgeStops(seedRole, ${dbl(edge.chromaCap)}, ${edge.spread ? 'hueSpread' : 'null'}, listOf(`,
    ...stops,
    '    ))',
    '    val stops = edge?.let { it + (1f to it.first().second) } ?: generated.map { (it.first / 360).toFloat() to it.second.toColor() }',
    '    return PiyoColorRoles(',
    ...names.map((n) => `        ${n} = ${local(n)}.toColor(),`),
    '        edgeStops = stops,',
    '        toneEdge = if (toneUsesEdge) stops else null,',
    '    )',
    '}',
    '',
  ].join('\n');
}

// 手で調整した色の組み合わせのファイルを作る
function presetsFile(t, pkg) {
  const entries = Object.entries(t.presets).map(([name, p]) => {
    const o = Object.entries(p.overrides).map(([k, v]) => `"${k}" to Color(${argbLiteral(v)})`);
    const e = p.edge ? `listOf(${p.edge.map((s) => `${flt(s.angle / 360)} to Color(${argbLiteral(s.color)})`).join(', ')})` : 'null';
    return `    ${pascal(name)}(Color(${argbLiteral(p.seed)}), mapOf(${o.join(', ')}), ${e}, ${!!p.toneUsesEdge}),`;
  });
  return [
    HEADER,
    `package ${pkg}`,
    '',
    'import androidx.compose.ui.graphics.Color',
    '',
    '// 手で調整した色の組み合わせ',
    'enum class PiyoPreset(',
    '    val seed: Color,',
    '    internal val overrides: Map<String, Color>,',
    '    internal val edge: List<Pair<Float, Color>>?,',
    '    internal val toneUsesEdge: Boolean,',
    ') {',
    ...entries.map((e, i) => (i === entries.length - 1 ? e.replace(/,$/, ';') : e)),
    '',
    '    // このプリセットの全roleを作る',
    '    fun roles(): PiyoColorRoles = generateRoles(seed, overrides, edge, toneUsesEdge)',
    '}',
    '',
  ].join('\n');
}

// Glass の色つきの値の、型と Kotlin の式
function glassField(path, token, ident) {
  const name = camel(path);
  // 色のルールを Color の式にする
  const c = (e) => `${colorExpr(e, ident)}.toColor()`;
  switch (token.$type) {
    case 'color':
      return [[name, 'Color', c(token.$value)]];
    case 'shadow':
      return [[name, 'List<PiyoShadow>', `listOf(${token.$value.map((s) =>
        `PiyoShadow(${c(s.color)}, ${dp(s.x)}, ${dp(s.y)}, ${dp(s.blur)}, ${dp(s.spread)}, ${s.inset})`).join(', ')})`]];
    case 'gradient':
      return [[name, 'PiyoGradient', `PiyoGradient(${flt(token.$value.angle)}, listOf(${token.$value.stops.map((s) => `${flt(s.at)} to ${c(s.color)}`).join(', ')}))`]];
    case 'toneStops':
      return ['primary', 'danger'].map((tone) => [name + pascal(tone), 'List<Pair<Float, Color>>',
        `${tone === 'primary' ? 'roles.toneEdge ?: ' : ''}listOf(${token.$value.map((s) => `${flt(s.angle / 360)} to mix(${ident(tone)}, ${ident('surface')}, ${dbl(s.amount)}).toColor()`).join(', ')})`]);
    default:
      return [];
  }
}

// Glass の色・影・グラデーションのファイルを作る
function glassColorsFile(t, pkg) {
  const tokens = flatten(t.styles.glass.glass).filter(([, tok]) => isColorish(tok.$type));
  const used = new Set(['primary', 'danger', 'surface']);
  tokens.forEach(([, tok]) => collectRefs(tok.$value, used));
  const fields = tokens.flatMap(([path, tok]) => glassField(path, tok, (n) => n));
  return [
    HEADER,
    `package ${pkg}`,
    '',
    'import androidx.compose.ui.graphics.Color',
    'import androidx.compose.ui.unit.dp',
    'import piyoron.core.PiyoGradient',
    'import piyoron.core.PiyoShadow',
    'import piyoron.core.color.Rgba',
    'import piyoron.core.color.mix',
    '',
    '// Glass スタイルの、色によって変わる値',
    'data class GlassColors(',
    ...fields.map(([n, type]) => `    val ${n}: ${type},`),
    ')',
    '',
    '// role から Glass の色・影・グラデーションを作る',
    'fun glassColors(roles: PiyoColorRoles): GlassColors {',
    ...[...used].sort().map((n) => `    val ${n} = Rgba.of(roles.${n})`),
    '    return GlassColors(',
    ...fields.map(([n, , e]) => `        ${n} = ${e},`),
    '    )',
    '}',
    '',
  ].join('\n');
}

// テストで比べる色を、名前と色コードの並びにする
export function glassExpected(t, resolve, toneEdge = null) {
  const out = [];
  // 色のルールを色コードにする
  const hex = (e) => toHex(evalColor(e, resolve));
  for (const [path, tok] of flatten(t.styles.glass.glass)) {
    const name = camel(path);
    if (tok.$type === 'color') out.push([name, hex(tok.$value)]);
    if (tok.$type === 'shadow') tok.$value.forEach((s, i) => out.push([`${name}.${i}`, hex(s.color)]));
    if (tok.$type === 'gradient') tok.$value.stops.forEach((s, i) => out.push([`${name}.${i}`, hex(s.color)]));
    if (tok.$type === 'toneStops') {
      for (const tone of ['primary', 'danger']) {
        if (tone === 'primary' && toneEdge) toneEdge.forEach((s, i) => out.push([`${name}Primary.${i}`, toHex(s.color)]));
        else tok.$value.forEach((s, i) => out.push([`${name}${pascal(tone)}.${i}`, toHex(mix(resolve(tone), resolve('surface'), s.amount))]));
      }
    }
  }
  return out;
}

// テストで色を取り出す式を作る
function glassGetters(t) {
  const out = [];
  for (const [path, tok] of flatten(t.styles.glass.glass)) {
    const name = camel(path);
    if (tok.$type === 'color') out.push([name, `it.${name}`]);
    if (tok.$type === 'shadow') tok.$value.forEach((_, i) => out.push([`${name}.${i}`, `it.${name}[${i}].color`]));
    if (tok.$type === 'gradient') tok.$value.stops.forEach((_, i) => out.push([`${name}.${i}`, `it.${name}.stops[${i}].second`]));
    if (tok.$type === 'toneStops') {
      for (const tone of ['Primary', 'Danger']) tok.$value.forEach((_, i) => out.push([`${name}${tone}.${i}`, `it.${name}${tone}[${i}].second`]));
    }
  }
  return out;
}

// テスト用の期待値のファイルを作る(JS の生成結果を Kotlin で再現できるか比べる)
function fixturesFile(t, pkg, cases) {
  const defs = t.semantic.color;
  const roleNames = Object.keys(defs).filter((k) => defs[k].$type === 'color');
  const edgeCount = defs.edgeStops.$value.stops.length + 1;
  const roleGet = [
    ...roleNames.map((n) => [n, `it.${n}`]),
    ...Array.from({ length: edgeCount }, (_, i) => [`edgeStops.${i}`, `it.edgeStops[${i}].second`]),
  ];
  const caseLines = cases.map((cs) => {
    const { roles, edgeStops, toneEdge, resolve } = generateRoles(t.semantic, cs);
    const r = [...roleNames.map((n) => [n, toHex(roles[n])]), ...edgeStops.map((s, i) => [`edgeStops.${i}`, toHex(s.color)])];
    const g = glassExpected(t, resolve, toneEdge);
    // 名前と色コードの組を mapOf にする
    const map = (pairs) => `mapOf(${pairs.map(([k, v]) => `"${k}" to ${argbLiteral(v)}`).join(', ')})`;
    const o = Object.entries(cs.overrides ?? {}).map(([k, v]) => `"${k}" to ${argbLiteral(v)}`);
    const e = cs.edge ? `listOf(${cs.edge.map((s) => `${flt(s.angle / 360)} to ${argbLiteral(s.color)}`).join(', ')})` : 'null';
    return `        ColorCase(${argbLiteral(cs.seed)}, mapOf(${o.join(', ')}), ${e}, ${!!cs.toneUsesEdge}, ${map(r)}, ${map(g)}),`;
  });
  return [
    HEADER,
    `package ${pkg}`,
    '',
    'import androidx.compose.ui.graphics.Color',
    '',
    '// 1つの入力と、JS で作った期待値',
    'internal data class ColorCase(',
    '    val seed: Long,',
    '    val overrides: Map<String, Long>,',
    '    val edge: List<Pair<Float, Long>>?,',
    '    val toneUsesEdge: Boolean,',
    '    val roles: Map<String, Long>,',
    '    val glass: Map<String, Long>,',
    ')',
    '',
    '// 名前つきで色を取り出す',
    'internal class Getter<T>(val name: String, val get: (T) -> Color)',
    '',
    '// 色生成の期待値',
    'internal object ColorFixtures {',
    '    val cases = listOf(',
    ...caseLines,
    '    )',
    '',
    '    val roles = listOf<Getter<PiyoColorRoles>>(',
    ...roleGet.map(([n, e]) => `        Getter("${n}") { ${e} },`),
    '    )',
    '',
    '    val glass = listOf<Getter<GlassColors>>(',
    ...glassGetters(t).map(([n, e]) => `        Getter("${n}") { ${e} },`),
    '    )',
    '}',
    '',
  ].join('\n');
}

// Kotlin のファイル一式を作る。戻り値は { 相対パス: 中身 }
export function buildKotlin(t, cases) {
  const pkg = t.config.kotlinPackage;
  const dir = pkg.replace(/\./g, '/');
  const main = `android/piyoron/src/main/kotlin/${dir}`;
  const test = `android/piyoron/src/test/kotlin/${dir}`;
  return {
    [`${main}/PiyoTokens.kt`]: tokensFile(t, pkg),
    [`${main}/PiyoColorRoles.kt`]: rolesFile(t, pkg),
    [`${main}/PiyoPreset.kt`]: presetsFile(t, pkg),
    [`${main}/GlassColors.kt`]: glassColorsFile(t, pkg),
    [`${test}/ColorFixtures.kt`]: fixturesFile(t, pkg, cases),
  };
}
