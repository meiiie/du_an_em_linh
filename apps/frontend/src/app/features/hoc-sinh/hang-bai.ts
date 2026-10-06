import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BaiCuaHocSinh, TEN_MUC } from '../../api/hoc-sinh';
import { Katex } from '../../shared/toan/katex';

const NGAY = new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' });

/** Chữ trạng thái của một bài, theo dữ liệu core trả (không tự suy đúng / sai). */
export function chuTrangThai(b: BaiCuaHocSinh): string {
  if (b.trangThai === 'DANG_LAM') return `Đang làm ${b.soBuocDat}/${b.soBuoc}`;
  if (b.trangThai === 'CHUA_LAM') return 'Chưa làm';
  if (b.ketQua === 'DAT') return 'Đạt';
  if (b.ketQua === 'KHONG_KIEM_DUOC') return 'Chờ thầy cô xem';
  return 'Chưa đạt';
}

/** Một hàng bài: vòng trạng thái, tên kỹ năng và hàm số, mức, hạn, trạng thái; cả hàng dẫn tới phiếu làm bài. */
@Component({
  selector: 'app-hang-bai',
  imports: [Katex, RouterLink],
  template: `
    <a class="hang" [routerLink]="['/hs/luyen', bai().maBai]" [attr.data-testid]="'bai-' + bai().maBai">
      <svg [class]="'vong ' + loaiVong()" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="nen" cx="12" cy="12" r="9" />
        @if (loaiVong() === 'dat') {
          <circle class="day" cx="12" cy="12" r="9" />
          <path d="M8 12.5l2.6 2.6L16 9.6" />
        } @else if (phanDat() > 0) {
          <circle class="tien" cx="12" cy="12" r="9" pathLength="1" [attr.stroke-dasharray]="phanDat() + ' 1'" />
        }
      </svg>
      <span class="ten">
        <strong>Bài {{ so() }} · {{ bai().tenKyNang }}</strong>
        <span class="ham"><app-katex [latex]="hamHang()" /></span>
      </span>
      <span class="muc">{{ tenMuc() }}@if (han(); as h) { · Hạn {{ h }}}</span>
      <span [class]="'tt ' + loaiVong()">{{ chu() }}</span>
      <svg class="mui" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6" /></svg>
    </a>
  `,
  styleUrl: './hang-bai.css',
})
export class HangBai {
  readonly bai = input.required<BaiCuaHocSinh>();
  readonly so = input.required<number>();

  protected readonly tenMuc = computed(() => TEN_MUC[this.bai().muc4]);
  protected readonly chu = computed(() => chuTrangThai(this.bai()));
  protected readonly han = computed(() => (this.bai().han ? NGAY.format(new Date(this.bai().han!)) : null));
  /** Trong hàng chỉ hàm số; phần «, y′ = …» của đề hiện đủ trên bảng ở phiếu làm bài. */
  protected readonly hamHang = computed(() => this.bai().deBaiLatex.split(String.raw`,\quad`)[0]);
  protected readonly loaiVong = computed(() => {
    const b = this.bai();
    if (b.trangThai === 'DA_NOP') return b.ketQua === 'DAT' ? 'dat' : b.ketQua === 'SAI' ? 'sai' : 'cho';
    return b.trangThai === 'DANG_LAM' ? 'dang-lam' : 'chua-lam';
  });
  protected readonly phanDat = computed(() => (this.bai().soBuoc ? this.bai().soBuocDat / this.bai().soBuoc : 0));
}
