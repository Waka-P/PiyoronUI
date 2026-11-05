# Piyoron UI

お気に入りのUIを元にした、自分用のデザインシステムとUIライブラリ。スタイルは「普通(Plain)」(既定)と「ガラス風(Glass)」。Jetpack Compose 版と Web 版(CSS、JS、React)がある。

名前は仮(`piyoron.config.json`、`android/gradle.properties`、このファイルに集約)。

## 特徴

- **部品は振る舞いだけ、見た目はスタイル。** ボタンやタブは状態・読み上げ・動きを持ち、塗り・枠・影はスタイルが描く。スタイルを足せば全部品の見た目が変わる
- **見た目の元はトークン(JSON)1か所。** CSS 変数と Kotlin の定数を同じ JSON から作る
- **色はシード色1色から作る。** OKLCH で役割(role)ごとの色を生成し、文字のコントラスト(AA)を自動で満たす。手で整えたプリセットもある
- **動きのエフェクト。** パネルを吸い込むジニー効果と、くびれのあるひょうたん型のタブ。スタイルから独立していて、どの画面にも付けられる

## 使い方(Compose)

### 入れる

JitPack から入れる。バージョンは GitHub のタグ名。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.Waka-P:PiyoronUI:0.1.0")
}
```

Compose のバージョンはライブラリが BOM ごと持ってくるので、書かなくてよい。minSdk 26。

### テーマ

```kotlin
PiyoTheme(seed = Color(0xFF2293C7)) { ... }   // シード色から(style を省くと Plain。飾りのない平らな面)
PiyoTheme(seed = seed, style = PiyoStyle.Glass) { ... }   // ガラス風にする
PiyoTheme(preset = PiyoPreset.Mint) { ... }    // プリセット: Ocean, Mint, Rose, Violet, Amber, Gold, Bubble
Piyo.colors.primary                            // 色の取り出し
```

シード色の保存は呼び出し側で行う(DataStore など)。端末の「アニメーションを削除」を既定で読み、動きを止める(`reduceMotion` で上書きできる)。

### 部品

```kotlin
PiyoButton("保存", onClick = {})
PiyoButton("確定", onClick = {}, tone = PiyoTone.Primary)
PiyoIconButton(icon, "追加", onClick = {})
PiyoChipGroup(listOf("新しい順", "古い順"), selected, { selected = it })
PiyoMultiChipGroup(listOf("文書", "画像"), selectedSet, { selectedSet = it })
PiyoTabs(listOf("すべて", "未読", "既読"), index, { index = it })
PiyoCard { PiyoText("お知らせ") }
PiyoMenuGroup(side = PiyoMenuSide.Bottom, alignment = PiyoMenuAlignment.EndEdge) {   // 開いている間にほかのボタンへ移ると、滑って切り替わる
    Row {
        PiyoMenu(copyIcon, "コピー") { PiyoMenuItem("テキスト", onClick = {}, detail = "本文と日付") }
        PiyoMenu(moreIcon, "その他") { PiyoMenuItem("削除", onClick = {}, tone = PiyoTone.Danger) }
    }
}
```

### 押したときの動き

ボタンと同じ「乗せると大きく、押すと縮む」動きを、どの要素にも付けられる。文字やアイコンも位置がずれずに中心から拡大・縮小する。

```kotlin
// 押せるようにして動きを付ける(波紋は出さない)。見た目の modifier より前に付ける
Box(Modifier.size(96.dp).piyoClickable(onClick = {}).clip(shape).background(brush))

// clickable などを自分で付けるときは、同じ interactionSource を渡す
Modifier.clickable(source, indication = null) { }.piyoPress(source)

// 量(dp)と速さ(ミリ秒)を変える。省いた値は既定(ボタンと同じ)
Modifier.piyoClickable(onClick = {}, motion = PiyoPressMotion(hoverGrow = 4.dp, touchShrink = 6.dp, activeDuration = 120))

// アプリ全体(Piyoron の部品も含む)の既定を変える
PiyoTheme(seed = seed, pressMotion = PiyoPressMotion(touchShrink = 3.dp)) { ... }
```

| 値 | 意味 | 既定 |
|---|---|---|
| `hoverGrow` | マウスを乗せたときに大きくなる量 | 1.5dp |
| `activeShrink` | マウスで押したときに、乗せたときの大きさから縮む量 | 1.1dp |
| `touchShrink` | 指で押したときに、元の大きさから縮む量 | 1.5dp |
| `hoverDuration` | 乗せたとき、離して戻るときの長さ | 180ms |
| `activeDuration` | 押したときの長さ | 70ms |

- 量は倍率ではなく、縁が動く量。部品の大きさによらず同じだけ動く(大きいカードでも動きすぎない)
- 動かさないときは `PiyoPressMotion.None`。端末の「アニメーションを削除」では動かない
- 背景や枠の modifier より前に付ける(後ろに付けると、背景や枠が拡大・縮小しない)

### エフェクト

```kotlin
// ひょうたん型のタブ(縦・横)
GourdTabRail(tabs, index, { index = it }, orientation = Orientation.Horizontal)

