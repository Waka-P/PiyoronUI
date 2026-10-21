// トークンの読み込みと走査

import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

// JSON を1つ読む
export function readJson(...parts) {
  return JSON.parse(readFileSync(join(root, ...parts), 'utf8'));
}

// フォルダ内の JSON を1つの木にまとめる
function readDir(...parts) {
  const dir = join(root, ...parts);
  return readdirSync(dir).filter((f) => f.endsWith('.json')).sort()
    .reduce((acc, f) => ({ ...acc, ...readJson(...parts, f) }), {});
}

// 全トークンを読む
export function loadTokens() {
  return {
    config: readJson('piyoron.config.json'),
    primitives: readJson('tokens', 'core', 'primitives.json'),
    semantic: readJson('tokens', 'core', 'semantic.json'),
    presets: readJson('tokens', 'core', 'presets.json'),
    effects: readDir('tokens', 'effects'),
    styles: { glass: readDir('tokens', 'styles', 'glass') },
  };
}

// $value を持つ葉を [道筋, トークン] の並びにする
export function flatten(tree, path = []) {
  if (tree && typeof tree === 'object' && '$value' in tree) return [[path, tree]];
  return Object.entries(tree).filter(([k]) => !k.startsWith('$'))
    .flatMap(([k, v]) => flatten(v, [...path, k]));
}
