// シード色から全roleを作って JSON で出す
// 使い方: node tools/generate-colors/index.js "#2293C7" / --preset mint

import { loadTokens } from '../lib/tokens.js';
import { generateRoles, rolesToHex } from './rules.js';

// 引数を読んで結果を出す
function main(args) {
  const { semantic, presets } = loadTokens();
  const i = args.indexOf('--preset');
  const input = i >= 0 ? presets[args[i + 1]] : { seed: args[0] ?? semantic.color.seed.$value, overrides: {} };
  if (!input) throw new Error(`プリセットがない: ${args[i + 1]}`);
  console.log(JSON.stringify(rolesToHex(generateRoles(semantic, input)), null, 2));
}

main(process.argv.slice(2));
