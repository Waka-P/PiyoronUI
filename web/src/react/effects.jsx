// 動きのある部品(メニュー、ひょうたん、ひょうたんとパネル)。動きと形は JS 版をそのまま使う

import { createContext, useContext, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { PiyoMenu as MenuCore, PiyoMenuGroup as MenuGroupCore } from '../menu.js';
import { GourdRailPanel, GourdTabRail } from '../gourd.js';
import { ThemeContext, cx } from './basic.jsx';

// ---------- メニュー ----------

// まとめて扱うメニューの組
const GroupContext = createContext(null);

// 開いているメニュー(中身から閉じるため)
const MenuContext = createContext(null);

// メニューの組。中のメニューは、開いている間にほかへ移ると滑って切り替わる。transition、side、align は組の既定
export function PiyoMenuGroup({ transition = 'fade', side = 'bottom', align = 'end-corner', children }) {
  const group = useMemo(() => new MenuGroupCore(), []);
  group.transition = transition;
  group.side = side;
  group.align = align;
  return <GroupContext.Provider value={group}>{children}</GroupContext.Provider>;
}

// 中身から、開いているメニューを閉じる
export function usePiyoMenu() {
  return useContext(MenuContext);
}

// メニュー。trigger は開くボタン、children は中身(自由に置ける)
// 組の中では transition、side、align を省くと組の既定になる
export function PiyoMenu({ trigger, transition, side, align, onOpenChange, className, style, children }) {
  const group = useContext(GroupContext);
  const theme = useContext(ThemeContext);
  const anchorRef = useRef(null);
  const menuRef = useRef(null);
  const [core, setCore] = useState(null);
  const changeRef = useRef(onOpenChange);
  changeRef.current = onOpenChange;
  useLayoutEffect(() => {
    const anchor = anchorRef.current.firstElementChild;
    const menu = new MenuCore(anchor, menuRef.current, { group: group ?? undefined, onOpenChange: (open) => changeRef.current?.(open) });
    setCore(menu);
    return () => menu.destroy();
  }, [group, theme]);
  useLayoutEffect(() => {
    if (!core) return;
    core.ownTransition = transition ?? (group ? undefined : 'fade');
    core.ownSide = side;
    core.ownAlign = align;
    if (core.shown) core.render();
  }, [core, transition, side, align, group]);
  const api = useMemo(() => ({ close: () => core?.close(), open: () => core?.open() }), [core]);
  return (
    <>
      <span ref={anchorRef} style={{ display: 'contents' }}>{trigger}</span>
      {createPortal(
        <div ref={menuRef} className={cx('piyo-menu', className)} style={style} hidden>
          <div className="piyo-menu-content">
            <div className="piyo-menu-body">
              <MenuContext.Provider value={api}>{children}</MenuContext.Provider>
            </div>
          </div>
        </div>,
        theme ?? document.body,
      )}
    </>
  );
}

// メニューの項目。押すとメニューを閉じて onSelect を呼ぶ。keepOpen なら閉じない。detail は右寄せの補足
export function PiyoMenuItem({ onSelect, keepOpen, tone, detail, icon, className, children, ...props }) {
  return (
    <button
      type="button"
      role="menuitem"
      tabIndex={-1}
      data-piyo-keep-open={keepOpen ? '' : undefined}
      className={cx('piyo-menu-item', tone === 'danger' && 'piyo-danger', className)}
      onClick={onSelect}
      {...props}
    >
      {icon}
      {children}
      {detail != null && <span className="piyo-menu-detail">{detail}</span>}
    </button>
  );
}

// メニューの区切り線
export function PiyoMenuDivider() {
  return <hr className="piyo-menu-divider" />;
}

// ---------- ひょうたん ----------

// ひょうたんの丸ボタンの並び(items は { value, icon, label })
function GourdButtons({ items }) {
  return items.map((item) => (
    <button key={item.value} type="button" className="piyo-gourd-button" aria-label={item.label}>
      {item.icon}
    </button>
  ));
}

// 選んだ項目の番号
function indexOf(items, value) {
  return Math.max(0, items.findIndex((i) => i.value === value));
}

// ひょうたん型のタブ。items は { value, icon, label } の並び。orientation は vertical か horizontal、bouncy で弾む動き
export function PiyoGourdTabs({ items, value, onValueChange, orientation = 'vertical', bouncy = false, className, ...props }) {
  const ref = useRef(null);
  const rail = useRef(null);
  const latest = useRef({ items, onValueChange });
  latest.current = { items, onValueChange };
  const index = indexOf(items, value);
  useLayoutEffect(() => {
    rail.current = new GourdTabRail(ref.current, {
      selected: index,
      bouncy,
      onSelect: (i) => latest.current.onValueChange?.(latest.current.items[i].value),
    });
    return () => rail.current.destroy();
    // 部品の数、向き、動きが変わったときだけ作り直す
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length, orientation, bouncy]);
  useEffect(() => {
    rail.current?.select(index);
  }, [index]);
  return (
    <div ref={ref} className={cx('piyo-gourd', className)} data-orientation={orientation} {...props}>
      <GourdButtons items={items} />
    </div>
  );
}

// ひょうたん型のタブとパネル。表示中のタブをもう一度押すとパネルが開閉する(ジニー効果)
// side はひょうたんを置く側(left、right、top、bottom)、align は辺のどこに寄せるか(-1〜1)。大きさは style や className で決める
export function PiyoGourdPanel({ items, value, onValueChange, open, onOpenChange, side = 'left', align = -1, bouncy = false, className, style, children }) {
  const ref = useRef(null);
  const panel = useRef(null);
  const latest = useRef({ items, onValueChange, onOpenChange });
  latest.current = { items, onValueChange, onOpenChange };
  const index = indexOf(items, value);
  useLayoutEffect(() => {
    panel.current = new GourdRailPanel(ref.current, {
      selected: index,
      open: open ?? true,
      side,
      align,
      bouncy,
      onSelect: (i) => latest.current.onValueChange?.(latest.current.items[i].value),
      onOpenChange: (o) => latest.current.onOpenChange?.(o),
    });
    return () => panel.current.destroy();
    // 部品の数、置き方、動きが変わったときだけ作り直す
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length, side, align, bouncy]);
  useEffect(() => {
    if (panel.current && panel.current.selected !== index) panel.current.select(index);
  }, [index]);
  useEffect(() => {
    if (open !== undefined) panel.current?.setOpen(open);
  }, [open]);
  return (
    <div ref={ref} className={cx('piyo-gourd-panel', className)} style={style}>
      <div className="piyo-gourd-panel-surface" />
      <div className="piyo-gourd">
        <GourdButtons items={items} />
      </div>
      <div className="piyo-gourd-panel-body">
        <div className="piyo-gourd-panel-content">{children}</div>
      </div>
    </div>
  );
}
