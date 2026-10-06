import { Component, input } from '@angular/core';

const CHU_KY = 600;
const BIEN_DO = 80;
const GIUA = 180;

/** Tổng riêng của chuỗi Fourier sóng vuông: (4/π) Σ sin(k·2πx/T)/k, k lẻ ≤ n. Vẽ 3 chu kỳ; CSS trôi đúng một chu kỳ (600) nên vòng lặp liền. */
function tongRieng(n: number): string {
  const diem: string[] = [];
  for (let x = 0; x <= 3 * CHU_KY; x += 4) {
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
 * màu Manim, trôi chậm. Trang trí, ẩn với trình đọc màn hình; `chay` false thì dừng (nút «Tạm dừng chuyển động»).
 */
@Component({
  selector: 'app-dai-bang',
  host: { 'aria-hidden': 'true', '[class.dung]': '!chay()' },
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
        <path [class]="'song ' + s.lop" [attr.d]="s.d" />
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
      animation: troi linear infinite;
    }
    .s1 {
      stroke: var(--m-blue);
      opacity: 0.55;
      animation-duration: 36s;
    }
    .s3 {
      stroke: var(--m-teal);
      opacity: 0.75;
      animation-duration: 36s;
    }
    .s9 {
      stroke: var(--m-yellow);
      animation-duration: 36s;
    }
    :host(.dung) .song {
      animation-play-state: paused;
    }
    @keyframes troi {
      to {
        transform: translateX(-600px);
      }
    }
  `,
})
export class DaiBang {
  readonly chay = input(true);
  protected readonly song = SONG;
  protected readonly giua = GIUA;
}
