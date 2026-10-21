# Glass スタイルの分解メモ

参照実装(Glass の元にした、作者の非公開の Web アプリ)の `tokens.css` と `glass.css` から抜き出した値と、移した先。

## ガラス感の3層

| 層 | 中身 | トークン |
|---|---|---|
| 塗り | 上から下へ、白 .62 → 淡いシード色 .5 | `color.fillTop` / `color.fillBottom` |
| 内側の影 | シード色 .14 のぼかし + 左上の白いハイライト | `glass.shadow.idle`(`color.innerGlow` / `color.innerHighlight`) |
| 枠 | 1px のスイープグラデーション。青 → 水色 → ミント → 青 | `color.edgeStops`、`glass.edge.width` |
| 線状の影 | 枠と同じ形を (1, 2) ずらして 1px ぼかす | `glass.ring.*`、`color.ringShadow` |

背景ぼかし(`backdrop-filter`)は `glass.backdrop.*` に値だけ残す。なくても成立する見た目を既定にする。

## 枠の色の作り方

元は色コード直書きの `conic-gradient`(13色)。シードを変えても青のまま残るので、シードからの差で持つ形に直した。

- 各点を OKLCH で測り、**明度は絶対値、色相はシードからの差、彩度はシードの彩度に対する比** で記録した
- 彩度は `chromaCap`(元のシードの彩度 .1226)で頭打ちにする。彩度の高いシードでも枠が派手になりすぎないため
- 既定シードでは、13色すべてが元の色コードと一致する(テストで確認)

| 角度 | 元の色 | 明度 | 彩度比 | 色相差 |
|---|---|---|---|---|
| 0 | `#84a8ea` | .731 | .851 | +27.5 |
| 35 | `#93b3ec` | .764 | .731 | +27.1 |
| 70 | `#7fc4f0` | .791 | .765 | +3.0 |
| 100 | `#8fbdf0` | .785 | .718 | +16.9 |
| 125 | `#7fd8d0` | .824 | .708 | -46.4 |
| 150 | `#a3e2e2` | .871 | .518 | -38.6 |
| 175 | `#78cfc4` | .797 | .702 | -49.4 |
| 200 | `#7fa6e4` | .721 | .820 | +24.8 |
| 230 | `#5f8fdc` | .650 | 1.037 | +24.9 |
| 260 | `#7396e2` | .679 | .976 | +30.0 |
| 290 | `#8cb6ee` | .767 | .756 | +21.2 |
| 315 | `#9ad6f0` | .844 | .580 | -7.9 |
| 340 | `#7fa8ea` | .728 | .869 | +25.1 |

色相差は -49 〜 +30 度。当初の目安(-25 〜 +30)より緑側に広いが、参照実装の値を優先した。

### シードの色相が Ocean から離れたとき

この色相差は Ocean の青のために決めた値なので、他の色にそのまま当てると系統の違う色に入る(Amber は黄緑とサーモン、Violet はマゼンタと水色、Mint は黄緑)。そこで色相差に倍率を掛ける(`hueSpread`)。

| シードの色相と Ocean(234.6)の差 | 倍率 |
|---|---|
| 15 度以内 | 1(Ocean は元のまま) |
| 15〜75 度 | 1 から .3 へなめらかに下がる(cos) |
| 75 度以上 | .3 |

同じ理由で、`primaryStrong`、文字色、面の色の色相差にも同じ倍率を掛ける。

### ゴールド

ゴールドは色相の違いではなく、明暗の反射で金属らしさを出すため、枠の13色を手で決めた(`presets.json` の `edge`)。黄色寄りの金(色相 約 85〜100)で、暗い部分(`#A4802B`)と白に近いハイライト(`#FBF8E1`)を交互に置き、左上と右下が光るようにした。茶色に寄らないよう、暗い部分も明度 .62 以上に保つ。

### シャボン玉(Bubble)

シャボン玉の見本画像から測った色。淡い青〜ラベンダーが中心で、左上にピンク寄りのライラック、右上に水色、右下にミントが少し入る。シードは見本のプラスの色 `#71ADDA`。選んだもの・主な操作の枠にも同じ並びを使う。

## 部品ごとの見た目(面の役割)

部品は面の役割(`SurfaceRole`)だけを渡し、Glass が役割ごとに組み立てる(`GlassStyle.kt`)。

