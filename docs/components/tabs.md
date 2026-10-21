# タブ(PiyoTabs)

## 書き方

```kotlin
PiyoTabs(listOf("すべて", "未読", "既読"), index, { index = it })
PiyoTabs(tabs, index, { index = it }, indicator = { MyIndicator() })  // つまみを差し替える
```

項目は同じ幅で並ぶ。幅は `modifier` で決める(`fillMaxWidth()` など)。

## 状態と見た目

| 部分 | 見た目(Glass) |
|---|---|
| 土台(`TabTrack`) | 極薄の塗り、スイープの枠を不透明度 .3 |
| つまみ(`TabThumb`) | 通常ボタンと選んだチップの中間の塗りと枠、立体的な内側の影、線状の影 |
| 文字 | 選んだ項目は `contentSelected`、他は `contentMuted` |

選ぶと、つまみが 250ms で滑る(`duration.segmented`、`easing.standard`)。途中で別の項目を選ぶと、今の位置から向きを変える。

## 寸法

| 項目 | 値 | トークン |
|---|---|---|
| 土台の内側の余白と枠 | 3dp + 1dp | `tabs.trackPadding` / `trackBorder` |
| 項目の余白 | 左右 4dp、上下 7dp | `tabs.itemPaddingX` / `itemPaddingY` |
| 高さ | 触れる範囲 48dp を満たす | `touch.minSize` |

## 読み上げ

各項目が Tab、全体が選択のグループ。

## 決めたこと

| 決めたこと | 理由 |
|---|---|
| つまみの動き(どこから・どこへ・どこまで)を `rememberIndicatorMotion` に分け、ひょうたんと共有 | 選んだ印が動く仕組みは同じなので、ひょうたんの印と使い回す。動き方(滑る / 伸び縮み)は使う側が決める |
| つまみは差し替えられる(`indicator`) | 印の形や見た目を、使う側が変えられるように |
| 枠を、参照実装の主な色の直線グラデーションから、ボタンと同じスイープに | 参照実装ではほぼ1色に見え、他の部品より色の変化が乏しかった |
| つまみを通常ボタンと選んだチップの中間に | 選んだ印として目立たせつつ、チップほど強くしない |
