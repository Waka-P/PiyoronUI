// 基本の部品(テーマ、ボタン、選ぶボタン、タブ、カード)。見た目は piyoron.css のクラスに任せる

import { createContext, useLayoutEffect, useState } from 'react';
import * as Tabs from '@radix-ui/react-tabs';
import * as ToggleGroup from '@radix-ui/react-toggle-group';
import { setPiyoPreset, setPiyoSeed } from '../theme.js';
import '../press.js';

// クラス名をつなぐ(空は飛ばす)
export function cx(...names) {
  return names.filter(Boolean).join(' ');
}

// 制御されていてもいなくても使える値(value があればそれを、なければ内部の値を使う)
export function useControllable(value, defaultValue, onChange) {
  const [inner, setInner] = useState(defaultValue);
  const controlled = value !== undefined;
  // 値を変える
  const set = (next) => {
    if (!controlled) setInner(next);
    onChange?.(next);
  };
  return [controlled ? value : inner, set];
}

// テーマの要素(メニューなど、外に出す部品の置き場所)
export const ThemeContext = createContext(null);

// テーマ(中の部品にスタイルと色を渡す)。preset はプリセットの名前、seed はシード色、reduceMotion で動きを減らす
export function PiyoTheme({ style = 'glass', preset, seed, reduceMotion, className, children, ...props }) {
  const [element, setElement] = useState(null);
  useLayoutEffect(() => {
    if (element && seed) setPiyoSeed(seed, { element });
    else if (element) setPiyoPreset(preset ?? 'ocean', { element });
  }, [element, seed, preset]);
  return (
    <div ref={setElement} data-piyoron-style={style} data-piyoron-reduce-motion={reduceMotion ? 'true' : undefined} className={className} {...props}>
      <ThemeContext.Provider value={element}>{children}</ThemeContext.Provider>
    </div>
  );
}

// ボタン。tone は default、primary、danger。文字がなくアイコンだけなら円になる(aria-label を付ける)
// 円のときは size と iconSize(px)で大きさを変えられる
export function PiyoButton({ tone = 'default', icon, size, iconSize, className, style, children, type = 'button', ...props }) {
  const iconOnly = icon && (children === undefined || children === null);
  const sized = { ...(size && { '--piyo-btn-size': `${size}px` }), ...(iconSize && { '--piyo-btn-icon-size': `${iconSize}px` }), ...style };
  return (
    <button type={type} className={cx('piyo-btn', iconOnly && 'piyo-icon-btn', tone !== 'default' && `piyo-${tone}`, className)} style={sized} {...props}>
      {icon}
      {children}
    </button>
  );
}

// 1つで使う選ぶボタン(押すたびに選ぶ・外す)
export function PiyoChip({ selected, defaultSelected = false, onSelectedChange, className, children, onClick, ...props }) {
  const [on, setOn] = useControllable(selected, defaultSelected, onSelectedChange);
  return (
    <button
      type="button"
      aria-pressed={on}
      className={cx('piyo-chip', className)}
      onClick={(e) => {
        onClick?.(e);
        setOn(!on);
      }}
      {...props}
    >
      {children}
    </button>
  );
}

// 選ぶボタンの並び。type が single なら1つだけ、multiple ならいくつでも選べる。矢印キーで移れる
export function PiyoChipGroup({ type = 'single', value, defaultValue, onValueChange, className, children, ...props }) {
  const single = type === 'single';
  const [current, setCurrent] = useControllable(value, defaultValue ?? (single ? '' : []), onValueChange);
  return (
    <ToggleGroup.Root
      type={type}
      value={current}
      onValueChange={(next) => {
        if (single && !next) return;
        setCurrent(next);
      }}
      className={cx('piyo-chip-group', className)}
      {...props}
    >
      {children}
    </ToggleGroup.Root>
  );
}

// 並びの中の選ぶボタン
export function PiyoChipItem({ value, className, children, ...props }) {
  return (
    <ToggleGroup.Item value={value} className={cx('piyo-chip', className)} {...props}>
      {children}
    </ToggleGroup.Item>
  );
}

// 項目の指定(文字か { value, label })をそろえる
function toItem(item) {
  return typeof item === 'object' ? item : { value: String(item), label: item };
}

// タブ。items は項目の並び。つまみは選んだ項目へ滑る。中身は PiyoTabPanel で置く。className と style はタブの帯に付く
export function PiyoTabs({ items, value, defaultValue, onValueChange, className, style, children, ...props }) {
  const list = items.map(toItem);
  const [current, setCurrent] = useControllable(value, defaultValue ?? list[0]?.value, onValueChange);
  const index = Math.max(0, list.findIndex((i) => i.value === current));
  return (
    <Tabs.Root value={current} onValueChange={setCurrent} style={{ display: 'contents' }} {...props}>
      <Tabs.List className={cx('piyo-tabs', className)} style={{ '--piyo-tabs-n': list.length, '--piyo-tabs-i': index, ...style }}>
        <span className="piyo-tabs-thumb" aria-hidden="true" />
        {list.map((item) => (
          <Tabs.Trigger key={item.value} value={item.value} className="piyo-tab" disabled={item.disabled}>
            {item.label}
          </Tabs.Trigger>
        ))}
      </Tabs.List>
      {children}
    </Tabs.Root>
  );
}

// タブの中身(選んだ項目のときだけ出る)
export function PiyoTabPanel({ value, children, ...props }) {
  return (
    <Tabs.Content value={value} {...props}>
      {children}
    </Tabs.Content>
  );
}

// カード。onClick を渡すと押せるカード(ボタン)になる
export function PiyoCard({ onClick, className, children, ...props }) {
  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={cx('piyo-card', 'piyo-pressable', className)} {...props}>
        {children}
      </button>
    );
  }
  return (
    <div className={cx('piyo-card', className)} {...props}>
      {children}
    </div>
  );
}
