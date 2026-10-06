import { httpResource } from '@angular/common/http';
import { Component, computed } from '@angular/core';
import { API_HS, BaiCuaHocSinh, Muc4, TEN_MUC } from '../../api/hoc-sinh';
import { Button } from '../../shared/ui/button';
import { HangBai } from './hang-bai';

const THU_TU: readonly Muc4[] = ['NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO'];

/** `/hs/bai`: mọi bài được giao (`GET /api/hs/bai`), nhóm theo bốn mức; mỗi hàng dẫn tới phiếu làm bài. */
@Component({
  selector: 'app-de-bai',
  imports: [Button, HangBai],
  template: `
    <header class="dau-trang"><h1>Đề bài</h1></header>
    @if (ds.isLoading() && !ds.hasValue()) {
      <div class="dang-tai" aria-busy="true"><span class="sr-only">Đang tải đề bài</span></div>
    } @else if (ds.error()) {
      <div class="trong" role="alert">
        <p>Chưa tải được đề bài. Kiểm tra kết nối rồi thử lại.</p>
        <button appButton variant="secondary" type="button" (click)="ds.reload()">Thử lại</button>
      </div>
    } @else if (!nhom().length) {
      <div class="trong"><p>Chưa có bài. Bài thầy cô giao sẽ hiện ở đây.</p></div>
    } @else {
      @for (n of nhom(); track n.muc) {
        <section [attr.aria-labelledby]="'muc-' + n.muc">
          <h2 [id]="'muc-' + n.muc" class="nhan-muc">{{ n.ten }}</h2>
          <ul class="danh-sach">
            @for (h of n.hang; track h.bai.maBai) {
              <li><app-hang-bai [bai]="h.bai" [so]="h.so" /></li>
            }
          </ul>
        </section>
      }
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
    .dang-tai {
      height: 220px;
      border-radius: var(--radius-card);
      background: var(--wash);
    }
    .trong .btn {
      margin-top: var(--space-3);
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
  protected readonly ds = httpResource<BaiCuaHocSinh[]>(() => API_HS.bai);
  protected readonly nhom = computed(() => {
    const bai = this.ds.value() ?? [];
    return THU_TU.map((muc) => ({
      muc,
      ten: TEN_MUC[muc],
      hang: bai.flatMap((b, i) => (b.muc4 === muc ? [{ bai: b, so: i + 1 }] : [])),
    })).filter((n) => n.hang.length > 0);
  });
}
