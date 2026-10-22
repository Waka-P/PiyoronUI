// カタログ(Android のサンプルと同じ構成)

import { piyoPresets, setPiyoPreset, setPiyoSeed, seedFromHue } from '../src/theme.js';
import { PiyoMenu, PiyoMenuGroup, PiyoMenuAlignment } from '../src/menu.js';
import { GourdRailPanel, GourdTabRail } from '../src/gourd.js';
import { PiyoGenie } from '../src/genie.js';
import { ICONS, barFraction, cssColor, drawBackdrop, drawBiasSlider, drawHueBar } from './draw.js';

// ---------- 小さな道具 ----------

// 要素を作る(attrs の on〜 は出来事、style は文字列、ほかは属性)
function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === undefined || v === null || v === false) continue;
    if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k === 'className') el.className = v;
    else el.setAttribute(k, v === true ? '' : v);
  }
  el.append(...children.flat().filter((c) => c !== null && c !== undefined && c !== false));
  return el;
}

// 線のアイコン
function icon(name) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('class', 'icon');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('aria-hidden', 'true');
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', ICONS[name]);
  svg.append(path);
  return svg;
}

// 小さな説明の文字
function caption(text) {
  return h('p', { className: 'caption' }, text);
}

// 見出しつきのまとまり
function section(title, ...children) {
  return h('section', { className: 'section' }, h('h2', { className: 'section-title' }, title), h('div', { className: 'section-body' }, ...children));
}

// ボタン(tone は primary か danger)
function button(text, { tone, iconName, disabled, onclick } = {}) {
  return h('button', { type: 'button', className: `piyo-btn${tone ? ` piyo-${tone}` : ''}`, disabled, onclick }, iconName && icon(iconName), text);
}

// 円のアイコンボタン(size と iconSize は px)
function iconButton(iconName, label, { tone, size, iconSize, onclick } = {}) {
  const style = [size && `--piyo-btn-size: ${size}px`, iconSize && `--piyo-btn-icon-size: ${iconSize}px`].filter(Boolean).join('; ');
  return h('button', { type: 'button', className: `piyo-btn piyo-icon-btn${tone ? ` piyo-${tone}` : ''}`, 'aria-label': label, style: style || undefined, onclick }, icon(iconName));
}

// 1つだけ選ぶ、選ぶボタンの並び。選ぶと onSelect(値) を呼ぶ
function chipGroup(items, selected, onSelect, { label = String, disabled = false } = {}) {
  const group = h('div', { className: 'piyo-chip-group', role: 'radiogroup' });
  for (const item of items) {
    const chip = h('button', {
      type: 'button',
      className: 'piyo-chip',
      role: 'radio',
      'aria-checked': String(item === selected),
      disabled,
      onclick: () => {
        group.querySelectorAll('.piyo-chip').forEach((c) => c.setAttribute('aria-checked', String(c === chip)));
        onSelect(item);
      },
    }, label(item));
    group.append(chip);
  }
  return group;
}

// いくつでも選べる、選ぶボタンの並び
function multiChipGroup(items, selected, onChange) {
  const on = new Set(selected);
  return h('div', { className: 'piyo-chip-group' }, items.map((item) => h('button', {
    type: 'button',
    className: 'piyo-chip',
    'aria-pressed': String(on.has(item)),
    onclick: (e) => {
      if (on.has(item)) on.delete(item);
      else on.add(item);
      e.currentTarget.setAttribute('aria-pressed', String(on.has(item)));
      onChange(on);
    },
  }, item)));
}

// タブ。width は横幅の割合
function tabs(items, selected, onSelect, width = 1) {
  const list = h('div', { className: 'piyo-tabs', role: 'tablist', style: `--piyo-tabs-n: ${items.length}; --piyo-tabs-i: ${selected}; width: ${width * 100}%` },
    h('span', { className: 'piyo-tabs-thumb', 'aria-hidden': 'true' }));
  items.forEach((label, i) => list.append(h('button', {
    type: 'button',
    className: 'piyo-tab',
    role: 'tab',
    'aria-selected': String(i === selected),
    onclick: () => {
      list.style.setProperty('--piyo-tabs-i', i);
      list.querySelectorAll('.piyo-tab').forEach((t, j) => t.setAttribute('aria-selected', String(j === i)));
      onSelect(i);
    },
  }, label)));
  return list;
}

