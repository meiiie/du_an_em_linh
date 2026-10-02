import { Component } from '@angular/core';

/**
 * `/gv`: trang chủ giáo viên, hiện trong khung `KhungTrang` của route cha. Chưa có API lớp ở services/core nên hiện
 * trạng thái rỗng như v0 khi giáo viên chưa có lớp (heading «Chưa có lớp», `apps/web/app/gv/page.tsx`); có lớp thì
 * heading thành «Lớp <tên lớp>».
 */
@Component({
  selector: 'app-trang-giao-vien',
  template: `
    <header class="dau-trang">
      <h1>Chưa có lớp</h1>
    </header>
    <section class="trong">
      <p>Lớp và học sinh của lớp sẽ hiện ở đây khi lớp được tạo.</p>
    </section>
  `,
})
export class TrangGiaoVien {}
