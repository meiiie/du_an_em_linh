import { Component, computed, inject } from '@angular/core';
import { Phien } from '../../core/auth/phien';
import { KhungTrang } from '../../shared/layout/khung-trang';

/** `/hs`: khung trang học sinh. Heading «Chào <tên>» giữ như v0 (`apps/web/app/hs/page.tsx`). */
@Component({
  selector: 'app-trang-hoc-sinh',
  imports: [KhungTrang],
  template: `
    <app-khung-trang>
      <header class="dau-trang">
        <h1>Chào {{ ten() }}</h1>
      </header>
      <section class="trong" aria-labelledby="tieu-de-bai-giao">
        <h2 id="tieu-de-bai-giao">Bài được giao</h2>
        <p>Chưa có bài. Bài thầy cô giao sẽ hiện ở đây.</p>
      </section>
    </app-khung-trang>
  `,
})
export class TrangHocSinh {
  private readonly phien = inject(Phien);
  protected readonly ten = computed(() => this.phien.nguoiDung()?.displayName ?? '');
}