// 帯の canvas(描く関数と、押した位置の割合を受ける関数を渡す)。repaint で描き直せる
function canvasBar(paint, onFraction) {
  const canvas = h('canvas', { className: 'bar' });
  // 押した位置で値を変えて描き直す
  const pick = (e) => {
    onFraction(barFraction(canvas, e.clientX));
    paint(canvas);
  };
  canvas.addEventListener('pointerdown', (e) => {
    canvas.setPointerCapture(e.pointerId);
    pick(e);
  });
  canvas.addEventListener('pointermove', (e) => {
    if (canvas.hasPointerCapture(e.pointerId)) pick(e);
  });
  new ResizeObserver(() => paint(canvas)).observe(canvas);
  canvas.repaint = () => paint(canvas);
  return canvas;
}

// 色相を選ぶ帯
function hueBar(hue, onHue) {
  let value = hue;
  return canvasBar((c) => drawHueBar(c, value, seedFromHue), (f) => {
    value = f * 359.9;
    onHue(value);
  });
}

// -1〜1 の値を選ぶ帯
function biasSlider(initial, onValue) {
  let value = initial;
  return canvasBar((c) => drawBiasSlider(c, value), (f) => {
    value = f * 2 - 1;
    onValue(value);
  });
}

// ---------- 状態 ----------

const CUSTOM = 'カスタム';
const PRESET_NAMES = piyoPresets.map((p) => p[0].toUpperCase() + p.slice(1));

const state = {
  palette: 'Ocean',
  hue: 265,
  reduce: false,
  page: 0,
};

// 作った部品の後片付け(ページを描き直すとき)
let cleanups = [];

// 色が変わったときに呼ぶ(ページを描き直すと外れる)
function onColors(fn) {
  document.addEventListener('piyo-colors', fn);
  cleanups.push(() => document.removeEventListener('piyo-colors', fn));
}

// 色を当てる
function applyColors() {
  if (state.palette === CUSTOM) setPiyoSeed(seedFromHue(state.hue));
  else setPiyoPreset(state.palette.toLowerCase());
  document.dispatchEvent(new Event('piyo-colors'));
}

// ---------- 色 ----------

// 主な role の色見本と、シード色の色コード
function swatches() {
  const roles = ['primary', 'primary-strong', 'content-selected', 'content-muted', 'content-default', 'background', 'danger'];
  const row = h('div', { className: 'swatches' });
  const seed = caption('');
  // 今の色で描き直す
  const paint = () => {
    row.replaceChildren(
      ...roles.map((r) => h('span', { className: 'swatch', style: `background: var(--piyo-color-${r})` })),
      h('span', { className: 'edge-strip' }, Array.from({ length: 13 }, (_, i) => h('span', { style: `background: var(--piyo-color-edge-${i})` }))),
    );
    seed.textContent = `seed ${cssColor('--piyo-color-seed').toUpperCase()}`;
  };
  paint();
  document.addEventListener('piyo-colors', paint);
  return [row, seed];
}

// 色のまとまり
function colorSection() {
  const hue = hueBar(state.hue, (v) => {
    state.hue = v;
    applyColors();
  });
  hue.hidden = state.palette !== CUSTOM;
  const reduce = h('button', {
    type: 'button',
    className: 'piyo-chip',
    'aria-pressed': String(state.reduce),
    onclick: (e) => {
      state.reduce = !state.reduce;
      e.currentTarget.setAttribute('aria-pressed', String(state.reduce));
      document.documentElement.dataset.piyoronReduceMotion = String(state.reduce);
    },
  }, icon('sparkle'), '動きを減らす');
  return section('色',
    chipGroup([...PRESET_NAMES, CUSTOM], state.palette, (p) => {
      state.palette = p;
      hue.hidden = p !== CUSTOM;
      applyColors();
      hue.repaint();
    }),
    hue,
    ...swatches(),
    h('div', {}, reduce));
}

