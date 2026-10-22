// メニュー(ボタンに少し重ねて出し、重なった所をくり抜く。開いている間にほかのボタンへ移ると滑って切り替わる)

import { GenieFrame, genieAlpha, layoutRect, menuPosition, ovalShape, roundRectShape, shapeToPath } from './geometry.js';
import { animate, cssNumber, cssValue, easingFromCss, lerp, reduceMotion } from './motion.js';
import { PaintedSurface } from './surface.js';

// 名前で指定できる、ボタンに対する位置(角はボタンの中心がメニューの角に、端は端どうしをそろえる)
export const PiyoMenuAlignment = {
  startCorner: { bias: -1, edges: false },
  startEdge: { bias: -1, edges: true },
  center: { bias: 0, edges: false },
  endEdge: { bias: 1, edges: true },
  endCorner: { bias: 1, edges: false },
};

// 位置の指定(名前、-1〜1 の数、{ bias, edges })をそろえる
function toAlignment(value) {
  if (typeof value === 'number') return { bias: value, edges: false };
  if (typeof value === 'string') {
    const key = value.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
    if (!PiyoMenuAlignment[key]) throw new Error(`メニューの位置の名前がない: ${value}`);
    return PiyoMenuAlignment[key];
  }
  return value ?? PiyoMenuAlignment.endCorner;
}

// メニューの寸法と動きの値(トークンの CSS 変数から読む)
function menuTokens(element) {
  // 1つ読む
  const n = (name) => cssNumber(`--piyo-menu-${name}`, element);
  return {
    overlap: n('overlap'),
    margin: n('margin'),
    notchGap: n('notch-gap'),
    radius: n('radius'),
    openScale: n('open-scale'),
    openDuration: n('open-duration'),
    fadeDuration: n('fade-duration'),
    slideDuration: n('slide-duration'),
    resizeDuration: n('resize-duration'),
    easing: easingFromCss(cssValue('--piyo-menu-easing', element)),
    fadeEasing: easingFromCss(cssValue('--piyo-menu-fade-easing', element)),
    genieDuration: cssNumber('--piyo-genie-duration', element),
    genieEasing: easingFromCss(cssValue('--piyo-genie-easing', element)),
    neck: cssNumber('--piyo-genie-neck', element),
  };
}

// 面の影と線を描くための、部品の外の余白
const SURFACE_MARGIN = 48;

// 番号の付いていない要素に付ける id の通し番号
let nextId = 0;

// まとめて扱うメニュー。開いているのは1つで、開いている間にほかへ移ると滑って切り替わる
// transition は開閉の動き(fade、scale、genie、none)、side はボタンのどちら側に出すか、align はボタンに対する位置
export class PiyoMenuGroup {
  // 組の既定を決め、外を押したときと Esc の処理をつなぐ
  constructor({ transition = 'fade', side = 'bottom', align = 'end-corner' } = {}) {
    this.transition = transition;
    this.side = side;
    this.align = align;
    this.menus = [];
    this.current = null;
    this.onPointerDown = (e) => {
      const c = this.current;
      if (!c || c.element.contains(e.target) || this.menus.some((m) => m.anchor.contains(e.target))) return;
      this.close(c);
    };
    this.onKeyDown = (e) => {
      if (e.key === 'Escape' && this.current) {
        const c = this.current;
        this.close(c);
        c.anchor.focus();
      }
    };
  }

  // ボタンを押したとき(開いていれば閉じ、ほかが開いていれば滑って移る)
  toggle(menu, focusFirst = false) {
    if (this.current === menu) this.close(menu);
    else if (this.current) this.switchTo(menu, focusFirst);
    else this.open(menu, focusFirst);
  }

  // マウスを乗せたとき(ほかが開いていれば滑って移る)
  hover(menu) {
    if (this.current && this.current !== menu) this.switchTo(menu, false);
  }

  // 開く
  open(menu, focusFirst) {
    this.current = menu;
    this.listen(true);
    menu.show(false);
    if (focusFirst) menu.focusItem(0);
  }

  // 閉じる
  close(menu) {
    if (this.current === menu) {
      this.current = null;
      this.listen(false);
    }
    menu.hide(false);
  }

  // 開いているメニューから、別のメニューへ滑って移る
  switchTo(menu, focusFirst) {
    const prev = this.current;
    this.current = menu;
    menu.show(true, prev);
    prev.hide(true, menu);
    if (focusFirst) menu.focusItem(0);
  }

