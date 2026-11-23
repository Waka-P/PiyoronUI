# カード(PiyoCard)

## 書き方

```kotlin
PiyoCard {                                   // 中身を縦に並べる
    PiyoText("お知らせ")
}
PiyoCard(onClick = { open() }) { ... }        // 押せるカード
PiyoCard(contentPadding = PaddingValues(12.dp), shape = Piyo.shapes.medium) { ... }
PiyoTheme(shapes = PiyoShapes(cardRoundness = 0.2f)) { ... }   // カードの丸み度合いを変える(アプリ全体)
```

```html
<div class="piyo-card">...</div>                                        <!-- 押せないカード -->
<button class="piyo-card piyo-pressable" type="button">...</button>     <!-- 押せるカード -->
<style>:root { --piyo-card-roundness: 0.2; }</style>                    <!-- 丸み度合いを変える -->
```

## 見た目(Glass)

| 層 | 内容 | トークン |
|---|---|---|
| 塗り | 160deg の斜めグラデーション(面 .96 → シード色を 5% 混ぜた面 .9) | `glass.card.fill` |
| 枠(押せるカード) | スイープを不透明度 .5(参照実装は .7) | `glass.card.edgeOpacity` |
| 枠(押せないカード) | スイープを不透明度 .3 | `glass.card.staticEdgeOpacity` |
| 線状の影(押せるカード) | .5px を (1.5, 1.5) ずらし、1px ぼかす(参照実装は (2, 2)) | `glass.card.ring*` |
| 線状の影(押せないカード) | 同じ形で、色の不透明度を .25 倍 | `glass.card.staticRingOpacity` |
| 文字 | `contentDefault` | |

ボタンと違い外の影と内側の影は持たない。面を大きく取るので、影を重ねると重く見えるため(参照実装どおり)。

## 寸法

| 項目 | 値 | トークン |
|---|---|---|
| 余白 | 左右 24dp、上下 20.8dp(参照実装の 1.5rem / 1.3rem) | `card.paddingX` / `paddingY` |
| 形 | 角の半径 = 丸み度合い x 基準の横幅 x (横幅 / 基準の横幅)^増え方。高さの 3 割まで(既定 0.0833、240、0.3、0.3) | `card.roundness` / `roundnessBase` / `roundnessGrowth` / `roundnessHeightCap` |

## 角の丸み(丸み度合い)

| 項目 | 内容 |
|---|---|
| 決め方 | 角の半径 = 丸み度合い x 基準の横幅 x (横幅 / 基準の横幅)^増え方。高さの 3 割と、短い辺の半分までに収める |
| 既定 | 度合い 0.0833、基準の横幅 240、増え方 0.3、高さの上限 0.3。カタログのジニーの「お知らせ」(240 x 150)は角丸 20 のまま。画面幅いっぱい(約 380)のカードは約 23 |
| 横幅で決める理由 | 中身が開いて高さが変わっても、角の丸みが変わらない。画面幅いっぱいのカードは、高さによらずどれも同じ丸みになる |
| 変え方 | Compose は `PiyoShapes(cardRoundness = ..., cardRoundnessGrowth = ...)`、形だけなら `piyoCardShape(0.1f, 0.3f)`。Web は `--piyo-card-roundness`、`--piyo-card-roundness-growth`、`--piyo-card-roundness-height-cap` |
| 理由 | 比例させると、相似形ではあっても、目は角の半径そのもので丸さを感じるので、大きいカードほど丸く見えた。固定だと、とても小さい面ととても大きい面で差が出る。その間を取り、半径をゆるやかに増やす |

## 押せるカード

参照実装のカードは押せない。押せる形は、ボタンと同じ共通の動き(押下で縮む、hover、フォーカスの輪郭、無効)を使い、読み上げの役割は Button にした。

押せるかどうかが見た目で分かるよう、面の種類を分けている(`SurfaceRole.Card` / `ClickableCard`)。

| スタイル | 押せないカード | 押せるカード |
|---|---|---|
| Glass | 枠と線状の影をかなり薄く(上の表) | 枠と線状の影を元の濃さ |
| Plain | 塗りだけ | 塗り + `ringShadow` の細い枠(`plain.edge.width`) |

## 他の部品での使い道

`Card` の面は、ひょうたんとパネル(`GourdRailPanel`)と、ジニーの出し入れ(`GenieVisibility` の既定の面)でも使う。

## 塗りカード、長押し

| 項目 | 内容 |
|---|---|
| 塗りカード | `PiyoCard(tone = PiyoTone.Primary)`。主な色で塗り、ふつうのカードと同じ線状の影と枠を付ける。プリセットが `toneFill` を持つときはその並びのグラデーション。文字は `contentOnPrimary` |
| 長押し | 押せるカードに `onLongClick` を渡すと、長押しも受ける。押したときの動きはふつうのカードと同じで、押した範囲に色を重ねない |

## 大きさの変化

| 項目 | 内容 |
|---|---|
| 既定 | 中身が増えたり減ったりして大きさが変わると、なめらかに変わる(220ms、`resize.duration` / `resize.easing`。メニューと同じ速さ) |
| 外し方 | `PiyoCard(animateSize = false)` |
| ほかの要素 | `Modifier.piyoAnimateSize()` を付けると、どの要素でも同じ動きになる。面(`piyoSurface`)を描く要素では、面のあとに付けると、面も変わる大きさに沿って描かれる |
| 動きを減らす | `PiyoTheme(reduceMotion = true)` のときは、すぐ変える |
| Web | まだない(CSS だけでは中身による高さの変化をなめらかにできず、JS で測る仕組みが要る) |
