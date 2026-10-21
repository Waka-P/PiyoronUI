// トークンから CSS 変数と Kotlin のコードを作る
// 使い方: npm run tokens

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { loadTokens, root } from '../lib/tokens.js';
import { buildCss } from './css.js';
import { buildKotlin } from './kotlin.js';
import { fixtureCases } from './cases.js';

// ファイルを書き出す
function write(rel, text) {
  const path = join(root, rel);
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, text.replace(/\r?\n/g, '\n'), 'utf8');
  console.log(`書き出し: ${rel}`);
}

// 全部を作る
function main() {
  const t = loadTokens();
  write('web/tokens.css', buildCss(t));
  for (const [rel, text] of Object.entries(buildKotlin(t, fixtureCases(t)))) write(rel, text);
}

main();
