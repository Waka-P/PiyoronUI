# ボタン(PiyoButton / PiyoIconButton)

## 書き方

```kotlin
PiyoButton("保存", onClick = {})                                   // 文字
PiyoButton("お気に入り", onClick = {}, icon = Icons.Star)            // アイコン付き
PiyoButton("確定", onClick = {}, tone = PiyoTone.Primary)           // 主な操作
PiyoButton("削除", onClick = {}, tone = PiyoTone.Danger)            // 取り消せない操作
PiyoIconButton(Icons.Plus, "追加", onClick = {})                    // 円のアイコンボタン
PiyoButton(onClick = {}) { PiyoText("自由な中身") }                  // 中身を自由に置く
```

## 状態と見た目

| 状態 | 見た目(Glass) |
|---|---|
| 通常 | 白から淡いシード色への塗り、スイープの枠、右下の線状の影 |
| hover(マウス) | 塗りが少し濃く、外の影が強くなり、1.04 倍に |
| 押下 | 指では .96 倍、マウスでは hover から .03 縮む |
| Primary / Danger | 塗りと枠を主な色 / danger 色で染める。文字は `contentOnPrimary` |
| 無効 | 不透明度 .45 |

## 寸法

| 項目 | 値 | トークン |
|---|---|---|
| 高さ | 38dp(触れる範囲は 48dp) | `control.height` |
| 左右の余白 | 16dp | `control.paddingX` |
| アイコンと文字の間 | 6dp | `control.gap` |
| アイコン | 20dp | `control.iconSize` |
| 形 | 丸い棒(`Piyo.shapes.pill`)、アイコンボタンは円 | |

## 読み上げ

役割は Button。`PiyoIconButton` は `contentDescription` を読む(省けない引数にしている)。

## 決めたこと

| 決めたこと | 理由 |
|---|---|
| 文字だけ・アイコン付きの形と、中身を自由に置く形の2つを用意 | よく使う形は1行で書け、特別な中身も置けるように |
| tone は Default / Primary / Danger の3つ | 参照実装の色付きボタンが主な操作と取り消せない操作の2種類だったため |
| 指で押したときも縮める | 参照実装は hover → active で大きさが変わるが、スマホでは hover がなく動きが見えなかった |

## 未解決

- 既定シードで Primary の白い文字は、塗りの上で AA(4.5:1)に届かない(参照実装と同じ色)。大きい文字・太字として扱うか、塗りを濃くするかは未決定
