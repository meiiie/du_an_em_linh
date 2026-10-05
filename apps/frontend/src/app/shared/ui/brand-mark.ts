import { Component, input } from '@angular/core';
import { TEN_SAN_PHAM } from '../../core/san-pham';

/**
 * Chữ hiệu «MathL+»: đậm 800, giãn −0,02 em; dấu «+» mang `--brand-plus` (`--accent` khi sáng, `--label` khi tối).
 * Trình đọc màn hình đọc tên liền một khối, không tách «+» thành phần tử riêng.
 */
@Component({
  selector: 'app-brand-mark',
  host: { '[attr.data-co]': 'size()' },
  template: `<span aria-hidden="true">{{ than }}<span class="cong">{{ dau }}</span></span><span class="sr-only">{{ ten }}</span>`,
  styles: `
    :host {
      display: inline-block;
      color: var(--ink);
      font-size: 20px;
      font-weight: 800;
      line-height: 1;
      letter-spacing: -0.02em;
      white-space: nowrap;
    }
    :host([data-co='md']) {
      font-size: 30px;
    }
    :host([data-co='lg']) {
      font-size: 44px;
    }
    .cong {
      color: var(--brand-plus);
    }
  `,
})
export class BrandMark {
  readonly size = input<'sm' | 'md' | 'lg'>('sm');
  protected readonly ten = TEN_SAN_PHAM;
  protected readonly than = TEN_SAN_PHAM.slice(0, -1);
  protected readonly dau = TEN_SAN_PHAM.slice(-1);
}
