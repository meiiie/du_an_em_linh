import { Component } from '@angular/core';
import { KhungTrang } from '../../shared/layout/khung-trang';

/**
 * `/gv`: khung trang giáo viên. Chưa có module lớp ở services/core nên hiện trạng thái rỗng như v0 khi giáo viên chưa
 * có lớp (heading «Chưa có lớp», `apps/web/app/gv/page.tsx`); có lớp thì heading thành «Lớp <tên lớp>».
 */
@Component({
  selector: 'app-trang-giao-vien',
  imports: [KhungTrang],
  template: `
    <app-khung-trang>
      <header class="dau-trang">
        <h1>Chưa có lớp</h1>
      </header>
      <section class="trong">
        <p>Lớp và học sinh của lớp sẽ hiện ở đây khi lớp được tạo.</p>
      </section>
    </app-khung-trang>
  `,
})
export class TrangGiaoVien {}
