# Web 版

CSS だけで使える部品(ボタン、選ぶボタン、タブ、カード)と、JS が要る部品(メニュー、ひょうたん、ひょうたんとパネル、ジニー)がある。React 版は、この CSS と JS を使う薄い部品。見た目、寸法、動きは Android 版と同じにしてある。

## ファイル

| 場所 | 中身 |
|---|---|
| `web/tokens.css` | トークンの CSS 変数(生成物) |
| `web/piyoron.css` | 部品の CSS(土台 + スタイルごとのレシピ) |
| `web/src/` | JS(色、メニュー、ひょうたん、形と動きの計算) |
| `web/src/react/` | React 版 |
| `web/demo/` | カタログ(`index.html` は素の HTML、`react.html` は React。どちらも Android のサンプルと同じ構成) |
| `web/test/` | 形と置き場所の計算のテスト |

## 使い方(CSS)

```html
<html data-piyoron-style="glass">
<link rel="stylesheet" href="piyoron/tokens.css">
<link rel="stylesheet" href="piyoron/piyoron.css">

<button class="piyo-btn piyo-primary">確定</button>
<button class="piyo-chip" aria-pressed="true">画像</button>
<div class="piyo-tabs" role="tablist" style="--piyo-tabs-n: 3; --piyo-tabs-i: 0">
  <span class="piyo-tabs-thumb"></span>
  <button class="piyo-tab" role="tab" aria-selected="true">すべて</button>
  ...
</div>
<div class="piyo-card">...</div>
```

| クラス | 部品 | 状態 |
|---|---|---|
| `.piyo-btn` | ボタン。`.piyo-primary`、`.piyo-danger`、`.piyo-icon-btn`(円。大きさは `--piyo-btn-size`、アイコンは `--piyo-btn-icon-size`) | `:disabled` |
| `.piyo-chip` | 選ぶボタン。並びは `.piyo-chip-group` | `aria-pressed` / `aria-checked` / `data-selected` |
| `.piyo-tabs` | タブ。つまみは `.piyo-tabs-thumb`、項目は `.piyo-tab` | `--piyo-tabs-n`(数)、`--piyo-tabs-i`(選んだ番号)、`aria-selected` |
| `.piyo-card` | カード。押せるなら `button.piyo-card.piyo-pressable` | |
| `.piyo-pressable` | カードに押したときの動きを付ける(`button.piyo-card.piyo-pressable`) | |

- スタイルは `data-piyoron-style`、プリセットの色は `data-piyoron-palette`(`ocean` など)で切り替える。どの要素に付けてもよく、中の部品に効く
- セレクタは `:where()` で包んである(詳細度 0)。使う側のクラス1つで上書きできる
- 端末の「動きを減らす」設定か、`data-piyoron-reduce-motion="true"` を付けた要素の中では、動きを止める
- ボタン、円のボタン、選ぶボタンは、Android 版と同じく押せる範囲を 48px 取る(見た目の外側に余白を付ける)。並べたときの間隔も Android 版と同じになる
- 押したときの動き(乗せると大きく、押すと縮む)は、倍率ではなく px の量(`press.*`)で決める。部品の大きさによらず縁が同じだけ動く
  - Android 版と同じく、文字も含めて部品全体を拡大・縮小する(`scale`)。拡大率は「1 + 量 ÷ 幅」と「1 + 量 ÷ 高さ」
  - 幅と高さは `press.js` が、マウスを乗せたときと押したときに測って `--piyo-w`、`--piyo-h` に入れる。CSS では部品が自分の大きさを知れない(コンテナクエリは先祖しか測れず、部品自身をコンテナにすると中身に合わせた幅が決まらない)ため
  - `press.js` は `piyoron` か `piyoron/react` を読み込むと自動で動く。CSS だけで使うときは、押したときの大きさは変わらない
  - 部品は描画の層に分け(`will-change: scale`)、拡大率そのものを動かす。動きの途中で文字を描き直さないので、文字が跳ねない。毎コマ描き直すと、文字が画素にそろい直されて前後に跳ねる

### Glass のレシピ

部品は、面の役割の変数を決めるだけ。塗り・枠・線状の影は共通の1つのレシピが描く。

| 変数 | 意味 |
|---|---|
| `--piyo-s-fill` | 塗り |
| `--piyo-s-shadow` | 外の影(box-shadow) |
| `--piyo-s-edge` / `--piyo-s-edge-opacity` | 枠(スイープのグラデーション)と濃さ |
| `--piyo-s-edge-mask` | 枠の形(角の丸みごとのマスク。`--piyo-glass-edge-mask-pill`、`-sm`、`-md`、`-lg`) |
| `--piyo-s-ring-color` / `-width` / `-blur` / `-x` / `-y` | 線状の影 |

枠は `::before` に、形の内側に太さ 1px の帯を残す SVG の線のマスクをかけて描く。線状の影は `::after` をずらしてぼかす。

