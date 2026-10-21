# Piyoron UI

お気に入りのUIを元にした、自分用のデザインシステムとUIライブラリ。最初のスタイルは「ガラス風(Glass)」。Jetpack Compose 版が先で、Web 版はこのあと作る。

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
PiyoTheme(seed = Color(0xFF2293C7)) { ... }   // シード色から(style を省くと Glass)
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
PiyoMenuGroup(transition = PiyoMenuTransition.Fade) {   // 開いている間にほかのボタンへ移ると、滑って切り替わる
    Row {
        PiyoMenu(copyIcon, "コピー") { PiyoMenuItem("テキスト", onClick = {}, detail = "本文と日付") }
        PiyoMenu(moreIcon, "その他") { PiyoMenuItem("削除", onClick = {}, tone = PiyoTone.Danger) }
    }
}
```

### エフェクト

```kotlin
// ひょうたん型のタブ(縦・横)
GourdTabRail(tabs, index, { index = it }, orientation = Orientation.Horizontal)

// ひょうたんとパネル。表示中のタブをもう一度押すとジニー効果で開閉する
GourdRailPanel(tabs, index, { index = it }, panelOpen = open, onPanelOpenChange = { open = it },
    side = GourdSide.Left, railAlignment = GourdAlignment.Center) { /* パネルの中身 */ }

// どの部品でも、ボタンへ吸い込むように出し入れする
val anchor = rememberGenieAnchor()
PiyoIconButton(icon, "お知らせ", { visible = !visible }, modifier = Modifier.genieAnchor(anchor))
GenieVisibility(visible, anchor) { /* カードの中身 */ }
```

## 構成

| 場所 | 中身 |
|---|---|
| `tokens/` | 見た目の元データ(JSON)。書き方は `tokens/schema.md` |
| `tools/` | 色の生成、CSS / Kotlin の書き出し、テスト、確認用のプレビュー |
| `web/tokens.css` | 生成した CSS 変数 |
| `android/piyoron` | Compose ライブラリ(`core`、`components`、`style.glass`、`effects`、`tokens` のパッケージ) |
| `android/sample` | カタログアプリ(全部品、エフェクト、色の切り替え) |
| `docs/` | 分解メモ(スタイル、部品、エフェクト) |

## ドキュメント

| ファイル | 内容 |
|---|---|
| `docs/styles/glass.md` | Glass の分解メモ(どの値か、なぜこの見た目か、Android での描き方) |
| `docs/components/` | 部品ごとのメモ |
| `docs/effects/README.md` | ジニーとひょうたんの形と動き、重さへの対策 |
| `tokens/schema.md` | トークンの書き方 |

## 開発

| コマンド | 内容 |
|---|---|
| `npm run tokens` | トークンから CSS と Kotlin を作り直す |
| `npm run colors -- "#D6457C"` | シード色から全 role を JSON で出す(`--preset mint` も可) |
| `npm test` | 色生成と生成物のテスト(JS) |
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