// 外枠、印、アイコンの色を差し替える(ガラスにしないとき)
GourdTabRail(tabs, index, { index = it },
    track = { shape -> Modifier.background(trackColor, shape) },
    indicator = { shape -> Box(Modifier.fillMaxSize().background(thumbColor, shape)) },
    iconColors = GourdIconColors(selected = onThumb, unselected = muted))

// ひょうたんとパネル。表示中のタブをもう一度押すとジニー効果で開閉する
GourdRailPanel(tabs, index, { index = it }, panelOpen = open, onPanelOpenChange = { open = it },
    side = GourdSide.Left, railAlignment = GourdAlignment.Center) { /* パネルの中身 */ }

// どの部品でも、ボタンへ吸い込むように出し入れする
val anchor = rememberGenieAnchor()
PiyoIconButton(icon, "お知らせ", { visible = !visible }, modifier = Modifier.genieAnchor(anchor))
GenieVisibility(visible, anchor) { /* カードの中身 */ }
```

## 使い方(Web)

```html
<html data-piyoron-style="plain">   <!-- ガラス風は "glass" -->
<link rel="stylesheet" href="piyoron/tokens.css">
<link rel="stylesheet" href="piyoron/piyoron.css">

<button class="piyo-btn piyo-primary">確定</button>
```

- CSS だけの部品: `.piyo-btn`、`.piyo-chip`、`.piyo-tabs`、`.piyo-card`
- 押したときの動き: 任意の要素に `.piyo-pressable`。量と速さは `--piyo-press-*` で変える(JS は `setPiyoPress(el, { touchShrink: 6 })`、React は `<PiyoPressable motion={...}>`)
- JS の部品: メニュー、ひょうたん、ひょうたんとパネル、ジニー(`import { PiyoMenu } from 'piyoron'`)
- React 版: `import { PiyoButton, PiyoMenu } from 'piyoron/react'`

詳しくは `docs/web.md`。

## 構成

| 場所 | 中身 |
|---|---|
| `tokens/` | 見た目の元データ(JSON)。書き方は `tokens/schema.md` |
| `tools/` | 色の生成、CSS / Kotlin の書き出し、テスト、確認用のプレビュー |
| `web/tokens.css` | 生成した CSS 変数 |
| `web/piyoron.css` | 部品の CSS |
| `web/src` | JS(色、メニュー、ひょうたん)と React 版(`web/src/react`) |
| `web/demo` | Web のカタログ(素の HTML と React。Android のサンプルと同じ構成) |
| `android/piyoron` | Compose ライブラリ(`core`、`components`、`style.plain`、`style.glass`、`effects`、`tokens` のパッケージ) |
| `android/sample` | カタログアプリ(全部品、エフェクト、色の切り替え) |
| `docs/` | 分解メモ(スタイル、部品、エフェクト) |

## ドキュメント

| ファイル | 内容 |
|---|---|
| `docs/styles/plain.md` | Plain の分解メモ(役割ごとの色) |
| `docs/styles/glass.md` | Glass の分解メモ(どの値か、なぜこの見た目か、Android での描き方) |
| `docs/components/` | 部品ごとのメモ |
| `docs/effects/README.md` | ジニーとひょうたんの形と動き、重さへの対策 |
| `docs/web.md` | Web 版の使い方、変わる形の描き方、Android 版との違い |
| `tokens/schema.md` | トークンの書き方 |

## 開発

| コマンド | 内容 |
|---|---|
| `npm run tokens` | トークンから CSS と Kotlin を作り直す |
| `npm run colors -- "#D6457C"` | シード色から全 role を JSON で出す(`--preset mint` も可) |
| `npm test` | 色生成、生成物、Web の形と置き場所のテスト(JS) |
| `cd web && npm install && npm run dev` | Web のカタログを開く(`/demo/index.html`、`/demo/react.html`) |
| `cd web && npm run build` | Web のライブラリを `web/dist` に出す |
| `cd android && ./gradlew :piyoron:testDebugUnitTest` | 色生成と形の計算のテスト(Kotlin。色は JS と同じ結果か) |
| `cd android && ./gradlew :sample:installDebug` | サンプルを入れる |
| `cd android && ./gradlew :piyoron:publishToMavenLocal` | ライブラリを手元の Maven に出す(JitPack も同じ処理。`jitpack.yml`) |

Node.js 24、JDK 17 以上(Android Studio 付属の JBR で可)が要る。生成した Kotlin はリポジトリに含めるので、ライブラリのビルドだけなら Node.js は要らない。

## 未定

| 項目 | 今の仮の値 |
|---|---|
| 名前 | Piyoron |
| ライセンス | 未定(決めるまで、他の人が使ったり改変したりする許可はない) |
| minSdk | 26 |
