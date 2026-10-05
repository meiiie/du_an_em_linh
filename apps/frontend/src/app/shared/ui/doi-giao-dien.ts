import { Component, computed, inject } from '@angular/core';
import { GiaoDien } from '../../core/giao-dien';
import { BieuTuong } from './bieu-tuong';

/**
 * Nút bật / tắt giao diện tối: tên cố định, trạng thái ở `aria-pressed` (nút bật tắt, WAI-ARIA). Hình là chế độ đang
 * dùng như ảnh mô phỏng A / B: mặt trời khi sáng, trăng khi tối.
 */
@Component({
  selector: 'app-doi-giao-dien',
  imports: [BieuTuong],
  template: `
    <button
      type="button"
      class="nut-icon"
      data-testid="doi-giao-dien"
      aria-label="Giao diện tối"
      title="Giao diện tối"
      [attr.aria-pressed]="toi()"
      (click)="giaoDien.doi()"
    >
      <app-bieu-tuong [ten]="toi() ? 'moon' : 'sun'" />
    </button>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    /* Nổi như ô vuông có viền của ảnh mô phỏng A / B, khác nút menu trong suốt. */
    .nut-icon {
      border: 1px solid var(--line-strong);
      background: var(--raise);
      box-shadow: var(--shadow-sm);
    }

    .nut-icon:hover {
      background: var(--wash);
    }
  `,
})
export class DoiGiaoDien {
  protected readonly giaoDien = inject(GiaoDien);
  protected readonly toi = computed(() => this.giaoDien.cheDo() === 'dark');
}