  // 外を押したときと Esc を聞く・やめる
  listen(on) {
    const method = on ? 'addEventListener' : 'removeEventListener';
    document[method]('pointerdown', this.onPointerDown, true);
    document[method]('keydown', this.onKeyDown);
  }
}

// メニュー。anchor は開くボタン、element は .piyo-menu(中身は自由に置ける)
// 開閉の動き、出す側、位置は、指定しなければ組の既定
// 項目(.piyo-menu-item)を押すと閉じる。閉じたくない項目には data-piyo-keep-open を付ける
export class PiyoMenu {
  // 中身を包み、面を用意し、ボタンとキー操作をつなぐ
  constructor(anchor, element, { group, transition, side, align, onOpenChange } = {}) {
    this.anchor = anchor;
    this.element = element;
    this.group = group ?? new PiyoMenuGroup();
    this.group.menus.push(this);
    this.ownTransition = transition;
    this.ownSide = side;
    this.ownAlign = align;
    this.onOpenChange = onOpenChange;
    this.wrap();
    this.surface = new PaintedSurface(element, { margin: SURFACE_MARGIN });
    this.k = menuTokens(element);
    this.shown = false;
    this.closing = false;
    this.time = 1;
    this.slide = null;
    this.box = null;
    this.pos = { x: 0, y: 0 };
    this.stops = {};
    element.hidden = true;
    this.setupAria();
    this.bind();
  }

  // 開閉の動き
  get transition() {
    return this.ownTransition ?? this.group.transition;
  }

  // 出す側
  get side() {
    return this.ownSide ?? this.group.side;
  }

  // ボタンに対する位置
  get align() {
    return toAlignment(this.ownAlign ?? this.group.align);
  }

  // 開閉の動きを変える
  set transition(value) {
    this.ownTransition = value;
  }

  // 出す側を変える
  set side(value) {
    this.ownSide = value;
    if (this.shown) this.render();
  }

  // ボタンに対する位置を変える
  set align(value) {
    this.ownAlign = value;
    if (this.shown) this.render();
  }

  // 開いているか
  get isOpen() {
    return this.group.current === this;
  }

  // 開く
  open() {
    if (!this.isOpen) this.group.toggle(this);
  }

  // 閉じる
  close() {
    if (this.isOpen) this.group.close(this);
  }

  // 中身を、大きさを測る箱と、くり抜く窓で包む
  wrap() {
    this.content = this.element.querySelector(':scope > .piyo-menu-content');
    if (!this.content) {
      this.content = document.createElement('div');
      this.content.className = 'piyo-menu-content';
      this.body = document.createElement('div');
      this.body.className = 'piyo-menu-body';
      this.body.append(...this.element.childNodes);
      this.content.append(this.body);
      this.element.append(this.content);
    } else {
      this.body = this.content.querySelector(':scope > .piyo-menu-body') ?? this.content;
    }
  }

  // 読み上げの役割をつける
  setupAria() {
    const el = this.element;
    if (!el.id) el.id = `piyo-menu-${++nextId}`;
    if (!el.hasAttribute('role')) el.setAttribute('role', 'menu');
    this.anchor.setAttribute('aria-haspopup', 'menu');
    this.anchor.setAttribute('aria-expanded', 'false');
    this.anchor.setAttribute('aria-controls', el.id);
    for (const item of this.items()) {
      if (!item.hasAttribute('role')) item.setAttribute('role', 'menuitem');
      item.tabIndex = -1;
    }
  }

  // 選べる項目の並び
  items() {
    return [...this.element.querySelectorAll('.piyo-menu-item')].filter((i) => !i.disabled && i.getAttribute('aria-disabled') !== 'true');
  }

  // i 番目の項目へフォーカスを移す(はみ出たら反対の端へ)
  focusItem(i) {
    const list = this.items();
    if (list.length) list[(i + list.length) % list.length].focus();
  }