// ---------- 部品のページ ----------

// ボタンのまとまり
function buttonSection() {
  let count = 0;
  const shown = caption('押した回数: 0');
  // 押した回数を足す
  const add = () => {
    count += 1;
    shown.textContent = `押した回数: ${count}`;
  };
  return section('ボタン',
    h('div', { className: 'row', style: 'gap: 16px' },
      iconButton('plus', '追加', { size: 64, iconSize: 30, onclick: add }),
      iconButton('plus', '追加', { onclick: add }),
      iconButton('check', '決定', { tone: 'primary', onclick: add }),
      iconButton('trash', '削除', { tone: 'danger', onclick: add })),
    h('div', { className: 'flow', style: 'column-gap: 12px' },
      button('保存', { onclick: add }),
      button('お気に入り', { iconName: 'star', onclick: add }),
      button('確定', { tone: 'primary', onclick: add }),
      button('削除', { tone: 'danger', iconName: 'trash', onclick: add }),
      button('無効', { disabled: true }),
      button('無効', { tone: 'primary', disabled: true })),
    shown);
}

// 選ぶボタンのまとまり
function chipSection() {
  return section('選ぶボタン',
    caption('並び順'),
    chipGroup(['新しい順', '古い順'], '新しい順', () => {}),
    caption('文字の大きさ'),
    chipGroup(['小', '中', '大'], '中', () => {}),
    caption('通知'),
    chipGroup(['オン', 'オフ'], 'オフ', () => {}),
    caption('複数選択'),
    multiChipGroup(['文書', '画像', '動画'], ['画像'], () => {}),
    caption('無効'),
    chipGroup(['選べない', '選択中'], '選択中', () => {}, { disabled: true }));
}

// タブのまとまり
function tabsSection() {
  return section('タブ',
    tabs(['すべて', '未読', '既読', '保存'], 0, () => {}),
    tabs(['カード', '一覧'], 1, () => {}),
    tabs(['今日', '今週', '今月'], 0, () => {}, 0.7));
}

// ボタンに対する位置の名前の選択肢
const ALIGN_NAMES = {
  左の角: PiyoMenuAlignment.startCorner,
  左端: PiyoMenuAlignment.startEdge,
  中央: PiyoMenuAlignment.center,
  右端: PiyoMenuAlignment.endEdge,
  右の角: PiyoMenuAlignment.endCorner,
};

// 開閉の動きの表示名
const TRANSITION_NAMES = { fade: 'フェード', scale: '拡大', genie: 'ジニー', none: 'なし' };

// メニューのボタンと中身を作り、組に入れる
function menu(group, iconName, label, content, { minWidth } = {}) {
  const trigger = iconButton(iconName, label);
  const panel = h('div', { className: 'piyo-menu', style: minWidth ? `--piyo-menu-min-width: ${minWidth}px` : undefined }, content);
  document.body.append(panel);
  const m = new PiyoMenu(trigger, panel, { group });
  cleanups.push(() => {
    m.destroy();
    panel.remove();
  });
  return { trigger, menu: m };
}

// メニューの項目
function item(text, onclick, { detail, iconName, danger } = {}) {
  return h('button', { type: 'button', className: `piyo-menu-item${danger ? ' piyo-danger' : ''}`, onclick },
    iconName && icon(iconName), text, detail && h('span', { className: 'piyo-menu-detail' }, detail));
}

// メニューの区切り線
function divider() {
  return h('hr', { className: 'piyo-menu-divider' });
}