- マスクは角の丸みのトークンから生成する(`tokens.css`)。ピル形と円は `ry="50%"` の1つで、大きさに関係なく使える
- 部品の角の丸みを変えたときは、`--piyo-s-edge-mask` も合わせる
- 前の「外の形から内の形を抜く」マスクは、ブラウザの倍率によって上下と左右の太さがずれた(125% で左右 1.13px、上下 0.81px など)。線のマスクでは四方がそろう
- 部品の位置や大きさに px の端数があると、その縁はぼけて薄く見える。Android(Compose)は位置を整数の画素に丸めるので起きない。Web 版でも、選ぶボタンの余白とカードの余白は整数の px に丸めている。使う側でも、行の高さなどは整数の px にするとよい

## 使い方(JS)

```js
import { setPiyoSeed, setPiyoPreset, PiyoMenu, PiyoMenuGroup, GourdTabRail, GourdRailPanel, PiyoGenie } from 'piyoron';

setPiyoSeed('#D6457C');            // シード色から全部の色を作る
setPiyoPreset('mint');             // 手で整えたプリセット

const group = new PiyoMenuGroup({ transition: 'genie', align: 'end-corner' });
new PiyoMenu(button, menuElement, { group });

new GourdTabRail(railElement, { selected: 0, onSelect: (i) => {} });
new GourdRailPanel(panelElement, { side: 'left', onSelect: (i) => {} });

const genie = new PiyoGenie(cardElement, button, { radius: 20 });
genie.setOpen(false);              // ボタンに吸い込まれる
```

| 部品 | 要素 | 主な指定 |
|---|---|---|
| `PiyoMenu` | ボタンと `.piyo-menu`(中身は自由) | `transition`(`fade` 既定、`scale`、`genie`、`none`)、`side`(`bottom`、`top`、`right`、`left`)、`align`(下の表か -1〜1 の数) |
| `PiyoMenuGroup` | なし | 中のメニューの既定。開いている間にほかのボタンへ移ると滑って切り替わる |
| `GourdTabRail` | `.piyo-gourd` と `.piyo-gourd-button` | `data-orientation="horizontal"` で横、`bouncy` で弾む動き。矢印キー、Home、End で項目を移れる(Tab で止まるのは選んだ丸ボタンだけ) |
| `GourdRailPanel` | `.piyo-gourd-panel` の中に `.piyo-gourd` と `.piyo-gourd-panel-content` | `side`、`align`、`open`。表示中のタブをもう一度押すと閉じる |
| `PiyoGenie` | 出し入れする要素と、吸い込まれる先のボタン | `open`、`surface`(カードの面を描くか)、`radius`。出し入れの間はボタンの上に描かない |

メニューの位置の名前:

| 名前 | 位置 |
|---|---|
| `start-corner` | ボタンの中心がメニューの始まりの角(左か上)に来る |
| `start-edge` | メニューとボタンの始まりの端をそろえる |
| `center` | 中央 |
| `end-edge` | 終わりの端をそろえる |
| `end-corner` | ボタンの中心が終わりの角に来る(既定) |

- メニューの項目(`.piyo-menu-item`)を押すと閉じる。閉じたくない項目には `data-piyo-keep-open`
- 外を押す、Esc、Tab で閉じる。矢印キー、Home、End で項目を移る
- 中身の大きさが変わると、メニューの大きさもなめらかに変わる
- 開いている間にボタンが動いたら(スクロールなど)閉じる

## 使い方(React)

```jsx
import { PiyoTheme, PiyoButton, PiyoChipGroup, PiyoChipItem, PiyoTabs, PiyoMenu, PiyoMenuItem } from 'piyoron/react';

<PiyoTheme preset="ocean">
  <PiyoButton tone="primary">確定</PiyoButton>
  <PiyoChipGroup type="single" value={sort} onValueChange={setSort}>
    <PiyoChipItem value="new">新しい順</PiyoChipItem>
  </PiyoChipGroup>
  <PiyoTabs items={['すべて', '未読']} value={tab} onValueChange={setTab} />
  <PiyoMenu trigger={<PiyoButton>ファイル</PiyoButton>} transition="genie">
    <PiyoMenuItem detail="Ctrl+N" onSelect={create}>新規作成</PiyoMenuItem>
  </PiyoMenu>
</PiyoTheme>
```

| 部品 | 中で使うもの |
|---|---|
| `PiyoTheme` | `preset` か `seed`(シード色)、`reduceMotion`。メニューはこの中に出す |
| `PiyoButton` / `PiyoCard` / `PiyoChip` | ふつうの `button` / `div`。円のボタンは `size`、`iconSize` |
| `PiyoChipGroup` / `PiyoChipItem` | Radix の ToggleGroup(矢印キーで移れる) |
| `PiyoTabs` / `PiyoTabPanel` | Radix の Tabs |
| `PiyoMenu` / `PiyoMenuGroup` / `PiyoMenuItem` / `PiyoMenuDivider` / `usePiyoMenu` | JS 版のメニュー。ボタンは `trigger` か `icon` と `label`。中身から閉じるときは `usePiyoMenu().close()` |
| `PiyoGourdTabs` / `PiyoGourdPanel` | JS 版のひょうたん。`value`(と `open`)を渡さなければ中で持つ(`defaultValue`、`defaultOpen`) |
| `PiyoGenie` | JS 版のジニー。`anchor` は吸い込まれる先(要素か ref) |

