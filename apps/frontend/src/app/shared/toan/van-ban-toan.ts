import { Component, computed, input } from '@angular/core';
import { Katex } from './katex';
import { tachVanBanToan } from './tach-van-ban-toan';

/** Interpolation cho chữ, KaTeX cho công thức; không dùng innerHTML từ dữ liệu. */
@Component({
  selector: 'app-van-ban-toan',
  imports: [Katex],
  template: `@for (p of phan(); track $index) {@if (p.math) {<app-katex [latex]="p.text" [khoi]="p.block" />} @else {{{p.text}}}}`,
})
export class VanBanToan {
  readonly text = input.required<string>();
  protected readonly phan = computed(() => tachVanBanToan(this.text()));
}
