import { afterRenderEffect, booleanAttribute, Component, DestroyRef, ElementRef, inject, input, signal } from '@angular/core';
import katex from 'katex';

/**
 * Công thức LaTeX vẽ bằng KaTeX (CSS nạp ở angular.json).
 * - `throwOnError: false`: LaTeX hỏng hiện nguyên chữ, màu `--muted`, thay vì làm vỡ trang. docs/DESIGN.md: lỗi là chữ
 *   mờ; đỏ `mark` chỉ dành cho bút chấm, học sinh đọc đỏ là «sai».
 * - `trust: false`: không cho `\href`, `\includegraphics`… chèn liên kết hay ảnh, vì chữ đến từ giáo viên, tài liệu và
 *   gia sư.
 * - `khoi` là công thức riêng một dòng, căn trái (styles.css). Dòng dài thì cuộn ngang trong chính nó, không đẩy trang;
 *   khi đang cuộn, khung nhận Tab và có nhãn để cuộn được bằng bàn phím (WCAG 2.1.1).
 * - `bang` vẽ công thức trên tấm bảng tối kiểu 3b1b (`--board`, chữ `--board-ink`), như nhau ở cả hai chế độ; cuộn ngang
 *   như `khoi`. Mặc định (không `bang`) vẫn là công thức trong dòng chữ.
 */
@Component({
  selector: 'app-katex',
  template: '',
  host: {
    '[class.khoi]': 'khoi()',
    '[class.bang]': 'bang()',
    '[attr.tabindex]': 'cuon() ? 0 : null',
    '[attr.role]': 'cuon() ? "group" : null',
    '[attr.aria-label]': 'cuon() ? "Công thức, cuộn ngang" : null',
  },
  styles: `
    :host {
      display: inline;
    }

    :host(.khoi),
    :host(.bang) {
      display: block;
      max-inline-size: 100%;
      overflow-x: auto;
      overflow-y: hidden;
    }

    /* Bảng luôn tối: chữ lỗi (--muted) và thanh cuộn lấy màu của bảng, không của trang sáng. */
    :host(.bang) {
      --muted: color-mix(in srgb, var(--board-ink) 65%, var(--board));
      padding: var(--space-4) var(--space-5);
      border: 1px solid var(--board-line);
      border-radius: var(--radius-card);
      background: var(--board);
      color: var(--board-ink);
      font-size: 1.25rem;
      color-scheme: dark;
    }
  `,
})
export class Katex {
  readonly latex = input.required<string>();
  readonly khoi = input(false, { transform: booleanAttribute });
  readonly bang = input(false, { transform: booleanAttribute });

  /** Công thức khối hay trên bảng rộng hơn khung, đang cuộn ngang. */
  protected readonly cuon = signal(false);

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    afterRenderEffect(() => {
      katex.render(this.latex(), this.el.nativeElement, {
        throwOnError: false,
        errorColor: 'var(--muted)',
        displayMode: this.khoi(),
        trust: false,
        strict: 'ignore',
        output: 'htmlAndMathml',
      });
      this.doCuon();
    });
    // Khung đổi bề ngang (xoay máy, đổi cỡ cửa sổ) thì đo lại. jsdom (test) không có ResizeObserver.
    if (typeof ResizeObserver === 'function') {
      const quanSat = new ResizeObserver(() => this.doCuon());
      quanSat.observe(this.el.nativeElement);
      inject(DestroyRef).onDestroy(() => quanSat.disconnect());
    }
  }

  private doCuon(): void {
    const el = this.el.nativeElement;
    this.cuon.set((this.khoi() || this.bang()) && el.scrollWidth > el.clientWidth);
  }
}
