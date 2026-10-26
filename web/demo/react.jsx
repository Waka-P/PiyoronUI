// React 版のカタログ(Android のサンプルと同じ構成)

import { StrictMode, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  PiyoButton, PiyoCard, PiyoChip, PiyoChipGroup, PiyoChipItem, PiyoGenie, PiyoGourdPanel, PiyoGourdTabs, PiyoMenu, PiyoMenuDivider,
  PiyoMenuGroup, PiyoMenuItem, PiyoTabs, PiyoTheme, piyoPresets, seedFromHue, usePiyoMenu,
} from '../src/react/index.js';
import { ICONS, barFraction, cssColor, drawBackdrop, drawBiasSlider, drawHueBar } from './draw.js';

// 線のアイコン
function Icon({ name }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d={ICONS[name]} />
    </svg>
  );
}

// 小さな説明の文字
function Caption({ children }) {
  return <p className="caption">{children}</p>;
}

// 見出しつきのまとまり
function Section({ title, children }) {
  return (
    <section className="section">
      <h2 className="section-title">{title}</h2>
      <div className="section-body">{children}</div>
    </section>
  );
}

// 1つだけ選ぶ、選ぶボタンの並び(文字の並びから作る)
function Chips({ items, value, onChange, label = String, disabled }) {
  return (
    <PiyoChipGroup type="single" value={value} onValueChange={onChange} disabled={disabled}>
      {items.map((item) => <PiyoChipItem key={item} value={item}>{label(item)}</PiyoChipItem>)}
    </PiyoChipGroup>
  );
}