// メニューのまとまり
function menuSection() {
  let last = 'まだ選んでいない';
  let density = 'ふつう';
  const result = caption('');
  // 選んだものの表示を変える
  const show = () => {
    result.textContent = `選んだもの: ${last}、行の間隔: ${density}`;
  };
  // 項目を選んだとき
  const pick = (text) => () => {
    last = text;
    show();
  };
  show();
  const row = new PiyoMenuGroup({ transition: 'fade', align: 'end-corner' });
  const column = new PiyoMenuGroup({ transition: 'fade', side: 'right', align: 'end-corner' });
  // 開閉の動きを変える
  const setTransition = (t) => {
    row.transition = t;
    column.transition = t;
  };
  // ボタンに対する位置を変える
  const setAlign = (a) => {
    row.align = a;
    column.align = a;
  };
  const biasCaption = caption('');
  // 数値の表示を変える
  const showBias = (v) => {
    biasCaption.textContent = `数値で決める: PiyoMenuAlignment(${v.toFixed(2)})(-1 でボタンの中心が始まりの角、1 で終わりの角)`;
  };
  showBias(1);
  onColors(() => slider.repaint());
  let alignChips = chipGroup(Object.keys(ALIGN_NAMES), '右の角', (name) => setAlign(ALIGN_NAMES[name]));
  // 数値で決めたら、名前の選択を外す
  const slider = biasSlider(1, (v) => {
    setAlign(v);
    showBias(v);
    alignChips.querySelectorAll('.piyo-chip').forEach((c) => c.setAttribute('aria-checked', 'false'));
  });

  // 行の間隔で中身の高さが変わる、自由な中身
  const spaces = { 詰める: 4, ふつう: 10, 広く: 24 };
  let gear = null;
  const body = h('div', { className: 'column', style: `padding: 12px; gap: ${spaces[density]}px` },
    h('span', { className: 'bold' }, '行の間隔'),
    chipGroup(['詰める', 'ふつう', '広く'], density, (d) => {
      density = d;
      body.style.gap = `${spaces[d]}px`;
      show();
    }),
    h('div', {}, button('閉じる', { onclick: () => gear.menu.close() })));

  const copy = menu(row, 'copy', 'コピー', [
    item('テキスト', pick('テキストをコピー'), { detail: '本文と日付' }),
    item('テキスト', pick('名前なしでコピー'), { detail: '名前なし' }),
    item('リンク', pick('リンクをコピー'), { detail: '共有用' }),
  ]);
  const download = menu(row, 'download', 'ダウンロード', [
    item('PDF', pick('PDF'), { detail: '印刷と同じ紙面' }),
    item('JSON', pick('JSON'), { detail: '元のデータ' }),
  ]);
  gear = menu(row, 'gear', '表示', [body], { minWidth: 220 });
  const more = menu(row, 'more', 'その他', [
    item('名前を変える', pick('名前を変える'), { iconName: 'star' }),
    item('複製', pick('複製'), { iconName: 'copy' }),
    divider(),
    item('削除', pick('削除'), { iconName: 'trash', danger: true }),
  ]);
  const home = menu(column, 'home', 'ホーム', [
    item('最近の項目', pick('最近の項目')),
    item('お気に入り', pick('お気に入り'), { iconName: 'star' }),
  ]);
  const search = menu(column, 'search', '検索', [
    item('すべて', pick('すべてを検索')),
    item('画像だけ', pick('画像を検索')),
    item('文書だけ', pick('文書を検索')),
  ]);
  const other = menu(column, 'more', 'その他', [
    item('設定', pick('設定'), { iconName: 'gear' }),
    divider(),
    item('削除', pick('削除'), { iconName: 'trash', danger: true }),
  ]);

  return section('メニュー',
    caption('開いている間に、ほかのボタンへマウスを乗せる(スマホでは押す)と、滑って切り替わる'),
    caption('開閉の動き'),
    chipGroup(Object.keys(TRANSITION_NAMES), 'fade', setTransition, { label: (t) => TRANSITION_NAMES[t] }),
    caption('ボタンに対する位置(名前で選ぶ)'),
    alignChips,
    biasCaption,
    slider,
    h('div', { className: 'row full', style: 'justify-content: flex-end; gap: 8px' }, copy.trigger, download.trigger, gear.trigger, more.trigger),
    result,
    caption('縦に並べたボタン(右に出す。位置は上の選択と同じ)'),
    h('div', { className: 'column', style: 'gap: 8px; align-items: flex-start' }, home.trigger, search.trigger, other.trigger));
}

