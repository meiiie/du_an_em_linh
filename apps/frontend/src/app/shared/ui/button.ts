import { booleanAttribute, Directive, input } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary';
export type ButtonSize = 'md' | 'lg';

/**
 * Nút theo giải phẫu ở docs/DESIGN.md, đặt trên phần tử gốc để giữ ngữ nghĩa
 * (`<button appButton>`, `<a appButton>`). Kiểu dáng ở `.btn*` trong styles.css.
 */
@Directive({
  selector: 'button[appButton], a[appButton]',
  host: {
    class: 'btn',
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
}
