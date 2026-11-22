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
| 形 | 角の半径 = 丸み度合い x 基準の長さ x (短い辺 / 基準の長さ)^増え方(既定 0.1333、150、0.3) | `card.roundness` / `roundnessBase` / `roundnessGrowth` |

## 角の丸み(丸み度合い)

| 項目 | 内容 |
|---|---|
| 決め方 | 角の半径 = 丸み度合い x 基準の長さ x (短い辺 / 基準の長さ)^増え方。半径は短い辺の半分まで |
| 既定 | 度合い 0.1333、基準の長さ 150、増え方 0.3。カタログのジニーの「お知らせ」(240 x 150)は角丸 20 のまま |
| 増え方 | 1 で短い辺に比例(前の決め方)、0 で固定の半径。0.3 だと、短い辺 60 で約 15、150 で 20、250 で約 23 と、ゆるやかに増える |
| 変え方 | Compose は `PiyoShapes(cardRoundness = ..., cardRoundnessGrowth = ...)`、形だけなら `piyoCardShape(0.2f, 0.3f)`。Web は `--piyo-card-roundness` と `--piyo-card-roundness-growth` |
| Web の仕組み | `card.js` が大きさを測り、`--piyo-card-radius` と、その半径に合わせた枠のマスク(`--piyo-card-edge-mask`)を入れる。読み込むと自動で始まり、あとから足したカードや大きさの変化にも効く。JS がないときは `radius.lg` |
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