値は `value` と `onValueChange` で外から持つことも、`defaultValue` で中に任せることもできる。

React 版を使うときは、`react`、`react-dom`、`@radix-ui/react-tabs`、`@radix-ui/react-toggle-group` を使う側で入れる(どれも「なくてもよい依存」にしてあるので、CSS と JS だけなら要らない)。

## 変わる形の描き方

メニュー、ひょうたん、つまみ、パネル、ジニーは形が毎コマ変わる。CSS の角丸では描けないので、JS が形を SVG のパスにして、層ごとのマスクに使う(`web/src/surface.js`)。

- マスクは、部品ごとにページの中に置いた小さな `<svg>` の `<mask>` に書き、層から `mask: url(#id)` で参照する。毎コマ書き換えるのはマスクの中身だけ。画像(data URL)を毎コマ作り直す方法より、ずっと軽い(ひょうたん単体の4本を動かしたとき、ブラウザの処理時間が約 4,000ms から約 720ms に減り、1秒に 60 コマを保つ)
- 外の影の層は、外の影の変数(`--piyo-s-shadow-filter`、今はメニューだけ)があるときだけ作る
- 線の太さは最初に1回だけ読む。パネルとジニーは、要素の大きさを書いた直後に読み直さない(毎コマ配置の計算が走らないように)

| 層 | 中身 | マスク |
|---|---|---|
| 外の影 | `filter: drop-shadow()` | 形の外だけ(影のもとは縁を少し削り、輪郭に黒が漏れないようにする) |
| 塗り | `--piyo-s-fill`、後ろのぼかし(メニュー) | 形の中(くり抜きを除く) |
| 線状の影 | `--piyo-s-ring-color` をマスクで線にしてから、ずらしてぼかす | 輪郭の真ん中の線(Android 版と同じ) |
| 枠 | `--piyo-s-edge` | 形の内側の線(Android 版と同じ。くり抜きの縁にも引く) |

- 形が重なる所(ひょうたん + 継ぎ目 + フィレット + パネル)は、塗りを合わせ、線は形ごとに引いて、ほかの形を縁から線の太さだけ縮めた範囲では消す。Android 版は形を1本の輪郭に結合して線を引く(`Path.op`)が、ブラウザには形の結合がないため
  - 辺がちょうど重なる所(パネルの上の辺と継ぎ目の上の辺など、合わせた形の外周)の線は、お互いに消し合わずに残る
  - 継ぎ目とフィレットは、隣の形に 3px 食い込ませる。内側の境の線が確実に消える(外形は変わらない)
  - 幅や高さがほぼ 0 の継ぎ目とフィレットは描かない
- ジニーのカードは、動いている間だけ、面を描く範囲を吸い込まれる先のボタンまで広げる(ボタンへ向かう形が途中で切れないように)
- ぼかしは、マスクで線にしたあとにかける(CSS は filter をマスクより先にかけるので、層を2重にする)
- 色は CSS 変数のまま使うので、スタイルや色を変えても JS は変わらない
- 形の計算(ひょうたん、継ぎ目、フィレット、ジニー、メニューの置き場所)は Android 版と同じ式(`web/src/geometry.js`)

## Android 版との違い

| 項目 | Web | 理由 |
|---|---|---|
| メニューの外の影 | `drop-shadow` を2つ重ねる(広がりの指定がないので、ぼかしを少し小さくした) | くり抜いた形に沿わせるため。box-shadow は四角にしか付かない |
| 形が変わる面の内側の影 | 上下の縁からのグラデーションで近い見た目にする | 形に沿った内側の影は CSS で描けない |
| 開閉中のメニューの後ろのぼかし | 薄くしている間は効かないことがある | 親を半透明にすると、ブラウザが後ろのぼかしを切る |
| 選ぶボタンとボタンの文字の行の高さ | 1(文字の大きさと同じ) | Android は書体の行の高さで決まり、端末で変わる。近い値に固定した |
| 1文字の選ぶボタンの横の間隔 | 押せる範囲の余白は縦だけ。幅が 48px より狭い選ぶボタンは、Android 版より横の間隔が数 px 狭い | 中身の幅に合わせた余白は CSS で付けられない |
| 書体 | トークンの書体(なければ OS の書体) | Android 版は端末の書体を使う |
| 押したときの動きの終わり | 動きが止まった瞬間に、文字と線が少し鮮明になり、表示の倍率によっては位置が 1px 未満ずれる(100% と 150% ではほぼ見えない) | ブラウザは、動きの間は描いたものを拡大して表示し、止まると最後の大きさで描き直す。Android は毎コマ描き直しても文字が跳ねない |
| 125% や 150% の表示での枠の太さ | 画素の途中に来る縁は、1割強薄く見えることがある(100% と 200% ではどこも同じ太さ) | ブラウザは、グラデーションの枠を画素にそろえて描けない。CSS の枠線(`border`)はそろうが、1色しか塗れない |
