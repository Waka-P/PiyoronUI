# Piyoron UI

Jetpack Compose と Web(CSS、JS、React)で同じ見た目と動きを使える、デザインシステムと UI ライブラリ。

- 見た目のスタイルは「普通(Plain)」(既定)と「ガラス風(Glass)」の2つ。テーマの1行で切り替わる
- 色はシード色1色から作る。文字のコントラスト(WCAG AA)を自動で満たす
- 押したときの動き、メニュー、ひょうたん型のタブ、ジニー効果などの動きを含む

| 項目 | 値 |
|---|---|
| 最新版 | 0.2.4 |
| Android | minSdk 26、Jetpack Compose(Material は使わない) |
| Web | CSS 変数、`color-mix`、`scale` / `translate` が使えるブラウザ。React 19 以上(React 版を使うとき) |
| ライセンス | MIT |

## 特徴

| 考え方 | 内容 |
|---|---|
| 部品は振る舞い、見た目はスタイル | ボタンやタブは状態・読み上げ・動きを持ち、塗り・枠・影はスタイルが描く。スタイルを替えると全部品の見た目が変わる |
| 見た目の元は1か所 | トークン(JSON)から CSS 変数と Kotlin の定数を作る。Android と Web で値がずれない |
| 色はシード色から | OKLCH で役割(主な色、面、文字、危険など)ごとの色を作る。手で整えたプリセットもある |
| 動きは部品から独立 | 押したときの動き、大きさの変化、ジニー効果、ひょうたん型のタブを、どの要素にも付けられる |

## はじめかた(Compose)

### 入れる

[JitPack](https://jitpack.io/#Waka-P/PiyoronUI) から入れる。バージョンは GitHub のタグ名。

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
    implementation("com.github.Waka-P:PiyoronUI:0.2.4")
}
```

Compose のバージョンはライブラリが BOM ごと持ってくるので、書かなくてよい。

### テーマで包む

```kotlin
setContent {
    PiyoTheme(seed = Color(0xFF2293C7)) {
        // 画面
    }
}
```

| 書き方 | 内容 |
|---|---|
| `PiyoTheme(seed = color)` | シード色から色を作る(スタイルを省くと Plain) |
| `PiyoTheme(seed = color, style = PiyoStyle.Glass)` | ガラス風にする |
| `PiyoTheme(preset = PiyoPreset.Mint)` | プリセット: Ocean、Mint、Rose、Violet、Amber、Gold、Bubble |
| `PiyoTheme(seed = color, shapes = PiyoShapes(cardRoundness = 0.1f))` | カードの丸みを変える |
| `PiyoTheme(seed = color, pressMotion = PiyoPressMotion(touchShrink = 3.dp))` | 押したときの動きの既定を変える |
| `Piyo.colors.primary`、`Piyo.shapes.pill` | テーマの色や形を取り出す |

- 端末の「アニメーションを削除」を既定で読み、動きを止める(`reduceMotion` で上書きできる)
- テーマの中で入力欄の外(押せるもの以外)を押すと、フォーカスが外れてキーボードが閉じる
- シード色の保存は使う側で行う(DataStore など)

### 部品

```kotlin
PiyoButton("保存", onClick = {})
PiyoButton("確定", onClick = {}, tone = PiyoTone.Primary)      // tone: Default、Primary、Danger
PiyoIconButton(icon, "追加", onClick = {})

PiyoChipGroup(listOf("新しい順", "古い順"), selected, { selected = it })   // 1つ選ぶ
PiyoMultiChipGroup(listOf("文書", "画像"), selectedSet, { selectedSet = it })   // いくつでも選ぶ
PiyoTabs(listOf("すべて", "未読", "既読"), index, { index = it })

PiyoTextField(text, { text = it }, placeholder = "名前")
PiyoTextField(query, { query = it }, placeholder = "探す", shape = Piyo.shapes.pill, leading = { PiyoIcon(searchIcon, null) })

PiyoCard { PiyoText("お知らせ") }
PiyoCard(onClick = {}, onLongClick = {}) { PiyoText("押せるカード") }
PiyoCard(tone = PiyoTone.Primary) { PiyoText("目立たせたい金額") }   // 塗りのカード

PiyoMenuGroup {   // 開いている間にほかのボタンへ移ると、滑って切り替わる
    Row {
        PiyoMenu(copyIcon, "コピー") { PiyoMenuItem("テキスト", onClick = {}, detail = "本文と日付") }
        PiyoMenu(moreIcon, "その他") { PiyoMenuItem("削除", onClick = {}, tone = PiyoTone.Danger) }
    }
}

PiyoText("文字")
PiyoIcon(icon, "説明")
```

| 部品 | メモ |
|---|---|
| ボタン | [docs/components/button.md](docs/components/button.md) |
| 選ぶボタン | [docs/components/chip.md](docs/components/chip.md) |
| タブ | [docs/components/tabs.md](docs/components/tabs.md) |
| 入力欄 | [docs/components/text-field.md](docs/components/text-field.md) |
| カード | [docs/components/card.md](docs/components/card.md) |
| メニュー | [docs/components/menu.md](docs/components/menu.md)。メニューの中身を自作するときは `Modifier.piyoMenuSelectable()` で、長押ししたまま指を動かして選ぶ操作に対応できる |

### 押したときの動き

ボタンと同じ「乗せると大きく、押すと縮む」動きを、どの要素にも付けられる。文字やアイコンも中心から拡大・縮小する。

```kotlin
// 押せるようにして動きを付ける(波紋は出さない)。見た目の modifier より前に付ける
Box(Modifier.size(96.dp).piyoClickable(onClick = {}).clip(shape).background(brush))

