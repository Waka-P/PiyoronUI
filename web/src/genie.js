// ジニー効果で出し入れする(閉じるとボタンに吸い込まれ、開くとそこから出てくる)

import { GenieFrame, genieAlpha, layoutRect, roundRectShape, shapeToPath } from './geometry.js';
import { animate, cssNumber, cssValue, easingFromCss, lerp, reduceMotion } from './motion.js';
import { PaintedSurface } from './surface.js';

// 吸い込まれる先の形(角の丸みは要素の border-radius。円のボタンなら円)
function anchorShape(anchor, dx, dy) {
  const r = layoutRect(anchor);
  const radius = Math.min(parseFloat(getComputedStyle(anchor).borderTopLeftRadius) || 0, r.width / 2, r.height / 2);
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
    const el = this.element;
    const box = el.getBoundingClientRect();
    const w = el.clientWidth;
    const h = el.clientHeight;
    const hidden = this.time >= 1;
    el.style.visibility = hidden ? 'hidden' : '';
    el.setAttribute('aria-hidden', String(!this.open));
    if (hidden) return;
    const p = this.easing(this.time);
    const a = layoutRect(this.anchor);
    const target = [(a.left + a.right) / 2 - box.left, (a.top + a.bottom) / 2 - box.top];
    const frame = p > 0 ? new GenieFrame(p, w, h, target, this.neck, this.radius) : null;
    const alpha = String(genieAlpha(p));
    if (this.surface) {
      this.surfaceElement.style.opacity = alpha;
      this.surface.draw([frame ? frame.placedShape() : roundRectShape(0, 0, w, h, this.radius)], w, h);
    }
    const c = this.content.style;
    if (frame) {
      const { transform, origin } = frame.transform();
      Object.assign(c, { clipPath: `path("${shapeToPath(frame.clipShape())}")`, transform, transformOrigin: origin, opacity: alpha });
    } else {
      Object.assign(c, { clipPath: '', transform: '', transformOrigin: '', opacity: '' });
    }
    // 出し入れの間は、ボタンの形の上に描かない
    const hole = p > 0 ? shapeToPath(anchorShape(this.anchor, box.left, box.top)) : '';
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
