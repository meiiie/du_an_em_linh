import { Component, input } from '@angular/core';

/**
 * Trang của khung P2 khi màn chưa có dữ liệu (#86): giữ đúng route và heading của bảng phụ lục trong
 * `specs/001-lat-cat-doc/spec.md`, và nói rõ màn sẽ hiện gì, không để trắng. Màn thật thay trang này ở issue của nó.
 */
@Component({
  selector: 'app-trang-cho',
  template: `
    <header class="dau-trang">
      <h1>{{ tieuDe() }}</h1>
    </header>
    <section class="trong">
      <p>{{ moTa() }}</p>
    </section>
  `,
})
export class TrangCho {
  /** `data.tieuDe` của route: heading `<h1>` như v0. */
  readonly tieuDe = input.required<string>();
  /** `data.moTa` của route: câu nói màn sẽ hiện gì. */
  readonly moTa = input.required<string>();
}
