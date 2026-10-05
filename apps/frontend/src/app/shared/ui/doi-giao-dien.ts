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
      class="nut"
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

    .nut {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      inline-size: var(--target);
      block-size: var(--target);
      padding: 0;
      border: 1px solid var(--line);
      border-radius: 10px;
      background: var(--raise);
      box-shadow: var(--shadow-sm);
      color: var(--ink-2);
      cursor: pointer;
      transition: background-color 150ms var(--ease);
    }

    .nut:hover {
      background: var(--wash);
      color: var(--ink);
    }

    app-bieu-tuong {
      inline-size: 20px;
      block-size: 20px;
    }
  `,
})
export class DoiGiaoDien {
  protected readonly giaoDien = inject(GiaoDien);
  protected readonly toi = computed(() => this.giaoDien.cheDo() === 'dark');
}