// カードのまとまり
function cardSection() {
  let opened = 0;
  const shown = caption('押すと縮む。押した回数: 0');
  return section('カード',
    h('div', { className: 'piyo-card' }, h('span', { className: 'bold' }, 'お知らせ'), caption('中身を縦に並べる面。斜めのグラデーション、薄い枠、線状の影')),
    h('button', {
      type: 'button',
      className: 'piyo-card piyo-pressable',
      onclick: () => {
        opened += 1;
        shown.textContent = `押すと縮む。押した回数: ${opened}`;
      },
    }, h('span', { className: 'bold' }, '押せるカード'), shown));
}

// ---------- エフェクトのページ ----------

// ひょうたんのタブ
const RAIL_TABS = [['home', 'ホーム'], ['search', '検索'], ['gear', '設定']];

// ひょうたんの丸ボタンを並べた要素
function railElement(horizontal = false) {
  return h('div', { className: 'piyo-gourd', 'data-orientation': horizontal ? 'horizontal' : undefined },
    RAIL_TABS.map(([name, label]) => h('button', { type: 'button', className: 'piyo-gourd-button', 'aria-label': label }, icon(name))));
}

// ひょうたんを置く側の選択肢
const SIDES = { 左: 'left', 右: 'right', 上: 'top', 下: 'bottom' };

// 寄せ方の選択肢(名前は辺の向きで変える。最後は間の値の例)
const VERTICAL_ALIGNS = ['上', '中央', '下', 'カスタム'];
const HORIZONTAL_ALIGNS = ['左', '真ん中', '右', 'カスタム'];
const ALIGN_VALUES = [-1, 0, 1];

// パネルの後ろの背景(格子と、ぼんやりした丸)
function backdrop() {
  const canvas = h('canvas', {});
  new ResizeObserver(() => drawBackdrop(canvas)).observe(canvas);
  onColors(() => drawBackdrop(canvas));
  return canvas;
}

// パネルの中身(選んだタブごと)
function panelContent(index) {
  const title = h('h3', { className: 'panel-title' }, RAIL_TABS[index][1]);
  if (index === 0) {
    return h('div', { className: 'panel-content' }, title, caption('最近の項目'),
      ['項目 A', '項目 B', '項目 C'].map((t) => h('span', {}, t)),
      h('div', {}, button('追加', { iconName: 'plus' })));
  }
  if (index === 1) {
    return h('div', { className: 'panel-content' }, title, caption('絞り込み'),
      chipGroup(['すべて', '画像'], 'すべて', () => {}),
      h('div', {}, button('検索', { tone: 'primary' })));
  }
  return h('div', { className: 'panel-content' }, title, caption('表示の設定'), h('span', {}, '文字の大きさ、通知、色'));
}

// ひょうたんとパネルのまとまり
function railPanelSection() {
  let side = '左';
  let align = 0;
  let custom = 0.5;
  let index = 0;
  let open = true;
  let panel = null;
  const stage = h('div', { className: 'rail-stage' }, backdrop());
  // 今の選択で、ひょうたんとパネルを作り直す
  const build = () => {
    panel?.destroy();
    stage.querySelector('.piyo-gourd-panel')?.remove();
    const content = h('div', { className: 'piyo-gourd-panel-content' }, panelContent(index));
    const el = h('div', { className: 'piyo-gourd-panel' }, railElement(), content);
    stage.append(el);
    panel = new GourdRailPanel(el, {
      selected: index,
      open,
      side: SIDES[side],
      align: align === 3 ? custom : ALIGN_VALUES[align],
      onSelect: (i) => {
        index = i;
        panel.select(i);
        content.replaceChildren(panelContent(i));
      },
      onOpenChange: (o) => {
        open = o;
      },
    });
  };
  const customBox = h('div', { className: 'column full', style: 'gap: 12px' });
  const customCaption = caption('');
  const alignBox = h('div', {});
  // 寄せ方の選択肢を、辺の向きに合わせて作り直す
  const renderAligns = () => {
    const names = side === '左' || side === '右' ? VERTICAL_ALIGNS : HORIZONTAL_ALIGNS;
    alignBox.replaceChildren(chipGroup(names, names[align], (n) => {
      align = names.indexOf(n);
      customBox.hidden = align !== 3;
      build();
    }));
  };
  customCaption.textContent = `GourdAlignment(${custom.toFixed(2)})`;
  customBox.append(biasSlider(custom, (v) => {
    custom = v;
    customCaption.textContent = `GourdAlignment(${v.toFixed(2)})`;
    build();
  }), customCaption);
  customBox.hidden = true;
  renderAligns();
  queueMicrotask(build);
  cleanups.push(() => panel?.destroy());
  return section('ひょうたんとパネル',
    caption('表示中のボタンをもう一度押すと、パネルが開閉する'),
    chipGroup(Object.keys(SIDES), side, (s) => {
      side = s;
      renderAligns();
      build();
    }),
    alignBox,
    customBox,
    stage);
}

