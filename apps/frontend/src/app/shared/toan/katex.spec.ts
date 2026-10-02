import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Katex } from './katex';

@Component({ imports: [Katex], template: `<app-katex [latex]="latex()" [khoi]="khoi()" />` })
class Vo {
  readonly latex = signal('x^2');
  readonly khoi = signal(false);
}

describe('Katex', () => {
  async function mo() {
    const fixture = TestBed.createComponent(Vo);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el, dat: async (latex: string) => (fixture.componentInstance.latex.set(latex), fixture.whenStable()) };
  }

  it('vẽ công thức, giữ nguyên LaTeX trong MathML cho trình đọc màn hình', async () => {
    const t = await mo();
    expect(t.el.querySelector('.katex')).not.toBeNull();
    expect(t.el.querySelector('annotation')?.textContent).toBe('x^2');
  });

  it('LaTeX hỏng không làm vỡ trang (throwOnError: false)', async () => {
    const t = await mo();
    await t.dat('\\frac{1}{');
    expect(t.el.querySelector('.katex-error')).not.toBeNull();
  });

  it('không chèn liên kết từ \\href (trust: false)', async () => {
    const t = await mo();
    await t.dat('\\href{https://vi-du.test}{bấm}');
    expect(t.el.querySelector('a')).toBeNull();
  });

  it('khoi → công thức riêng một dòng', async () => {
    const t = await mo();
    t.fixture.componentInstance.khoi.set(true);
    await t.fixture.whenStable();
    expect(t.el.querySelector('.katex-display')).not.toBeNull();
  });
});
