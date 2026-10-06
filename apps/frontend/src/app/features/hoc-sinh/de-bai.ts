import { Component, inject } from '@angular/core';
import { BaiHocSinh, Muc4, TEN_MUC } from './bai-mau';
import { HangBai } from './hang-bai';

const THU_TU: readonly Muc4[] = ['NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO'];

/** `/hs/bai`: mọi bài được giao, nhóm theo bốn mức; mỗi hàng dẫn tới phiếu làm bài. */
@Component({
  selector: 'app-de-bai',
  imports: [HangBai],
  template: `
    <header class="dau-trang">
      <h1>Đề bài</h1>
      @if (du.laMau) {
        <p class="mau">Bài mẫu để xem trước. Bài của lớp sẽ hiện khi thầy cô giao.</p>
      }
    </header>
    @for (n of nhom; track n.muc) {
      <section [attr.aria-labelledby]="'muc-' + n.muc">
        <h2 [id]="'muc-' + n.muc" class="nhan-muc">{{ n.ten }}</h2>
        <ul class="danh-sach">
          @for (b of n.bai; track b.ma) {
            <li><app-hang-bai [bai]="b" [so]="n.so[$index]" /></li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--space-6);
    }
    h1 {
      margin: 0;
    }
    .mau {
      margin: var(--space-2) 0 0;
      color: var(--muted);
      font-size: 14px;
    }
    .danh-sach {
      margin: 0;
      padding: 0;
      overflow: hidden;
      list-style: none;
      border: 1px solid var(--line);
      border-radius: var(--radius-card);
      background: var(--raise);
    }
    .danh-sach li + li {
      border-top: 1px solid var(--line);
    }
  `,
})
export class DeBai {
  protected readonly du = inject(BaiHocSinh);
  protected readonly nhom = THU_TU.map((muc) => ({
    muc,
    ten: TEN_MUC[muc],
    bai: this.du.bai.filter((b) => b.muc === muc),
    so: this.du.bai.flatMap((b, i) => (b.muc === muc ? [i + 1] : [])),
  })).filter((n) => n.bai.length > 0);
}