// clickable などを自分で付けるときは、同じ interactionSource を渡す
Modifier.clickable(source, indication = null) { }.piyoPress(source)

// 量(dp)と速さ(ミリ秒)を変える。省いた値は既定(ボタンと同じ)
Modifier.piyoClickable(onClick = {}, motion = PiyoPressMotion(hoverGrow = 4.dp, touchShrink = 6.dp))
```

| 値 | 意味 | 既定 |
|---|---|---|
| `hoverGrow` | マウスを乗せたときに大きくなる量 | 1.5dp |
| `activeShrink` | マウスで押したときに、乗せたときの大きさから縮む量 | 1.1dp |
| `touchShrink` | 指で押したときに、元の大きさから縮む量 | 1.5dp |
| `hoverDuration` | 乗せたとき、離して戻るときの長さ | 180ms |
| `activeDuration` | 押したときの長さ | 70ms |

- 量は倍率ではなく、縁が動く量。大きいカードでも動きすぎない
- 動かさないときは `PiyoPressMotion.None`

### そのほかの便利な Modifier

| 書き方 | 内容 |
|---|---|
| `Modifier.piyoAnimateSize()` | 中身で大きさが変わるとき、なめらかに変える(カードには既定で付いている。`PiyoCard(animateSize = false)` で外す) |
| `Modifier.piyoClearFocusOnTap()` | 押せるもの以外を押したらフォーカスを外す。テーマの中では既定で効くので、ダイアログなど別の窓の中身に付ける |

### エフェクト

```kotlin
// ひょうたん型のタブ(縦・横)。大きさは buttonSize で変える(既定は 48dp)
GourdTabRail(tabs, index, { index = it }, orientation = Orientation.Horizontal)

// 外枠、印、アイコンの色を差し替える
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

形と動きの説明は [docs/effects/README.md](docs/effects/README.md)。

## はじめかた(Web)

npm には出していない。リポジトリから作って使う。

```sh
git clone https://github.com/Waka-P/PiyoronUI.git
cd PiyoronUI/web
npm install
npm run build      # web/dist に JS を出す
```

`web/tokens.css`、`web/piyoron.css`、`web/dist/` を自分のプロジェクトに置いて読み込む。

```html
<html data-piyoron-style="plain">   <!-- ガラス風は "glass" -->
<link rel="stylesheet" href="piyoron/tokens.css">
<link rel="stylesheet" href="piyoron/piyoron.css">

<button class="piyo-btn piyo-primary">確定</button>
```

| 使い方 | 部品 |
|---|---|
| CSS だけ | `.piyo-btn`、`.piyo-chip`、`.piyo-tabs`、`.piyo-card` |
| 押したときの動き | 任意の要素に `.piyo-pressable`。量と速さは `--piyo-press-*` で変える |
| JS | メニュー、ひょうたん、ひょうたんとパネル、ジニー(`import { PiyoMenu } from 'piyoron'`) |
| React | `import { PiyoButton, PiyoMenu } from 'piyoron/react'` |

詳しくは [docs/web.md](docs/web.md)。

## 見本を動かす

| 見本 | 動かし方 |
|---|---|
| Android のカタログアプリ(全部品、エフェクト、色の切り替え) | `cd android && ./gradlew :sample:installDebug` |
| Web のカタログ(素の HTML と React) | `cd web && npm install && npm run dev` で `/demo/index.html`、`/demo/react.html` を開く |

## ドキュメント

| ファイル | 内容 |
|---|---|
| [docs/styles/plain.md](docs/styles/plain.md) | Plain の色の役割 |
| [docs/styles/glass.md](docs/styles/glass.md) | Glass の作り(どの値か、なぜこの見た目か、Android での描き方) |
| [docs/components/](docs/components/README.md) | 部品ごとの使い方と決めごと |
| [docs/effects/README.md](docs/effects/README.md) | ジニーとひょうたんの形と動き、重さへの対策 |
| [docs/web.md](docs/web.md) | Web 版の使い方、Android 版との違い |
| [tokens/schema.md](tokens/schema.md) | トークンの書き方 |

## 構成

| 場所 | 中身 |
|---|---|
| `tokens/` | 見た目の元データ(JSON) |
| `tools/` | 色の生成、CSS / Kotlin の書き出し、テスト、確認用のプレビュー |
| `web/` | CSS(`tokens.css` は生成物、`piyoron.css` は部品)、JS と React 版(`src/`)、カタログ(`demo/`) |
| `android/piyoron` | Compose ライブラリ(`core`、`components`、`style.plain`、`style.glass`、`effects`、`tokens` のパッケージ) |
| `android/sample` | カタログアプリ |
| `docs/` | スタイル、部品、エフェクトの説明 |

## 開発

| コマンド | 内容 |
|---|---|
| `npm run tokens` | トークンから CSS と Kotlin を作り直す |
| `npm run colors -- "#D6457C"` | シード色から全 role を JSON で出す(`--preset mint` も可) |
| `npm test` | 色生成、生成物、Web の形と置き場所のテスト(JS) |
| `cd android && ./gradlew :piyoron:testDebugUnitTest` | 色生成と形の計算のテスト(Kotlin。色が JS と同じ結果か) |
| `cd android && ./gradlew :piyoron:publishToMavenLocal` | ライブラリを手元の Maven に出す(JitPack も同じ処理。`jitpack.yml`) |

- Node.js 24、JDK 17 以上(Android Studio 付属の JBR で可)が要る
- 生成した Kotlin はリポジトリに含めるので、ライブラリのビルドだけなら Node.js は要らない
- 名前(`Piyo` や CSS の接頭辞など)の設定は `piyoron.config.json` と `android/gradle.properties` にまとめている

## ライセンス

MIT([LICENSE](LICENSE))