| 役割 | 塗り | 内側の影・外の影 | 枠 | 線状の影 | 使う部品 |
|---|---|---|---|---|---|
| `Button`(tone Default) | 上→下 `fillTop` → `fillBottom`(hover で `glass.fill.hover*`) | `glass.shadow.idle` / `hover` | スイープ(`edgeStops`) | `glass.ring.*` | `PiyoButton`、`PiyoIconButton` |
| `Button`(Primary / Danger) | `glass.tone.*` の単色 | 同上 | tone ごとのスイープ(`glass.tone.edge`) | 同上 | 同上 |
| `Chip`(未選択) | シード色を `chip.tintRest` / `tintHover` だけ + 白い光沢 | `chip.shadowRest` / `shadowHover` | 斜めの直線グラデーション(100deg、端は `edgeSideRatio`) | なし | `PiyoChip` |
| `Chip`(選択) | `fillSelected*` の濃さ + 光沢 | `glass.shadow.idle` | Primary のスイープ | `chip.ring*` | 同上 |
| `TabTrack` | `tabs.trackFill`(極薄) | なし | スイープを不透明度 .3 | なし | `PiyoTabs` の土台、ひょうたん |
| `TabThumb` | 通常ボタンと選んだチップの中間(`tabs.thumbFill`) | `tabs.thumbShadow`(立体的な内側の影) | 通常と Primary のスイープを半々 | `tabs.ring*`(枠の上に重ねる) | タブのつまみ、ひょうたんの印 |
| `Card` | 160deg の斜めグラデーション(`card.fill`) | なし | スイープを不透明度 .7 | .5px(`card.ring*`) | `PiyoCard`、ひょうたんとパネル、ジニーの面 |
| `Field` | `Button` と同じ(仮) | | | | 入力欄を作るときに決める |

## なぜこの見た目か

- **ガラス感は背景ぼかしではなく3層で出す。** 白から淡いシード色への半透明の塗り、角度で色が変わる細い枠、ずらしてぼかした線状の影。背景ぼかしは端末や描画の負担が大きく、背景によって見え方が変わるので、既定にしない
- **枠を角度で色が変わるスイープにする。** 光が当たる向きで縁の色が変わるガラスの感じを出す。単色の枠より輪郭が軽く見える
- **線状の影を右下にずらす。** 面を少し浮かせ、左上から光が当たっている向きを揃える。影を面全体ではなく線にするのは、半透明の塗りの下に影が透けて濁らないようにするため
- **選んだものは塗りの濃さと線状の影で示す。** 色を変えるだけより、押し込まれた・浮いた、の違いで分かるようにする

## Android での描き方

| 層 | 描き方 | 理由 |
|---|---|---|
| 外の影 | 少しずつ広げた形を薄く3枚重ね、形の外だけに描く | Compose の `dropShadow` は半透明の塗りの下に透ける。`BlurMaskFilter` は形が変わるたびに CPU で作り直して重い |
| 塗り | 形を `Brush` で塗る(色の混ぜ方は CSS と同じ sRGB の `mixSrgb`) | Compose の `lerp` は Oklab で混ぜるので、CSS の `color-mix` と色がずれる |
| 内側の影 | Compose の `innerShadow`。毎コマ形が変わる面(`PiyoChangingShape`)だけ、縁から薄れるグラデーションの近似 | 形が変わるたびにぼかしを作り直すと重い |
| 線状の影 | 太さと濃さの違う線を3本重ねる | 外の影と同じ |
| 枠 | スイープの色を面の大きさの画像に一度だけ描き(上下で継ぎ目の位置を変えて、継ぎ目の画素の抜けを防ぐ)、その画像を色にして線を描く | 14色のスイープで毎コマ大きな線を描くと重い。`docs/effects/README.md` |

## 元の変数と移した先

