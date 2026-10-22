# 部品メモ

部品は振る舞い(状態、読み上げ、動き)だけを持ち、見た目は面の役割(`SurfaceRole`)を渡して現在のスタイルに描かせる。部品の中に色や寸法は書かず、トークン(`PiyoTokens`)とスタイルだけを見る。

| 部品 | メモ | 面の役割 |
|---|---|---|
| `PiyoButton`、`PiyoIconButton` | [button.md](button.md) | `Button` |
| `PiyoChip`、`PiyoChipGroup`、`PiyoMultiChipGroup` | [chip.md](chip.md) | `Chip` |
| `PiyoTabs` | [tabs.md](tabs.md) | `TabTrack`、`TabThumb` |
| `PiyoCard` | [card.md](card.md) | `Card` |
| `PiyoMenu`、`PiyoMenuGroup`、`PiyoMenuItem`、`PiyoMenuDivider` | [menu.md](menu.md) | `Menu`、`MenuItem`、`Divider` |
| `GourdTabRail`、`GourdRailPanel`、`GenieVisibility` | [../effects/README.md](../effects/README.md) | スタイルから独立 |

## 全部品に共通

| 項目 | 内容 | トークン |
|---|---|---|
| 触れる範囲 | 見た目が小さくても 48dp 以上(`Modifier.touchTarget`) | `touch.minSize` |
| 押したときの動き | マウスで hover すると幅と高さが 1.5 大きく、押すとそこから 1.1 縮む。指で押すと元より 1.5 小さくなる。倍率ではなく量で決めるので、部品の大きさによらず縁が同じだけ動く(大きいカードでも動きすぎない)。速い一押しでも見えるよう、押した・離したの知らせから動かす(`Modifier.pressMotion`) | `press.*` |
| フォーカス | キーボードで選ぶと、形に沿った 2dp の輪郭を 2dp 外に出す | `control.focusWidth` / `focusOffset` |
| 無効 | 不透明度 .45、押せない | `control.disabledOpacity` |
| 文字・アイコンの色 | スタイルの `contentColor(role, state, tone)` で決め、`LocalPiyoContentColor` で中に配る | |
| 動きを減らす | `PiyoTheme(reduceMotion)` が true なら、押下・色・つまみの動きを時間 0 にする | |
| ジニーの吸い込み先 | `genieAnchor` を付けると、見た目の位置と形(タッチ範囲を除く)を知らせる | |

## 確認

`android/sample` のカタログアプリ(部品のページ)で、プリセットとカスタム色を切り替えて全部品を見る。
