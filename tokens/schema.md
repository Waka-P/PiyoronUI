# トークンの書き方

見た目の元データ。CSS と Kotlin はここから自動生成する(`npm run tokens`)。

## ファイル

| ファイル | 中身 | 参照してよい先 |
|---|---|---|
| `core/primitives.json` | 角丸、余白、時間、イージング、フォント、文字、押下の動き、タッチ領域、部品の寸法 | なし |
| `core/semantic.json` | 色の役割(role)と生成ルール | role 同士 |
| `core/presets.json` | 手で調整した色の組み合わせ。`overrides`(role の上書き)、`edge`(枠の色の並びの上書き)、`toneUsesEdge`(色付きの枠にも枠の並びを使う) | なし |
| `effects/*.json` | ひょうたん、ジニー(スタイルから独立) | なし |
| `styles/<name>/*.json` | スタイル専用の値 | semantic の role |
| `../piyoron.config.json` | 名前、接頭辞、Kotlin のパッケージ | - |

## 形式

DTCG(Design Tokens Community Group)形式に寄せる。`$value` を持つものがトークン、それ以外はグループ。

```json
"radius": { "md": { "$type": "dimension", "$value": 12 } }
```

| `$type` | 値 | CSS | Kotlin |
|---|---|---|---|
| `dimension` | 数(px / dp) | `12px` | `12.dp` |
| `duration` | 数(ミリ秒) | `180ms` | `180` |
| `number` | 数 | `1.04` | `1.04f` |
| `fontSize` | 数(px / sp) | `14px` | `14.sp` |
| `fontWeight` | 数(100〜900) | `500` | `FontWeight(500)` |
| `cubicBezier` | `[x1, y1, x2, y2]` | `cubic-bezier(...)` | `CubicBezierEasing(...)` |
| `fontFamily` | 名前の配列 | フォント指定 | `List<String>` |
| `color` | 色のルール(下記) | 色、`color-mix()` | `Color`(実行時に生成) |
| `colorStops` | 枠の色の並び(semantic の `edgeStops` のみ) | `--piyo-color-edge-N` | `List<Pair<Float, Color>>` |
| `hueSpread` | `{ center, plateau, width, min }`。色相のずらし幅の倍率 | - | `HueSpread` |
| `shadow` | 影の層の配列 `{ color, x, y, blur, spread, inset }` | `box-shadow` | `List<PiyoShadow>` |
| `gradient` | `{ angle, stops: [{ at, color }] }` | `linear-gradient()` | `PiyoGradient` |
| `toneStops` | `[{ angle, amount }]`。tone 色と surface の混色 | tone ごとの `conic-gradient()` | tone ごとの `List<Pair<Float, Color>>` |

## 色のルール

| 書き方 | 意味 |
|---|---|
| `"#2293C7"` | 色コード |
| `"{color.primary}"` | role の参照 |
| `{ "from": X }` | X を元にする |
| `{ "mix": [A, B, p] }` | `color-mix(in srgb, A p, B)` と同じ |
| `{ "over": [上, 下] }` | 重ねた色 |
| `"oklch": { "l": op, "c": op, "h": op }` | OKLCH の明度・彩度・色相を動かす |
| `"alpha": k` | 不透明度に k を掛ける |
| `"contrast": { "against": [上, ..., 下], "min": 4.5 }` | 重ねた背景との比が足りるまで明度を動かす |

- `op` は `set`(置き換え)、`add`(足す)、`scale`(掛ける)、`max`(上限)。この順にかける
- `add` に `"spread": true` を付けると、足す量に `hueSpread` の倍率を掛ける(色相のずらし幅を、シードの色相に応じて縮める)
- 1つのルールの中では、元の色 → `oklch` → `alpha` → `contrast` の順にかける
- role の結果は 8bit に丸める
- **スタイルの色は `from` / `mix` / `alpha` だけ使える。** CSS の `color-mix()` で表せるようにして、パレットの切り替えを CSS 変数だけで済ませるため

## 名前

| 種類 | 規則 | 例 |
|---|---|---|
| JSON のキー | camelCase | `primaryStrong`、`hoverTop` |
| CSS 変数 | `--<接頭辞>-<道筋の kebab-case>` | `--piyo-color-primary-strong`、`--piyo-glass-fill-hover-top` |
| Kotlin(色以外) | `PiyoTokens` / `GlassTokens` の入れ子 object、PascalCase | `PiyoTokens.Radius.Md`、`GlassTokens.Ring.Width` |
| Kotlin(色) | role は `PiyoColorRoles`、スタイルは `GlassColors` の camelCase | `roles.primaryStrong`、`glass.fillHoverTop` |

## CSS の切り替え

| 属性 | 効果 |
|---|---|
| なし | 既定(Glass、Ocean) |
| `data-piyoron-palette="mint"` | その要素から下の色を差し替える |
| `data-piyoron-style="glass"` | スタイルの指定(今は Glass のみ) |

## piyoron.config.json

| キー | 用途 |
|---|---|
| `name` | 表示名 |
| `symbol` | コード上の記号の頭(`Piyo`) |
| `cssPrefix` | CSS 変数の接頭辞 |
| `dataAttribute` | `data-*` 属性の名前 |
| `kotlinPackage` | 生成する Kotlin のパッケージ |

## 手順

1. JSON を書き換える
2. `npm run tokens` で `web/tokens.css` と `android/piyoron/src/*/kotlin/piyoron/tokens/` を作り直す
3. `npm test`(JS)と `android/gradlew :piyoron:testDebugUnitTest`(Kotlin)を通す。生成物が古いと JS のテストが落ちる
