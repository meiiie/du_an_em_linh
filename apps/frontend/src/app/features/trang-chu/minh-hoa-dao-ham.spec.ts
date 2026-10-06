import { TestBed } from '@angular/core/testing';
import { MinhHoaDaoHam } from './minh-hoa-dao-ham';

describe('MinhHoaDaoHam', () => {
  async function mo() {
    const fixture = TestBed.createComponent(MinhHoaDaoHam);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const thanh = el.querySelector<HTMLInputElement>('input[type=range]')!;
    return {
      fixture,
      el,
      thanh,
      soDo: () =>
        [...el.querySelectorAll('.so-do > span')].map((s) => s.textContent!.replace(/\s+/g, ' ').trim()).join(' '),
      dat: async (x: string) => {
        thanh.value = x;
        thanh.dispatchEvent(new Event('input'));
        await fixture.whenStable();
      },
    };
  }

  it('mặc định x = 2: hệ số góc tiếp tuyến −0,24, trình đọc màn hình nghe cùng số', async () => {
    const t = await mo();
    expect(t.soDo()).toBe('x = 2 y′ = −0,24');
    expect(t.thanh.getAttribute('aria-valuetext')).toBe('x = 2, y′ = −0,24');
    expect(t.el.querySelector('svg')!.getAttribute('aria-label')).toBe(
      'Đồ thị y = 2x/(x² + 1), tiếp tuyến tại x = 2, hệ số góc −0,24',
    );
  });

  it('kéo thanh tới x = 0: y′ = 2; tới x = 1: tiếp tuyến nằm ngang, y′ = 0', async () => {
    const t = await mo();
    await t.dat('0');
    expect(t.soDo()).toBe('x = 0 y′ = 2');
    await t.dat('1');
    expect(t.soDo()).toBe('x = 1 y′ = 0');
    const tt = t.el.querySelector('.tiep-tuyen')!;
    expect(tt.getAttribute('y1')).toBe(tt.getAttribute('y2'));
  });

  it('công thức vẽ bằng KaTeX, không gắn nhãn cực trị trên hình', async () => {
    const t = await mo();
    expect(t.el.querySelector('.katex annotation')?.textContent).toBe('y = \\dfrac{2x}{x^{2} + 1}');
    expect(t.el.querySelector('svg text')).toBeNull();
  });
});
