// React 版のカタログ

import { StrictMode, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  PiyoButton, PiyoCard, PiyoChipGroup, PiyoChipItem, PiyoGourdPanel, PiyoGourdTabs, PiyoMenu, PiyoMenuDivider, PiyoMenuGroup,
  PiyoMenuItem, PiyoTabPanel, PiyoTabs, PiyoTheme, piyoPresets, seedFromHue, usePiyoMenu,
} from '../src/react/index.js';

// 線のアイコン
function Icon({ d }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d={d} />
    </svg>
  );
}

// アイコンの形
const PATHS = {
  plus: 'M12 5v14M5 12h14',
  check: 'M5 12.5l5 5L19 7',
  trash: 'M4 7h16M9 7V4.5h6V7M6.5 7l1 12.5h9l1-12.5',
  home: 'M4 11l8-7 8 7v9h-5v-6H9v6H4z',
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4',
  user: 'M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM4 20c1-4 4-6 8-6s7 2 8 6',
  dots: 'M5 12h.01M12 12h.01M19 12h.01',
};

// ひょうたんの項目
const RAIL = [
  { value: 'home', label: 'ホーム', icon: <Icon d={PATHS.home} /> },
  { value: 'search', label: '検索', icon: <Icon d={PATHS.search} /> },
  { value: 'me', label: '自分', icon: <Icon d={PATHS.user} /> },
];

// 中身から閉じるメニューの中身
function FreeContent() {
  const menu = usePiyoMenu();
  return (
    <div style={{ padding: '8px 12px', maxWidth: 220 }}>
      <strong>自由な中身</strong>
      <p className="caption">項目でなくても置ける。閉じるのは usePiyoMenu().close()</p>
      <PiyoButton tone="primary" onClick={menu.close}>閉じる</PiyoButton>
    </div>
  );
}

