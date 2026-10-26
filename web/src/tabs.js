// タブ(つまみを選んだ項目へ滑らせる)。押す、矢印キー、長押ししたまま動かす、で選べる

import { dragSelect } from './drag-select.js';

// タブ。element は .piyo-tabs(中に .piyo-tabs-thumb と .piyo-tab を並べる)。選ぶと onSelect(番号)を呼ぶ
// selected を省くと、aria-selected="true" の項目(なければ先頭)を選ぶ
export class PiyoTabs {
  // 項目を集め、押したとき、キー、長押しの処理をつなぐ
  constructor(element, { selected, onSelect } = {}) {
    this.element = element;
    this.tabs = [...element.querySelectorAll('.piyo-tab')];
    this.onSelect = onSelect;
    element.setAttribute('role', 'tablist');
    element.style.setProperty('--piyo-tabs-n', this.tabs.length);
    this.abort = new AbortController();
    const signal = this.abort.signal;
    this.tabs.forEach((tab, i) => {
      tab.setAttribute('role', 'tab');
      tab.addEventListener('click', () => this.choose(i), { signal });
      tab.addEventListener('keydown', (e) => {
        const n = this.tabs.length;
        const next = { ArrowRight: i + 1, ArrowDown: i + 1, ArrowLeft: i - 1, ArrowUp: i - 1, Home: 0, End: n - 1 }[e.key];
        if (next === undefined) return;
        e.preventDefault();
        const j = (next + n) % n;
        this.tabs[j].focus();
        this.choose(j);
      }, { signal });
    });
    // 長押ししたまま動かすと、つまみが指の下の項目へ動き、離した所で選ぶ
    this.stopDrag = dragSelect(element, {
      items: () => this.tabs,
      onPreview: (i) => this.show(i ?? this.selected, true),
      onCommit: (i) => this.choose(i),
      onCancel: () => this.show(this.selected, false),
    });
    const initial = selected ?? Math.max(0, this.tabs.findIndex((t) => t.getAttribute('aria-selected') === 'true'));
    this.select(initial);
  }

  // 使う人が選んだとき(選んで、onSelect に知らせる)
  choose(index) {
    const changed = index !== this.selected;
    this.select(index);
    if (changed) this.onSelect?.(index);
  }

  // 選んだ項目を変える(つまみが滑る)
  select(index) {
    this.selected = index;
    this.tabs.forEach((t, i) => {
      t.setAttribute('aria-selected', String(i === index));
      t.tabIndex = i === index ? 0 : -1;
    });
    this.show(index, false);
  }

  // つまみを index の項目に置く。preview なら長押しで動かしている途中の印を付ける(文字の色もそちらに合わせる)
  show(index, preview) {
    this.element.style.setProperty('--piyo-tabs-i', index);
    this.tabs.forEach((t, i) => {
      if (preview && i === index) t.setAttribute('data-piyo-preview', '');
      else t.removeAttribute('data-piyo-preview');
    });
  }

  // 後片付け
  destroy() {
    this.abort.abort();
    this.stopDrag();
  }
}
