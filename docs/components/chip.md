# 選ぶボタン(PiyoChip / PiyoChipGroup / PiyoMultiChipGroup)

## 書き方

```kotlin
PiyoChipGroup(listOf("新しい順", "古い順"), selected, { selected = it })            // 1つだけ選ぶ
PiyoMultiChipGroup(listOf("文書", "画像", "動画"), selectedSet, { selectedSet = it }) // いくつでも選ぶ
PiyoChip("通知", checked, { checked = !checked })                                   // 1つだけ置く
```

並びは幅に合わせて折り返す(`FlowRow`)。項目の型は自由で、表示は `label` で決める。

## 状態と見た目

| 状態 | 見た目(Glass) |
|---|---|
| 未選択 | ほぼ白(シード色を少しだけ)+ 白い光沢。枠は斜めの直線グラデーションで薄い。線状の影なし |
| hover | シード色が少し濃く、枠と文字が主な色に寄る |
| 選択 | 塗りが選択の濃さ(`fillSelected*`)になり、枠は主な色のスイープ、線状の影が出る |
| 押下・無効 | ボタンと同じ |

選ぶ・外すの変化は 180ms で色と濃さを動かす(`duration.toggle`)。

## 寸法

| 項目 | 値 | トークン |
|---|---|---|
| 余白 | 左右 14dp、上下 6dp | `chip.paddingX` / `paddingY` |
| 並びの間 | 8dp | `chip.gap` |
| 形 | 丸い棒 | |

## 読み上げ

| 使い方 | 役割 |
|---|---|
| `PiyoChipGroup`(1つだけ) | 各項目が RadioButton、並び全体が選択のグループ |
| `PiyoMultiChipGroup`、`PiyoChip` | Checkbox |

## 決めたこと

| 決めたこと | 理由 |
|---|---|
| 並びを2種類(1つだけ / いくつでも)に分けた | 選んだ値の型(1つ / 集合)が違い、読み上げの役割も違うため |
| 選択の塗りは色の役割(`fillSelectedTop` / `Bottom`)として持つ | タブのつまみなど、他の部品でも「選んだ」の濃さを揃えるため |

## 未解決

- hover の文字色(主な色)は、未選択の塗りの上で AA に届かない(参照実装と同じ)
