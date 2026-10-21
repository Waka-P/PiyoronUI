# カード(PiyoCard)

## 書き方

```kotlin
PiyoCard {                                   // 中身を縦に並べる
    PiyoText("お知らせ")
}
PiyoCard(onClick = { open() }) { ... }        // 押せるカード
PiyoCard(contentPadding = PaddingValues(12.dp), shape = Piyo.shapes.medium) { ... }
```

## 見た目(Glass)

| 層 | 内容 | トークン |
|---|---|---|
| 塗り | 160deg の斜めグラデーション(面 .96 → シード色を 5% 混ぜた面 .9) | `glass.card.fill` |
| 枠 | スイープを不透明度 .7 | `glass.card.edgeOpacity` |
| 線状の影 | .5px を (2, 2) ずらし、1px ぼかす | `glass.card.ring*` |
| 文字 | `contentDefault` | |

ボタンと違い外の影と内側の影は持たない。面を大きく取るので、影を重ねると重く見えるため(参照実装どおり)。

## 寸法

| 項目 | 値 | トークン |
|---|---|---|
| 余白 | 左右 24dp、上下 20.8dp(参照実装の 1.5rem / 1.3rem) | `card.paddingX` / `paddingY` |
| 形 | 角丸 大(20dp) | `radius.lg` |

## 押せるカード

参照実装のカードは押せない。押せる形は、ボタンと同じ共通の動き(押下で縮む、hover、フォーカスの輪郭、無効)を使い、読み上げの役割は Button にした。

## 他の部品での使い道

`Card` の面は、ひょうたんとパネル(`GourdRailPanel`)と、ジニーの出し入れ(`GenieVisibility` の既定の面)でも使う。
