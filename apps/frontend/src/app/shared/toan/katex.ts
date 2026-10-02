import { afterRenderEffect, booleanAttribute, Component, ElementRef, inject, input } from '@angular/core';
import katex from 'katex';

/**
 * Công thức LaTeX vẽ bằng KaTeX (CSS nạp ở angular.json). `throwOnError: false`: LaTeX hỏng hiện nguyên chữ màu đỏ thay
 * vì làm vỡ trang. `trust: false`: không cho `\href`, `\includegraphics`… chèn liên kết hay ảnh, vì chữ đến từ giáo viên,
 * tài liệu và gia sư. `khoi` là công thức riêng một dòng; dòng dài thì cuộn ngang trong chính nó, không đẩy trang.
 */
@Component({
  selector: 'app-katex',
  template: '',
  host: { '[class.khoi]': 'khoi()' },
  styles: `
    :host {
      display: inline;
    }

    :host(.khoi) {
      display: block;
      max-inline-size: 100%;
      overflow-x: auto;
      overflow-y: hidden;
    }
  `,
})
export class Katex {
  readonly latex = input.required<string>();
  readonly khoi = input(false, { transform: booleanAttribute });

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    afterRenderEffect(() => {
      katex.render(this.latex(), this.el.nativeElement, {
        throwOnError: false,
        displayMode: this.khoi(),
        trust: false,
        strict: 'ignore',
        output: 'htmlAndMathml',
      });
    });
  }
}
