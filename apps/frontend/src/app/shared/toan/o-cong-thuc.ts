import {
  afterNextRender,
  booleanAttribute,
  Component,
  computed,
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

let dangNap: Promise<boolean> | undefined;

/**
 * Nạp MathLive một lần cho cả trang, lười (gói riêng, chỉ khi có ô công thức). Phông tự host ở `/mathlive/fonts`
 * (angular.json chép từ `node_modules/mathlive/fonts`), tắt âm thanh: không lấy gì từ CDN (docs/DESIGN.md: không CDN).
 * Nạp lỗi thì trả `false` và ô dùng LaTeX dự phòng.
 */
export function napMathLive(): Promise<boolean> {
  dangNap ??= import('mathlive')
    .then(({ MathfieldElement }) => {
      MathfieldElement.fontsDirectory = '/mathlive/fonts';
      MathfieldElement.soundsDirectory = null;
      return true;
    })
    .catch(() => false);
  return dangNap;
}

/** Cách nạp MathLive; test thay bằng `() => Promise.resolve(false)` để dùng ô LaTeX. */
export const NAP_MATHLIVE = new InjectionToken<() => Promise<boolean>>('NAP_MATHLIVE', { factory: () => napMathLive });

/** Phần tử `<math-field>` (chỉ phần dùng ở đây). */
interface MathField extends HTMLElement {
  value: string;
}

/**
 * Ô nhập công thức cho Signal Forms (`[formField]`, `FormValueControl<string>`): giá trị là chuỗi LaTeX. Có MathLive
 * thì gõ công thức trực quan; MathLive chưa nạp xong, nạp lỗi, hay người dùng chọn «Gõ LaTeX» thì là ô chữ LaTeX dự
 * phòng. Ô không tự kiểm đúng / sai: chấm là việc của máy chủ (rule angular-frontend).
 */
@Component({
  selector: 'app-o-cong-thuc',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `
    @if (dungMathLive()) {
      <math-field
        #oToan
        class="o o-toan"
        [attr.aria-label]="nhan()"
        [attr.data-testid]="testId()"
        [attr.read-only]="readonly() ? '' : null"
        [attr.disabled]="disabled() ? '' : null"
        (input)="nhanTuMathLive()"
      ></math-field>
    } @else {
      <input
        class="o o-latex"
        type="text"
        [attr.aria-label]="nhan()"
        [attr.data-testid]="testId()"
        [value]="value()"
        [disabled]="disabled()"
        [readOnly]="readonly()"
        spellcheck="false"
        autocomplete="off"
        autocapitalize="off"
        (input)="nhanTuLatex($event)"
      />
    }
    @if (coMathLive()) {
      <button type="button" class="doi-cach" [attr.aria-pressed]="!dungMathLive()" (click)="doiCach()">Gõ LaTeX</button>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--space-2);
      max-inline-size: 100%;
    }

    .o {
      flex: 1 1 12rem;
      min-inline-size: 0;
      min-block-size: var(--target);
      padding: var(--space-2) var(--space-3);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      background: var(--canvas);
      color: var(--ink);
      font-size: 16px;
    }

    .o-latex {
      font-family: var(--font-mono);
    }

    .o:focus-within,
    .o:focus {
      outline: 2px solid var(--ink);
      outline-offset: 1px;
    }

    .doi-cach {
      min-block-size: var(--target);
      padding: 0 var(--space-3);
      border: 0;
      border-radius: var(--radius);
      background: none;
      color: var(--muted);
      font: inherit;
      font-size: 14px;
      cursor: pointer;
    }

    .doi-cach[aria-pressed='true'] {
      color: var(--ink);
      font-weight: 500;
    }

    .doi-cach:hover {
      background: var(--wash);
    }
  `,
})
export class OCongThuc implements FormValueControl<string> {
  readonly value = model('');
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly readonly = input(false, { transform: booleanAttribute });
  /** Nhãn cho trình đọc màn hình (ô không có `<label>` riêng thì bắt buộc). */
  readonly nhan = input.required<string>();
  readonly testId = input<string>();

  /** MathLive đã nạp xong. */
  protected readonly coMathLive = signal(false);
  private readonly muonLatex = signal(false);
  protected readonly dungMathLive = computed(() => this.coMathLive() && !this.muonLatex());

  private readonly oToan = viewChild<ElementRef<MathField>>('oToan');

  constructor() {
    const nap = inject(NAP_MATHLIVE);
    afterNextRender(() => {
      void nap().then((ok) => this.coMathLive.set(ok));
    });
    // Đẩy giá trị vào <math-field> chỉ khi khác, để không làm nhảy con trỏ khi người dùng đang gõ.
    effect(() => {
      const o = this.oToan()?.nativeElement;
      const giaTri = this.value();
      if (o && o.value !== giaTri) o.value = giaTri;
    });
  }

  protected nhanTuMathLive(): void {
    const o = this.oToan()?.nativeElement;
    if (o) this.value.set(o.value);
  }

  protected nhanTuLatex(event: Event): void {
    this.value.set((event.target as HTMLInputElement).value);
  }

  protected doiCach(): void {
    this.muonLatex.update((v) => !v);
  }
}
