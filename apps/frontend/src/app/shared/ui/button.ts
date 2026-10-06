import { booleanAttribute, computed, Directive, input } from '@angular/core';

/** `vien-*`: nút viên của trang công khai `/`, theo trang Wiii (docs/DESIGN.md «Giải phẫu nút»). */
export type ButtonVariant = 'primary' | 'secondary' | 'vien-dam' | 'vien-kem' | 'vien-vien';
export type ButtonSize = 'md' | 'lg';

/**
 * Nút theo giải phẫu ở docs/DESIGN.md, đặt trên phần tử gốc để giữ ngữ nghĩa
 * (`<button appButton>`, `<a appButton>`). Kiểu dáng ở `.btn*` trong styles.css.
 */
@Directive({
  selector: 'button[appButton], a[appButton]',
  host: {
    '[class.btn]': '!vien()',
    '[class.vien]': 'vien()',
    '[class.vien-dam]': "variant() === 'vien-dam'",
    '[class.vien-kem]': "variant() === 'vien-kem'",
    '[class.vien-vien]': "variant() === 'vien-vien'",
    '[class.btn-primary]': "variant() === 'primary'",
    '[class.btn-secondary]': "variant() === 'secondary'",
    '[class.btn-lg]': "size() === 'lg'",
    '[class.btn-block]': 'block()',
  },
})
export class Button {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly block = input(false, { transform: booleanAttribute });
  protected readonly vien = computed(() => this.variant().startsWith('vien-'));
}