  // ボタン、項目、キー、大きさの変化の処理をつなぐ(destroy でまとめて外す)
  bind() {
    this.abort = new AbortController();
    const signal = this.abort.signal;
    this.anchor.addEventListener('click', (e) => this.group.toggle(this, e.detail === 0), { signal });
    this.anchor.addEventListener('pointerenter', (e) => {
      if (e.pointerType === 'mouse') this.group.hover(this);
    }, { signal });
    this.anchor.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        if (!this.isOpen) this.group.toggle(this);
        this.focusItem(e.key === 'ArrowDown' ? 0 : -1);
      }
    }, { signal });
    this.element.addEventListener('click', (e) => {
      const item = e.target.closest('.piyo-menu-item');
      if (item && !item.hasAttribute('data-piyo-keep-open') && this.element.contains(item)) this.close();
    }, { signal });
    this.element.addEventListener('keydown', (e) => {
      const list = this.items();
      const i = list.indexOf(document.activeElement);
      const move = { ArrowDown: i + 1, ArrowUp: i - 1, Home: 0, End: -1 }[e.key];
      if (move !== undefined) {
        e.preventDefault();
        this.focusItem(move);
      } else if (e.key === 'Tab') {
        this.close();
      }
    }, { signal });
    this.observer = new ResizeObserver(() => this.resize());
    this.observer.observe(this.body);
    this.onViewport = () => {
      if (!this.shown) return;
      // 開いている間にボタンが動いた(スクロール、画面の大きさの変化)ら閉じる
      const a = this.anchorRect();
      const last = this.lastAnchor;
      if (this.isOpen && last && (Math.abs(a.left - last.left) > 1 || Math.abs(a.top - last.top) > 1)) this.close();
      else this.render();
    };
    window.addEventListener('resize', this.onViewport, { signal });
    window.addEventListener('scroll', this.onViewport, { signal, capture: true });
  }

  // 中身の大きさ(開閉の変形を含まない、元の大きさ)
  measure() {
    return { width: this.body.offsetWidth, height: this.body.offsetHeight };
  }

  // 中身の大きさが変わったら、メニューの大きさをなめらかに合わせる
  resize() {
    if (!this.shown || !this.box) return;
    const goal = this.measure();
    if (Math.abs(goal.width - this.box.width) < 0.5 && Math.abs(goal.height - this.box.height) < 0.5) return;
    const from = { ...this.box };
    this.run('resize', reduceMotion(this.element) ? 0 : this.k.resizeDuration, (t) => {
      const s = this.k.easing(t);
      this.box = { width: lerp(from.width, goal.width, s), height: lerp(from.height, goal.height, s) };
    });
  }

  // 名前ごとに1つ、動きを走らせる(同じ名前の前の動きは止める)。毎コマ描き直す
  run(name, duration, onFrame, onEnd) {
    this.stops[name]?.();
    this.stops[name] = animate(duration, (t) => {
      onFrame(t);
      this.render();
    }, () => {
      delete this.stops[name];
      onEnd?.();
      this.render();
    });
  }

  // 開閉にかける時間(動きの種類ごと)
  duration() {
    if (reduceMotion(this.element)) return 0;
    return { fade: this.k.fadeDuration, scale: this.k.openDuration, genie: this.k.genieDuration }[this.transition] ?? 0;
  }

  // 出す(slide なら from のメニューの位置から滑って入る)
  show(slide, from) {
    const wasShown = this.shown;
    this.shown = true;
    this.element.hidden = false;
    this.anchor.setAttribute('aria-expanded', 'true');
    this.box = this.measure();
    this.lastAnchor = null;
    this.place();
    this.onOpenChange?.(true);
    if (slide && !reduceMotion(this.element)) {
      this.stops.time?.();
      this.closing = false;
      this.time = 1;
      this.slide = { in: true, from: { ...from.pos }, t: 0 };
      this.run('slide', this.k.slideDuration, (t) => {
        this.slide.t = t;
      }, () => {
        this.slide = null;
      });
      return;
    }
    this.slide = null;
    const start = wasShown && this.closing ? 1 - this.time : 0;
    this.closing = false;
    this.time = start;
    this.run('time', this.duration() * (1 - start), (t) => {
      this.time = lerp(start, 1, t);
    });
  }

  // しまう(slide なら to のメニューの方へ滑って消える)
  hide(slide, to) {
    if (!this.shown) return;
    this.anchor.setAttribute('aria-expanded', 'false');
    this.onOpenChange?.(false);
    // 見えなくする
    const finish = () => {
      this.shown = false;
      this.slide = null;
      this.element.hidden = true;
    };
    if (slide && !reduceMotion(this.element)) {
      this.stops.time?.();
      this.time = 1;
      this.closing = false;
      this.slide = { in: false, to, t: 0 };
      this.run('slide', this.k.slideDuration, (t) => {
        this.slide.t = t;
      }, finish);
      return;
    }
    this.stops.slide?.();
    this.slide = null;
    const start = this.closing ? this.time : 1 - this.time;
    this.closing = true;
    this.time = start;
    this.run('time', this.duration() * (1 - start), (t) => {
      this.time = lerp(start, 1, t);
    }, finish);
  }

  // ボタンの四角(画面から見た位置)
  anchorRect() {
    return layoutRect(this.anchor);
  }

  // 置き場所を求める
  place() {
    const area = { width: document.documentElement.clientWidth, height: document.documentElement.clientHeight };
    this.pos = menuPosition(this.anchorRect(), this.box, area, this.k.overlap, this.k.margin, this.side, this.align);
  }

  // 今の状態で、置き場所、面の形、中身の変形を描く
  render() {
    if (!this.shown || !this.box) return;
    const k = this.k;
    this.place();
    const { width: w, height: h } = this.box;
    const a = this.anchorRect();
    if (this.isOpen) this.lastAnchor = { left: a.left, top: a.top };
    const target = [(a.left + a.right) / 2 - this.pos.x, (a.top + a.bottom) / 2 - this.pos.y];
    const g = k.notchGap;
    const notch = ovalShape(a.left - this.pos.x - g, a.top - this.pos.y - g, a.right - this.pos.x + g, a.bottom - this.pos.y + g);
    const t = this.time;
    let alpha = 1;
    let scale = 1;
    let frame = null;
    if (this.transition === 'genie') {
      const p = k.genieEasing(this.closing ? t : 1 - t);
      if (p > 0) frame = new GenieFrame(p, w, h, target, k.neck, k.radius);
      alpha = genieAlpha(p);
    } else if (this.transition === 'scale') {
      const moved = k.easing(t);
      const faded = k.fadeEasing(Math.min(1, (t * k.openDuration) / k.fadeDuration));
      scale = this.closing ? lerp(1, k.openScale, moved) : lerp(k.openScale, 1, moved);
      alpha = this.closing ? 1 - faded : faded;
    } else if (this.transition === 'fade') {
      const faded = k.fadeEasing(t);
      alpha = this.closing ? 1 - faded : faded;
    }
    let dx = 0;
    let dy = 0;
    if (this.slide) {
      const s = k.easing(this.slide.t);
      if (this.slide.in) {
        dx = (this.slide.from.x - this.pos.x) * (1 - s);
        dy = (this.slide.from.y - this.pos.y) * (1 - s);
        alpha *= s;
      } else {
        dx = (this.slide.to.pos.x - this.pos.x) * s;
        dy = (this.slide.to.pos.y - this.pos.y) * s;
        alpha *= 1 - s;
      }
    }
    const style = this.element.style;
    Object.assign(style, {
      left: `${this.pos.x}px`,
      top: `${this.pos.y}px`,
      width: `${w}px`,
      height: `${h}px`,
      opacity: alpha >= 1 ? '' : String(alpha),
      transform: dx || dy || scale !== 1 ? `translate(${dx}px, ${dy}px) scale(${scale})` : '',
      transformOrigin: `${target[0]}px ${target[1]}px`,
    });
    const shape = frame ? frame.placedShape() : roundRectShape(0, 0, w, h, k.radius);
    this.surface.draw([shape], w, h, [notch]);
    const c = this.content.style;
    if (frame) {
      const { transform, origin } = frame.transform();
      Object.assign(c, { clipPath: `path("${shapeToPath(frame.clipShape())}")`, transform, transformOrigin: origin });
    } else {
      Object.assign(c, { clipPath: `path(evenodd, "M-9999 -9999H9999V9999H-9999Z${shapeToPath(notch)}")`, transform: '', transformOrigin: '' });
    }
  }

  // 後片付け
  destroy() {
    if (this.isOpen) {
      this.group.current = null;
      this.group.listen(false);
    }
    Object.values(this.stops).forEach((stop) => stop());
    this.element.hidden = true;
    this.anchor.setAttribute('aria-expanded', 'false');
    this.observer.disconnect();
    this.abort.abort();
    this.surface.destroy();
    this.group.menus = this.group.menus.filter((m) => m !== this);
  }
}