| 元の変数 | 移した先 | 備考 |
|---|---|---|
| `--color-primary` / `--color-primary-strong` | `color.primary` / `color.primaryStrong` | strong はシードから OKLCH で作る |
| `--color-surface` / `--color-surface-muted` / `--color-bg` | `color.surface` / `color.surfaceMuted` / `color.background` | |
| `--color-text` / `--color-text-muted` | `color.contentDefault` / `color.contentMuted` | |
| `--color-on-primary` | `color.contentOnPrimary` | |
| `--color-danger` / `-strong`、`--color-ok`、`--color-warn` | `color.danger` / `dangerStrong` / `ok` / `warn` | シードとは独立 |
| `--glass-fill` | `color.fillTop` / `color.fillBottom` | |
| `--glass-fill-hover` | `glass.fill.hoverTop` / `hoverBottom` | |
| `--glass-edge` / `--glass-edge-width` | `color.edgeStops` / `glass.edge.width` | |
| `--glass-shadow` / `-hover` | `glass.shadow.idle` / `hover` | |
| `--glass-ring-shadow*` | `color.ringShadow`、`glass.ring.*` | hover 時のずれは同じ値なので1つにまとめた |
| `--glass-primary-fill*` / `--glass-danger-fill*` | `glass.tone.*` | |
| `glass.css` の色付き枠(`--edge-tone` の conic) | `glass.tone.edge` | tone ごとに生成 |
| `--toggle-gloss` | `glass.chip.glossTop` / `glossBottom` | |
| `--toggle-tint-rest` / `-hover` | `glass.chip.tintRest` / `tintHover` | |
| `--toggle-tint-checked` | `color.fillSelectedTop` / `fillSelectedBottom` | 選択の塗りは role として持つ |
| `--toggle-edge-*`、枠の 100deg と .65 | `glass.chip.edgeRest` / `edgeHover` / `edgeAngle` / `edgeSideRatio` | |
| `--toggle-shadow*` | `glass.chip.shadowRest` / `shadowHover` | 選択時は `glass.shadow.idle` |
| `--toggle-ring-*` | `glass.chip.ring*` | |
| `--seg-*` | `glass.tabs.*` | 枠(`--seg-track-edge`、`--seg-thumb-edge`)は移さず、`edgeStops` に変えた(下記) |
| `.segmented` の `padding: 3px` と枠 1px | `tabs.trackPadding` / `tabs.trackBorder`(core) | 部品の配置に使うので共通側 |
| `--glass-card-*`、`--card-ring-*` | `glass.card.*` | |
| `--press-*` | `press.*`(core) | スタイルではなく共通の動き |
| `--radius-*` | `radius.*`(core) | |
| `--segmented-duration` | `duration.segmented`(core) | イージングは `easing.standard` |
| `--rail-*`、`--side-fillet`、`--rail-switch-duration` | `gourd.*`(effects) | |
| `--panel-toggle-duration`、ジニーのイージング、`NECK` | `genie.*`(effects) | |

## 移さなかったもの

| 元 | 理由 |
|---|---|
| `.chip`(状態ラベル)、`--glass-chip-fill: #b7d6e41a` | 選ぶボタンとは別の部品。色が直書き。部品を作るときに決める |
| `--menu-*`、`--shadow-card` など一般の影 | Glass の部品で使っていない。必要になってから足す |
| 地図、ルート、比較、優先の印などの色 | アプリ固有 |
| `--topbar-*`、`--panel-width-*` など配置の値 | アプリ固有 |
| `.print-choice` の `--toggle-border: 1.5px` | 印刷画面だけの例外 |

## 参照実装との違い

| 違い | 理由 |
|---|---|
| 既定シードの `contentSelected` が `#1d7bbf` → `#1073b7` | 選択の塗りの上で 4.04:1 しかなく、AA(4.5:1)を満たすよう自動で暗くした |
| 既定シードの `contentMuted` が `#6b7486` → `#697284` | 同上(塗りの上で 4.38:1) |
| タブの枠が、主な色の直線グラデーションから、ボタンと同じスイープグラデーションに | 参照実装ではほぼ1色に見え、他の部品と比べて色の変化が乏しい。このライブラリでは揃える。土台は不透明度 .3 |
| タブのつまみの塗りと枠が、通常ボタンと選んだチップの中間に | 選んだ印として通常ボタンより目立たせつつ、チップほど強くしないため |

## 青紫の例

青紫系のシードを想定した見本画像の、円周の色を測った結果。

| 角度 | 0 | 60 | 120 | 180 | 240 | 300 |
|---|---|---|---|---|---|---|
| 例の色 | `#aba7d0` | `#96b1e8` | `#99d0e8` | `#90aee2` | `#a5a3cb` | `#e2b5e3` |
| 色相 | 289 | 264 | 227 | 261 | 287 | 326 |

- 色相の範囲(約 220〜325)は、今の表を青紫のシード(色相 約 275)に当てはめた範囲とほぼ同じ
- ただし並びが違う。例では 270〜330 度にピンクがあり、今の表ではその位置は水色(色相差 -8)。彩度も例のほうが低い(.06〜.09)
- 今の生成方式(1つの表の色相を回す)では、この並びは出せない。プリセットで枠の色を上書きするか、表を色相ごとに変えるかが要る(未決定)