// カタログ全体
function App() {
  const [preset, setPreset] = useState('ocean');
  const [hue, setHue] = useState(null);
  const [sort, setSort] = useState('new');
  const [kinds, setKinds] = useState(['image']);
  const [tab, setTab] = useState('all');
  const [transition, setTransition] = useState('fade');
  const [align, setAlign] = useState(1);
  const [rail, setRail] = useState('home');
  const [page, setPage] = useState('home');
  const [open, setOpen] = useState(true);
  const [recent, setRecent] = useState(false);
  return (
    <PiyoTheme preset={preset} seed={hue === null ? undefined : seedFromHue(hue)}>
      <main>
        <h1>Piyoron カタログ(React)</h1>

        <section>
          <h2>色</h2>
          <div className="stack">
            <PiyoChipGroup type="single" value={hue === null ? preset : ''} onValueChange={(v) => { setPreset(v); setHue(null); }}>
              {piyoPresets.map((name) => <PiyoChipItem key={name} value={name}>{name[0].toUpperCase() + name.slice(1)}</PiyoChipItem>)}
            </PiyoChipGroup>
            <label className="caption" htmlFor="hue">カスタム(色相)</label>
            <input id="hue" type="range" min="0" max="359" value={hue ?? 200} onChange={(e) => setHue(Number(e.target.value))} />
          </div>
        </section>

        <section>
          <h2>ボタン</h2>
          <div className="stack">
            <div className="row">
              <PiyoButton icon={<Icon d={PATHS.plus} />} aria-label="追加" />
              <PiyoButton tone="primary" icon={<Icon d={PATHS.check} />} aria-label="決定" />
              <PiyoButton tone="danger" icon={<Icon d={PATHS.trash} />} aria-label="削除" />
            </div>
            <div className="row">
              <PiyoButton>保存</PiyoButton>
              <PiyoButton tone="primary">確定</PiyoButton>
              <PiyoButton tone="danger" icon={<Icon d={PATHS.trash} />}>削除</PiyoButton>
              <PiyoButton disabled>無効</PiyoButton>
            </div>
          </div>
        </section>

        <section>
          <h2>選ぶボタン</h2>
          <div className="stack">
            <PiyoChipGroup type="single" value={sort} onValueChange={setSort}>
              <PiyoChipItem value="new">新しい順</PiyoChipItem>
              <PiyoChipItem value="old">古い順</PiyoChipItem>
            </PiyoChipGroup>
            <PiyoChipGroup type="multiple" value={kinds} onValueChange={setKinds}>
              <PiyoChipItem value="doc">文書</PiyoChipItem>
              <PiyoChipItem value="image">画像</PiyoChipItem>
              <PiyoChipItem value="video">動画</PiyoChipItem>
            </PiyoChipGroup>
          </div>
        </section>

        <section>
          <h2>タブ</h2>
          <PiyoTabs items={[{ value: 'all', label: 'すべて' }, { value: 'unread', label: '未読' }, { value: 'read', label: '既読' }]} value={tab} onValueChange={setTab}>
            <PiyoTabPanel value="all"><p className="caption">すべての中身</p></PiyoTabPanel>
            <PiyoTabPanel value="unread"><p className="caption">未読の中身</p></PiyoTabPanel>
            <PiyoTabPanel value="read"><p className="caption">既読の中身</p></PiyoTabPanel>
          </PiyoTabs>
        </section>

        <section>
          <h2>カード</h2>
          <div className="stack">
            <PiyoCard><strong>お知らせ</strong><span className="caption">中身を縦に並べる面</span></PiyoCard>
            <PiyoCard onClick={() => {}}><strong>押せるカード</strong><span className="caption">押すと縮む</span></PiyoCard>
          </div>
        </section>

        <section>
          <h2>メニュー</h2>
          <div className="stack">
            <PiyoChipGroup type="single" value={transition} onValueChange={setTransition}>
              {['fade', 'scale', 'genie', 'none'].map((t) => <PiyoChipItem key={t} value={t}>{t[0].toUpperCase() + t.slice(1)}</PiyoChipItem>)}
            </PiyoChipGroup>
            <p className="caption">ボタンに対する位置({align.toFixed(2)})</p>
            <input type="range" min="-1" max="1" step="0.01" value={align} onChange={(e) => setAlign(Number(e.target.value))} />
            <PiyoMenuGroup transition={transition} align={align}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <PiyoMenu trigger={<PiyoButton>ファイル</PiyoButton>}>
                  <PiyoMenuItem detail="Ctrl+N">新規作成</PiyoMenuItem>
                  <PiyoMenuItem detail="Ctrl+O">開く</PiyoMenuItem>
                  <PiyoMenuItem keepOpen onSelect={() => setRecent(!recent)}>{recent ? '最近のファイルを隠す' : '最近のファイルを表示'}</PiyoMenuItem>
                  {recent && <PiyoMenuItem>報告書.txt</PiyoMenuItem>}
                  {recent && <PiyoMenuItem>見積もり.csv</PiyoMenuItem>}
                  <PiyoMenuDivider />
                  <PiyoMenuItem tone="danger">削除</PiyoMenuItem>
                </PiyoMenu>
                <PiyoMenu trigger={<PiyoButton>表示</PiyoButton>}>
                  <PiyoMenuItem>拡大</PiyoMenuItem>
                  <PiyoMenuItem>縮小</PiyoMenuItem>
                  <PiyoMenuItem disabled>全画面</PiyoMenuItem>
                </PiyoMenu>
                <PiyoMenu trigger={<PiyoButton icon={<Icon d={PATHS.dots} />} aria-label="その他" />}>
                  <FreeContent />
                </PiyoMenu>
              </div>
            </PiyoMenuGroup>
          </div>
        </section>

        <section>
          <h2>ひょうたん</h2>
          <div className="row" style={{ alignItems: 'flex-start', gap: 24 }}>
            <PiyoGourdTabs items={RAIL} value={rail} onValueChange={setRail} />
            <PiyoGourdTabs items={RAIL} value={rail} onValueChange={setRail} orientation="horizontal" bouncy />
          </div>
        </section>

        <section>
          <h2>ひょうたんとパネル</h2>
          <PiyoGourdPanel items={RAIL} value={page} onValueChange={setPage} open={open} onOpenChange={setOpen} style={{ height: 240 }}>
            <div style={{ padding: 16 }}>
              <strong>{RAIL.find((r) => r.value === page).label}</strong>
              <p className="caption">表示中のタブをもう一度押すと閉じる</p>
            </div>
          </PiyoGourdPanel>
        </section>
      </main>
    </PiyoTheme>
  );
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
