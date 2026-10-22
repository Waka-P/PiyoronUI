// ジニー効果で出し入れする(閉じるとボタンに吸い込まれ、開くとそこから出てくる)

import { GenieFrame, genieAlpha, layoutRect, moveShape, roundRectShape, shapeToPath } from './geometry.js';
import { animate, cssNumber, cssValue, easingFromCss, lerp, reduceMotion } from './motion.js';
import { PaintedSurface } from './surface.js';

// 吸い込まれる先の形(r は先の四角、corner は角の丸み。dx、dy だけずらす)
function anchorShape(r, corner, dx, dy) {
  const radius = Math.min(corner, r.width / 2, r.height / 2);
  return roundRectShape(r.left - dx, r.top - dy, r.width, r.height, radius);
}

// ジニーで出し入れする面。element の中身を、anchor(ボタンなど)へ吸い込む
// surface が true なら、面(カード)を変形に沿わせて描く。radius は角の丸み(省くとトークンの大きい丸み)
export class PiyoGenie {
  // 面と中身の層を用意する
  constructor(element, anchor, { open = true, surface = true, radius } = {}) {
    this.element = element;
    this.anchor = anchor;
    this.open = open;
    this.time = open ? 0 : 1;
    element.classList.add('piyo-genie');
    this.content = element.querySelector(':scope > .piyo-genie-content');
    if (!this.content) {
      this.content = document.createElement('div');
      this.content.className = 'piyo-genie-content';
      this.content.append(...element.childNodes);
      element.append(this.content);
    }
    if (surface) {
      this.surfaceElement = document.createElement('div');
      this.surfaceElement.className = 'piyo-genie-surface';
      element.prepend(this.surfaceElement);
      this.surface = new PaintedSurface(this.surfaceElement);
    }
    this.radius = radius ?? cssNumber('--piyo-radius-lg', element);
    this.duration = cssNumber('--piyo-genie-duration', element);
    this.easing = easingFromCss(cssValue('--piyo-genie-easing', element));
    this.neck = cssNumber('--piyo-genie-neck', element);
    this.observer = new ResizeObserver(() => this.render());
    this.observer.observe(element);
    this.render();
  }

  // 出すか、しまうか
  setOpen(open) {
    if (open === this.open) return;
    this.open = open;
    this.stop?.();
    const goal = open ? 0 : 1;
    if (reduceMotion(this.element)) {
      this.time = goal;
      this.render();
      return;
    }
    const start = this.time;
    this.stop = animate(this.duration * Math.abs(goal - start), (t) => {
      this.time = lerp(start, goal, t);
      this.render();
    });
  }

  // 出し入れを切り替える
  toggle() {
    this.setOpen(!this.open);
  }

  // 今の時間の形と中身の変形を描く
  render() {
    // 読むものは先にまとめて読む(書いたあとに読むと、毎コマ配置の計算が走る)
    const el = this.element;
    const box = el.getBoundingClientRect();
    const w = el.clientWidth;
    const h = el.clientHeight;
    const a = layoutRect(this.anchor);
    const corner = this.time > 0 && this.time < 1 ? parseFloat(getComputedStyle(this.anchor).borderTopLeftRadius) || 0 : 0;
    const hidden = this.time >= 1;
    el.style.visibility = hidden ? 'hidden' : '';
    el.setAttribute('aria-hidden', String(!this.open));
    if (hidden) return;
    const p = this.easing(this.time);
    const target = [(a.left + a.right) / 2 - box.left, (a.top + a.bottom) / 2 - box.top];
    const frame = p > 0 ? new GenieFrame(p, w, h, target, this.neck, this.radius) : null;
    const alpha = String(genieAlpha(p));
    if (this.surface) {
      // 動いている間は、面を描く範囲にカードと行き先のボタンの両方を含める(ボタンへ向かう形が途中で切れないように)
      // 止まっているときはカードの範囲だけ(塗りのグラデーションの範囲を変えない)
      const pad = frame ? 4 : 0;
      const x0 = frame ? Math.min(0, a.left - box.left - pad) : 0;
      const y0 = frame ? Math.min(0, a.top - box.top - pad) : 0;
      const x1 = frame ? Math.max(w, a.right - box.left + pad) : w;
      const y1 = frame ? Math.max(h, a.bottom - box.top + pad) : h;
      Object.assign(this.surfaceElement.style, { inset: 'auto', left: `${x0}px`, top: `${y0}px`, width: `${x1 - x0}px`, height: `${y1 - y0}px`, opacity: alpha });
      const shape = frame ? frame.placedShape() : roundRectShape(0, 0, w, h, this.radius);
      this.surface.draw([moveShape(shape, -x0, -y0)], x1 - x0, y1 - y0);
    }
    const c = this.content.style;
    if (frame) {
      const { transform, origin } = frame.transform();
      Object.assign(c, { clipPath: `path("${shapeToPath(frame.clipShape())}")`, transform, transformOrigin: origin, opacity: alpha });
    } else {
      Object.assign(c, { clipPath: '', transform: '', transformOrigin: '', opacity: '' });
    }
    // 出し入れの間は、ボタンの形の上に描かない
    const hole = p > 0 ? shapeToPath(anchorShape(a, corner, box.left, box.top)) : '';
    el.style.clipPath = hole ? `path(evenodd, "M-9999 -9999H9999V9999H-9999Z${hole}")` : '';
  }

  // 後片付け
  destroy() {
    this.stop?.();
    this.observer.disconnect();
    this.surface?.destroy();
    this.surfaceElement?.remove();
    this.element.style.clipPath = '';
    this.element.style.visibility = '';
  }
}
