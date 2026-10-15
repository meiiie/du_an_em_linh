import { Component, computed, input } from '@angular/core';
import { Katex } from './katex';

/** Interpolation cho chữ, KaTeX cho công thức; không dùng innerHTML từ dữ liệu. */
@Component({
  selector: 'app-van-ban-toan',
  imports: [Katex],
  template: `@for (p of phan(); track $index) {@if (p.math) {<app-katex [latex]="p.text" [khoi]="p.block" />} @else {{{p.text}}}}`,
})
export class VanBanToan {
  readonly text = input.required<string>();
  protected readonly phan = computed(() => {
    const text = this.text();
    const parts: { text: string; math: boolean; block?: boolean }[] = [];
    const re = /(?<!\\)(?:\$\$([\s\S]+?)\$\$|\$([^$\n]+)(?<!\\)\$|\\\(([\s\S]+?)\\\)|\\\[([\s\S]+?)\\\])/g;
    let start = 0;
    for (const m of text.matchAll(re)) {
      parts.push({ text: text.slice(start, m.index), math: false }, { text: m[1] ?? m[2] ?? m[3] ?? m[4], math: true, block: !!(m[1] ?? m[4]) });
      start = m.index + m[0].length;
    }
    parts.push({ text: text.slice(start), math: false });
    return parts;
  });
}
