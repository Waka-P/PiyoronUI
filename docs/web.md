# Web 版

CSS だけで使える部品(ボタン、選ぶボタン、タブ、カード)と、JS が要る部品(メニュー、ひょうたん、ひょうたんとパネル)がある。React 版は、この CSS と JS を使う薄い部品。

## ファイル

| 場所 | 中身 |
|---|---|
| `web/tokens.css` | トークンの CSS 変数(生成物) |
| `web/piyoron.css` | 部品の CSS(土台 + スタイルごとのレシピ) |
| `web/src/` | JS(色、メニュー、ひょうたん、形と動きの計算) |
| `web/src/react/` | React 版 |
| `web/demo/` | カタログ(`index.html` は素の HTML、`react.html` は React) |
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
| `.piyo-btn` | ボタン。`.piyo-primary`、`.piyo-danger`、`.piyo-icon-btn`(円) | `:disabled` |
| `.piyo-chip` | 選ぶボタン。並びは `.piyo-chip-group` | `aria-pressed` / `aria-checked` / `data-selected` |
| `.piyo-tabs` | タブ。つまみは `.piyo-tabs-thumb`、項目は `.piyo-tab` | `--piyo-tabs-n`(数)、`--piyo-tabs-i`(選んだ番号)、`aria-selected` |
| `.piyo-card` | カード。押せるなら `button.piyo-card.piyo-pressable` | |
| `.piyo-pressable` | 押すと縮む動きだけを付ける | |

- スタイルは `data-piyoron-style`、プリセットの色は `data-piyoron-palette`(`ocean` など)で切り替える。どの要素に付けてもよく、中の部品に効く
- セレクタは `:where()` で包んである(詳細度 0)。使う側のクラス1つで上書きできる
- 端末の「動きを減らす」設定では、動きを止める

### Glass のレシピ

部品は、面の役割の変数を決めるだけ。塗り・枠・線状の影は共通の1つのレシピが描く。

| 変数 | 意味 |
|---|---|
| `--piyo-s-fill` | 塗り |
| `--piyo-s-shadow` | 外の影(box-shadow) |
| `--piyo-s-edge` / `--piyo-s-edge-opacity` | 枠(スイープのグラデーション)と濃さ |
| `--piyo-s-ring-color` / `-width` / `-blur` / `-x` / `-y` | 線状の影 |

枠は `::before` をマスクで縁だけにくり抜き、線状の影は `::after` をずらしてぼかす。

## 使い方(JS)

```js
import { setPiyoSeed, setPiyoPreset, PiyoMenu, PiyoMenuGroup, GourdTabRail, GourdRailPanel } from 'piyoron';

setPiyoSeed('#D6457C');            // シード色から全部の色を作る
setPiyoPreset('mint');             // 手で整えたプリセット

const group = new PiyoMenuGroup({ transition: 'genie', align: 'end-corner' });
new PiyoMenu(button, menuElement, { group });

new GourdTabRail(railElement, { selected: 0, onSelect: (i) => {} });
new GourdRailPanel(panelElement, { side: 'left', onSelect: (i) => {} });
```

| 部品 | 要素 | 主な指定 |
|---|---|---|
| `PiyoMenu` | ボタンと `.piyo-menu`(中身は自由) | `transition`(`fade` 既定、`scale`、`genie`、`none`)、`side`(`bottom`、`top`、`right`、`left`)、`align`(下の表か -1〜1 の数) |
| `PiyoMenuGroup` | なし | 中のメニューの既定。開いている間にほかのボタンへ移ると滑って切り替わる |
| `GourdTabRail` | `.piyo-gourd` と `.piyo-gourd-button` | `data-orientation="horizontal"` で横、`bouncy` で弾む動き |
| `GourdRailPanel` | `.piyo-gourd-panel` の中に `.piyo-gourd` と `.piyo-gourd-panel-content` | `side`、`align`、`open`。表示中のタブをもう一度押すと閉じる |

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
| `PiyoTheme` | `preset` か `seed`(シード色)。メニューはこの中に出す |
| `PiyoButton` / `PiyoCard` / `PiyoChip` | ふつうの `button` / `div` |
| `PiyoChipGroup` / `PiyoChipItem` | Radix の ToggleGroup(矢印キーで移れる) |
| `PiyoTabs` / `PiyoTabPanel` | Radix の Tabs |
| `PiyoMenu` / `PiyoMenuGroup` / `PiyoMenuItem` / `PiyoMenuDivider` / `usePiyoMenu` | JS 版のメニュー。中身から閉じるときは `usePiyoMenu().close()` |
| `PiyoGourdTabs` / `PiyoGourdPanel` | JS 版のひょうたん |

値は `value` と `onValueChange` で外から持つことも、`defaultValue` で中に任せることもできる。

## 変わる形の描き方

メニュー、ひょうたん、つまみ、パネルは形が毎コマ変わる。CSS の角丸では描けないので、JS が形を SVG のパスにして、層ごとのマスクに使う(`web/src/surface.js`)。

| 層 | 中身 | マスク |
|---|---|---|
| 外の影 | `filter: drop-shadow()` | 形の外だけ |
| 塗り | `--piyo-s-fill`、後ろのぼかし(メニュー) | 形の中(くり抜きを除く) |
| 線状の影 | `--piyo-s-ring-color` をずらしてぼかす | 形の外側の線 |
| 枠 | `--piyo-s-edge` | 形の内側の線(くり抜きの縁にも引く) |

- 形が重なる所(ひょうたん + 継ぎ目 + パネル)は、塗りを合わせ、線は合わせた形の外周だけに引く。Android 版の「線を太く描いてから塗りで消す」と同じ考え方
- 色は CSS 変数のまま使うので、スタイルや色を変えても JS は変わらない
- 形の計算(ひょうたん、継ぎ目、フィレット、ジニー、メニューの置き場所)は Android 版と同じ式(`web/src/geometry.js`)

## Android 版との違い

| 項目 | Web | 理由 |
|---|---|---|
| メニューの項目の高さ | マウスでは余白だけ、指で操作する画面(`pointer: coarse`)だけ最小 44px | マウスでは 44px は間延びする。参照実装の Web も余白だけ |
| メニューの外の影 | `drop-shadow` を2つ重ねる(広がりの指定がないので、ぼかしを少し小さくした) | くり抜いた形に沿わせるため。box-shadow は四角にしか付かない |
| 形が変わる面の内側の影 | 上下の縁からのグラデーションで近い見た目にする | 形に沿った内側の影は CSS で描けない |
| 開閉中のメニューの後ろのぼかし | 薄くしている間は効かないことがある | 親を半透明にすると、ブラウザが後ろのぼかしを切る |