// 帯の canvas(draw で描き、押した位置の割合を onFraction に渡す。deps が変わると描き直す)
function Bar({ draw, onFraction, deps }) {
  const ref = useRef(null);
  useLayoutEffect(() => {
    const canvas = ref.current;
    draw(canvas);
    const observer = new ResizeObserver(() => draw(canvas));
    observer.observe(canvas);
    return () => observer.disconnect();
    // 値と色が変わったときだけ描き直す
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  // 押した位置で値を変える
  const pick = (e) => onFraction(barFraction(ref.current, e.clientX));
  return (
    <canvas
      ref={ref}
      className="bar"
      onPointerDown={(e) => {
        e.currentTarget.setPointerCapture(e.pointerId);
        pick(e);
      }}
      onPointerMove={(e) => e.currentTarget.hasPointerCapture(e.pointerId) && pick(e)}
    />
  );
}

// -1〜1 の値を選ぶ帯
function BiasSlider({ value, onChange, colorKey }) {
  return <Bar draw={(c) => drawBiasSlider(c, value)} onFraction={(f) => onChange(f * 2 - 1)} deps={[value, colorKey]} />;
}

// ---------- 色 ----------

const CUSTOM = 'カスタム';
const PRESET_NAMES = piyoPresets.map((p) => p[0].toUpperCase() + p.slice(1));
const ROLES = ['primary', 'primary-strong', 'content-selected', 'content-muted', 'content-default', 'background', 'danger'];

// 主な role の色見本と、シード色の色コード
function Swatches({ colorKey }) {
  const ref = useRef(null);
  const [seed, setSeed] = useState('');
  useEffect(() => {
    setSeed(cssColor('--piyo-color-seed', ref.current).toUpperCase());
  }, [colorKey]);
  return (
    <>
      <div className="swatches" ref={ref}>
        {ROLES.map((r) => <span key={r} className="swatch" style={{ background: `var(--piyo-color-${r})` }} />)}
        <span className="edge-strip">
          {Array.from({ length: 13 }, (_, i) => <span key={i} style={{ background: `var(--piyo-color-edge-${i})` }} />)}
        </span>
      </div>
      <Caption>seed {seed}</Caption>
    </>
  );
}

// 色のまとまり
function ColorSection({ palette, setPalette, hue, setHue, reduce, setReduce, colorKey }) {
  return (
    <Section title="色">
      <Chips items={[...PRESET_NAMES, CUSTOM]} value={palette} onChange={setPalette} />
      {palette === CUSTOM && <Bar draw={(c) => drawHueBar(c, hue, seedFromHue)} onFraction={(f) => setHue(f * 359.9)} deps={[hue]} />}
      <Swatches colorKey={colorKey} />
      <PiyoChip selected={reduce} onSelectedChange={setReduce}><Icon name="sparkle" />動きを減らす</PiyoChip>
    </Section>
  );
}

// ---------- 部品のページ ----------

// ボタンのまとまり
function ButtonSection() {
  const [count, setCount] = useState(0);
  // 押した回数を足す
  const add = () => setCount((c) => c + 1);
  return (
    <Section title="ボタン">
      <div className="row" style={{ gap: 16 }}>
        <PiyoButton icon={<Icon name="plus" />} aria-label="追加" size={64} iconSize={30} onClick={add} />
        <PiyoButton icon={<Icon name="plus" />} aria-label="追加" onClick={add} />
        <PiyoButton icon={<Icon name="check" />} aria-label="決定" tone="primary" onClick={add} />
        <PiyoButton icon={<Icon name="trash" />} aria-label="削除" tone="danger" onClick={add} />
      </div>
      <div className="flow" style={{ columnGap: 12 }}>
        <PiyoButton onClick={add}>保存</PiyoButton>
        <PiyoButton icon={<Icon name="star" />} onClick={add}>お気に入り</PiyoButton>
        <PiyoButton tone="primary" onClick={add}>確定</PiyoButton>
        <PiyoButton tone="danger" icon={<Icon name="trash" />} onClick={add}>削除</PiyoButton>
        <PiyoButton disabled>無効</PiyoButton>
        <PiyoButton tone="primary" disabled>無効</PiyoButton>
      </div>
      <Caption>押した回数: {count}</Caption>
    </Section>
  );
}

// 選ぶボタンのまとまり
function ChipSection() {
  const [sort, setSort] = useState('新しい順');
  const [size, setSize] = useState('中');
  const [notice, setNotice] = useState('オフ');
  const [extras, setExtras] = useState(['画像']);
  return (
    <Section title="選ぶボタン">
      <Caption>並び順</Caption>
      <Chips items={['新しい順', '古い順']} value={sort} onChange={setSort} />
      <Caption>文字の大きさ</Caption>
      <Chips items={['小', '中', '大']} value={size} onChange={setSize} />
      <Caption>通知</Caption>
      <Chips items={['オン', 'オフ']} value={notice} onChange={setNotice} />
      <Caption>複数選択</Caption>
      <PiyoChipGroup type="multiple" value={extras} onValueChange={setExtras}>
        {['文書', '画像', '動画'].map((k) => <PiyoChipItem key={k} value={k}>{k}</PiyoChipItem>)}
      </PiyoChipGroup>
      <Caption>無効</Caption>
      <Chips items={['選べない', '選択中']} value="選択中" onChange={() => {}} disabled />
    </Section>
  );
}

// タブのまとまり
function TabsSection() {
  const [kind, setKind] = useState('すべて');
  const [mode, setMode] = useState('一覧');
  const [view, setView] = useState('今日');
  return (
    <Section title="タブ">
      <Caption>長押ししたまま指を動かすと、選ぶ項目を移せる(離した所で選ぶ。外で離すと取り消し)</Caption>
      <PiyoTabs items={['すべて', '未読', '既読', '保存']} value={kind} onValueChange={setKind} style={{ width: '100%' }} />
      <PiyoTabs items={['カード', '一覧']} value={mode} onValueChange={setMode} style={{ width: '100%' }} />
      <PiyoTabs items={['今日', '今週', '今月']} value={view} onValueChange={setView} style={{ width: '70%' }} />
    </Section>
  );
}

// ボタンに対する位置の名前の選択肢
const ALIGN_NAMES = { 左の角: 'start-corner', 左端: 'start-edge', 中央: 'center', 右端: 'end-edge', 右の角: 'end-corner' };

// 開閉の動きの表示名
const TRANSITION_NAMES = { fade: 'フェード', scale: '拡大', genie: 'ジニー', none: 'なし' };

// 行の間隔で中身の高さが変わる、自由な中身
function DensityContent({ density, setDensity }) {
  const menu = usePiyoMenu();
  const spaces = { 詰める: 4, ふつう: 10, 広く: 24 };
  return (
    <div className="column" style={{ padding: 12, gap: spaces[density] }}>
      <span className="bold">行の間隔</span>
      <Chips items={['詰める', 'ふつう', '広く']} value={density} onChange={setDensity} />
      <div><PiyoButton onClick={menu.close}>閉じる</PiyoButton></div>
    </div>
  );
}

// メニューのまとまり
function MenuSection({ colorKey }) {
  const [last, setLast] = useState('まだ選んでいない');
  const [transition, setTransition] = useState('fade');
  const [alignName, setAlignName] = useState('右の角');
  const [bias, setBias] = useState(1);
  const [density, setDensity] = useState('ふつう');
  const align = alignName ? ALIGN_NAMES[alignName] : bias;
  // 項目を選んだとき
  const pick = (text) => () => setLast(text);
  return (
    <Section title="メニュー">
      <Caption>開いている間に、ほかのボタンへマウスを乗せる(スマホでは押す)と、滑って切り替わる</Caption>
      <Caption>項目を長押ししたまま動かすと、離した所の項目を選べる</Caption>
      <Caption>開閉の動き</Caption>
      <Chips items={Object.keys(TRANSITION_NAMES)} value={transition} onChange={setTransition} label={(t) => TRANSITION_NAMES[t]} />
      <Caption>ボタンに対する位置(名前で選ぶ)</Caption>
      <Chips items={Object.keys(ALIGN_NAMES)} value={alignName ?? ''} onChange={setAlignName} />
      <Caption>数値で決める: PiyoMenuAlignment({bias.toFixed(2)})(-1 でボタンの中心が始まりの角、1 で終わりの角)</Caption>
      <BiasSlider value={bias} colorKey={colorKey} onChange={(v) => {
        setBias(v);
        setAlignName(null);
      }} />
      <PiyoMenuGroup transition={transition} align={align}>
        <div className="row full" style={{ justifyContent: 'flex-end', gap: 8 }}>
          <PiyoMenu icon={<Icon name="copy" />} label="コピー">
            <PiyoMenuItem detail="本文と日付" onSelect={pick('テキストをコピー')}>テキスト</PiyoMenuItem>
            <PiyoMenuItem detail="名前なし" onSelect={pick('名前なしでコピー')}>テキスト</PiyoMenuItem>
            <PiyoMenuItem detail="共有用" onSelect={pick('リンクをコピー')}>リンク</PiyoMenuItem>
          </PiyoMenu>
          <PiyoMenu icon={<Icon name="download" />} label="ダウンロード">
            <PiyoMenuItem detail="印刷と同じ紙面" onSelect={pick('PDF')}>PDF</PiyoMenuItem>
            <PiyoMenuItem detail="元のデータ" onSelect={pick('JSON')}>JSON</PiyoMenuItem>
          </PiyoMenu>
          <PiyoMenu icon={<Icon name="gear" />} label="表示" minWidth={220}>
            <DensityContent density={density} setDensity={setDensity} />
          </PiyoMenu>
          <PiyoMenu icon={<Icon name="more" />} label="その他">
            <PiyoMenuItem icon={<Icon name="star" />} onSelect={pick('名前を変える')}>名前を変える</PiyoMenuItem>
            <PiyoMenuItem icon={<Icon name="copy" />} onSelect={pick('複製')}>複製</PiyoMenuItem>
            <PiyoMenuDivider />
            <PiyoMenuItem icon={<Icon name="trash" />} tone="danger" onSelect={pick('削除')}>削除</PiyoMenuItem>
          </PiyoMenu>
        </div>
      </PiyoMenuGroup>
      <Caption>選んだもの: {last}、行の間隔: {density}</Caption>
      <Caption>縦に並べたボタン(右に出す。位置は上の選択と同じ)</Caption>
      <PiyoMenuGroup transition={transition} side="right" align={align}>
        <div className="column" style={{ gap: 8, alignItems: 'flex-start' }}>
          <PiyoMenu icon={<Icon name="home" />} label="ホーム">
            <PiyoMenuItem onSelect={pick('最近の項目')}>最近の項目</PiyoMenuItem>
            <PiyoMenuItem icon={<Icon name="star" />} onSelect={pick('お気に入り')}>お気に入り</PiyoMenuItem>
          </PiyoMenu>
          <PiyoMenu icon={<Icon name="search" />} label="検索">
            <PiyoMenuItem onSelect={pick('すべてを検索')}>すべて</PiyoMenuItem>
            <PiyoMenuItem onSelect={pick('画像を検索')}>画像だけ</PiyoMenuItem>
            <PiyoMenuItem onSelect={pick('文書を検索')}>文書だけ</PiyoMenuItem>
          </PiyoMenu>
          <PiyoMenu icon={<Icon name="more" />} label="その他">
            <PiyoMenuItem icon={<Icon name="gear" />} onSelect={pick('設定')}>設定</PiyoMenuItem>
            <PiyoMenuDivider />
            <PiyoMenuItem icon={<Icon name="trash" />} tone="danger" onSelect={pick('削除')}>削除</PiyoMenuItem>
          </PiyoMenu>
        </div>
      </PiyoMenuGroup>
    </Section>
  );
}

// カードのまとまり
function CardSection() {
  const [opened, setOpened] = useState(0);
  return (
    <Section title="カード">
      <PiyoCard><span className="bold">お知らせ</span><Caption>中身を縦に並べる面。斜めのグラデーション、薄い枠、線状の影</Caption></PiyoCard>
      <PiyoCard onClick={() => setOpened((n) => n + 1)}><span className="bold">押せるカード</span><Caption>押すと縮む。押した回数: {opened}</Caption></PiyoCard>
    </Section>
  );
}

// ---------- エフェクトのページ ----------

// ひょうたんのタブ
const RAIL = [
  { value: 'home', label: 'ホーム', icon: <Icon name="home" /> },
  { value: 'search', label: '検索', icon: <Icon name="search" /> },
  { value: 'gear', label: '設定', icon: <Icon name="gear" /> },
];

// ひょうたんを置く側の選択肢
const SIDES = { 左: 'left', 右: 'right', 上: 'top', 下: 'bottom' };

// 寄せ方の選択肢(名前は辺の向きで変える。最後は間の値の例)
const VERTICAL_ALIGNS = ['上', '中央', '下', 'カスタム'];
const HORIZONTAL_ALIGNS = ['左', '真ん中', '右', 'カスタム'];
const ALIGN_VALUES = [-1, 0, 1];

// パネルの後ろの背景(格子と、ぼんやりした丸)
function Backdrop({ colorKey }) {
  const ref = useRef(null);
  useLayoutEffect(() => {
    const canvas = ref.current;
    drawBackdrop(canvas);
    const observer = new ResizeObserver(() => drawBackdrop(canvas));
    observer.observe(canvas);
    return () => observer.disconnect();
  }, [colorKey]);
  return <canvas ref={ref} />;
}

// パネルの中身(選んだタブごと)
function PanelContent({ index }) {
  const [kind, setKind] = useState('すべて');
  return (
    <div className="panel-content">
      <h3 className="panel-title">{RAIL[index].label}</h3>
      {index === 0 && (
        <>
          <Caption>最近の項目</Caption>
          {['項目 A', '項目 B', '項目 C'].map((t) => <span key={t}>{t}</span>)}
          <div><PiyoButton icon={<Icon name="plus" />}>追加</PiyoButton></div>
        </>
      )}
      {index === 1 && (
        <>
          <Caption>絞り込み</Caption>
          <Chips items={['すべて', '画像']} value={kind} onChange={setKind} />
          <div><PiyoButton tone="primary">検索</PiyoButton></div>
        </>
      )}
      {index === 2 && (
        <>
          <Caption>表示の設定</Caption>
          <span>文字の大きさ、通知、色</span>
        </>
      )}
    </div>
  );
}

// ひょうたんとパネルのまとまり
function RailPanelSection({ colorKey }) {
  const [side, setSide] = useState('左');
  const [align, setAlign] = useState(0);
  const [custom, setCustom] = useState(0.5);
  const [page, setPage] = useState('home');
  const [open, setOpen] = useState(true);
  const names = side === '左' || side === '右' ? VERTICAL_ALIGNS : HORIZONTAL_ALIGNS;
  return (
    <Section title="ひょうたんとパネル">
      <Caption>表示中のボタンをもう一度押すと、パネルが開閉する</Caption>
      <Chips items={Object.keys(SIDES)} value={side} onChange={setSide} />
      <Chips items={names} value={names[align]} onChange={(n) => setAlign(names.indexOf(n))} />
      {align === 3 && (
        <div className="column full" style={{ gap: 12 }}>
          <BiasSlider value={custom} onChange={setCustom} colorKey={colorKey} />
          <Caption>GourdAlignment({custom.toFixed(2)})</Caption>
        </div>
      )}
      <div className="rail-stage">
        <Backdrop colorKey={colorKey} />
        <PiyoGourdPanel
          items={RAIL}
          value={page}
          onValueChange={setPage}
          open={open}
          onOpenChange={setOpen}
          side={SIDES[side]}
          align={align === 3 ? custom : ALIGN_VALUES[align]}
        >
          <PanelContent index={RAIL.findIndex((r) => r.value === page)} />
        </PiyoGourdPanel>
      </div>
    </Section>
  );
}

// ひょうたん単体のまとまり
function RailSection() {
  const [value, setValue] = useState('home');
  return (
    <Section title="ひょうたん単体">
      <div className="row" style={{ gap: 24 }}>
        <PiyoGourdTabs items={RAIL} value={value} onValueChange={setValue} />
        <PiyoGourdTabs items={RAIL} value={value} onValueChange={setValue} bouncy />
        <Caption>選ぶと、印が伸びて移る。右は弾みあり(bouncy)</Caption>
      </div>
      <PiyoGourdTabs items={RAIL} value={value} onValueChange={setValue} orientation="horizontal" />
      <PiyoGourdTabs items={RAIL} value={value} onValueChange={setValue} orientation="horizontal" bouncy />
      <Caption>横向き(orientation = Horizontal)</Caption>
    </Section>
  );
}

// ジニーのまとまり
function GenieSection() {
  const [visible, setVisible] = useState(true);
  const anchor = useRef(null);
  return (
    <Section title="ジニー">
      <div className="row" style={{ gap: 16, alignItems: 'flex-start' }}>
        <PiyoButton
          ref={anchor}
          icon={<Icon name="sparkle" />}
          aria-label={visible ? 'カードをしまう' : 'カードを出す'}
          tone={visible ? 'default' : 'primary'}
          onClick={() => setVisible(!visible)}
        />
        <PiyoGenie open={visible} anchor={anchor} radius={20} className="genie-card">
          <div className="genie-body">
            <span className="bold">お知らせ</span>
            <Caption>左のボタンを押すと、このカードがボタンに吸い込まれる</Caption>
          </div>
        </PiyoGenie>
      </div>
    </Section>
  );
}

// ---------- 全体 ----------

// カタログ全体
function App() {
  const [palette, setPalette] = useState('Ocean');
  const [hue, setHue] = useState(265);
  const [reduce, setReduce] = useState(false);
  const [page, setPage] = useState(new URLSearchParams(location.search).get('page') === '1' ? 'エフェクト' : '部品');
  const custom = palette === CUSTOM;
  const colorKey = custom ? `hue-${hue}` : palette;
  return (
    <PiyoTheme
      className="theme-page"
      preset={custom ? undefined : palette.toLowerCase()}
      seed={custom ? seedFromHue(hue) : undefined}
      reduceMotion={reduce}
    >
      <main>
        <h1 className="title">Piyoron カタログ</h1>
        <ColorSection palette={palette} setPalette={setPalette} hue={hue} setHue={setHue} reduce={reduce} setReduce={setReduce} colorKey={colorKey} />
        <PiyoTabs items={['部品', 'エフェクト']} value={page} onValueChange={setPage} />
        <div className="page">
          {page === '部品' ? (
            <>
              <ButtonSection />
              <ChipSection />
              <TabsSection />
              <MenuSection colorKey={colorKey} />
              <CardSection />
            </>
          ) : (
            <>
              <RailPanelSection colorKey={colorKey} />
              <RailSection />
              <GenieSection />
            </>
          )}
        </div>
      </main>
    </PiyoTheme>
  );
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
