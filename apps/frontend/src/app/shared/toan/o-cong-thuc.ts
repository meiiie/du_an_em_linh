import {
  afterNextRender,
  booleanAttribute,
  Component,
  CUSTOM_ELEMENTS_SCHEMA,
  effect,
  ElementRef,
  inject,
  InjectionToken,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { FormValueControl } from '@angular/forms/signals';
import { Katex } from './katex';

let dangNap: Promise<boolean> | undefined;

/** Chú giải tiếng Việt cho MathLive 0.107 (bản này chưa có tiếng Việt; khóa thiếu thì MathLive dùng tiếng Anh). */
const CHU_MATHLIVE: Record<string, string> = {
  'tooltip.toggle virtual keyboard': 'Bật hoặc tắt bàn phím toán',
  'tooltip.menu': 'Menu',
  'tooltip.undo': 'Hoàn tác',
  'tooltip.redo': 'Làm lại',
  'tooltip.numeric': 'Số',
  'tooltip.symbols': 'Ký hiệu',
  'tooltip.alphabetic': 'Chữ cái',
  'tooltip.greek': 'Chữ Hy Lạp',
  'tooltip.cut to clipboard': 'Cắt',
  'tooltip.copy to clipboard': 'Chép',
  'tooltip.paste from clipboard': 'Dán',
};

/**
 * Nạp MathLive một lần cho cả trang, lười (gói riêng, chỉ khi có ô công thức). Phông tự host ở `/mathlive/fonts`
 * (angular.json chép từ `node_modules/mathlive/fonts`), tắt âm thanh: không lấy gì từ CDN (docs/DESIGN.md: không CDN).
 * Nạp lỗi thì trả `false`; ô gõ bằng bàn phím vẫn dùng được.
 */
export function napMathLive(): Promise<boolean> {
  dangNap ??= import('mathlive')
    .then(({ MathfieldElement }) => {
      MathfieldElement.fontsDirectory = '/mathlive/fonts';
      MathfieldElement.soundsDirectory = null;
      MathfieldElement.strings = { vi: CHU_MATHLIVE };
      MathfieldElement.locale = 'vi';
      return true;
    })
    .catch(() => false);
  return dangNap;
}

/** Cách nạp MathLive; test thay bằng `() => Promise.resolve(false)` để chỉ có ô gõ bằng bàn phím. */
export const NAP_MATHLIVE = new InjectionToken<() => Promise<boolean>>('NAP_MATHLIVE', { factory: () => napMathLive });

/** Phần tử `<math-field>` (chỉ phần dùng ở đây). */
interface MathField extends HTMLElement {
  value: string;
  menuItems: readonly unknown[];
  mathVirtualKeyboardPolicy: string;
}

/**
 * Ô nhập công thức cho Signal Forms (`[formField]`, `FormValueControl<string>`): giá trị là chuỗi LaTeX. Như `MathInput`
 * của v0 (`apps/web/components/math-input.tsx`):
 * - MathLive là đường chính, hiện khi nạp xong. Ô gõ bằng bàn phím ngay dưới luôn dùng được (MathLive chưa nạp, nạp lỗi,
 *   hay người dùng thích gõ). Hai ô cùng một giá trị và không ô nào thay chỗ ô kia, nên đang gõ không mất tiêu điểm.
 * - `data-testid` như v0: `<testId>` cho ô gõ, `mf-<testId>` cho `math-field`, `hieu-<testId>` cho dòng «Máy hiểu là».
 * - Nhãn thấy được là `<span>`. Không bọc ô trong `<label>`: v0 bọc và mất phím đầu trên máy chạm, phải vá.
 * - Menu của MathLive tắt, như v0: menu toàn chữ tiếng Anh, và nếu sau này có Compute Engine thì «Evaluate» lộ đáp án.
 *
 * Ô không tự kiểm đúng / sai: chấm là việc của máy chủ (rule angular-frontend). `invalid` (Signal Forms đặt) chỉ để đặt
 * `aria-invalid`.
 */
@Component({
  selector: 'app-o-cong-thuc',
  imports: [Katex],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    <span class="nhan">{{ nhan() }}</span>
    <math-field
      #oToan
      class="o o-toan"
      [hidden]="!coMathLive()"
      [attr.aria-label]="nhan()"
      [attr.aria-invalid]="invalid() ? 'true' : null"
      [attr.aria-describedby]="moTaBoi() ?? null"
      [attr.data-testid]="testId() ? 'mf-' + testId() : null"
      [attr.read-only]="readonly() ? '' : null"
      [attr.disabled]="disabled() ? '' : null"
      (input)="nhanTuMathLive()"
    ></math-field>
    <input
      class="o o-latex"
      type="text"
      inputmode="text"
      [attr.aria-label]="nhan() + ' — gõ bằng bàn phím'"
      [attr.aria-invalid]="invalid() ? 'true' : null"
      [attr.aria-describedby]="moTaBoi() ?? null"
      [attr.data-testid]="testId() ?? null"
      [placeholder]="goiY()"
      [value]="value()"
      [disabled]="disabled()"
      [readOnly]="readonly()"
      spellcheck="false"
      autocomplete="off"
      autocapitalize="off"
      (input)="nhanTuLatex($event)"
    />
    @if (xemTruoc() && value().trim()) {
      <span class="phu" [attr.data-testid]="testId() ? 'hieu-' + testId() : null">
        Máy hiểu là: <app-katex [latex]="value()" />
      </span>
    }
    @if (napLoi()) {
      <span class="phu">Chưa tải được ô gõ công thức trực quan. Gõ bằng bàn phím ở ô trên.</span>
    }
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--space-2);
      max-inline-size: 100%;
    }

    .nhan {
      font-size: 14px;
      font-weight: 500;
    }

    .o {
      min-inline-size: 0;
      min-block-size: var(--target);
      padding: var(--space-2) var(--space-3);
      border: 1px solid var(--line-strong);
      border-radius: var(--radius);
      background: var(--raise);
      color: var(--ink);
      font-size: 16px;
    }

    /* MathLive đặt display trong shadow DOM, đè [hidden] của trình duyệt. */
    .o-toan[hidden] {
      display: none !important;
    }

    .o-latex {
      font-family: var(--font-mono);
    }

    .o:focus-within,
    .o:focus {
      outline: 2px solid var(--focus);
      outline-offset: 1px;
    }

    /* Ô khóa không trông như ô đang dùng được. */
    .o:disabled,
    .o[disabled],
    .o[readonly],
    .o[read-only] {
      background: var(--wash);
      color: var(--muted);
    }

    .phu {
      color: var(--muted);
      font-size: 14px;
    }
  `,
})
export class OCongThuc implements FormValueControl<string> {
  readonly value = model('');
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly readonly = input(false, { transform: booleanAttribute });
  /** Signal Forms đặt khi giá trị không hợp lệ; ô chỉ đặt `aria-invalid`. */
  readonly invalid = input(false, { transform: booleanAttribute });
  /** Nhãn thấy được, cũng là tên của ô cho trình đọc màn hình. */
  readonly nhan = input.required<string>();
  readonly testId = input<string>();
  /** `id` của dòng phản hồi (lỗi của bước…) cho `aria-describedby`. */
  readonly moTaBoi = input<string>();
  readonly goiY = input('Gõ bằng bàn phím, ví dụ 3x^2-12x+9');
  /** Dòng «Máy hiểu là: …»; tắt cho ô lẫn chữ tiếng Việt, như v0. */
  readonly xemTruoc = input(true, { transform: booleanAttribute });

  /** MathLive đã nạp xong. */
  protected readonly coMathLive = signal(false);
  protected readonly napLoi = signal(false);

  private readonly oToan = viewChild.required<ElementRef<MathField>>('oToan');

  constructor() {
    const nap = inject(NAP_MATHLIVE);
    afterNextRender(() => {
      void nap().then((ok) => {
        if (ok) {
          const o = this.oToan().nativeElement;
          o.menuItems = [];
          // Như v0: máy chạm thì bàn phím ảo tự hiện, chuột và bàn phím thì chỉ hiện khi bấm nút.
          const cham = typeof matchMedia === 'function' && matchMedia('(pointer: coarse)').matches;
          o.mathVirtualKeyboardPolicy = cham ? 'auto' : 'manual';
        }
        this.coMathLive.set(ok);
        this.napLoi.set(!ok);
      });
    });
    // Chỉ đẩy giá trị vào <math-field> khi MathLive đã nạp: gán trước khi phần tử được định nghĩa tạo thuộc tính riêng
    // che getter của MathLive (lỗi v0 đã gặp). Chỉ gán khi khác, để không làm nhảy con trỏ khi đang gõ.
    effect(() => {
      const giaTri = this.value();
      if (!this.coMathLive()) return;
      const o = this.oToan().nativeElement;
      if (o.value !== giaTri) o.value = giaTri;
    });
  }

  protected nhanTuMathLive(): void {
    this.value.set(this.oToan().nativeElement.value);
  }

  protected nhanTuLatex(event: Event): void {
    this.value.set((event.target as HTMLInputElement).value);
  }
}
