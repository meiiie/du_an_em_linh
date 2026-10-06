import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Katex } from '../../shared/toan/katex';
import { BaiGiao, NAM_BUOC, TEN_MUC } from './bai-mau';

/** Một hàng bài: vòng trạng thái, tên và hàm số, mức, trạng thái bằng chữ; cả hàng dẫn tới phiếu làm bài. */
@Component({
  selector: 'app-hang-bai',
  imports: [Katex, RouterLink],
  template: `
    <a class="hang" [routerLink]="['/hs/luyen', bai().ma]" [attr.data-testid]="'bai-' + bai().ma">
      <svg class="vong" [class]="'vong ' + bai().trangThai.loai" viewBox="0 0 24 24" aria-hidden="true">
        <circle class="nen" cx="12" cy="12" r="9" />
        @if (bai().trangThai.loai === 'dat') {
          <circle class="day" cx="12" cy="12" r="9" />
          <path d="M8 12.5l2.6 2.6L16 9.6" />
        } @else if (phanDat() > 0) {
          <circle class="tien" cx="12" cy="12" r="9" pathLength="1" [attr.stroke-dasharray]="phanDat() + ' 1'" />
        }
      </svg>
      <span class="ten">
        <strong>Bài {{ so() }} · {{ bai().kyNang }}</strong>
        <span class="ham"><app-katex [latex]="bai().ham" /></span>
      </span>
      <span class="muc">{{ tenMuc() }}</span>
      <span class="tt" [class]="'tt ' + bai().trangThai.loai">{{ chuTrangThai() }}</span>
      <svg class="mui" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6" /></svg>
    </a>
  `,
  styleUrl: './hang-bai.css',
})
export class HangBai {
  readonly bai = input.required<BaiGiao>();
  readonly so = input.required<number>();

  protected readonly tenMuc = computed(() => TEN_MUC[this.bai().muc]);
  protected readonly phanDat = computed(() => {
    const t = this.bai().trangThai;
    return t.loai === 'dang-lam' ? t.buocDat / NAM_BUOC.length : 0;
  });
  protected readonly chuTrangThai = computed(() => {
    const t = this.bai().trangThai;
    if (t.loai === 'dat') return 'Đạt';
    if (t.loai === 'dang-lam') return `Đang làm ${t.buocDat}/${NAM_BUOC.length}`;
    return 'Chưa làm';
  });
}
