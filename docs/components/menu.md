# メニュー(PiyoMenu / PiyoMenuGroup / PiyoMenuItem / PiyoMenuDivider)

アイコンのボタンを押すと、その下に項目の一覧が開く。`PiyoMenuGroup` の中に並べたメニューは、1つを開いている間にほかのボタンへ移ると、閉じずに滑って切り替わる。

## 書き方

```kotlin
PiyoMenuGroup {
    Row {
        PiyoMenu(Icons.Copy, "コピー") {
            PiyoMenuItem("テキスト", onClick = { copyText() }, detail = "本文と日付")
            PiyoMenuItem("リンク", onClick = { copyLink() })
        }
        PiyoMenu(Icons.More, "その他") {
            PiyoMenuItem("複製", onClick = { duplicate() }, icon = Icons.Copy)
            PiyoMenuDivider()
            PiyoMenuItem("削除", onClick = { delete() }, tone = PiyoTone.Danger)
        }
    }
}
```

- `PiyoMenu` だけで使ってもよい(1つで開閉する)
- 項目を押すと、メニューを閉じてから `onClick` を呼ぶ
- `PiyoTheme` の中で使う(メニューはテーマが持つ最前面の層に描く)

## 動き

| 操作 | 動き | トークン |
|---|---|---|
| 開く | ボタンの中心から .3 倍で広がる(200ms)。不透明度は 160ms で上がる | `menu.openScale` / `openDuration` / `fadeDuration` / `easing` / `fadeEasing` |
| 閉じる | ボタンの中心へ縮んで消える | 同上 |
| 切り替え | 開いている間に同じ組のほかのボタンへマウスを乗せる(スマホでは押す)と、新しいメニューは前のメニューの位置から滑ってきて、前のメニューは新しいメニューの方へ滑りながら消える(220ms) | `menu.slideDuration` |
| 閉じる操作 | メニューとボタンの外を押す、Esc、戻るボタン、ボタンが動いた(スクロール、画面の大きさの変化) | |

動きを減らす設定では、どれもアニメーションなしで切り替える。

## 置き方

ボタンの下に、右端をそろえて置く(間は 4dp)。下に入らなければボタンの上に置く。画面の端から 8dp は空ける(`menuPosition`)。

## 見た目(Glass)

| 部分 | 内容 | トークン |
|---|---|---|
| 面(`Menu`) | 面の色 .94 の半透明、主な色 .16 の 1dp の枠、2段の外の影、角丸 14dp、内側の余白 5dp、幅 140dp 以上 | `glass.menu.fill` / `edge` / `shadow`、`menu.radius` / `padding` / `minWidth` |
| 項目(`MenuItem`) | マウスを乗せる・フォーカス・押すと、淡い主な色の背景(角丸 9dp)と強い主な色の文字。120ms で変わる | `glass.menu.itemHover`、`menu.itemRadius` / `itemDuration` |
| 削除の項目(`MenuItem`、Danger) | 文字は危険の色、背景は淡い危険の色 | `glass.menu.itemDangerHover` |
| 補足 | 右寄せ、11.5sp、薄い文字色 | `menu.detailSize` / `detailGap` |
| 区切り線(`Divider`) | 1dp、左右 8dp 内側 | `glass.menu.divider`、`menu.dividerInset` / `dividerSpace` |

## 読み上げ

メニューのボタンは Button(`contentDescription` を読む)。項目は Button。

## 決めたこと

| 決めたこと | 理由 |
|---|---|
| メニューは別ウィンドウ(Popup)ではなく、`PiyoTheme` が持つ最前面の層に描く | 別ウィンドウだと、滑る間にウィンドウの外へ出た部分が切れる。層は押す操作を横取りしないので、ほかのボタンへのマウスの移動もそのまま届く |
| スマホでは、開いている間にほかのボタンを押すと滑って切り替える | スマホにはマウスを乗せる操作がない。参照実装では押すと一度閉じてから開く |
| 項目の高さは 48dp 以上 | 触れる範囲の決まり。参照実装は約 34px |
| 削除の項目の上の区切り線は、`PiyoMenuDivider` で明示する | 参照実装は「最初でない削除の項目」に自動で付ける。Compose では項目が自分の位置を知らないので、置く側が決める |
| 区切り線と削除の背景の色は、役割の色から作る | 参照実装は色コードの直書き(`#e5ebee`、`#fdeaea`)。シードや危険の色を変えても合うように |
| 戻るボタンで閉じる | Android の習慣。このために `androidx.activity:activity-compose` に依存する |
| 背景ぼかしは使わない | Glass の他の部品と同じく、なくても成立する見た目を既定にする |
| 面の不透明度を .8 から .94 に | 参照実装は .8 に背景ぼかしを重ねて、下の文字をぼかしている。ぼかしなしの .8 では下の文字が透けて項目が読みにくかった(実機で確認) |
| 影は本物のぼかしで描く | 影のぼかしが 32dp と大きく、重ねて近づける描き方では段が四角く見えた。形が変わらない面なので、本物のぼかしでも重くならない |

## 未解決

- キーボードで開いたとき、最初の項目にフォーカスを移していない
- 押して開くボタンとしての状態(開いている・閉じている)を読み上げに出していない
