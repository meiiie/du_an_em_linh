import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Katex } from './katex';

@Component({ imports: [Katex], template: `<app-katex [latex]="latex()" [khoi]="khoi()" [bang]="bang()" />` })
class Vo {
  readonly latex = signal('x^2');
  readonly khoi = signal(false);
  readonly bang = signal(false);
}

describe('Katex', () => {
  async function mo() {
    const fixture = TestBed.createComponent(Vo);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return {
      fixture,
      el,
      host: () => el.querySelector<HTMLElement>('app-katex')!,
      dat: async (latex: string) => (fixture.componentInstance.latex.set(latex), fixture.whenStable()),
    };
  }

  it('vẽ công thức, giữ nguyên LaTeX trong MathML cho trình đọc màn hình', async () => {
    const t = await mo();
    expect(t.el.querySelector('.katex')).not.toBeNull();
    expect(t.el.querySelector('annotation')?.textContent).toBe('x^2');
  });

  it('LaTeX hỏng không làm vỡ trang (throwOnError: false), chữ màu --muted chứ không đỏ', async () => {
    const t = await mo();
    await t.dat('\\frac{1}{');
    const loi = t.el.querySelector('.katex-error');
    expect(loi).not.toBeNull();
    expect(loi?.getAttribute('style')).toContain('var(--muted)');
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

  it('bang: tấm bảng tối, công thức rộng hơn bảng → bảng nhận Tab như khối; mặc định vẫn trong dòng', async () => {
    const t = await mo();
    expect(t.host().classList).not.toContain('bang');
    expect(t.el.querySelector('.katex-display')).toBeNull();

    t.fixture.componentInstance.bang.set(true);
    await t.fixture.whenStable();
    expect(t.host().classList).toContain('bang');
    Object.defineProperty(t.host(), 'scrollWidth', { configurable: true, get: () => 600 });
    Object.defineProperty(t.host(), 'clientWidth', { configurable: true, get: () => 358 });
    await t.dat('y = x^3 - 3x^2 + 3x - 1');
    expect(t.host().getAttribute('tabindex')).toBe('0');
    expect(t.host().getAttribute('aria-label')).toBe('Công thức, cuộn ngang');
  });

  it('công thức khối rộng hơn khung → khung nhận Tab, có nhãn; vừa khung thì không', async () => {
    const t = await mo();
    t.fixture.componentInstance.khoi.set(true);
    await t.fixture.whenStable();
    expect(t.host().getAttribute('tabindex')).toBeNull();

    // jsdom không đo bố cục: giả bề ngang của nội dung và của khung.
    Object.defineProperty(t.host(), 'scrollWidth', { configurable: true, get: () => 600 });
    Object.defineProperty(t.host(), 'clientWidth', { configurable: true, get: () => 358 });
    await t.dat('y = x^3 - 3x^2 + 3x - 1');
    expect(t.host().getAttribute('tabindex')).toBe('0');
    expect(t.host().getAttribute('role')).toBe('group');
    expect(t.host().getAttribute('aria-label')).toBe('Công thức, cuộn ngang');

    Object.defineProperty(t.host(), 'scrollWidth', { configurable: true, get: () => 200 });
    await t.dat('x^2');
    expect(t.host().getAttribute('tabindex')).toBeNull();
  });
});
