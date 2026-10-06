import { Component, computed, signal } from '@angular/core';
import { Katex } from '../../shared/toan/katex';

const X0 = -4.2;
const X1 = 4.2;
const Y0 = -1.4;
const Y1 = 1.4;
const W = 400;
const H = 260;
const X_MIN = -4;
const X_MAX = 4;
const KX = W / (X1 - X0);
const KY = H / (Y1 - Y0);
const NUA_TIEP_TUYEN = 80;

const sx = (x: number) => ((x - X0) / (X1 - X0)) * W;
const sy = (y: number) => H - ((y - Y0) / (Y1 - Y0)) * H;
// SP-10 (như v0): hàm minh họa không có trong ngân hàng bài (không phải đa thức), để trang công khai không lộ bài giao.
const f = (x: number) => (2 * x) / (x * x + 1);
const fp = (x: number) => (2 * (1 - x * x)) / ((x * x + 1) * (x * x + 1));

const dinhDang = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });
const so = (n: number) => dinhDang.format(Math.abs(n) < 0.005 ? 0 : n).replace('-', '−');

const DUONG_CONG = Array.from({ length: 121 }, (_, i) => {
  const t = X0 + ((X1 - X0) * i) / 120;
  return `${sx(t).toFixed(1)},${sy(f(t)).toFixed(1)}`;
}).join(' ');
const LUOI_DOC = [-4, -3, -2, -1, 1, 2, 3, 4].map(sx);
const LUOI_NGANG = [-1, 1].map(sy);

/**
 * Hình trên trang công khai, vẽ trên tấm bảng 3b1b: kéo điểm (chuột, chạm) hay thanh trượt (bàn phím) đổi `x`; tiếp
 * tuyến và `y′` tính lại từ `x`. Không gắn nhãn cực trị (docs/DESIGN.md).
 */
@Component({
  selector: 'app-minh-hoa-dao-ham',
  imports: [Katex],
  template: `
    <figure class="bang" id="hinh">
      <app-katex class="cong-thuc" khoi latex="y = \\dfrac{2x}{x^{2} + 1}" translate="no" />
      <svg
        [attr.viewBox]="'0 0 ' + w + ' ' + h"
        role="img"
        [attr.aria-label]="moTa()"
        (pointerdown)="batDau($event)"
        (pointermove)="keo($event)"
        (pointerup)="dangKeo.set(false)"
        (pointercancel)="dangKeo.set(false)"
      >
        @for (gx of luoiDoc; track gx) {
          <line class="luoi" [attr.x1]="gx" [attr.x2]="gx" y1="0" [attr.y2]="h" />
        }
        @for (gy of luoiNgang; track gy) {
          <line class="luoi" x1="0" [attr.x2]="w" [attr.y1]="gy" [attr.y2]="gy" />
        }
        <line class="truc" x1="0" [attr.x2]="w" [attr.y1]="truc.y" [attr.y2]="truc.y" />
        <line class="truc" [attr.x1]="truc.x" [attr.x2]="truc.x" y1="0" [attr.y2]="h" />
        <polyline class="duong" pathLength="1" [attr.points]="duongCong" />
        <line class="tiep-tuyen" [attr.x1]="tiepTuyen().x1" [attr.y1]="tiepTuyen().y1" [attr.x2]="tiepTuyen().x2" [attr.y2]="tiepTuyen().y2" />
        <circle class="diem" [class.dang-keo]="dangKeo()" [attr.cx]="diem().cx" [attr.cy]="diem().cy" r="6" />
      </svg>
      <div class="so-do" translate="no">
        <span><span class="mo">x =&nbsp;</span>{{ xHien() }}</span>
        <span><span class="mo">y′ =&nbsp;</span><span class="vang">{{ dhHien() }}</span></span>
      </div>
      <input
        type="range"
        [min]="xMin"
        [max]="xMax"
        step="0.1"
        [value]="x()"
        aria-label="Vị trí x trên đường cong"
        [attr.aria-valuetext]="'x = ' + xHien() + ', y′ = ' + dhHien()"
        (input)="x.set(+$any($event.target).value)"
      />
    </figure>
    <p class="chu-thich">Đạo hàm là hệ số góc của tiếp tuyến tại điểm đó. Kéo điểm để xem.</p>
  `,
  styleUrl: './minh-hoa-dao-ham.css',
})
export class MinhHoaDaoHam {
  protected readonly w = W;
  protected readonly h = H;
  protected readonly xMin = X_MIN;
  protected readonly xMax = X_MAX;
  protected readonly duongCong = DUONG_CONG;
  protected readonly luoiDoc = LUOI_DOC;
  protected readonly luoiNgang = LUOI_NGANG;
  protected readonly truc = { x: sx(0), y: sy(0) };

  readonly x = signal(2);
  protected readonly dangKeo = signal(false);

  protected readonly diem = computed(() => ({ cx: sx(this.x()), cy: sy(f(this.x())) }));
  protected readonly tiepTuyen = computed(() => {
    const x = this.x();
    const y = f(x);
    const k = fp(x);
    // Đoạn tiếp tuyến dài cố định trên màn, để chỗ dốc đoạn không vọt khỏi bảng.
    const dx = NUA_TIEP_TUYEN / Math.hypot(KX, KY * k);
    return { x1: sx(x - dx), y1: sy(y - k * dx), x2: sx(x + dx), y2: sy(y + k * dx) };
  });
  protected readonly xHien = computed(() => so(this.x()));
  protected readonly dhHien = computed(() => so(fp(this.x())));
  protected readonly moTa = computed(() => `Đồ thị y = 2x/(x² + 1), tiếp tuyến tại x = ${this.xHien()}, hệ số góc ${this.dhHien()}`);

  protected batDau(e: PointerEvent): void {
    (e.currentTarget as Element).setPointerCapture(e.pointerId);
    this.dangKeo.set(true);
    this.datTheoTro(e);
  }

  protected keo(e: PointerEvent): void {
    if (this.dangKeo()) this.datTheoTro(e);
  }

  private datTheoTro(e: PointerEvent): void {
    const khung = (e.currentTarget as Element).getBoundingClientRect();
    const x = X0 + ((e.clientX - khung.left) / khung.width) * (X1 - X0);
    this.x.set(Math.round(Math.min(X_MAX, Math.max(X_MIN, x)) * 10) / 10);
  }
}