// ひょうたん単体のまとまり
function railSection() {
  let index = 0;
  const rails = [];
  const els = [railElement(), railElement(), railElement(true), railElement(true)];
  // 全部のひょうたんで同じ項目を選ぶ
  const select = (i) => {
    index = i;
    rails.forEach((r) => r.select(i));
  };
  queueMicrotask(() => {
    els.forEach((el, n) => rails.push(new GourdTabRail(el, { selected: index, bouncy: n % 2 === 1, onSelect: select })));
  });
  cleanups.push(() => rails.forEach((r) => r.destroy()));
  return section('ひょうたん単体',
    h('div', { className: 'row', style: 'gap: 24px' }, els[0], els[1], caption('選ぶと、印が伸びて移る。右は弾みあり(bouncy)')),
    h('div', {}, els[2]),
    h('div', {}, els[3]),
    caption('横向き(orientation = Horizontal)'));
}

// ジニーのまとまり
function genieSection() {
  let visible = true;
  const card = h('div', { className: 'genie-card' },
    h('div', { className: 'genie-body' }, h('span', { className: 'bold' }, 'お知らせ'), caption('左のボタンを押すと、このカードがボタンに吸い込まれる')));
  const trigger = iconButton('sparkle', 'カードをしまう', {
    onclick: () => {
      visible = !visible;
      genie.setOpen(visible);
      trigger.classList.toggle('piyo-primary', !visible);
      trigger.setAttribute('aria-label', visible ? 'カードをしまう' : 'カードを出す');
    },
  });
  let genie = null;
  queueMicrotask(() => {
    genie = new PiyoGenie(card, trigger, { radius: 20 });
  });
  cleanups.push(() => genie?.destroy());
  return section('ジニー', h('div', { className: 'row', style: 'gap: 16px; align-items: flex-start' }, trigger, card));
}

// ---------- 全体 ----------

// ページの中身を描く(題名、色、ページの切り替えは残す)
function renderPage() {
  cleanups.forEach((c) => c());
  cleanups = [];
  const sections = state.page === 0
    ? [buttonSection(), chipSection(), tabsSection(), menuSection(), cardSection()]
    : [railPanelSection(), railSection(), genieSection()];
  pageBox.replaceChildren(...sections);
}

// 全体を描く
function render() {
  document.getElementById('app').replaceChildren(
    h('h1', { className: 'title' }, 'Piyoron カタログ'),
    colorSection(),
    tabs(['部品', 'エフェクト'], state.page, (i) => {
      state.page = i;
      renderPage();
    }),
    pageBox,
  );
  renderPage();
}

// ページの中身の置き場所
const pageBox = h('div', { className: 'page' });

// URL でページと色を選べる(確認用。?page=1、?palette=Gold)
const query = new URLSearchParams(location.search);
state.page = Number(query.get('page') ?? 0);
state.palette = query.get('palette') ?? state.palette;
applyColors();
render();
document.body.dataset.ready = 'true';
