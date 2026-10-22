// テーマの色(シード色やプリセットから CSS 変数を書く)

import semantic from '../../tokens/core/semantic.json' with { type: 'json' };
import presets from '../../tokens/core/presets.json' with { type: 'json' };
import { generateRoles } from '../../tools/generate-colors/rules.js';
import { toHex } from '../../tools/generate-colors/color.js';

// CSS 変数の頭
const PREFIX = '--piyo';

// 役割の名前(primaryStrong)を CSS 変数の名前(--piyo-color-primary-strong)にする
function roleVar(name) {
  return `${PREFIX}-color-${name.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`;
}

// 書いた CSS 変数の名前(書き直すときに消すため)
const written = new WeakMap();

// 前に書いた CSS 変数を消す
function clear(element) {
  for (const name of written.get(element) ?? []) element.style.removeProperty(name);
  written.delete(element);
}

// 役割の色を CSS 変数に書く
function write(element, input) {
  clear(element);
  const { roles, edgeStops, toneEdge } = generateRoles(semantic, input);
  const names = [];
  // 1つ書く
  const set = (name, value) => {
    element.style.setProperty(name, value);
    names.push(name);
  };
  for (const [name, color] of Object.entries(roles)) set(roleVar(name), toHex(color));
  edgeStops.slice(0, -1).forEach((stop, i) => set(`${PREFIX}-color-edge-${i}`, toHex(stop.color)));
  if (toneEdge) {
    set(`${PREFIX}-glass-tone-edge-primary`, `var(${PREFIX}-glass-edge)`);
    set(`${PREFIX}-glass-tabs-thumb-edge`, `var(${PREFIX}-glass-edge)`);
  }
  written.set(element, names);
}

// シード色からテーマの色を作る(色コード。danger などは overrides で上書きできる)
export function setPiyoSeed(seed, { element = document.documentElement, overrides = {} } = {}) {
  delete element.dataset.piyoronPalette;
  write(element, { seed, overrides });
}

// 手で整えたプリセットにする(ocean、mint、rose、violet、amber、gold、bubble)
export function setPiyoPreset(name, { element = document.documentElement } = {}) {
  if (!presets[name]) throw new Error(`プリセットがない: ${name}`);
  clear(element);
  element.dataset.piyoronPalette = name;
}

// プリセットの名前の一覧
export const piyoPresets = Object.keys(presets);

// 色相(0〜360)からシード色を作る(カタログの色相の帯と同じ明るさと鮮やかさ)
export function seedFromHue(hue) {
  const s = 0.72;
  const v = 0.78;
  // HSV の1つの成分
  const f = (n) => {
    const k = (n + hue / 60) % 6;
    return v - v * s * Math.max(0, Math.min(k, 4 - k, 1));
  };
  return toHex({ r: f(5), g: f(3), b: f(1), a: 1 });
}
