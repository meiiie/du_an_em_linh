import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';

const CHU_KY = 600;
const BIEN_DO = 80;
const GIUA = 180;

/** Tổng riêng của chuỗi Fourier sóng vuông: (4/π) Σ sin(k·2πx/T)/k, k lẻ ≤ n, trên 2 chu kỳ (bề ngang dải). */
function tongRieng(n: number): string {
  const diem: string[] = [];
  for (let x = 0; x <= 2 * CHU_KY; x += 4) {
    let y = 0;
    for (let k = 1; k <= n; k += 2) y += Math.sin((k * 2 * Math.PI * x) / CHU_KY) / k;
    diem.push(`${x},${(GIUA - (4 / Math.PI) * BIEN_DO * y).toFixed(1)}`);
  }
  return 'M' + diem.join('L');
}

const SONG = [
  { d: tongRieng(1), lop: 's1' },
  { d: tongRieng(3), lop: 's3' },
  { d: tongRieng(9), lop: 's9' },
];

/**
 * Dải bảng tối toàn chiều ngang, thay cho phim quang học của trang Wiii: ba tổng riêng Fourier tiến dần tới sóng vuông,
 * màu Manim. Lần đầu dải vào tầm nhìn thì vẽ dần từng đường như 3b1b (xong trong 2,4 s, không lặp), rồi đứng yên; máy đặt
 * giảm chuyển động hay không có IntersectionObserver thì hiện sẵn. Trang trí, ẩn với trình đọc màn hình.
 */
@Component({
  selector: 'app-dai-bang',
  host: { 'aria-hidden': 'true', '[class.cho]': "trangThai() === 'cho'", '[class.ve]': "trangThai() === 've'" },
  template: `
    <svg viewBox="0 0 1200 360" preserveAspectRatio="xMidYMid slice">
      <defs>
        <pattern id="luoi-bang" width="40" height="40" patternUnits="userSpaceOnUse">
          <path d="M40 0H0V40" />
        </pattern>
      </defs>
      <rect class="luoi" width="1200" height="360" fill="url(#luoi-bang)" />
      <line class="truc" x1="0" x2="1200" [attr.y1]="giua" [attr.y2]="giua" />
      @for (s of song; track s.lop) {
        <path [class]="'song ' + s.lop" pathLength="1" [attr.d]="s.d" />
      }
    </svg>
  `,
  styles: `
    :host {
      display: block;
      background: var(--board);
      color-scheme: dark;
    }
    svg {
      display: block;
      width: 100%;
      height: clamp(220px, 28vw, 380px);
    }
    pattern path {
      fill: none;
      stroke: var(--board-line);
      stroke-width: 1;
    }
    .truc {
      stroke: var(--board-rule);
      stroke-width: 1;
      opacity: 0.6;
    }
    .song {
      fill: none;
      stroke-width: 3;
      stroke-linecap: round;
      stroke-linejoin: round;
      stroke-dasharray: 1;
    }
    .s1 {
      stroke: var(--m-blue);
      opacity: 0.55;
    }
    .s3 {
      stroke: var(--m-teal);
      opacity: 0.75;
      animation-delay: 0.4s;
    }
    .s9 {
      stroke: var(--m-yellow);
      animation-delay: 0.8s;
    }
    :host(.cho) .song {
      stroke-dashoffset: 1;
    }
    :host(.ve) .song {
      animation-name: ve;
      animation-duration: 1.6s;
      animation-timing-function: var(--ease);
      animation-fill-mode: both;
    }
    @keyframes ve {
      from {
        stroke-dashoffset: 1;
      }
      to {
        stroke-dashoffset: 0;
      }
    }
  `,
})
export class DaiBang {
  protected readonly song = SONG;
  protected readonly giua = GIUA;
  /** `tinh`: hiện sẵn; `cho`: ẩn nét, đợi vào tầm nhìn; `ve`: đang / đã vẽ. */
  protected readonly trangThai = signal<'tinh' | 'cho' | 've'>('tinh');

  constructor() {
    const giam = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (giam || typeof IntersectionObserver !== 'function') return;
    this.trangThai.set('cho');
    const quanSat = new IntersectionObserver(
      (muc) => {
        if (!muc.some((m) => m.isIntersecting)) return;
        this.trangThai.set('ve');
        quanSat.disconnect();
      },
      { threshold: 0.35 },
    );
    quanSat.observe(inject<ElementRef<HTMLElement>>(ElementRef).nativeElement);
    inject(DestroyRef).onDestroy(() => quanSat.disconnect());
  }
}
