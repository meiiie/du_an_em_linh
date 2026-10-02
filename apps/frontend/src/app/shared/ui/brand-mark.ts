import { booleanAttribute, Component, input } from '@angular/core';

/** Dấu sản phẩm: đồ thị hàm trên nền mực (port từ apps/web/components/brand-mark.tsx); `dao` cho nền tối (ray mực). */
@Component({
  selector: 'app-brand-mark',
  host: { 'aria-hidden': 'true', '[attr.data-co]': 'size()', '[attr.data-dao]': "dao() ? '' : null" },
  template: `
    <svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path
        d="M4 20C7 20 8 8 11.5 8s3.7 14 7 14 3.5-8 6.5-12"
        stroke="currentColor"
        stroke-width="2.2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path d="M4 25h20" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" opacity="0.4" />
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius);
      background: var(--ink);
      color: var(--chalk);
      inline-size: 32px;
      block-size: 32px;
    }
    :host([data-dao]) {
      background: var(--chalk);
      color: var(--ink);
    }
    :host([data-co='md']) {
      inline-size: 40px;
      block-size: 40px;
    }
    :host([data-co='lg']) {
      inline-size: 64px;
      block-size: 64px;
    }
    svg {
      inline-size: 50%;
      block-size: 50%;
    }
  `,
})
export class BrandMark {
  readonly size = input<'sm' | 'md' | 'lg'>('sm');
  readonly dao = input(false, { transform: booleanAttribute });
}
